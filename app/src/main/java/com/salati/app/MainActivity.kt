
package com.salati.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
    private val prefs by lazy {
        getSharedPreferences("salati", Context.MODE_PRIVATE)
    }

    private lateinit var content: LinearLayout
    private var statusView: TextView? = null
    private var timesView: TextView? = null

    private val navy = Color.rgb(16, 43, 53)
    private val gold = Color.rgb(229, 199, 122)
    private val white = Color.WHITE
    private val muted = Color.rgb(220, 231, 232)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        requestNeededPermissions()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(navy)
        }

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 24, 20, 24)
        }

        val scroll = ScrollView(this).apply {
            addView(content)
        }

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1, 0, 1f
            )
        )

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(4, 8, 4, 8)
            setBackgroundColor(Color.rgb(11, 32, 40))
        }

        addNavButton(nav, "الرئيسية") { showHome() }
        addNavButton(nav, "المواقيت") { showPrayerTimes() }
        addNavButton(nav, "القرآن") { showQuran() }
        addNavButton(nav, "إنجازاتي") { showStreak() }

        root.addView(nav)
        setContentView(root)

        showHome()
    }

    private fun addNavButton(
        parent: LinearLayout,
        title: String,
        action: () -> Unit
    ) {
        val button = TextView(this).apply {
            text = title
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(gold)
            setPadding(5, 14, 5, 14)
            setOnClickListener { action() }
        }

        parent.addView(
            button,
            LinearLayout.LayoutParams(0, -2, 1f)
        )
    }

    private fun clearContent() {
        content.removeAllViews()
        statusView = null
        timesView = null
    }

    private fun addText(
        text: String,
        size: Float,
        color: Int = white,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            setPadding(8, 12, 8, 12)
            if (bold) setTypeface(null, Typeface.BOLD)
        }
    }

    private fun addCard(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 16)
            background = GradientDrawable().apply {
                setColor(Color.rgb(25, 58, 68))
                cornerRadius = 24f
            }
        }

        val params = LinearLayout.LayoutParams(-1, -2)
        params.setMargins(0, 10, 0, 10)
        content.addView(card, params)
        return card
    }

    private fun addButton(
        parent: LinearLayout,
        title: String,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = title
            setOnClickListener { action() }
        }
        parent.addView(
            button,
            LinearLayout.LayoutParams(-1, -2)
        )
    }

    // الرئيسية

    private fun showHome() {
        clearContent()

        content.addView(
            addText("صَلَاتِي", 32f, gold, true)
        )

        content.addView(
            addText("وقتك مع الله أولى", 16f, muted)
        )

        val today = SimpleDateFormat(
            "EEEE، d MMMM",
            Locale("ar")
        ).format(Date())

        val dateCard = addCard()
        dateCard.addView(addText(today, 17f, gold, true))
        dateCard.addView(
            addText("مرحباً بك في صلاتي", 20f, white, true)
        )
        dateCard.addView(
            addText(
                "اجعل صلاتك أولويتك واجعل يومك عامراً بالذكر",
                14f,
                muted
            )
        )

        val prayerCard = addCard()
        prayerCard.addView(
            addText("مواقيت الصلاة", 19f, gold, true)
        )

        val saved = prefs.getString("times", null)
        prayerCard.addView(
            addText(
                saved ?: "حدّد موقعك لمعرفة مواقيت الصلاة",
                16f
            )
        )

        addButton(prayerCard, "تحديث مواقيت الصلاة") {
            loadPrayerTimes()
        }

        addButton(prayerCard, "عرض جميع المواقيت") {
            showPrayerTimes()
        }

        val verseCard = addCard()
        verseCard.addView(
            addText("آية اليوم", 20f, gold, true)
        )
        verseCard.addView(
            addText(getDailyVerse(), 21f, white)
        )

        val streakCard = addCard()
        streakCard.addView(
            addText("إنجازك اليومي", 19f, gold, true)
        )
        streakCard.addView(
            addText(
                "🔥 ${prefs.getInt("streak_count", 0)} يوم متتالي",
                23f,
                white,
                true
            )
        )
        addButton(streakCard, "عرض إنجازاتي") {
            showStreak()
        }

        val appsCard = addCard()
        appsCard.addView(
            addText("وضع الصلاة", 19f, gold, true)
        )
        appsCard.addView(
            addText(
                "اختر التطبيقات التي تريد تقييدها أثناء الصلاة",
                14f,
                muted
            )
        )

        addAppCheck(
            appsCard,
            "YouTube",
            "com.google.android.youtube"
        )
        addAppCheck(
            appsCard,
            "TikTok",
            "com.zhiliaoapp.musically"
        )
        addAppCheck(
            appsCard,
            "Instagram",
            "com.instagram.android"
        )
        addAppCheck(
            appsCard,
            "Facebook",
            "com.facebook.katana"
        )

        addButton(appsCard, "تفعيل صلاحية حظر التطبيقات") {
            startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            )
        }

        content.addView(
            addText(
                "اللهم اجعل الصلاة راحةً لقلوبنا",
                15f,
                gold
            )
        )
    }

    // صفحة المواقيت

    private fun showPrayerTimes() {
        clearContent()

        content.addView(
            addText("مواقيت الصلاة", 27f, gold, true)
        )

        val card = addCard()

        timesView = addText(
            prefs.getString("times", null)
                ?: "لم يتم تحديد المواقيت بعد",
            21f
        )

        card.addView(timesView)

        statusView = addText(
            prefs.getString("date", null)?.let {
                "آخر تحديث: $it"
            } ?: "حدّد موقعك لتحديث المواقيت",
            14f,
            muted
        )

        card.addView(statusView)

        addButton(card, "تحديد موقعي وتحديث المواقيت") {
            loadPrayerTimes()
        }

        content.addView(
            addText(
                "التذكير قبل الصلاة بخمس دقائق",
                15f,
                gold
            )
        )
    }

    // صفحة القرآن

    private fun showQuran() {
        clearContent()

        content.addView(
            addText("آية اليوم", 28f, gold, true)
        )

        val card = addCard()

        card.addView(
            addText(
                getDailyVerse(),
                25f,
                white
            )
        )

        card.addView(
            addText(
                "تتغير الآية يومياً",
                14f,
                muted
            )
        )
    }

    private fun getDailyVerse(): String {
        val verses = listOf(
            "﴿ وَأَقِمِ الصَّلَاةَ لِذِكْرِي ﴾\nطه: 14",
            "﴿ إِنَّ اللَّهَ مَعَ الصَّابِرِينَ ﴾\nالبقرة: 153",
            "﴿ فَاذْكُرُونِي أَذْكُرْكُمْ ﴾\nالبقرة: 152",
            "﴿ وَهُوَ مَعَكُمْ أَيْنَ مَا كُنْتُمْ ﴾\nالحديد: 4",
            "﴿ وَاسْتَعِينُوا بِالصَّبْرِ وَالصَّلَاةِ ﴾\nالبقرة: 45",
            "﴿ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ ﴾\nالرعد: 28"
        )

        val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return verses[day % verses.size]
    }

    // صفحة الإنجازات

    private fun showStreak() {
        clearContent()

        content.addView(
            addText("إنجازاتي", 28f, gold, true)
        )

        val count = prefs.getInt("streak_count", 0)
        val lastDate = prefs.getString("last_checkin", "")

        val card = addCard()

        card.addView(
            addText("🔥", 45f, gold)
        )

        card.addView(
            addText(
                "$count يوم متتالي",
                25f,
                white,
                true
            )
        )

        card.addView(
            addText(
                "سجّل إنجازك اليومي بنفسك",
                15f,
                muted
            )
        )

        val today = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(Date())

        if (lastDate == today) {
            card.addView(
                addText(
                    "تم تسجيل إنجاز اليوم بالفعل ✓",
                    17f,
                    gold
                )
            )
        } else {
            addButton(card, "تسجيل إنجاز اليوم") {
                recordDailyStreak()
                showStreak()
            }
        }

        card.addView(
            addText(
                "الاستمرار أهم من الرقم، وكل يوم فرصة جديدة",
                14f,
                muted
            )
        )
    }

    private fun recordDailyStreak() {
        val today = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(Date())

        val last = prefs.getString("last_checkin", "")

        if (last == today) return

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
    }

    // التطبيقات المحظورة

    private fun addAppCheck(
        parent: LinearLayout,
        title: String,
        pkg: String
    ) {
        val check = CheckBox(this).apply {
            text = title
            setTextColor(white)

            isChecked = prefs.getStringSet(
                "blocked_apps",
                emptySet()
            )?.contains(pkg) == true

            setOnCheckedChangeListener { _, checked ->
                val set = prefs.getStringSet(
                    "blocked_apps",
                    emptySet()
                )?.toMutableSet() ?: mutableSetOf()

                if (checked) {
                    set.add(pkg)
                } else {
                    set.remove(pkg)
                }

                prefs.edit()
                    .putStringSet("blocked_apps", set)
                    .apply()
            }
        }

        parent.addView(check)
    }

    // الصلاحيات والموقع

    private fun requestNeededPermissions() {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= 33) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = list.filter {
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
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            updateStatus("اسمح للتطبيق بالوصول إلى الموقع أولاً.")
            requestNeededPermissions()
            return
        }

        updateStatus("جارٍ تحديد موقعك وجلب المواقيت...")

        try {
            val lm = getSystemService(LOCATION_SERVICE) as LocationManager

            val location: Location? = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER
            ).mapNotNull { provider ->
                try {
                    lm.getLastKnownLocation(provider)
                } catch (_: Exception) {
                    null
                }
            }.maxByOrNull { it.time }

            if (location == null) {
                updateStatus(
                    "تعذر الحصول على موقع. فعّل الموقع وحاول مرة أخرى."
                )
                return
            }

            fetchTimes(location.latitude, location.longitude)

        } catch (e: Exception) {
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
            try {
                val today = SimpleDateFormat(
                    "dd-MM-yyyy",
                    Locale.US
                ).format(Date())

                val url = URL(
                    "https://api.aladhan.com/v1/timings/$today?latitude=$lat&longitude=$lon&method=3"
                )

                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 15000
                    requestMethod = "GET"
                }

                val body = conn.inputStream.bufferedReader().use {
                    it.readText()
                }

                conn.disconnect()

                val root = JSONObject(body).getJSONObject("data")
                val timings = root.getJSONObject("timings")

                val names = listOf(
                    "Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"
                )

                val arabic = listOf(
                    "الفجر", "الظهر", "العصر", "المغرب", "العشاء"
                )

                val values = names.map {
                    timings.getString(it).substringBefore(" ")
                }

                val display = arabic.zip(values).joinToString("\n") {
                    "${it.first}   ${it.second}"
                }

                prefs.edit()
                    .putString("times", display)
                    .putString("date", today)
                    .putFloat("lat", lat.toFloat())
                    .putFloat("lon", lon.toFloat())
                    .apply()

                scheduleAlarms(names, values)

                runOnUiThread {
                    timesView?.text = display
                    statusView?.text = "تم تحديث مواقيت اليوم بنجاح."

                    if (timesView == null) {
                        showHome()
                    }
                }

            } catch (e: Exception) {
                updateStatus(
                    "تعذر جلب المواقيت. تحقق من الإنترنت وحاول مجدداً."
                )
            }
        }
    }

    private fun scheduleAlarms(
        names: List<String>,
        values: List<String>
    ) {
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

            PrayerAlarmReceiver.schedule(
                this,
                prayer.timeInMillis - 5 * 60 * 1000L,
                index * 2,
                "reminder",
                name
            )

            PrayerAlarmReceiver.schedule(
                this,
                prayer.timeInMillis,
                index * 2 + 1,
                "start",
                name
            )
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
