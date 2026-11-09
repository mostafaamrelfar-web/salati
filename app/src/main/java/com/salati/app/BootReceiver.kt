
package com.salati.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {

        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val prefs = context.getSharedPreferences(
            "salati",
            Context.MODE_PRIVATE
        )

        val lat = prefs.getFloat("lat", 0f).toDouble()
        val lon = prefs.getFloat("lon", 0f).toDouble()

        if (lat == 0.0 && lon == 0.0) {
            return
        }

        val pendingResult = goAsync()

        Thread {
            try {
                val today = SimpleDateFormat(
                    "dd-MM-yyyy",
                    Locale.US
                ).format(Date())

                val url = java.net.URL(
                    "https://api.aladhan.com/v1/timings/$today?latitude=$lat&longitude=$lon&method=3"
                )

                val connection =
                    url.openConnection() as java.net.HttpURLConnection

                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val body = connection.inputStream.bufferedReader().use {
                    it.readText()
                }

                connection.disconnect()

                val timings = org.json.JSONObject(body)
                    .getJSONObject("data")
                    .getJSONObject("timings")

                val names = listOf(
                    "Fajr",
                    "Dhuhr",
                    "Asr",
                    "Maghrib",
                    "Isha"
                )

                val values = names.map {
                    timings.getString(it).substringBefore(" ")
                }

                val now = Calendar.getInstance()

                names.forEachIndexed { index, name ->

                    val parts = values[index].split(":")

                    val prayer = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                        set(Calendar.MINUTE, parts[1].toInt())
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    if (prayer.timeInMillis <= now.timeInMillis) {
                        return@forEachIndexed
                    }

                    val reminderTime =
                        prayer.timeInMillis - 5 * 60 * 1000L

                    if (reminderTime > now.timeInMillis) {
                        PrayerAlarmReceiver.schedule(
                            context,
                            reminderTime,
                            index * 2,
                            "reminder",
                            name
                        )
                    }

                    PrayerAlarmReceiver.schedule(
                        context,
                        prayer.timeInMillis,
                        index * 2 + 1,
                        "start",
                        name
                    )
                }

                prefs.edit()
                    .putString("date", today)
                    .apply()

            } catch (_: Exception) {
                // إعادة الجدولة التالية تتم عند تحديث المواقيت
            } finally {
                pendingResult.finish()
            }

        }.start()
    }
}
