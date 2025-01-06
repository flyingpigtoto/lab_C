package com.example.lab_c.models

// Replaces RunEntity for storing run data in memory (CSV-based)
data class MyRun(
    val id: String,             // Some unique ID (we'll generate from dateTime)
    val dateTime: String,       // "yyyy-MM-dd HH:mm:ss"
    val distance: Float,        // in meters
    val duration: Long,         // in seconds
    val hrData: List<HRDataPoint>,
    val speedData: List<SpeedDataPoint>
)

// Replaces your old HRDataPoint
data class HRDataPoint(
    val timestamp: Long,
    val bpm: Int
)

// Replaces your old SpeedDataPoint
data class SpeedDataPoint(
    val timestamp: Long,
    val speed: Float
)
