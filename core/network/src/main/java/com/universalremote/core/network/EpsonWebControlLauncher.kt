package com.universalremote.core.network

import android.content.Context
import android.content.Intent
import android.net.Uri

object EpsonWebControlLauncher {
    fun open(context: Context, host: String): Boolean {
        val parts = host.split('.')
        if (parts.size != 4 || parts.any { it.toIntOrNull() !in 0..255 }) return false
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://$host/")))
            true
        } catch (_: android.content.ActivityNotFoundException) { false }
    }
}
