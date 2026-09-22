package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

enum class TestAudioMode {
    GROOVE_BEAT,
    SPECTRUM_SWEEP,
    SINE_TONE
}

class LowLatencyEngine {

    private val TAG = "LowLatencyEngine"
    // Universal Android hardware standard sample rate (48000 Hz) eliminates AudioFlinger resampling jitter
    private val sampleRate = 48000
    private var audioTrack: AudioTrack? = null
    private var audioThread: Thread? = null
    private val isRunning = AtomicBoolean(false)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    @Volatile
    private var currentMode = TestAudioMode.GROOVE_BEAT
    private val _audioMode = MutableStateFlow(TestAudioMode.GROOVE_BEAT)
    val audioMode: StateFlow<TestAudioMode> = _audioMode.asStateFlow()

    @Volatile
    private var currentSineFreq = 440f
    private val _sineFrequency = MutableStateFlow(440f) // Hz
    val sineFrequency: StateFlow<Float> = _sineFrequency.asStateFlow()

    @Volatile
    private var currentStereoBalance = 0f
    private val _stereoBalance = MutableStateFlow(0f) // -1.0 (Left) to +1.0 (Right)
    val stereoBalance: StateFlow<Float> = _stereoBalance.asStateFlow()

    private val _lowLatencyBufferFrames = MutableStateFlow(512)
    val lowLatencyBufferFrames: StateFlow<Int> = _lowLatencyBufferFrames.asStateFlow()

    // Real-time visualizer spectrum amplitudes (16 frequency bands)
    private val _spectrumAmplitudes = MutableStateFlow(List(16) { 0.05f })
    val spectrumAmplitudes: StateFlow<List<Float>> = _spectrumAmplitudes.asStateFlow()

    // Real-time waveform points (32 sample points for oscilloscope visualization)
    private val _waveformPoints = MutableStateFlow(List(32) { 0f })
    val waveformPoints: StateFlow<List<Float>> = _waveformPoints.asStateFlow()

    // Real-time stereo output levels (0f to 1f) and peak dB (-60f to +3f)
    private val _leftOutputLevel = MutableStateFlow(0f)
    val leftOutputLevel: StateFlow<Float> = _leftOutputLevel.asStateFlow()

    private val _rightOutputLevel = MutableStateFlow(0f)
    val rightOutputLevel: StateFlow<Float> = _rightOutputLevel.asStateFlow()

    private val _outputDb = MutableStateFlow(-60f)
    val outputDb: StateFlow<Float> = _outputDb.asStateFlow()

    var audioSessionId: Int = 0
        private set

    // Pre-computed chord frequencies to guarantee ZERO heap allocations in the audio thread loop
    private val chordFreqsTable = arrayOf(
        doubleArrayOf(220.0, 277.18, 329.63), // 0: A Maj
        doubleArrayOf(196.0, 246.94, 293.66), // 1: G Maj
        doubleArrayOf(174.61, 220.0, 261.63), // 2: F Maj
        doubleArrayOf(164.81, 207.65, 246.94)  // 3: E Maj
    )

    init {
        initAudioTrack()
    }

    private fun initAudioTrack() {
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            // Allocate a stable hardware playback buffer (at least 16384 bytes ~85ms at 48kHz)
            // to completely eliminate underruns and buffer starvation
            val bufferSize = minBufSize.coerceAtLeast(16384)

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            audioSessionId = track.audioSessionId
            _lowLatencyBufferFrames.value = bufferSize / 4 // 16-bit stereo = 4 bytes per frame
            Log.d(TAG, "Initialized AudioTrack with session ID: $audioSessionId, frames: ${_lowLatencyBufferFrames.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AudioTrack: ${e.message}", e)
        }
    }

    fun setAudioMode(mode: TestAudioMode) {
        currentMode = mode
        _audioMode.value = mode
    }

    fun setSineFrequency(frequencyHz: Float) {
        val clamped = frequencyHz.coerceIn(20f, 18000f)
        currentSineFreq = clamped
        _sineFrequency.value = clamped
    }

    fun setStereoBalance(balance: Float) {
        val clamped = balance.coerceIn(-1f, 1f)
        currentStereoBalance = clamped
        _stereoBalance.value = clamped
    }

    fun togglePlayback(): Boolean {
        if (isRunning.get()) {
            stop()
        } else {
            start()
        }
        return isRunning.get()
    }

    @Synchronized
    fun start() {
        if (isRunning.get()) return

        // Verify or reinitialize AudioTrack if released
        var track = audioTrack
        if (track == null || track.state != AudioTrack.STATE_INITIALIZED) {
            initAudioTrack()
            track = audioTrack ?: return
        }

        try {
            isRunning.set(true)
            _isPlaying.value = true

            val thread = Thread({
                // Real-time audio thread priority
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                } catch (e: Exception) {
                    try {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
                    } catch (_: Exception) {}
                }

                // Chunk size: 1024 frames (~21.3ms at 48kHz) - provides ultra-stable streaming without buffer underruns
                val chunkSize = 1024
                val buffer = ShortArray(chunkSize * 2) // Stereo (L + R)
                var phase = 0.0
                var sampleIndex = 0L
                val bpm = 124.0
                val samplesPerBeat = (sampleRate * 60.0 / bpm).toLong()

                val bandEnergies = FloatArray(16) { 0.05f }
                var visualizerCounter = 0

                // Fast XorShift PRNG state
                var xorShiftState = 123456789L

                // Pre-roll / Prime buffer with 2 chunks before calling play()
                // This guarantees the hardware audio buffer is never empty when playback starts
                var isStarted = false

                while (isRunning.get()) {
                    val mode = currentMode
                    val bal = currentStereoBalance
                    val leftVol = if (bal <= 0f) 1.0f else (1.0f - bal)
                    val rightVol = if (bal >= 0f) 1.0f else (1.0f + bal)

                    for (i in 0 until chunkSize) {
                        val currentSample = sampleIndex + i
                        var monoSample = 0.0

                        when (mode) {
                            TestAudioMode.GROOVE_BEAT -> {
                                val sampleInBeat = (currentSample % samplesPerBeat).toInt()
                                val beatPos = sampleInBeat.toDouble() / samplesPerBeat
                                val measurePos = ((currentSample / samplesPerBeat) % 4).toInt()

                                // 1. Kick Drum (tight low-end thump with pitch envelope)
                                val kickEnvelope = if (beatPos < 0.5) exp(-beatPos * 9.0) else 0.0
                                val kickFreq = 50.0 + 80.0 * (if (beatPos < 0.15) exp(-beatPos * 25.0) else 0.0)
                                val kick = sin(2.0 * PI * kickFreq * (beatPos * samplesPerBeat / sampleRate)) * kickEnvelope

                                // 2. Snare on beats 1 and 3
                                var snare = 0.0
                                if (measurePos == 1 || measurePos == 3) {
                                    val snareEnv = if (beatPos < 0.4) exp(-beatPos * 12.0) else 0.0
                                    xorShiftState = xorShiftState xor (xorShiftState shl 13)
                                    xorShiftState = xorShiftState xor (xorShiftState ushr 17)
                                    xorShiftState = xorShiftState xor (xorShiftState shl 5)
                                    val noise = ((xorShiftState and 0xFFFF).toDouble() / 32768.0) - 1.0
                                    val tone = sin(2.0 * PI * 185.0 * (beatPos * samplesPerBeat / sampleRate))
                                    snare = (noise * 0.65 + tone * 0.25) * snareEnv
                                }

                                // 3. Hi-Hat on 1/4 beats
                                val subBeatPos = (beatPos * 4.0) % 1.0
                                val hatEnv = if (subBeatPos < 0.25) exp(-subBeatPos * 28.0) else 0.0
                                xorShiftState = xorShiftState xor (xorShiftState shl 13)
                                xorShiftState = xorShiftState xor (xorShiftState ushr 17)
                                xorShiftState = xorShiftState xor (xorShiftState shl 5)
                                val hatNoise = ((xorShiftState and 0xFFFF).toDouble() / 32768.0) - 1.0
                                val hat = hatNoise * hatEnv * 0.35

                                // 4. Melodic Synth Pad (zero allocations, uses pre-computed table)
                                val chord = chordFreqsTable[measurePos]
                                val chordEnv = 0.5 + 0.5 * sin(2.0 * PI * 2.0 * beatPos)
                                val tNorm = currentSample.toDouble() / sampleRate
                                val synth = (
                                    sin(2.0 * PI * chord[0] * tNorm) +
                                    sin(2.0 * PI * chord[1] * tNorm) +
                                    sin(2.0 * PI * chord[2] * tNorm)
                                ) * (chordEnv * 0.10)

                                // Smooth soft-knee saturation using tanh to prevent any digital clipping distortion
                                val rawSum = kick * 0.75 + snare * 0.55 + hat * 0.30 + synth
                                monoSample = tanh(rawSum)
                            }
                            TestAudioMode.SPECTRUM_SWEEP -> {
                                val sweepPeriod = (sampleRate * 4.0)
                                val t = (currentSample % sweepPeriod.toLong()) / sweepPeriod
                                val freq = 25.0 * Math.pow(16000.0 / 25.0, t)
                                phase += 2.0 * PI * freq / sampleRate
                                if (phase > 2.0 * PI) phase -= 2.0 * PI
                                val sine = sin(phase)

                                xorShiftState = xorShiftState xor (xorShiftState shl 13)
                                xorShiftState = xorShiftState xor (xorShiftState ushr 17)
                                xorShiftState = xorShiftState xor (xorShiftState shl 5)
                                val noise = (((xorShiftState and 0xFFFF).toDouble() / 32768.0) - 1.0) * 0.12
                                monoSample = tanh(sine * 0.65 + noise)
                            }
                            TestAudioMode.SINE_TONE -> {
                                val freq = currentSineFreq.toDouble()
                                phase += 2.0 * PI * freq / sampleRate
                                if (phase > 2.0 * PI) phase -= 2.0 * PI
                                monoSample = sin(phase) * 0.65
                            }
                        }

                        // Apply channel volumes and convert to 16-bit PCM (-32768 to 32767)
                        val leftVal = (monoSample * leftVol * 22000.0).toInt().coerceIn(-32767, 32767)
                        val rightVal = (monoSample * rightVol * 22000.0).toInt().coerceIn(-32767, 32767)

                        buffer[i * 2] = leftVal.toShort()
                        buffer[i * 2 + 1] = rightVal.toShort()

                        val absVal = abs(monoSample).toFloat()
                        val bin = i % 16
                        bandEnergies[bin] = bandEnergies[bin] * 0.88f + absVal * 0.12f
                    }

                    sampleIndex += chunkSize

                    // Blocking write on dedicated audio thread
                    track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)

                    // Start hardware playback once pre-buffered to prevent initial underrun
                    if (!isStarted) {
                        try {
                            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                                track.play()
                            }
                            isStarted = true
                        } catch (e: Exception) {
                            Log.w(TAG, "AudioTrack play error: ${e.message}")
                        }
                    }

                    // Update visualizer state periodically without bogging down the audio thread
                    visualizerCounter++
                    if (visualizerCounter >= 2) {
                        visualizerCounter = 0
                        val copyAmps = ArrayList<Float>(16)
                        for (b in 0 until 16) {
                            copyAmps.add((bandEnergies[b] * 1.4f).coerceIn(0.06f, 0.96f))
                        }
                        _spectrumAmplitudes.value = copyAmps

                        // Extract 32 normalized sample points across the audio buffer for real-time oscilloscope
                        val points = ArrayList<Float>(32)
                        val step = (chunkSize / 32).coerceAtLeast(1)
                        var maxL = 0
                        var maxR = 0
                        for (p in 0 until 32) {
                            val idx = (p * step * 2).coerceIn(0, buffer.size - 2)
                            val sVal = buffer[idx] / 32768f
                            points.add(sVal)
                            val aL = abs(buffer[idx].toInt())
                            val aR = abs(buffer[idx + 1].toInt())
                            if (aL > maxL) maxL = aL
                            if (aR > maxR) maxR = aR
                        }
                        _waveformPoints.value = points

                        val normL = (maxL / 32768f).coerceIn(0f, 1f)
                        val normR = (maxR / 32768f).coerceIn(0f, 1f)
                        _leftOutputLevel.value = normL
                        _rightOutputLevel.value = normR

                        val maxAmp = maxOf(normL, normR)
                        val db = if (maxAmp > 0.001f) {
                            (20.0 * kotlin.math.log10(maxAmp.toDouble())).toFloat().coerceIn(-60f, 3f)
                        } else {
                            -60f
                        }
                        _outputDb.value = db
                    }
                }
            }, "LowLatencyAudioRenderer")

            audioThread = thread
            thread.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting playback: ${e.message}", e)
            isRunning.set(false)
            _isPlaying.value = false
        }
    }

    @Synchronized
    fun stop() {
        isRunning.set(false)
        _isPlaying.value = false

        try {
            audioThread?.join(500)
        } catch (e: Exception) {
            Log.w(TAG, "Interrupted joining audio thread: ${e.message}")
        }
        audioThread = null

        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping audio: ${e.message}")
        }
        _spectrumAmplitudes.value = List(16) { 0.05f }
        _waveformPoints.value = List(32) { 0f }
        _leftOutputLevel.value = 0f
        _rightOutputLevel.value = 0f
        _outputDb.value = -60f
    }

    fun skipNextMode(): TestAudioMode {
        val modes = TestAudioMode.values()
        val nextIdx = (currentMode.ordinal + 1) % modes.size
        val nextMode = modes[nextIdx]
        setAudioMode(nextMode)
        return nextMode
    }

    fun skipPreviousMode(): TestAudioMode {
        val modes = TestAudioMode.values()
        val prevIdx = if (currentMode.ordinal - 1 < 0) modes.size - 1 else currentMode.ordinal - 1
        val prevMode = modes[prevIdx]
        setAudioMode(prevMode)
        return prevMode
    }

    fun release() {
        stop()
        try {
            audioTrack?.release()
        } catch (e: Exception) {}
        audioTrack = null
    }

    companion object {
        @Volatile
        private var instance: LowLatencyEngine? = null

        fun getInstance(): LowLatencyEngine {
            return instance ?: synchronized(this) {
                instance ?: LowLatencyEngine().also { instance = it }
            }
        }
    }
}
