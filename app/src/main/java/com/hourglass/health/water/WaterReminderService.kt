package com.hourglass.health.water

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hourglass.health.MainActivity
import java.util.Calendar

class WaterReminderService : Service() {

    companion object {
        const val CHANNEL_ID = "water_reminder_channel"
        const val NOTIFICATION_ID = 1001
        const val REMINDER_NOTIFICATION_ID = 1002
        const val ACTION_START = "com.hourglass.health.START_WATER_REMINDER"
        const val ACTION_STOP = "com.hourglass.health.STOP_WATER_REMINDER"
        const val PREF_NAME = "water_reminder"
        const val PREF_INTERVAL = "interval_minutes"
        const val PREF_ENABLED = "enabled"
        const val PREF_START_HOUR = "start_hour"
        const val PREF_END_HOUR = "end_hour"

        fun isServiceRunning(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(PREF_ENABLED, false)
        }

        fun getInterval(context: Context): Int {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(PREF_INTERVAL, 60)
        }

        fun setInterval(context: Context, minutes: Int) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putInt(PREF_INTERVAL, minutes).apply()
        }

        fun setEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(PREF_ENABLED, enabled).apply()
        }
    }

    private var intervalMinutes = 60
    private var running = false
    private var reminderThread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        intervalMinutes = getInterval(this)

        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startReminder()
            ACTION_STOP -> stopReminder()
        }
        return START_STICKY
    }

    private fun startReminder() {
        if (running) return
        running = true
        setEnabled(this, true)

        reminderThread = Thread {
            while (running) {
                val now = Calendar.getInstance()
                val hour = now.get(Calendar.HOUR_OF_DAY)
                val startHour = getSharedPreferences(PREF_NAME, MODE_PRIVATE).getInt(PREF_START_HOUR, 8)
                val endHour = getSharedPreferences(PREF_NAME, MODE_PRIVATE).getInt(PREF_END_HOUR, 22)

                if (hour in startHour until endHour) {
                    showWaterReminder()
                }

                try {
                    Thread.sleep((intervalMinutes * 60 * 1000).toLong())
                } catch (e: InterruptedException) {
                    break
                }
            }
        }.apply { isDaemon = true; start() }
    }

    private fun stopReminder() {
        running = false
        reminderThread?.interrupt()
        reminderThread = null
        setEnabled(this, false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun showWaterReminder() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_notification_clear_all)
            .setContentTitle("💧 喝水时间")
            .setContentText("已过 ${intervalMinutes} 分钟，该喝杯水了！")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(REMINDER_NOTIFICATION_ID, notification)
    }

    private fun buildForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_notification_clear_all)
            .setContentTitle("💧 喝水提醒已开启")
            .setContentText("每 ${intervalMinutes} 分钟提醒一次")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "喝水提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "定时喝水提醒通知"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
