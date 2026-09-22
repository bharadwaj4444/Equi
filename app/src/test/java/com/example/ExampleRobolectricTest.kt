package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Equalizer", appName)
  }

  @Test
  fun `test LowLatencyEngine skip modes`() {
    val engine = com.example.audio.LowLatencyEngine.getInstance()
    engine.setAudioMode(com.example.audio.TestAudioMode.GROOVE_BEAT)
    assertEquals(com.example.audio.TestAudioMode.GROOVE_BEAT, engine.audioMode.value)

    val next = engine.skipNextMode()
    assertEquals(com.example.audio.TestAudioMode.SPECTRUM_SWEEP, next)

    val prev = engine.skipPreviousMode()
    assertEquals(com.example.audio.TestAudioMode.GROOVE_BEAT, prev)
  }

  @Test
  fun `test SpectrumBitmapRenderer generates valid spectrum bitmap`() {
    val bitmap = com.example.audio.SpectrumBitmapRenderer.renderSpectrumBitmap(
      amplitudes = List(16) { 0.5f },
      presetName = "Rock & Metal",
      isEqEnabled = true,
      bassBoostPercent = 40f,
      isPlaying = true
    )
    org.junit.Assert.assertNotNull(bitmap)
    assertEquals(640, bitmap.width)
    assertEquals(220, bitmap.height)
  }

  @Test
  fun `test EqualizerActionBridge emission`() {
    com.example.audio.EqualizerActionBridge.emitAction(com.example.audio.EqualizerNotificationAction.TOGGLE_EQ)
    com.example.audio.EqualizerActionBridge.emitAction(com.example.audio.EqualizerNotificationAction.NEXT_PRESET)
    com.example.audio.EqualizerActionBridge.emitAction(com.example.audio.EqualizerNotificationAction.CYCLE_BASS)
  }

  @Test
  fun `test EqualizerNotificationManager channel creation and dismissal`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.audio.EqualizerNotificationManager.createNotificationChannel(context)
    com.example.audio.EqualizerNotificationManager.dismissNotification(context)
  }
}
