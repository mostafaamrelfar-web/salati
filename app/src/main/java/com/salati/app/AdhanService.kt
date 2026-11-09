
package com.salati.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AdhanService : Service() {

    private var player: MediaPlayer? = null

    companion object {
        const val CHANNEL_ID = "salati_adhan_service"
        const val NOTIFICATION_ID = 2001
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val notification: Notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("صَلَاتِي")
                .setContentText("يتم تشغيل الأذان")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

        startForeground(NOTIFICATION_ID, notification)

        if (player == null) {
            try {
                player = MediaPlayer.create(this, R.raw.adhan)

                if (player == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                player?.setOnCompletionListener {
                    stopSelf()
                }

                player?.setOnErrorListener { _, _, _ ->
                    stopSelf()
                    true
                }

                player?.start()

            } catch (_: Exception) {
                player?.release()
                player = null
                stopSelf()
                return START_NOT_STICKY
            }
        }

        return START_NOT_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "تشغيل الأذان",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(
                NotificationManager::class.java
            )

            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        player?.release()
        player = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
