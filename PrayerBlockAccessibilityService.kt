package com.salati.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class PrayerBlockAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        val prefs = getSharedPreferences("salati", MODE_PRIVATE)
        val activeUntil = prefs.getLong("mode_until", 0L)
        val blocked = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()
        if (System.currentTimeMillis() < activeUntil && blocked.contains(pkg)) {
            try {
                startActivity(Intent(this, LockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            } catch (_: Exception) { }
        }
    }

    override fun onInterrupt() {}
}
