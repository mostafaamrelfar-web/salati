
package com.salati.app

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color

class LockActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var timer: TextView

    private val navy = Color.rgb(15, 25, 48)
    private val gold = Color.rgb(218, 181, 105)

    private val tick = object : Runnable {

        override fun run() {

            val until = getSharedPreferences(
                "salati",
                MODE_PRIVATE
            ).getLong("mode_until", 0L)

            val remaining = (
                (until - System.currentTimeMillis())
                    .coerceAtLeast(0L) / 1000L
            )

            if (remaining <= 0) {
                finish()
                return
            }

            val minutes = remaining / 60
            val seconds = remaining % 60

            timer.text = "متبقي $minutes:${seconds.toString().padStart(2, '0')}"

            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 32, 28, 32)
            setBackgroundColor(navy)
        }

        val title = TextView(this).apply {
            text = "حان وقت الصلاة"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(gold)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val subtitle = TextView(this).apply {
            text = "اترك هاتفك وخذ وقتك مع الصلاة"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(8, 18, 8, 18)
        }

        timer = TextView(this).apply {
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(gold)
            setPadding(8, 24, 8, 24)
        }

        val verse = TextView(this).apply {
            text = "﴿ وَأَقِمِ الصَّلَاةَ لِذِكْرِي ﴾\nسورة طه - الآية 14"
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(8, 25, 8, 25)
        }

        val emergency = Button(this).apply {
            text = "خروج للطوارئ"
            setOnClickListener {
                getSharedPreferences(
                    "salati",
                    MODE_PRIVATE
                ).edit()
                    .putLong("mode_until", 0L)
                    .apply()

                finish()
            }
        }

        box.addView(title)
        box.addView(subtitle)
        box.addView(timer)
        box.addView(verse)
        box.addView(emergency)

        setContentView(box)

        handler.post(tick)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }
}
