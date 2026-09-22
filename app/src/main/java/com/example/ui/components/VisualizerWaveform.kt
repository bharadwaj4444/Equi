package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.RedClip
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioTrackBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.VioletNeon
import kotlin.math.sin

@Composable
fun VisualizerWaveform(
    amplitudes: List<Float>,
    isPlaying: Boolean,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vis_anim")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_phase"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .testTag("audio_visualizer_waveform"),
        shape = RoundedCornerShape(12.dp),
        color = StudioCardElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val barCount = 24
                val totalSpacing = (barCount - 1) * 3.5f
                val barWidth = (w - totalSpacing) / barCount

                for (i in 0 until barCount) {
                    val x = i * (barWidth + 3.5f)

                    // Compute dynamic height
                    val baseAmp = if (amplitudes.isNotEmpty()) {
                        val ampIdx = (i * amplitudes.size / barCount).coerceIn(0, amplitudes.size - 1)
                        amplitudes[ampIdx]
                    } else 0.1f

                    val normalizedHeight = if (isPlaying && isEnabled) {
                        val dynamicMultiplier = (1.0f + 0.35f * sin(pulseAnim + i * 0.45f)).toFloat()
                        (baseAmp * dynamicMultiplier).coerceIn(0.06f, 0.96f)
                    } else if (isEnabled) {
                        // Completely calm and stable baseline without artificial fluctuation
                        0.08f
                    } else {
                        0.04f
                    }
                    val barHeight = h * normalizedHeight
                    val y = h - barHeight

                    val barBrush = if (isEnabled) {
                        if (normalizedHeight > 0.85f) {
                            Brush.verticalGradient(
                                colors = listOf(RedClip, VioletNeon, CyanNeon),
                                startY = y,
                                endY = h
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(VioletNeon, CyanNeon, EmeraldNeon),
                                startY = y,
                                endY = h
                            )
                        }
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(TextMuted, StudioTrackBg),
                            startY = y,
                            endY = h
                        )
                    }

                    // Draw rounded bar
                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth * 0.5f, barWidth * 0.5f)
                    )

                    // Draw peak floating LED indicator
                    if (isEnabled && isPlaying && normalizedHeight > 0.2f) {
                        val peakY = (y - 3.dp.toPx()).coerceAtLeast(0f)
                        drawRoundRect(
                            color = if (normalizedHeight > 0.85f) RedClip else CyanNeon,
                            topLeft = Offset(x, peakY),
                            size = Size(barWidth, 2.dp.toPx()),
                            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}
