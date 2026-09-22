package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BandInfo(
    val index: Short,
    val centerFreqHz: Int,
    val minMillibels: Short,
    val maxMillibels: Short,
    val currentMillibels: Short
)

data class HardwarePresetInfo(
    val index: Short,
    val name: String
)

class AudioSessionManager(private val context: Context) {

    private val TAG = "AudioSessionManager"

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var currentSessionId: Int = 0 // 0 = Global System Mix
    private var currentPackageName: String = "System Audio Mix (Session 0)"

    private val _activeSessionFlow = MutableStateFlow(Pair(0, "System Audio Mix (Session 0)"))
    val activeSessionFlow: StateFlow<Pair<Int, String>> = _activeSessionFlow.asStateFlow()

    private val _bandsFlow = MutableStateFlow<List<BandInfo>>(emptyList())
    val bandsFlow: StateFlow<List<BandInfo>> = _bandsFlow.asStateFlow()

    private val _hardwarePresetsFlow = MutableStateFlow<List<HardwarePresetInfo>>(emptyList())
    val hardwarePresetsFlow: StateFlow<List<HardwarePresetInfo>> = _hardwarePresetsFlow.asStateFlow()

    private val _isMasterEnabled = MutableStateFlow(true)
    val isMasterEnabled: StateFlow<Boolean> = _isMasterEnabled.asStateFlow()

    private var currentBassBoostStrength: Short = 0
    private var currentVirtualizerStrength: Short = 0
    private var currentLoudnessGain: Int = 0

    // Standard 10-band precision center frequencies (ISO standard octave bands)
    private val standard10BandFrequencies = listOf(31, 63, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
    private val fine10BandLevels = ShortArray(10) { 0 }

    init {
        attachSession(0, "System Audio Mix (Session 0)")
    }

    @Synchronized
    fun attachSession(sessionId: Int, packageName: String? = null) {
        if (equalizer != null && currentSessionId == sessionId) {
            // Already attached to this session, no need to recreate effects
            return
        }
        try {
            releaseEffects()

            currentSessionId = sessionId
            currentPackageName = packageName ?: if (sessionId == 0) "System Audio Mix (Session 0)" else "Audio Session #$sessionId"
            _activeSessionFlow.value = Pair(currentSessionId, currentPackageName)

            // Equalizer priority = 0 (or 1000 for high priority)
            val eq = Equalizer(1000, sessionId)
            eq.enabled = _isMasterEnabled.value
            equalizer = eq

            val levelRange = eq.bandLevelRange // [min, max] in millibels e.g. [-1500, 1500]
            val minMb = levelRange.getOrNull(0) ?: -1500
            val maxMb = levelRange.getOrNull(1) ?: 1500

            // Expose a refined 10-band studio equalizer interface
            val bandList = standard10BandFrequencies.mapIndexed { i, freq ->
                BandInfo(
                    index = i.toShort(),
                    centerFreqHz = freq,
                    minMillibels = minMb,
                    maxMillibels = maxMb,
                    currentMillibels = fine10BandLevels[i]
                )
            }
            _bandsFlow.value = bandList

            // Apply fine levels to underlying hardware equalizer
            syncFineBandsToHardware(eq)

            // Query hardware presets
            val hwPresets = mutableListOf<HardwarePresetInfo>()
            val numPresets = eq.numberOfPresets
            for (p in 0 until numPresets) {
                val pIndex = p.toShort()
                val pName = try { eq.getPresetName(pIndex) } catch (e: Exception) { "Preset #$p" }
                hwPresets.add(HardwarePresetInfo(pIndex, pName))
            }
            _hardwarePresetsFlow.value = hwPresets

            // Bass Boost
            try {
                val bb = BassBoost(1000, sessionId)
                bb.enabled = _isMasterEnabled.value
                if (currentBassBoostStrength > 0 && bb.strengthSupported) {
                    bb.setStrength(currentBassBoostStrength)
                }
                bassBoost = bb
            } catch (e: Exception) {
                Log.w(TAG, "BassBoost not supported on session $sessionId: ${e.message}")
            }

            // Virtualizer
            try {
                val virt = Virtualizer(1000, sessionId)
                virt.enabled = _isMasterEnabled.value
                if (currentVirtualizerStrength > 0 && virt.strengthSupported) {
                    virt.setStrength(currentVirtualizerStrength)
                }
                virtualizer = virt
            } catch (e: Exception) {
                Log.w(TAG, "Virtualizer not supported on session $sessionId: ${e.message}")
            }

            // Loudness Enhancer
            try {
                val le = LoudnessEnhancer(sessionId)
                le.enabled = _isMasterEnabled.value
                if (currentLoudnessGain > 0) {
                    le.setTargetGain(currentLoudnessGain)
                }
                loudnessEnhancer = le
            } catch (e: Exception) {
                Log.w(TAG, "LoudnessEnhancer not supported on session $sessionId: ${e.message}")
            }

            // Broadcast open session so system and apps recognize our audio effect control panel
            broadcastOpenSession(sessionId)

        } catch (e: Exception) {
            Log.e(TAG, "Error attaching session $sessionId: ${e.message}", e)
            // Hardware equalizer fallback or expansion (e.g. 10-band ISO standard equalizer)
            if (_bandsFlow.value.isEmpty()) {
                _bandsFlow.value = listOf(
                    BandInfo(0, 31, -1500, 1500, 0),     // Sub-Bass
                    BandInfo(1, 63, -1500, 1500, 0),     // Low Bass
                    BandInfo(2, 125, -1500, 1500, 0),    // Bass / Warmth
                    BandInfo(3, 250, -1500, 1500, 0),    // Low Mids
                    BandInfo(4, 500, -1500, 1500, 0),    // Body / Mids
                    BandInfo(5, 1000, -1500, 1500, 0),   // Vocal Presence
                    BandInfo(6, 2000, -1500, 1500, 0),   // Definition / Clarity
                    BandInfo(7, 4000, -1500, 1500, 0),   // Presence
                    BandInfo(8, 8000, -1500, 1500, 0),   // High Treble
                    BandInfo(9, 16000, -1500, 1500, 0)   // Air / Brilliance
                )
            }
        }
    }

    private fun broadcastOpenSession(sessionId: Int) {
        try {
            val intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to broadcast open session: ${e.message}")
        }
    }

    private fun broadcastCloseSession(sessionId: Int) {
        try {
            val intent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to broadcast close session: ${e.message}")
        }
    }

    @Synchronized
    fun setMasterEnabled(enabled: Boolean) {
        _isMasterEnabled.value = enabled
        try { equalizer?.enabled = enabled } catch (e: Exception) { Log.w(TAG, e.message ?: "") }
        try { bassBoost?.enabled = enabled } catch (e: Exception) { Log.w(TAG, e.message ?: "") }
        try { virtualizer?.enabled = enabled } catch (e: Exception) { Log.w(TAG, e.message ?: "") }
        try { loudnessEnhancer?.enabled = enabled } catch (e: Exception) { Log.w(TAG, e.message ?: "") }
    }

    @Synchronized
    fun setBandLevel(bandIndex: Short, millibels: Short) {
        val i = bandIndex.toInt()
        if (i in fine10BandLevels.indices) {
            fine10BandLevels[i] = millibels
        }

        equalizer?.let { syncFineBandsToHardware(it) }

        val current = _bandsFlow.value.toMutableList()
        val idx = current.indexOfFirst { it.index == bandIndex }
        if (idx >= 0) {
            current[idx] = current[idx].copy(currentMillibels = millibels)
            _bandsFlow.value = current
        }
    }

    @Synchronized
    fun setAllBands(millibelList: List<Short>) {
        millibelList.forEachIndexed { i, mb ->
            if (i in fine10BandLevels.indices) {
                fine10BandLevels[i] = mb
            }
        }
        equalizer?.let { syncFineBandsToHardware(it) }

        val current = _bandsFlow.value.mapIndexed { idx, band ->
            if (idx < millibelList.size) {
                band.copy(currentMillibels = millibelList[idx])
            } else {
                band
            }
        }
        _bandsFlow.value = current
    }

    private fun syncFineBandsToHardware(eq: Equalizer) {
        try {
            val numHwBands = eq.numberOfBands
            if (numHwBands <= 0) return

            // Query hardware band center frequencies
            val hwFreqs = IntArray(numHwBands.toInt()) { i ->
                try { eq.getCenterFreq(i.toShort()) / 1000 } catch (e: Exception) { 1000 }
            }

            // Map each hardware band to the weighted average of the nearby 10 refined fine bands
            for (hwIdx in 0 until numHwBands) {
                val hwCenter = hwFreqs[hwIdx].toDouble()
                var weightSum = 0.0
                var weightedLevel = 0.0

                for (fineIdx in 0 until standard10BandFrequencies.size) {
                    val fineFreq = standard10BandFrequencies[fineIdx].toDouble()
                    // Distance in octaves
                    val octDist = kotlin.math.abs(kotlin.math.ln(hwCenter / fineFreq) / kotlin.math.ln(2.0))
                    val weight = kotlin.math.max(0.0, 1.0 - octDist / 1.5)
                    if (weight > 0) {
                        weightedLevel += fine10BandLevels[fineIdx] * weight
                        weightSum += weight
                    }
                }

                val finalLevel = if (weightSum > 0.0) {
                    (weightedLevel / weightSum).toInt().coerceIn(-1500, 1500).toShort()
                } else {
                    fine10BandLevels.getOrElse(hwIdx) { 0 }
                }

                try {
                    eq.setBandLevel(hwIdx.toShort(), finalLevel)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set hardware band $hwIdx: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing fine bands to hardware: ${e.message}")
        }
    }

    @Synchronized
    fun applyHardwarePreset(presetIndex: Short) {
        try {
            equalizer?.usePreset(presetIndex)
            equalizer?.let { eq ->
                val numHwBands = eq.numberOfBands
                val hwFreqs = IntArray(numHwBands.toInt()) { i ->
                    try { eq.getCenterFreq(i.toShort()) / 1000 } catch (e: Exception) { 1000 }
                }
                val hwLevels = ShortArray(numHwBands.toInt()) { i ->
                    try { eq.getBandLevel(i.toShort()) } catch (e: Exception) { 0.toShort() }
                }

                // Interpolate hardware preset levels back into 10 refined fine bands
                for (fineIdx in 0 until standard10BandFrequencies.size) {
                    val fineFreq = standard10BandFrequencies[fineIdx].toDouble()
                    var bestDist = Double.MAX_VALUE
                    var closestLevel: Short = 0
                    for (h in 0 until numHwBands) {
                        val octDist = kotlin.math.abs(kotlin.math.ln(hwFreqs[h].toDouble() / fineFreq) / kotlin.math.ln(2.0))
                        if (octDist < bestDist) {
                            bestDist = octDist
                            closestLevel = hwLevels[h]
                        }
                    }
                    fine10BandLevels[fineIdx] = closestLevel
                }

                val current = _bandsFlow.value.mapIndexed { idx, band ->
                    band.copy(currentMillibels = fine10BandLevels[idx])
                }
                _bandsFlow.value = current
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying hardware preset $presetIndex: ${e.message}")
        }
    }

    @Synchronized
    fun setBassBoostStrength(strength: Short) { // 0 to 1000
        currentBassBoostStrength = strength
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(strength)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting bass boost: ${e.message}")
        }
    }

    @Synchronized
    fun setVirtualizerStrength(strength: Short) { // 0 to 1000
        currentVirtualizerStrength = strength
        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(strength)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting virtualizer: ${e.message}")
        }
    }

    @Synchronized
    fun setLoudnessGain(targetGainMb: Int) { // 0 to 1500 mB
        currentLoudnessGain = targetGainMb
        try {
            loudnessEnhancer?.setTargetGain(targetGainMb)
        } catch (e: Exception) {
            Log.w(TAG, "Error setting loudness gain: ${e.message}")
        }
    }

    @Synchronized
    fun releaseEffects() {
        broadcastCloseSession(currentSessionId)
        try { equalizer?.release() } catch (e: Exception) {}
        try { bassBoost?.release() } catch (e: Exception) {}
        try { virtualizer?.release() } catch (e: Exception) {}
        try { loudnessEnhancer?.release() } catch (e: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
    }

    companion object {
        @Volatile
        private var instance: AudioSessionManager? = null

        fun getInstance(context: Context): AudioSessionManager {
            return instance ?: synchronized(this) {
                instance ?: AudioSessionManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
