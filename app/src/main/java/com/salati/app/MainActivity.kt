
package com.salati.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val navy = Color.rgb(15, 25, 48)
    private val gold = Color.rgb(218, 181, 105)
    private val white = Color.WHITE
    private val muted = Color.rgb(190, 195, 205)

    private val executor = Executors.newSingleThreadExecutor()

    private val prefs by lazy {
        getSharedPreferences("salati", Context.MODE_PRIVATE)
    }

    private var statusView: TextView? = null
    private var timesView: TextView? = null
    private var nextPrayerView: TextView? = null
    private var adhanSwitch: Switch? = null
    private var streakView: TextView? = null

    private val prayerNames = listOf(
        "Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"
    )

    private val arabicNames = listOf(
        "الفجر", "الظهر", "العصر", "المغرب", "العشاء"
    )

    private val verses = listOf(
        "﴿ وَأَقِمِ الصَّلَاةَ لِذِكْرِي ﴾\nسورة طه - الآية 14",
        "﴿ إِنَّ الصَّلَاةَ تَنْهَىٰ عَنِ الْفَحْشَاءِ وَالْمُنكَرِ ﴾\nسورة العنكبوت - الآية 45",
        "﴿ وَاسْتَعِينُوا بِالصَّبْرِ وَالصَّلَاةِ ﴾\nسورة البقرة - الآية 45",
        "﴿ حَافِظُوا عَلَى الصَّلَوَاتِ ﴾\nسورة البقرة - الآية 238",
        "﴿ إِنَّنِي أَنَا اللَّهُ لَا إِلَٰهَ إِلَّا أَنَا فَاعْبُدْنِي وَأَقِمِ الصَّلَاةَ لِذِكْرِي ﴾\nسورة طه - الآية 14"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
        requestNeededPermissions()
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(navy)
            setPadding(22, 24, 22, 20)
        }

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val title = makeText(
            "صَلَاتِي",
            30f,
            gold,
            true
        )
        title.gravity = Gravity.CENTER
        content.addView(title)

        val subtitle = makeText(
            "رفيقك إلى الصلاة",
            15f,
            muted
        )
        subtitle.gravity = Gravity.CENTER
        content.addView(subtitle)

        addSpace(content, 24)

        addSectionTitle(content, "الصلاة القادمة")

        nextPrayerView = makeText(
            "جارٍ حساب الصلاة القادمة...",
            21f,
            gold,
            true
        )
        nextPrayerView?.gravity = Gravity.CENTER
        content.addView(nextPrayerView)

        addSpace(content, 15)

        addSectionTitle(content, "مواقيت الصلاة")

        timesView = makeText(
            prefs.getString("times", "لم يتم تحميل المواقيت بعد") ?: "",
            18f,
            white
        )
        timesView?.gravity = Gravity.CENTER
        content.addView(timesView)

        addSpace(content, 10)

        statusView = makeText(
            "جاهز",
            14f,
            muted
        )
        statusView?.gravity = Gravity.CENTER
        content.addView(statusView)

        val refreshButton = makeButton("تحديث مواقيت الصلاة")
        refreshButton.setOnClickListener {
            loadPrayerTimes()
        }
        content.addView(refreshButton)

        addSpace(content, 20)

        addSectionTitle(content, "الأذان")

        adhanSwitch = Switch(this).apply {
            text = "تشغيل الأذان عند دخول وقت الصلاة"
            setTextColor(white)
            textSize = 16f
            isChecked = prefs.getBoolean("adhan_enabled", true)

            setOnCheckedChangeListener { _, checked ->
                prefs.edit()
                    .putBoolean("adhan_enabled", checked)
                    .apply()

                if (!checked) {
                    stopService(
                        Intent(this@MainActivity, AdhanService::class.java)
                    )
                }
            }
        }
        content.addView(adhanSwitch)

        val testAdhanButton = makeButton("تجربة تشغيل الأذان")
        testAdhanButton.setOnClickListener {
            if (prefs.getBoolean("adhan_enabled", true)) {
                startAdhanService()
            } else {
                Toast.makeText(
                    this,
                    "فعّل الأذان أولاً",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        content.addView(testAdhanButton)

        val stopAdhanButton = makeButton("إيقاف الأذان")
        stopAdhanButton.setOnClickListener {
            stopService(Intent(this, AdhanService::class.java))
        }
        content.addView(stopAdhanButton)

        addSpace(content, 20)

        addSectionTitle(content, "آية اليوم")

        val verseIndex = (
            Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % verses.size
        )

        val verseView = makeText(
            verses[verseIndex],
            17f,
            gold
        )
        verseView.gravity = Gravity.CENTER
        content.addView(verseView)

        addSpace(content, 20)

        addSectionTitle(content, "إنجازاتي")

        val streak = prefs.getInt("streak_count", 0)

        streakView = makeText(
            "سلسلة إنجازك الحالية: $streak يوم",
            17f,
            white
        )
        streakView?.gravity = Gravity.CENTER
        content.addView(streakView)

        val checkinButton = makeButton("تسجيل إنجاز اليوم")
        checkinButton.setOnClickListener {
            recordCheckin()
        }
        content.addView(checkinButton)

        addSpace(content, 20)

        addSectionTitle(content, "التطبيقات المحظورة وقت الصلاة")

        addAppCheck(content, "YouTube", "com.google.android.youtube")
        addAppCheck(content, "TikTok", "com.zhiliaoapp.musically")
        addAppCheck(content, "Instagram", "com.instagram.android")
        addAppCheck(content, "Facebook", "com.facebook.katana")

        addSpace(content, 20)

        val accessibilityButton = makeButton(
            "إعدادات صلاحية حظر التطبيقات"
        )
        accessibilityButton.setOnClickListener {
            startActivity(
                Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
            )
        }
        content.addView(accessibilityButton)

        addSpace(content, 25)

        val footer = makeText(
            "صَلَاتِي - لا تنسَ صلاتك",
            14f,
            gold
        )
        footer.gravity = Gravity.CENTER
        content.addView(footer)

        scroll.addView(content)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun makeText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL

            if (bold) {
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            setPadding(8, 9, 8, 9)
        }
    }

    private fun makeButton(value: String): Button {
        return Button(this).apply {
            text = value
            setTextColor(navy)
            textSize = 15f
            setBackgroundColor(gold)

            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 7, 0, 7)
            layoutParams = params
        }
    }

    private fun addSectionTitle(
        parent: LinearLayout,
        value: String
    ) {
        val title = makeText(value, 20f, gold, true)
        parent.addView(title)
    }

    private fun addSpace(
        parent: LinearLayout,
        height: Int
    ) {
        val space = TextView(this)
        space.height = height
        parent.addView(space)
    }

    private fun addAppCheck(
        parent: LinearLayout,
        title: String,
        pkg: String
    ) {
        val selected = prefs.getStringSet(
            "blocked_apps",
            emptySet()
        )?.contains(pkg) == true

        val check = CheckBox(this).apply {
            text = title
            setTextColor(white)
            isChecked = selected

            setOnCheckedChangeListener { _, checked ->
                val apps = prefs.getStringSet(
                    "blocked_apps",
                    emptySet()
                )?.toMutableSet() ?: mutableSetOf()

                if (checked) {
                    apps.add(pkg)
                } else {
                    apps.remove(pkg)
                }

                prefs.edit()
                    .putStringSet("blocked_apps", apps)
                    .apply()
            }
        }

        parent.addView(check)
    }

    private fun recordCheckin() {
        val today = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(Date())

        val last = prefs.getString("last_checkin", "")

        if (last == today) {
            Toast.makeText(
                this,
                "تم تسجيل إنجاز اليوم بالفعل",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }

        val yesterdayText = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(yesterday.time)

        val oldCount = prefs.getInt("streak_count", 0)

        val newCount = if (last == yesterdayText) {
            oldCount + 1
        } else {
            1
        }

        prefs.edit()
            .putString("last_checkin", today)
            .putInt("streak_count", newCount)
            .apply()

        streakView?.text = "سلسلة إنجازك الحالية: $newCount يوم"

        Toast.makeText(
            this,
            "تم تسجيل إنجاز اليوم",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(
                this,
                it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missing.toTypedArray(),
                10
            )
        }
    }

    private fun loadPrayerTimes() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            updateStatus("اسمح للتطبيق بالوصول إلى الموقع أولاً.")
            requestNeededPermissions()
            return
        }

        updateStatus("جارٍ تحديد موقعك وجلب المواقيت...")

        try {
            val manager = getSystemService(LOCATION_SERVICE) as LocationManager

            val location: Location? = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER
            ).mapNotNull { provider ->
                try {
                    manager.getLastKnownLocation(provider)
                } catch (_: SecurityException) {
                    null
                } catch (_: Exception) {
                    null
                }
            }.maxByOrNull { it.time }

            if (location == null) {
                updateStatus(
                    "تعذر الحصول على الموقع. فعّل الموقع وحاول مرة أخرى."
                )
                return
            }

            fetchTimes(location.latitude, location.longitude)

        } catch (_: Exception) {
            updateStatus("تعذر تحديد الموقع.")
        }
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            statusView?.text = message
        }
    }

    private fun fetchTimes(lat: Double, lon: Double) {
        executor.execute {
            var connection: HttpURLConnection? = null

            try {
                val today = SimpleDateFormat(
                    "dd-MM-yyyy",
                    Locale.US
                ).format(Date())

                val url = URL(
                    "https://api.aladhan.com/v1/timings/$today?latitude=$lat&longitude=$lon&method=3"
                )

                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.requestMethod = "GET"

                val body = connection.inputStream.bufferedReader().use {
                    it.readText()
                }

                val root = JSONObject(body).getJSONObject("data")
                val timings = root.getJSONObject("timings")

                val values = prayerNames.map {
                    timings.getString(it).substringBefore(" ")
                }

                val display = arabicNames.zip(values).joinToString("\n") {
                    "${it.first}       ${it.second}"
                }

                prefs.edit()
                    .putString("times", display)
                    .putString("date", today)
                    .putFloat("lat", lat.toFloat())
                    .putFloat("lon", lon.toFloat())
                    .apply()

                scheduleAlarms(values)
                updateNextPrayer(values)

                runOnUiThread {
                    timesView?.text = display
                    statusView?.text = "تم تحديث مواقيت اليوم بنجاح."
                }

            } catch (_: Exception) {
                updateStatus(
                    "تعذر جلب المواقيت. تحقق من الإنترنت وحاول مجدداً."
                )
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun scheduleAlarms(values: List<String>) {
        val now = Calendar.getInstance()

        prayerNames.forEachIndexed { index, name ->
            val parts = values[index].split(":")

            if (parts.size < 2) return@forEachIndexed

            val prayer = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                set(Calendar.MINUTE, parts[1].toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (prayer.timeInMillis <= now.timeInMillis) {
                return@forEachIndexed
            }

            val reminderTime = prayer.timeInMillis - 5 * 60 * 1000L

            if (reminderTime > now.timeInMillis) {
                PrayerAlarmReceiver.schedule(
                    this,
                    reminderTime,
                    index * 2,
                    "reminder",
                    name
                )
            }

            PrayerAlarmReceiver.schedule(
                this,
                prayer.timeInMillis,
                index * 2 + 1,
                "start",
                name
            )
        }
    }

    private fun updateNextPrayer(values: List<String>) {
        val now = Calendar.getInstance()

        var nextName = ""
        var nextTime: Calendar? = null

        values.forEachIndexed { index, value ->
            val parts = value.split(":")
            if (parts.size < 2) return@forEachIndexed

            val prayer = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                set(Calendar.MINUTE, parts[1].toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (
                prayer.timeInMillis > now.timeInMillis &&
                (nextTime == null ||
                    prayer.timeInMillis < nextTime!!.timeInMillis)
            ) {
                nextTime = prayer
                nextName = arabicNames[index]
            }
        }

        if (nextTime == null) {
            nextPrayerView?.text = "انتهت صلوات اليوم"
            return
        }

        val remaining = nextTime!!.timeInMillis - now.timeInMillis
        val hours = remaining / 3600000
        val minutes = (remaining % 3600000) / 60000

        nextPrayerView?.text =
            "$nextName\nمتبقي $hours ساعة و$minutes دقيقة"
    }

    private fun startAdhanService() {
        val intent = Intent(this, AdhanService::class.java)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, intent)
            } else {
                startService(intent)
            }
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "تعذر تشغيل الأذان",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
