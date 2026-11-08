package com.salati.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var status: TextView
    private lateinit var times: TextView
    private val prefs by lazy { getSharedPreferences("salati", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        requestNeededPermissions()
        showSavedTimes()
    }

    private fun buildUi() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 36, 28, 28)
            setBackgroundColor(0xFF102B35.toInt())
        }
        fun label(text: String, size: Float, color: Int = 0xFFFFFFFF.toInt()): TextView =
            TextView(this).apply {
                this.text = text; textSize = size; setTextColor(color)
                gravity = Gravity.CENTER; setPadding(8, 12, 8, 12)
            }
        page.addView(label("صَلَاتِي", 30f, 0xFFE5C77A.toInt()))
        page.addView(label("وقتك مع الله أولى", 16f, 0xFFDCE7E8.toInt()))
        status = label("اضغط تحديث المواقيت لتحديد موقعك", 16f)
        page.addView(status)
        times = label("الفجر  —\nالظهر  —\nالعصر  —\nالمغرب  —\nالعشاء  —", 18f)
        page.addView(times)

        val refresh = Button(this).apply {
            text = "تحديد موقعي وتحديث المواقيت"
            setOnClickListener { loadPrayerTimes() }
        }
        page.addView(refresh, matchWrap())

        page.addView(label("التذكير قبل الصلاة: 5 دقائق  •  وضع الصلاة: 15 دقيقة", 14f, 0xFFE5C77A.toInt()))

        val appsTitle = label("التطبيقات التي تريد حظرها أثناء الصلاة", 16f)
        page.addView(appsTitle)
        addAppCheck(page, "YouTube", "com.google.android.youtube")
        addAppCheck(page, "TikTok", "com.zhiliaoapp.musically")
        addAppCheck(page, "Instagram", "com.instagram.android")
        addAppCheck(page, "Facebook", "com.facebook.katana")

        val access = Button(this).apply {
            text = "تفعيل صلاحية حظر التطبيقات"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        page.addView(access, matchWrap())
        page.addView(label("تعمل خدمة الحظر فقط أثناء وضع الصلاة، ويمكنك دائماً استخدام زر الطوارئ.", 12f, 0xFFDCE7E8.toInt()))
        setContentView(ScrollView(this).apply { addView(page) })
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = 8; bottomMargin = 8 }

    private fun addAppCheck(parent: LinearLayout, title: String, pkg: String) {
        val check = CheckBox(this).apply {
            text = title
            setTextColor(0xFFFFFFFF.toInt())
            isChecked = prefs.getStringSet("blocked_apps", emptySet())?.contains(pkg) == true
            setOnCheckedChangeListener { _, checked ->
                val set = prefs.getStringSet("blocked_apps", emptySet())?.toMutableSet() ?: mutableSetOf()
                if (checked) set.add(pkg) else set.remove(pkg)
                prefs.edit().putStringSet("blocked_apps", set).apply()
            }
        }
        parent.addView(check)
    }

    private fun requestNeededPermissions() {
        val list = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) list.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = list.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) ActivityCompat.requestPermissions(this, missing.toTypedArray(), 10)
    }

    private fun loadPrayerTimes() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            status.text = "اسمح للتطبيق بالوصول إلى الموقع أولاً."
            requestNeededPermissions()
            return
        }
        status.text = "جارٍ تحديد موقعك وجلب المواقيت..."
        try {
            val lm = getSystemService(LOCATION_SERVICE) as LocationManager
            val location: Location? = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .mapNotNull { provider -> try { lm.getLastKnownLocation(provider) } catch (_: Exception) { null } }
                .maxByOrNull { it.time }
            if (location == null) {
                status.text = "تعذر الحصول على موقع حديث. فعّل الموقع ثم حاول مرة أخرى."
                return
            }
            fetchTimes(location.latitude, location.longitude)
        } catch (e: Exception) {
            status.text = "تعذر تحديد الموقع: ${e.localizedMessage}"
        }
    }

    private fun fetchTimes(lat: Double, lon: Double) {
        executor.execute {
            try {
                val today = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
                val url = URL("https://api.aladhan.com/v1/timings/$today?latitude=$lat&longitude=$lon&method=3")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000; readTimeout = 15000; requestMethod = "GET"
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val root = JSONObject(body).getJSONObject("data")
                val timings = root.getJSONObject("timings")
                val names = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
                val arabic = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")
                val values = names.map { timings.getString(it).substringBefore(" ") }
                val display = arabic.zip(values).joinToString("\n") { "${it.first}   ${it.second}" }
                prefs.edit().putString("times", display).putString("date", today)
                    .putFloat("lat", lat.toFloat()).putFloat("lon", lon.toFloat()).apply()
                scheduleAlarms(names, values)
                runOnUiThread {
                    status.text = "تم تحديد الموقع وتحديث مواقيت اليوم."
                    times.text = display
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "تعذر جلب المواقيت. تحقق من الإنترنت وحاول مجدداً." }
            }
        }
    }

    private fun scheduleAlarms(names: List<String>, values: List<String>) {
        val now = Calendar.getInstance()
        names.forEachIndexed { index, name ->
            val parts = values[index].split(":")
            val prayer = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                set(Calendar.MINUTE, parts[1].toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (prayer.timeInMillis <= now.timeInMillis) return@forEachIndexed
            PrayerAlarmReceiver.schedule(this, prayer.timeInMillis - 5 * 60 * 1000L, index * 2, "reminder", name)
            PrayerAlarmReceiver.schedule(this, prayer.timeInMillis, index * 2 + 1, "start", name)
        }
    }

    private fun showSavedTimes() {
        prefs.getString("times", null)?.let { times.text = it }
        prefs.getString("date", null)?.let { status.text = "آخر تحديث للمواقيت: $it" }
    }
}
