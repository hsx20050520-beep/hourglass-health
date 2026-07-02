package com.hourglass.health.water

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.hourglass.health.MainActivity

class WaterReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val interval = intent?.getIntExtra("interval_minutes", 60) ?: 60

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, WaterReminderService.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_notification_clear_all)
            .setContentTitle("💧 喝水时间")
            .setContentText("已过 ${interval} 分钟，该喝杯水了！")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(1002, notification)
    }
}
