package com.example.audio

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class EqualizerNotificationAction {
    TOGGLE_EQ,
    NEXT_PRESET,
    CYCLE_BASS,
    CYCLE_VIRTUALIZER,
    TOGGLE_PLAY,
    STOP
}

object EqualizerActionBridge {

    private val _actions = MutableSharedFlow<EqualizerNotificationAction>(extraBufferCapacity = 8)
    val actions: SharedFlow<EqualizerNotificationAction> = _actions.asSharedFlow()

    fun emitAction(action: EqualizerNotificationAction) {
        _actions.tryEmit(action)
    }
}
