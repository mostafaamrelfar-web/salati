
package com.salati.app

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import androidx.core.app.NotificationCompat

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("type") ?: return
        val prayer = intent.getStringExtra("prayer") ?: "الصلاة"

        createChannels(context)

        if (type == "start") {
            context.getSharedPreferences("salati", Context.MODE_PRIVATE)
                .edit()
                .putLong(
                    "mode_until",
                    System.currentTimeMillis() + 15 * 60 * 1000L
                )
                .apply()

            notify(
                context,
                "حان وقت $prayer",
                "حان وقت الصلاة. تقبل الله منك.",
                ADHAN_CHANNEL
            )

            // تشغيل الأذان إذا كان مفعلاً
            val prefs = context.getSharedPreferences(
                "salati",
                Context.MODE_PRIVATE
            )

            if (prefs.getBoolean("adhan_enabled", true)) {
    val serviceIntent = Intent(context, AdhanService::class.java)

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    } catch (_: Exception) {
        // يظل إشعار الصلاة يعمل حتى لو تعذر تشغيل الصوت
    }
}

        } else {
            val verse = verses[
                (System.currentTimeMillis() / 86400000L % verses.size).toInt()
            ]

            notify(
                context,
                "اقترب موعد $prayer",
                "باقي 5 دقائق.\n$verse",
                REMINDER_CHANNEL
            )
        }
    }

    private fun notify(
        context: Context,
        title: String,
        text: String,
        channel: String
    ) {
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        manager.notify(title.hashCode(), notification)
    }

    private fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(
                NotificationChannel(
                    REMINDER_CHANNEL,
                    "التذكير قبل الصلاة",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    ADHAN_CHANNEL,
                    "التنبيه عند دخول وقت الصلاة",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }

    companion object {
        const val REMINDER_CHANNEL = "salati_reminders_v1"
        const val ADHAN_CHANNEL = "salati_adhan_audio_v1"

        val verses = listOf(
            "﴿ وَأَقِمِ الصَّلَاةَ لِذِكْرِي ﴾ — طه: 14",
            "﴿ إِنَّ الصَّلَاةَ تَنْهَىٰ عَنِ الْفَحْشَاءِ وَالْمُنْكَرِ ﴾ — العنكبوت: 45",
            "﴿ حَافِظُوا عَلَى الصَّلَوَاتِ وَالصَّلَاةِ الْوُسْطَىٰ ﴾ — البقرة: 238",
            "﴿ وَاسْتَعِينُوا بِالصَّبْرِ وَالصَّلَاةِ ﴾ — البقرة: 45"
        )

        fun schedule(
            context: Context,
            at: Long,
            id: Int,
            type: String,
            prayer: String
        ) {
            if (at <= System.currentTimeMillis()) return

            val intent = Intent(
                context,
                PrayerAlarmReceiver::class.java
            ).apply {
                putExtra("type", type)
                putExtra("prayer", prayer)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

            val alarm = context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

            try {
                if (Build.VERSION.SDK_INT >= 23) {
                    alarm.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        at,
                        pendingIntent
                    )
                } else {
                    alarm.setExact(
                        AlarmManager.RTC_WAKEUP,
                        at,
                        pendingIntent
                    )
                }
            } catch (_: SecurityException) {
                alarm.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    at,
                    pendingIntent
                )
            }
        }
    }
}

object AdhanPlayer {

    private var player: MediaPlayer? = null

    @Synchronized
    fun start(context: Context) {
        stop()

        try {
            player = MediaPlayer.create(
                context.applicationContext,
                R.raw.adhan
            )?.apply {
                setOnCompletionListener {
                    stop()
                }
                start()
            }
        } catch (_: Exception) {
            stop()
        }
    }

    @Synchronized
    fun stop() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }

        player?.release()
        player = null
    }
}

