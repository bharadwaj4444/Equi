package com.example.data.repository

import com.example.data.db.PresetDao
import com.example.data.model.PresetEntity
import kotlinx.coroutines.flow.Flow

class PresetRepository(private val presetDao: PresetDao) {

    val allPresets: Flow<List<PresetEntity>> = presetDao.getAllPresets()

    suspend fun initializeDefaultPresetsIfEmpty() {
        if (presetDao.getPresetCount() == 0) {
            val defaults = listOf(
                PresetEntity(
                    name = "Flat",
                    bandLevels = "0,0,0,0,0,0,0,0,0,0",
                    bassBoost = 0,
                    virtualizer = 0,
                    loudnessGain = 0,
                    isDefault = true,
                    category = "Studio"
                ),
                PresetEntity(
                    name = "Bass Extreme",
                    bandLevels = "900,850,650,400,150,0,-50,-100,-150,-200",
                    bassBoost = 850,
                    virtualizer = 200,
                    loudnessGain = 300,
                    isDefault = true,
                    category = "Bass"
                ),
                PresetEntity(
                    name = "Rock & Metal",
                    bandLevels = "500,450,250,50,-100,50,200,350,500,600",
                    bassBoost = 400,
                    virtualizer = 300,
                    loudnessGain = 200,
                    isDefault = true,
                    category = "Genre"
                ),
                PresetEntity(
                    name = "Electronic / EDM",
                    bandLevels = "650,600,450,200,0,100,250,400,500,550",
                    bassBoost = 600,
                    virtualizer = 400,
                    loudnessGain = 300,
                    isDefault = true,
                    category = "Genre"
                ),
                PresetEntity(
                    name = "Pop",
                    bandLevels = "-100,100,250,400,450,300,150,50,-50,-100",
                    bassBoost = 300,
                    virtualizer = 250,
                    loudnessGain = 150,
                    isDefault = true,
                    category = "Genre"
                ),
                PresetEntity(
                    name = "Hip-Hop",
                    bandLevels = "750,700,450,200,50,150,200,250,300,350",
                    bassBoost = 700,
                    virtualizer = 300,
                    loudnessGain = 250,
                    isDefault = true,
                    category = "Genre"
                ),
                PresetEntity(
                    name = "Jazz",
                    bandLevels = "350,300,200,100,0,50,150,250,350,400",
                    bassBoost = 200,
                    virtualizer = 350,
                    loudnessGain = 100,
                    isDefault = true,
                    category = "Genre"
                ),
                PresetEntity(
                    name = "Classical",
                    bandLevels = "400,350,250,100,-50,-100,100,250,350,450",
                    bassBoost = 150,
                    virtualizer = 500,
                    loudnessGain = 100,
                    isDefault = true,
                    category = "Acoustic"
                ),
                PresetEntity(
                    name = "Vocal Clarity",
                    bandLevels = "-350,-250,-100,200,500,700,600,400,200,100",
                    bassBoost = 0,
                    virtualizer = 200,
                    loudnessGain = 300,
                    isDefault = true,
                    category = "Voice"
                ),
                PresetEntity(
                    name = "Acoustic",
                    bandLevels = "350,300,200,150,100,150,250,350,400,450",
                    bassBoost = 250,
                    virtualizer = 300,
                    loudnessGain = 150,
                    isDefault = true,
                    category = "Acoustic"
                ),
                PresetEntity(
                    name = "Treble Booster",
                    bandLevels = "-250,-200,-150,-50,50,150,350,550,750,850",
                    bassBoost = 0,
                    virtualizer = 150,
                    loudnessGain = 200,
                    isDefault = true,
                    category = "Tone"
                ),
                PresetEntity(
                    name = "Deep Club",
                    bandLevels = "800,750,550,250,0,100,300,450,550,600",
                    bassBoost = 800,
                    virtualizer = 500,
                    loudnessGain = 350,
                    isDefault = true,
                    category = "Bass"
                )
            )
            presetDao.insertAll(defaults)
        }
    }

    suspend fun saveCustomPreset(
        name: String,
        bandLevels: List<Short>,
        bassBoost: Int,
        virtualizer: Int,
        loudnessGain: Int
    ): Long {
        val levelsStr = bandLevels.joinToString(",")
        val entity = PresetEntity(
            name = name.trim(),
            bandLevels = levelsStr,
            bassBoost = bassBoost,
            virtualizer = virtualizer,
            loudnessGain = loudnessGain,
            isDefault = false,
            category = "Custom"
        )
        return presetDao.insertPreset(entity)
    }

    suspend fun deletePreset(id: Long) {
        presetDao.deleteCustomPresetById(id)
    }
}
