package com.hourglass.health.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.hourglass.health.model.SleepData
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class HealthConnectManager(private val context: Context) {

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions = setOf(
        HealthPermission.READ_HEART_RATE,
        HealthPermission.READ_SLEEP,
        HealthPermission.READ_STEPS,
        HealthPermission.READ_SPO2,
    )

    fun getPermissionIntent() = PermissionController.createRequestPermissionResultContract()
        .createIntent(context, permissions)

    suspend fun readTodaySleep(): SleepData? {
        return try {
            val now = ZonedDateTime.now()
            val startOfToday = now.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfToday = startOfToday.plusSeconds(86400)
            val rangeStart = startOfToday.minusSeconds(12 * 3600)

            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(rangeStart, endOfToday)
            )
            val response = healthConnectClient.readRecords(request)
            val session = response.records.maxByOrNull { it.startTime.toEpochMilli() } ?: return null

            var totalMinutes = 0L
            var deepMinutes = 0L
            var lightMinutes = 0L
            var remMinutes = 0L
            var awakeMinutes = 0L

            session.stages.forEach { stage ->
                val mins = Duration.between(stage.startTime, stage.endTime).toMinutes()
                totalMinutes += mins
                when (stage.stage) {
                    SleepSessionRecord.STAGE_TYPE_DEEP -> deepMinutes += mins
                    SleepSessionRecord.STAGE_TYPE_LIGHT -> lightMinutes += mins
                    SleepSessionRecord.STAGE_TYPE_REM -> remMinutes += mins
                    else -> awakeMinutes += mins
                }
            }

            if (totalMinutes == 0L) return null

            SleepData(
                date = session.startTime.toString().substringBefore("T"),
                bedTime = session.startTime.toEpochMilli(),
                wakeTime = session.endTime.toEpochMilli(),
                totalMinutes = totalMinutes.toInt(),
                deepMinutes = deepMinutes.toInt(),
                lightMinutes = lightMinutes.toInt(),
                remMinutes = remMinutes.toInt(),
                awakeMinutes = awakeMinutes.toInt()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun readLatestHeartRate(): Int {
        return try {
            val now = Instant.now()
            val thirtyMinAgo = now.minusSeconds(1800)
            val request = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(thirtyMinAgo, now)
            )
            val response = healthConnectClient.readRecords(request)
            val latest = response.records.maxByOrNull { it.endTime.toEpochMilli() }
            latest?.samples?.lastOrNull()?.beatsPerMinute ?: 0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun readTodaySteps(): Int {
        return try {
            val now = ZonedDateTime.now()
            val startOfDay = now.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = startOfDay.plusSeconds(86400)

            val request = AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            )
            val response = healthConnectClient.aggregate(request)
            response[StepsRecord.COUNT_TOTAL]?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
