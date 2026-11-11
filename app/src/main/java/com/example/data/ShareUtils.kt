package com.example.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Shares the actual audio file of a song (WhatsApp, Telegram, Bluetooth...).
 * Falls back to sharing the song name as text when the song has no file URI.
 */
fun shareSongFile(context: Context, song: Song) {
    val intent = Intent(Intent.ACTION_SEND)
    val uriString = song.contentUri

    if (!uriString.isNullOrBlank()) {
        val uri = Uri.parse(uriString)
        val mime = try {
            context.contentResolver.getType(uri)
        } catch (e: Exception) {
            null
        }
        intent.type = mime ?: "audio/*"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.clipData = ClipData.newRawUri(song.title, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } else {
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, "${song.title} - ${song.artist}")
    }
    intent.putExtra(Intent.EXTRA_TITLE, song.title)

    try {
        context.startActivity(Intent.createChooser(intent, song.title))
    } catch (e: Exception) {
        // No app available to handle the share
    }
}
