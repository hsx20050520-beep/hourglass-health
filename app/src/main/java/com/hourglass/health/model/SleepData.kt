package com.hourglass.health.model

data class SleepData(
    val date: String,              // "2026-07-02"
    val bedTime: Long,             // timestamp 上床时间
    val wakeTime: Long,            // timestamp 起床时间
    val totalMinutes: Int,
    val deepMinutes: Int,
    val lightMinutes: Int,
    val remMinutes: Int,
    val awakeMinutes: Int,
    val avgHeartRate: Int = 0,
    val avgSpo2: Int = 0,
    val respiratoryRate: Float = 0f
) {
    val sleepScore: Int
        get() {
            // Score based on duration (ideal 7-9h)
            val durationScore = when {
                totalMinutes in 420..540 -> 40
                totalMinutes in 360..419 || totalMinutes in 541..600 -> 30
                totalMinutes in 300..359 || totalMinutes in 601..660 -> 20
                else -> 10
            }
            // Deep sleep ratio (ideal 15-25%)
            val deepRatio = if (totalMinutes > 0) deepMinutes.toFloat() / totalMinutes else 0f
            val deepScore = when {
                deepRatio in 0.15f..0.25f -> 30
                deepRatio in 0.10f..0.14f || deepRatio in 0.26f..0.30f -> 20
                else -> 10
            }
            // REM ratio (ideal 20-25%)
            val remRatio = if (totalMinutes > 0) remMinutes.toFloat() / totalMinutes else 0f
            val remScore = when {
                remRatio in 0.20f..0.25f -> 20
                remRatio in 0.15f..0.19f || remRatio in 0.26f..0.30f -> 15
                else -> 10
            }
            // Awake time (less awake = better)
            val awakeRatio = if (totalMinutes > 0) awakeMinutes.toFloat() / totalMinutes else 0f
            val awakeScore = when {
                awakeRatio < 0.05f -> 10
                awakeRatio < 0.10f -> 7
                awakeRatio < 0.15f -> 4
                else -> 0
            }
            return durationScore + deepScore + remScore + awakeScore
        }

    val qualityLabel: String
        get() = when {
            sleepScore >= 80 -> "优秀"
            sleepScore >= 60 -> "良好"
            sleepScore >= 40 -> "一般"
            else -> "较差"
        }

    val qualityColor: Long
        get() = when {
            sleepScore >= 80 -> 0xFF16A34A
            sleepScore >= 60 -> 0xFF3B82F6
            sleepScore >= 40 -> 0xFFD97706
            else -> 0xFFDC2626
        }
}
