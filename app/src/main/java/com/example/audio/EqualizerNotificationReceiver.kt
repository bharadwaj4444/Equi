package com.example.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class EqualizerNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TOGGLE_EQ = "com.example.action.TOGGLE_EQ"
        const val ACTION_NEXT_PRESET = "com.example.action.NEXT_PRESET"
        const val ACTION_CYCLE_BASS = "com.example.action.CYCLE_BASS"
        const val ACTION_TOGGLE_PLAY = "com.example.action.TOGGLE_PLAY"
        const val ACTION_STOP = "com.example.action.STOP"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE_EQ -> {
                EqualizerActionBridge.emitAction(EqualizerNotificationAction.TOGGLE_EQ)
            }
            ACTION_NEXT_PRESET -> {
                EqualizerActionBridge.emitAction(EqualizerNotificationAction.NEXT_PRESET)
            }
            ACTION_CYCLE_BASS -> {
                EqualizerActionBridge.emitAction(EqualizerNotificationAction.CYCLE_BASS)
            }
            ACTION_TOGGLE_PLAY -> {
                EqualizerActionBridge.emitAction(EqualizerNotificationAction.TOGGLE_PLAY)
            }
            ACTION_STOP -> {
                EqualizerActionBridge.emitAction(EqualizerNotificationAction.STOP)
            }
        }
    }
}
