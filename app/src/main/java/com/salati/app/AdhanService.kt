

package com.salati.app

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder

class AdhanService : Service() {

    private var player: MediaPlayer? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (player == null) {
            player = MediaPlayer.create(
                applicationContext,
                R.raw.adhan
            )

            player?.setOnCompletionListener {
                stopSelf()
            }

            player?.start()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
