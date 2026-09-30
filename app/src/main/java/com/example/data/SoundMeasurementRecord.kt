package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sound_records")
data class SoundMeasurementRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val locationTag: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long,
    val minDb: Float,
    val maxDb: Float,
    val avgDb: Float,
    val peakDb: Float,
    val dominantFreqHz: Int,
    val weightingType: String = "dBA",
    val riskLevel: String, // "Aman", "Tenang", "Sedang", "Bising", "Bahaya"
    val notes: String = "",
    val samplesJson: String = "" // Comma-separated or JSON float array of sample points
)
