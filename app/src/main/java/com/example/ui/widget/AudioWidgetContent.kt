package com.example.ui.widget

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.view.KeyEvent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.audio.AudioSessionManager
import com.example.audio.LowLatencyEngine
import com.example.audio.TestAudioMode
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.RedClip
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon
import kotlin.math.sin

/**
 * Floating / Mini-Player Widget Content.
 * Displays real-time audio waveform and responsive play, pause, and skip commands.
 */
@Composable
fun AudioWidgetContent(
    modifier: Modifier = Modifier,
    engine: LowLatencyEngine = LowLatencyEngine.getInstance(),
    onOpenFullApp: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val audioSessionManager = remember { AudioSessionManager.getInstance(context) }

    val isPlaying by engine.isPlaying.collectAsState()
    val audioMode by engine.audioMode.collectAsState()
    val waveformPoints by engine.waveformPoints.collectAsState()
    val amplitudes by engine.spectrumAmplitudes.collectAsState()
    val leftLevel by engine.leftOutputLevel.collectAsState()
    val rightLevel by engine.rightOutputLevel.collectAsState()
    val outputDb by engine.outputDb.collectAsState()

    // Standby oscilloscope ambient wave animation when paused
    val infiniteTransition = rememberInfiniteTransition(label = "standby_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Execute playback action without tearing down the global Session 0 equalizer
    fun handlePlayPause() {
        engine.togglePlayback()
    }

    fun handleSkipNext() {
        engine.skipNextMode()
        sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun handleSkipPrev() {
        engine.skipPreviousMode()
        sendMediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                BorderStroke(
                    1.dp,
                    if (isPlaying) CyanNeon.copy(alpha = 0.5f) else StudioBorder
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = CyanNeon),
        color = StudioCardBg,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Bar: Live Badge, Audio Mode Label, Full App launcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) EmeraldNeon else AmberGlow)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPlaying) "DSP ENGINE ACTIVE" else "STUDIO STANDBY",
                        color = if (isPlaying) EmeraldNeon else AmberGlow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Current Source Mode Pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StudioCardElevated,
                        border = BorderStroke(1.dp, StudioBorder)
                    ) {
                        Text(
                            text = when (audioMode) {
                                TestAudioMode.GROOVE_BEAT -> "Groove Beat"
                                TestAudioMode.SPECTRUM_SWEEP -> "Sweep 20-20k"
                                TestAudioMode.SINE_TONE -> "Sine 440Hz"
                            },
                            color = CyanNeon,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Open Full App Button
                    IconButton(
                        onClick = {
                            if (onOpenFullApp != null) {
                                onOpenFullApp()
                            } else {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(StudioCardElevated)
                            .testTag("widget_open_full_app_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Open Full Equalizer",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real-Time Waveform & Oscilloscope Canvas Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianDark)
                    .border(BorderStroke(1.dp, StudioBorder.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
                    .testTag("widget_realtime_waveform_box")
            ) {
                // Waveform rendering
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val midY = h / 2f

                    // Subtle horizontal grid reference lines
                    val gridAlpha = 0.25f
                    val gridColor = StudioBorder.copy(alpha = gridAlpha)
                    drawLine(gridColor, Offset(0f, midY), Offset(w, midY), strokeWidth = 1f)
                    drawLine(gridColor, Offset(0f, midY - h * 0.3f), Offset(w, midY - h * 0.3f), strokeWidth = 0.75f)
                    drawLine(gridColor, Offset(0f, midY + h * 0.3f), Offset(w, midY + h * 0.3f), strokeWidth = 0.75f)

                    val activePoints = if (isPlaying && waveformPoints.isNotEmpty()) {
                        waveformPoints
                    } else {
                        // Ambient resting harmonic wave
                        List(32) { i ->
                            val norm = i / 31f
                            val wave1 = sin(norm * 4 * Math.PI + phase).toFloat() * 0.18f
                            val wave2 = sin(norm * 8 * Math.PI - phase * 0.5f).toFloat() * 0.08f
                            wave1 + wave2
                        }
                    }

                    if (activePoints.size >= 2) {
                        val path = Path()
                        val stepX = w / (activePoints.size - 1)

                        val firstY = midY - (activePoints[0] * (h * 0.42f)).coerceIn(-midY + 4f, midY - 4f)
                        path.moveTo(0f, firstY)

                        for (i in 0 until activePoints.size - 1) {
                            val x0 = i * stepX
                            val y0 = midY - (activePoints[i] * (h * 0.42f)).coerceIn(-midY + 4f, midY - 4f)
                            val x1 = (i + 1) * stepX
                            val y1 = midY - (activePoints[i + 1] * (h * 0.42f)).coerceIn(-midY + 4f, midY - 4f)

                            val cx = (x0 + x1) / 2f
                            path.cubicTo(cx, y0, cx, y1, x1, y1)
                        }

                        // Gradient stroke line
                        val strokeBrush = Brush.horizontalGradient(
                            colors = if (isPlaying) {
                                listOf(CyanNeon, EmeraldNeon, CyanNeon)
                            } else {
                                listOf(CyanNeon.copy(alpha = 0.4f), VioletNeon.copy(alpha = 0.4f), CyanNeon.copy(alpha = 0.4f))
                            }
                        )

                        drawPath(
                            path = path,
                            brush = strokeBrush,
                            style = Stroke(width = if (isPlaying) 2.5f else 1.8f, cap = StrokeCap.Round)
                        )
                    }

                    // Base mini-spectrum equalizer micro-bars
                    val barCount = amplitudes.size.coerceAtMost(16)
                    val barWidth = (w - (barCount - 1) * 3f) / barCount
                    for (b in 0 until barCount) {
                        val amp = if (isPlaying) amplitudes[b] else 0.06f
                        val barHeight = (amp * (h * 0.38f)).coerceIn(2f, h * 0.5f)
                        val bx = b * (barWidth + 3f)
                        val by = h - barHeight

                        drawRoundRect(
                            color = if (isPlaying) CyanNeon.copy(alpha = 0.35f) else TextMuted.copy(alpha = 0.2f),
                            topLeft = Offset(bx, by),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                        )
                    }
                }

                // Digital Peak readout badge overlay
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ObsidianDark.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    val dbStr = if (isPlaying) {
                        if (outputDb > -1.5f) "CLIP 0dB" else String.format("%.1f dB", outputDb)
                    } else {
                        "OFF"
                    }
                    Text(
                        text = dbStr,
                        color = if (outputDb > -1.5f && isPlaying) RedClip else if (isPlaying) CyanNeon else TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Dual stereo level micro-ladder on left
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    StereoBar(channel = "L", level = if (isPlaying) leftLevel else 0f)
                    StereoBar(channel = "R", level = if (isPlaying) rightLevel else 0f)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Transport Control Deck: Skip Previous, Play/Pause, Skip Next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio Status Information
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPlaying) "Streaming 44.1kHz • 24-bit" else "Low-Latency Audio Ready",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = "Realtime Audio DSP • Equalizer Pipeline",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }

                // Command Buttons (Skip Prev, Play/Pause, Skip Next)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip Previous
                    IconButton(
                        onClick = { handleSkipPrev() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(StudioCardElevated)
                            .border(BorderStroke(1.dp, StudioBorder), CircleShape)
                            .testTag("widget_skip_previous_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Skip Previous Mode / Track",
                            tint = CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Play / Pause Master Button
                    Surface(
                        shape = CircleShape,
                        color = if (isPlaying) RedClip else CyanNeon,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .clickable { handlePlayPause() }
                            .testTag("widget_play_pause_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause Audio" else "Play Audio",
                                tint = ObsidianDark,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Skip Next
                    IconButton(
                        onClick = { handleSkipNext() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(StudioCardElevated)
                            .border(BorderStroke(1.dp, StudioBorder), CircleShape)
                            .testTag("widget_skip_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Skip Next Mode / Track",
                            tint = CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StereoBar(channel: String, level: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = channel,
            color = TextMuted,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(8.dp)
        )
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(StudioCardElevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(fraction = level.coerceIn(0f, 1f))
                    .background(
                        when {
                            level > 0.85f -> RedClip
                            level > 0.6f -> AmberGlow
                            else -> EmeraldNeon
                        }
                    )
            )
        }
    }
}

/**
 * Sends a system media key event so external media players (Spotify, YouTube Music, etc.)
 * can also respond to play/pause/skip when system audio mix is targeted.
 */
private fun sendMediaKeyEvent(context: Context, keyCode: Int) {
    try {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        audioManager.dispatchMediaKeyEvent(downEvent)
        audioManager.dispatchMediaKeyEvent(upEvent)
    } catch (_: Exception) {
        // Fallback silently if system prevents dispatch
    }
}
