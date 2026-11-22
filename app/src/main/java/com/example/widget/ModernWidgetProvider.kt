package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.example.audio.PlaybackBridge

/**
 * Shared behaviour of the two new widgets. Every button press is a broadcast that ends up here.
 */
abstract class ModernWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        ModernWidgets.updateAll(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        // the widget was resized: its background must be drawn again at the new size
        ModernWidgets.updateAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val handled = when (intent.action) {
            ModernWidgets.ACTION_PLAY_PAUSE -> {
                PlaybackBridge.onTogglePlayPause(context)
                true
            }
            ModernWidgets.ACTION_NEXT -> {
                PlaybackBridge.onNext(context)
                true
            }
            ModernWidgets.ACTION_PREV -> {
                PlaybackBridge.onPrevious(context)
                true
            }
            ModernWidgets.ACTION_LIKE -> {
                PlaybackBridge.onToggleLike(context)
                true
            }
            ModernWidgets.ACTION_REPLAY -> {
                // start the current song again (and play it if it was paused)
                PlaybackBridge.onSeekTo(0L, context)
                val playing = PlaybackBridge.controller?.isPlaying?.value ?: false
                if (!playing) PlaybackBridge.onPlay(context)
                true
            }
            ModernWidgets.ACTION_SEEK -> {
                val fraction = intent.getFloatExtra(ModernWidgets.EXTRA_FRACTION, 0f)
                PlaybackBridge.onSeekToFraction(fraction, context)
                true
            }
            else -> false
        }

        if (handled) {
            val pending = goAsync()
            // refreshes the old widget too, and (through its hook) these two
            MusicAppWidgetProvider.updateAllWidgets(context)
            ModernWidgets.whenIdle { pending?.finish() }
        }
    }
}

/** Wide liquid-glass widget. */
class MusicGlassWidgetProvider : ModernWidgetProvider()

/** Square cover-art widget. */
class MusicCoverWidgetProvider : ModernWidgetProvider()
