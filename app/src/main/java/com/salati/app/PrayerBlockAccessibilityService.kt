
package com.salati.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

class PrayerBlockAccessibilityService : AccessibilityService() {

    private var lastBlockedPackage: String? = null
    private var lastLaunchTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        if (
            event?.eventType !=
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            return
        }

        val packageName = event.packageName?.toString() ?: return

        if (packageName == this.packageName) {
            return
        }

        val prefs = getSharedPreferences("salati", MODE_PRIVATE)

        val activeUntil = prefs.getLong("mode_until", 0L)

        if (System.currentTimeMillis() >= activeUntil) {
            lastBlockedPackage = null
            return
        }

        val blockedApps = prefs.getStringSet(
            "blocked_apps",
            emptySet()
        ) ?: emptySet()

        if (!blockedApps.contains(packageName)) {
            lastBlockedPackage = null
            return
        }

        val now = SystemClock.elapsedRealtime()

        if (
            lastBlockedPackage == packageName &&
            now - lastLaunchTime < 1500
        ) {
            return
        }

        lastBlockedPackage = packageName
        lastLaunchTime = now

        try {
            val lockIntent = Intent(
                this,
                LockActivity::class.java
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra("blocked_package", packageName)
            }

            startActivity(lockIntent)

        } catch (_: Exception) {
            lastBlockedPackage = null
        }
    }

    override fun onInterrupt() {
        lastBlockedPackage = null
    }
}
