package com.example.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.util.Log

class AudioEffectSessionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
        val packageName = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME) ?: "External Media Player"

        Log.d("AudioSessionReceiver", "Received action: $action, session: $sessionId, pkg: $packageName")

        if (sessionId == -1) return

        val manager = AudioSessionManager.getInstance(context)

        when (action) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                // Attach equalizer to the opened audio session
                manager.attachSession(sessionId, packageName)
            }
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                // Fall back to system audio mix
                manager.attachSession(0, "System Audio Mix (Session 0)")
            }
        }
    }
}
