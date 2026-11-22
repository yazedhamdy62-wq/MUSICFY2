package com.example.widget

import android.app.Activity
import android.os.Bundle
import com.example.audio.PlaybackBridge
import com.example.data.shareSongFile

/**
 * Invisible activity started by the widget's share button: it opens the system share sheet for the
 * song that is playing and closes itself. (A share sheet can only be started from an Activity.)
 */
class WidgetShareActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val controller = PlaybackBridge.controller ?: PlaybackBridge.getOrInitController(this)
            val song = controller.currentSong.value
            if (song.id != "none" && song.id != "empty_state") {
                shareSongFile(this, song)
            }
        } catch (_: Exception) {
        }
        finish()
    }
}
