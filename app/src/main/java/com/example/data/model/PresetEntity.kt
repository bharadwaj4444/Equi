package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    // Comma-separated band levels in millibels (e.g. "0,300,500,200,-100")
    val bandLevels: String,
    val bassBoost: Int = 0, // 0 to 1000
    val virtualizer: Int = 0, // 0 to 1000
    val loudnessGain: Int = 0, // 0 to 1500 mB
    val isDefault: Boolean = false,
    val category: String = "Custom",
    val createdAt: Long = System.currentTimeMillis()
)
