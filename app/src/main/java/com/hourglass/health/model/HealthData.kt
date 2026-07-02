package com.hourglass.health.model

data class HealthData(
    val heartRate: Int = 0,
    val steps: Int = 0,
    val calories: Int = 0,
    val spo2: Int = 0,
    val stress: Int = 0,
    val batteryLevel: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class BandStatus(
    val connected: Boolean = false,
    val deviceName: String = "",
    val deviceAddress: String = "",
    val batteryLevel: Int = -1
)

data class WaterReminderConfig(
    var enabled: Boolean = false,
    var intervalMinutes: Int = 60,  // default every 60 min
    var startHour: Int = 8,         // 8:00 start
    var endHour: Int = 22           // 22:00 end
)
