package dev.prootcode.app.ui.terminal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import dev.prootcode.app.MainActivity
import dev.prootcode.app.R

class TerminalService : Service() {
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Terminal sessions", NotificationManager.IMPORTANCE_LOW),
        )
        val contentIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val builder = Notification.Builder(this, CHANNEL_ID)
        builder.setContentTitle(getString(R.string.terminal_notification_title))
        builder.setContentText(getString(R.string.terminal_notification_text))
        builder.setSmallIcon(R.drawable.ic_terminal)
        builder.setContentIntent(contentIntent)
        builder.setOngoing(true)
        return builder.build()
    }

    private companion object {
        private const val CHANNEL_ID = "terminal"
        private const val NOTIFICATION_ID = 1
    }
}
