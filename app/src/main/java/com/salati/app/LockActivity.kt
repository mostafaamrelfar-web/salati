package com.salati.app

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity

class LockActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timer: TextView
    private val tick = object : Runnable {
        override fun run() {
            val until = getSharedPreferences("salati", MODE_PRIVATE).getLong("mode_until", 0L)
            val remaining = ((until - System.currentTimeMillis()).coerceAtLeast(0L) / 1000L)
            if (remaining <= 0) { finish(); return }
            timer.text = "متبقي ${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')}"
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32); setBackgroundColor(0xFF102B35.toInt())
        }
        val title = TextView(this).apply {
            text = "حان وقت الصلاة"; textSize = 28f; gravity = Gravity.CENTER
            setTextColor(0xFFE5C77A.toInt())
        }
        timer = TextView(this).apply {
            textSize = 20f; gravity = Gravity.CENTER; setTextColor(-1); setPadding(8, 24, 8, 24)
        }
        val verse = TextView(this).apply {
            text = "﴿ وَأَقِمِ الصَّلَاةَ لِذِكْرِي ﴾\nطه: 14"
            textSize = 22f; gravity = Gravity.CENTER; setTextColor(-1); setPadding(8, 20, 8, 20)
        }
        val emergency = Button(this).apply {
            text = "خروج للطوارئ"
            setOnClickListener {
                getSharedPreferences("salati", MODE_PRIVATE).edit().putLong("mode_until", 0L).apply()
                finish()
            }
        }
        box.addView(title); box.addView(timer); box.addView(verse); box.addView(emergency)
        setContentView(box)
        handler.post(tick)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }
}
