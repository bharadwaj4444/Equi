package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioSessionManager
import com.example.audio.EqualizerActionBridge
import com.example.audio.EqualizerNotificationAction
import com.example.audio.EqualizerNotificationManager
import com.example.audio.LowLatencyEngine
import com.example.audio.TestAudioMode
import com.example.data.db.AppDatabase
import com.example.data.model.PresetEntity
import com.example.data.repository.PresetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BandUiModel(
    val index: Short,
    val centerFreqHz: Int,
    val freqLabel: String,
    val bandCategory: String,
    val levelDb: Float,
    val minDb: Float = -15.0f,
    val maxDb: Float = 15.0f
)

data class EqualizerUiState(
    val isMasterEnabled: Boolean = true,
    val bands: List<BandUiModel> = emptyList(),
    val bassBoostPercent: Float = 0f, // 0 to 100%
    val virtualizerPercent: Float = 0f, // 0 to 100%
    val loudnessGainDb: Float = 0f, // 0 to 15 dB
    val stereoBalance: Float = 0f, // -1 (Left) to +1 (Right)
    val lowLatencyMode: Boolean = true,
    val selectedPresetName: String = "Flat",
    val activeSessionId: Int = 0,
    val activeSessionName: String = "System Audio Mix (Session 0)",
    val isAuditionPlaying: Boolean = false,
    val auditionMode: TestAudioMode = TestAudioMode.GROOVE_BEAT,
    val sineFrequency: Float = 440f,
    val visualizerAmplitudes: List<Float> = List(16) { 0.05f },
    val lowLatencyBufferFrames: Int = 256,
    val waveformPoints: List<Float> = List(32) { 0f },
    val leftOutputLevel: Float = 0f,
    val rightOutputLevel: Float = 0f,
    val outputDb: Float = -60f
)

class EqualizerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("equalizer_audition_prefs", Context.MODE_PRIVATE)

    private val audioSessionManager = AudioSessionManager.getInstance(application)
    private val lowLatencyEngine = LowLatencyEngine.getInstance()
    private val presetRepository: PresetRepository

    init {
        // Restore persisted audition settings
        val savedModeStr = prefs.getString("audition_mode", TestAudioMode.GROOVE_BEAT.name)
        val initialAuditionMode = try {
            TestAudioMode.valueOf(savedModeStr ?: TestAudioMode.GROOVE_BEAT.name)
        } catch (_: Exception) {
            TestAudioMode.GROOVE_BEAT
        }
        val initialSineFreq = prefs.getFloat("audition_sine_freq", 440f)
        val initialBalance = prefs.getFloat("stereo_balance", 0f)

        lowLatencyEngine.setAudioMode(initialAuditionMode)
        lowLatencyEngine.setSineFrequency(initialSineFreq)
        lowLatencyEngine.setStereoBalance(initialBalance)

        val database = AppDatabase.getInstance(application)
        presetRepository = PresetRepository(database.presetDao())
        viewModelScope.launch {
            presetRepository.initializeDefaultPresetsIfEmpty()
        }

        // Initialize Android notification channel for playback window
        EqualizerNotificationManager.createNotificationChannel(application)
    }

    val allPresets: StateFlow<List<PresetEntity>> = presetRepository.allPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(
        EqualizerUiState(
            auditionMode = try {
                TestAudioMode.valueOf(
                    application.getSharedPreferences("equalizer_audition_prefs", Context.MODE_PRIVATE)
                        .getString("audition_mode", TestAudioMode.GROOVE_BEAT.name) ?: TestAudioMode.GROOVE_BEAT.name
                )
            } catch (_: Exception) {
                TestAudioMode.GROOVE_BEAT
            },
            sineFrequency = application.getSharedPreferences("equalizer_audition_prefs", Context.MODE_PRIVATE)
                .getFloat("audition_sine_freq", 440f),
            stereoBalance = application.getSharedPreferences("equalizer_audition_prefs", Context.MODE_PRIVATE)
                .getFloat("stereo_balance", 0f)
        )
    )
    val uiState: StateFlow<EqualizerUiState> = _uiState.asStateFlow()

    init {
        // Collect bands from AudioSessionManager
        viewModelScope.launch {
            audioSessionManager.bandsFlow.collect { bandInfos ->
                if (bandInfos.isNotEmpty()) {
                    val bandModels = bandInfos.map { info ->
                        val freqLabel = formatFreq(info.centerFreqHz)
                        val category = getBandCategory(info.centerFreqHz)
                        val levelDb = info.currentMillibels / 100f
                        val minDb = info.minMillibels / 100f
                        val maxDb = info.maxMillibels / 100f
                        BandUiModel(
                            index = info.index,
                            centerFreqHz = info.centerFreqHz,
                            freqLabel = freqLabel,
                            bandCategory = category,
                            levelDb = levelDb,
                            minDb = minDb,
                            maxDb = maxDb
                        )
                    }
                    _uiState.value = _uiState.value.copy(bands = bandModels)
                }
            }
        }

        // Collect active session
        viewModelScope.launch {
            audioSessionManager.activeSessionFlow.collect { (sessionId, sessionName) ->
                _uiState.value = _uiState.value.copy(
                    activeSessionId = sessionId,
                    activeSessionName = sessionName
                )
            }
        }

        // Collect LowLatency Engine visualizer data
        viewModelScope.launch {
            lowLatencyEngine.spectrumAmplitudes.collect { amps ->
                _uiState.value = _uiState.value.copy(visualizerAmplitudes = amps)
            }
        }

        // Collect LowLatency Engine playback state and update notification window
        viewModelScope.launch {
            lowLatencyEngine.isPlaying.collect { isPlaying ->
                _uiState.value = _uiState.value.copy(isAuditionPlaying = isPlaying)
                if (isPlaying) {
                    updateNotification()
                } else {
                    EqualizerNotificationManager.dismissNotification(application)
                }
            }
        }

        // Listen for actions dispatched from the Android Notification Window
        viewModelScope.launch {
            EqualizerActionBridge.actions.collect { action ->
                when (action) {
                    EqualizerNotificationAction.TOGGLE_EQ -> {
                        setMasterEnabled(!_uiState.value.isMasterEnabled)
                    }
                    EqualizerNotificationAction.NEXT_PRESET -> {
                        cycleNextPreset()
                    }
                    EqualizerNotificationAction.CYCLE_BASS -> {
                        cycleBassBoost()
                    }
                    EqualizerNotificationAction.CYCLE_VIRTUALIZER -> {
                        cycleVirtualizer()
                    }
                    EqualizerNotificationAction.TOGGLE_PLAY -> {
                        toggleAuditionPlayback()
                    }
                    EqualizerNotificationAction.STOP -> {
                        if (_uiState.value.isAuditionPlaying) {
                            toggleAuditionPlayback()
                        }
                        EqualizerNotificationManager.dismissNotification(application)
                    }
                }
            }
        }

        // Collect buffer frame metrics
        viewModelScope.launch {
            lowLatencyEngine.lowLatencyBufferFrames.collect { frames ->
                _uiState.value = _uiState.value.copy(lowLatencyBufferFrames = frames)
            }
        }

        // Collect real-time oscilloscope waveform
        viewModelScope.launch {
            lowLatencyEngine.waveformPoints.collect { points ->
                _uiState.value = _uiState.value.copy(waveformPoints = points)
            }
        }

        // Collect real-time stereo output levels & dB
        viewModelScope.launch {
            lowLatencyEngine.leftOutputLevel.collect { lLevel ->
                _uiState.value = _uiState.value.copy(leftOutputLevel = lLevel)
            }
        }
        viewModelScope.launch {
            lowLatencyEngine.rightOutputLevel.collect { rLevel ->
                _uiState.value = _uiState.value.copy(rightOutputLevel = rLevel)
            }
        }
        viewModelScope.launch {
            lowLatencyEngine.outputDb.collect { db ->
                _uiState.value = _uiState.value.copy(outputDb = db)
            }
        }
    }

    private fun formatFreq(hz: Int): String {
        return if (hz >= 1000) {
            val khz = hz / 1000f
            if (khz % 1f == 0f) "${khz.toInt()} kHz" else String.format("%.1f kHz", khz)
        } else {
            "$hz Hz"
        }
    }

    private fun getBandCategory(hz: Int): String {
        return when {
            hz <= 40 -> "Sub-Bass"
            hz <= 90 -> "Low-Bass"
            hz <= 180 -> "Bass / Warmth"
            hz <= 350 -> "Low Mids"
            hz <= 700 -> "Midrange"
            hz <= 1400 -> "Upper Mids"
            hz <= 2800 -> "Definition"
            hz <= 6000 -> "Presence"
            hz <= 11000 -> "High Treble"
            else -> "Air / Sparkle"
        }
    }

    fun setMasterEnabled(enabled: Boolean) {
        audioSessionManager.setMasterEnabled(enabled)
        _uiState.value = _uiState.value.copy(isMasterEnabled = enabled)
        updateNotification()
    }

    fun setBandLevelDb(index: Short, db: Float) {
        val millibels = (db * 100).toInt().coerceIn(-1500, 1500).toShort()
        audioSessionManager.setBandLevel(index, millibels)

        val updatedBands = _uiState.value.bands.map { band ->
            if (band.index == index) band.copy(levelDb = db) else band
        }
        _uiState.value = _uiState.value.copy(
            bands = updatedBands,
            selectedPresetName = "Custom"
        )
    }

    fun stepBandLevel(index: Short, deltaDb: Float) {
        val current = _uiState.value.bands.find { it.index == index } ?: return
        val newDb = (current.levelDb + deltaDb).coerceIn(current.minDb, current.maxDb)
        setBandLevelDb(index, newDb)
    }

    fun resetToFlat() {
        _uiState.value.bands.forEach { band ->
            setBandLevelDb(band.index, 0f)
        }
        _uiState.value = _uiState.value.copy(
            selectedPresetName = "Flat",
            bassBoostPercent = 0f,
            virtualizerPercent = 0f,
            loudnessGainDb = 0f
        )
        audioSessionManager.setBassBoostStrength(0)
        audioSessionManager.setVirtualizerStrength(0)
        audioSessionManager.setLoudnessGain(0)
    }

    fun setBassBoost(percent: Float) {
        val p = percent.coerceIn(0f, 100f)
        _uiState.value = _uiState.value.copy(bassBoostPercent = p)
        val strength = ((p / 100f) * 1000).toInt().toShort()
        audioSessionManager.setBassBoostStrength(strength)
    }

    fun setVirtualizer(percent: Float) {
        val p = percent.coerceIn(0f, 100f)
        _uiState.value = _uiState.value.copy(virtualizerPercent = p)
        val strength = ((p / 100f) * 1000).toInt().toShort()
        audioSessionManager.setVirtualizerStrength(strength)
    }

    fun setLoudness(gainDb: Float) {
        val g = gainDb.coerceIn(0f, 15f)
        _uiState.value = _uiState.value.copy(loudnessGainDb = g)
        val millibels = (g * 100).toInt()
        audioSessionManager.setLoudnessGain(millibels)
    }

    fun setStereoBalance(balance: Float) {
        val b = balance.coerceIn(-1f, 1f)
        _uiState.value = _uiState.value.copy(stereoBalance = b)
        lowLatencyEngine.setStereoBalance(b)
        prefs.edit().putFloat("stereo_balance", b).apply()
    }

    fun setLowLatencyMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(lowLatencyMode = enabled)
    }

    fun applyPreset(preset: PresetEntity) {
        val levels = preset.bandLevels.split(",").mapNotNull { it.trim().toShortOrNull() }
        if (levels.isNotEmpty()) {
            val targetLevels: List<Short> = if (levels.size == 10) {
                levels
            } else if (levels.size == 5) {
                // Smoothly map legacy 5-band presets into the 10 refined frequency bands
                listOf(
                    levels[0],                                       // 31 Hz (Sub-Bass)
                    levels[0],                                       // 63 Hz (Low Bass)
                    ((levels[0] + levels[1]) / 2).toShort(),         // 125 Hz
                    levels[1],                                       // 250 Hz
                    ((levels[1] + levels[2]) / 2).toShort(),         // 500 Hz
                    levels[2],                                       // 1000 Hz
                    ((levels[2] + levels[3]) / 2).toShort(),         // 2000 Hz
                    levels[3],                                       // 4000 Hz
                    ((levels[3] + levels[4]) / 2).toShort(),         // 8000 Hz
                    levels[4]                                        // 16000 Hz
                )
            } else {
                // If custom length, pad or interpolate
                List(10) { i ->
                    val srcIdx = (i.toFloat() / 9f * (levels.size - 1)).toInt().coerceIn(levels.indices)
                    levels[srcIdx]
                }
            }
            audioSessionManager.setAllBands(targetLevels)
        }
        val bbPercent = (preset.bassBoost / 10f).coerceIn(0f, 100f)
        val virtPercent = (preset.virtualizer / 10f).coerceIn(0f, 100f)
        val loudDb = (preset.loudnessGain / 100f).coerceIn(0f, 15f)

        setBassBoost(bbPercent)
        setVirtualizer(virtPercent)
        setLoudness(loudDb)

        _uiState.value = _uiState.value.copy(
            selectedPresetName = preset.name
        )
        updateNotification()
    }

    fun saveCustomPreset(name: String) {
        viewModelScope.launch {
            val bandMillibels = _uiState.value.bands.map { (it.levelDb * 100).toInt().toShort() }
            val bb = (_uiState.value.bassBoostPercent * 10).toInt()
            val virt = (_uiState.value.virtualizerPercent * 10).toInt()
            val loud = (_uiState.value.loudnessGainDb * 100).toInt()

            presetRepository.saveCustomPreset(
                name = name,
                bandLevels = bandMillibels,
                bassBoost = bb,
                virtualizer = virt,
                loudnessGain = loud
            )
            _uiState.value = _uiState.value.copy(selectedPresetName = name)
        }
    }

    fun deletePreset(preset: PresetEntity) {
        viewModelScope.launch {
            if (!preset.isDefault) {
                presetRepository.deletePreset(preset.id)
                if (_uiState.value.selectedPresetName == preset.name) {
                    resetToFlat()
                }
            }
        }
    }

    fun toggleAuditionPlayback() {
        lowLatencyEngine.togglePlayback()
        // Session 0 (Global Mix) already processes both system media and audition audio.
        // Keeping Session 0 active guarantees the actual music playing on the device is never glitched or interrupted.
    }

    fun setAuditionMode(mode: TestAudioMode) {
        lowLatencyEngine.setAudioMode(mode)
        _uiState.value = _uiState.value.copy(auditionMode = mode)
        prefs.edit().putString("audition_mode", mode.name).apply()
    }

    fun skipNextAuditionMode() {
        val nextMode = lowLatencyEngine.skipNextMode()
        _uiState.value = _uiState.value.copy(auditionMode = nextMode)
        prefs.edit().putString("audition_mode", nextMode.name).apply()
    }

    fun skipPreviousAuditionMode() {
        val prevMode = lowLatencyEngine.skipPreviousMode()
        _uiState.value = _uiState.value.copy(auditionMode = prevMode)
        prefs.edit().putString("audition_mode", prevMode.name).apply()
    }

    fun setSineFrequency(freq: Float) {
        lowLatencyEngine.setSineFrequency(freq)
        _uiState.value = _uiState.value.copy(sineFrequency = freq)
        prefs.edit().putFloat("audition_sine_freq", freq).apply()
    }

    fun switchToSystemSession() {
        audioSessionManager.attachSession(0, "System Audio Mix (Session 0)")
        _uiState.value = _uiState.value.copy(activeSessionName = "System Audio Mix (Session 0)")
    }

    fun switchToAuditionSession() {
        // Retain Session 0 so both audition test audio and background media enjoy uninterrupted studio equalization
        _uiState.value = _uiState.value.copy(activeSessionName = "Audition Auditing (Global Mix)")
    }

    fun cycleNextPreset() {
        val presetList = allPresets.value
        if (presetList.isNotEmpty()) {
            val currentIndex = presetList.indexOfFirst { it.name == _uiState.value.selectedPresetName }
            val nextIndex = if (currentIndex in presetList.indices) (currentIndex + 1) % presetList.size else 0
            applyPreset(presetList[nextIndex])
        }
    }

    fun cycleBassBoost() {
        val current = _uiState.value.bassBoostPercent
        val next = when {
            current < 15f -> 30f
            current < 45f -> 60f
            current < 80f -> 100f
            else -> 0f
        }
        setBassBoost(next)
        updateNotification()
    }

    fun cycleVirtualizer() {
        val current = _uiState.value.virtualizerPercent
        val next = when {
            current < 20f -> 50f
            current < 75f -> 100f
            else -> 0f
        }
        setVirtualizer(next)
        updateNotification()
    }

    fun updateNotification() {
        val state = _uiState.value
        if (state.isAuditionPlaying) {
            EqualizerNotificationManager.showOrUpdateNotification(
                context = getApplication(),
                isPlaying = state.isAuditionPlaying,
                presetName = state.selectedPresetName,
                isEqEnabled = state.isMasterEnabled,
                bassPercent = state.bassBoostPercent,
                amplitudes = state.visualizerAmplitudes,
                modeName = state.auditionMode.name
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        EqualizerNotificationManager.dismissNotification(getApplication())
        lowLatencyEngine.release()
        audioSessionManager.releaseEffects()
    }
}
