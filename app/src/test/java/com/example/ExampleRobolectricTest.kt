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
}
