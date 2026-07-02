package com.hourglass.health.sleep

import com.hourglass.health.model.SleepData

object SleepAnalyzer {

    fun analyze(sleep: SleepData): SleepAnalysis {
        val durationAdvice = analyzeDuration(sleep.totalMinutes)
        val deepAdvice = analyzeDeepSleep(sleep)
        val remAdvice = analyzeREM(sleep)
        val timingAdvice = analyzeTiming(sleep)
        val score = sleep.sleepScore

        val allIssues = durationAdvice.issues + deepAdvice.issues + remAdvice.issues + timingAdvice.issues
        val allTips = durationAdvice.tips + deepAdvice.tips + remAdvice.tips + timingAdvice.tips

        return SleepAnalysis(
            score = score,
            label = sleep.qualityLabel,
            totalHours = sleep.totalMinutes / 60f,
            deepPercent = if (sleep.totalMinutes > 0) sleep.deepMinutes.toFloat() / sleep.totalMinutes * 100 else 0f,
            remPercent = if (sleep.totalMinutes > 0) sleep.remMinutes.toFloat() / sleep.totalMinutes * 100 else 0f,
            lightPercent = if (sleep.totalMinutes > 0) sleep.lightMinutes.toFloat() / sleep.totalMinutes * 100 else 0f,
            issues = allIssues,
            tips = allTips,
            recommendedBedTime = timingAdvice.recommendedBedTime,
            recommendedWakeTime = timingAdvice.recommendedWakeTime
        )
    }

    private fun analyzeDuration(minutes: Int): TimeAdvice {
        val issues = mutableListOf<String>()
        val tips = mutableListOf<String>()
        when {
            minutes < 360 -> {
                issues.add("睡眠不足（<6小时）")
                tips.add("目标睡足7-8小时，比平时早睡30分钟")
            }
            minutes in 360..419 -> {
                tips.add("睡眠接近充足，尝试再增加30分钟")
            }
            minutes in 420..540 -> {
                tips.add("睡眠时长理想，继续保持")
            }
            minutes > 540 -> {
                tips.add("睡眠偏长（>9小时），可能睡眠质量不高或过度疲劳")
            }
        }
        return TimeAdvice(issues, tips)
    }

    private fun analyzeDeepSleep(sleep: SleepData): TimeAdvice {
        val issues = mutableListOf<String>()
        val tips = mutableListOf<String>()
        val ratio = if (sleep.totalMinutes > 0) sleep.deepMinutes.toFloat() / sleep.totalMinutes else 0f
        when {
            ratio < 0.10f -> {
                issues.add("深睡比例偏低（<10%）")
                tips.add("减少睡前酒精摄入，保持规律作息")
                tips.add("睡前1小时避免使用电子设备")
            }
            ratio < 0.15f -> {
                tips.add("深睡比例略偏低，建议睡前一小时放松")
            }
            ratio in 0.15f..0.25f -> {
                tips.add("深睡比例理想")
            }
        }
        return TimeAdvice(issues, tips)
    }

    private fun analyzeREM(sleep: SleepData): TimeAdvice {
        val issues = mutableListOf<String>()
        val tips = mutableListOf<String>()
        val ratio = if (sleep.totalMinutes > 0) sleep.remMinutes.toFloat() / sleep.totalMinutes else 0f
        when {
            ratio < 0.15f -> {
                issues.add("REM比例偏低")
                tips.add("确保睡眠不被打断，REM主要在睡眠后半段")
            }
            ratio in 0.20f..0.25f -> {
                tips.add("REM比例理想")
            }
        }
        return TimeAdvice(issues, tips)
    }

    private fun analyzeTiming(sleep: SleepData): TimingAdvice {
        val bedHour = java.util.Calendar.getInstance().apply { timeInMillis = sleep.bedTime }
            .get(java.util.Calendar.HOUR_OF_DAY)
        val wakeHour = java.util.Calendar.getInstance().apply { timeInMillis = sleep.wakeTime }
            .get(java.util.Calendar.HOUR_OF_DAY)

        val issues = mutableListOf<String>()
        val tips = mutableListOf<String>()

        var recBed = "23:00"
        var recWake = "07:00"

        when {
            bedHour >= 0 && bedHour < 22 -> {
                recBed = "22:30"
                recWake = "06:30"
                tips.add("上床时间偏早，建议保持在22:30-23:30之间最好")
            }
            bedHour in 22..22 -> {
                recBed = "22:30"
                recWake = "06:30"
                tips.add("上床时间合理")
            }
            bedHour in 23..23 -> {
                recBed = "23:00"
                recWake = "07:00"
                tips.add("上床时间适中")
            }
            bedHour >= 0 && bedHour < 2 -> {
                recBed = "23:00"
                recWake = "07:00"
                issues.add("睡得太晚（凌晨后）")
                tips.add("建议逐步提前上床时间，每天提前15分钟")
            }
        }

        when {
            wakeHour < 5 -> issues.add("起床过早")
            wakeHour in 6..8 -> tips.add("起床时间理想")
            wakeHour > 9 -> {
                issues.add("起床偏晚")
                tips.add("建议固定起床时间，包括周末")
            }
        }

        return TimingAdvice(issues, tips, recBed, recWake)
    }
}

data class SleepAnalysis(
    val score: Int,
    val label: String,
    val totalHours: Float,
    val deepPercent: Float,
    val remPercent: Float,
    val lightPercent: Float,
    val issues: List<String>,
    val tips: List<String>,
    val recommendedBedTime: String,
    val recommendedWakeTime: String
)

private data class TimeAdvice(
    val issues: List<String> = emptyList(),
    val tips: List<String> = emptyList()
)

private data class TimingAdvice(
    val issues: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val recommendedBedTime: String = "23:00",
    val recommendedWakeTime: String = "07:00"
)
