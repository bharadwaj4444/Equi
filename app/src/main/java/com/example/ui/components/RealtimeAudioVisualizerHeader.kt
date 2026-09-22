package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BandUiModel
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.RedClip
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioTrackBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon
import kotlin.math.sin

/**
 * Real-Time Waveform Visualizer & Audio Level Meter Header.
 *
 * Replaces the static audio session indicator with an interactive studio-grade
 * real-time waveform display, stereo VU output level meters, and clear live
 * feedback for system and audition audio output.
 */
@Composable
fun RealtimeAudioVisualizerHeader(
    activeSessionName: String,
    isLowLatency: Boolean,
    bufferFrames: Int,
    isMasterEnabled: Boolean,
    isPlaying: Boolean,
    amplitudes: List<Float>,
    waveformPoints: List<Float>,
    leftOutputLevel: Float,
    rightOutputLevel: Float,
    outputDb: Float,
    bands: List<BandUiModel> = emptyList(),
    onSwitchToSystem: () -> Unit,
    onSwitchToAudition: () -> Unit,
    onToggleAudition: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rt_header_anim")
    val scanPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_phase"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("realtime_waveform_visualizer_header"),
        shape = RoundedCornerShape(16.dp),
        color = StudioCardBg,
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Parse session title and optional tag (e.g. "System Audio Mix (Session 0)")
            val sessionRegex = Regex("""^(.*?)\s*\((.*?)\)$""")
            val matchResult = sessionRegex.find(activeSessionName.trim())
            val sessionTitle = matchResult?.groupValues?.get(1)?.trim() ?: activeSessionName
            val sessionTag = matchResult?.groupValues?.get(2)?.trim() ?: ""

            // Top Status Bar: Session Details, Digital Output dB Readout, Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Rock-Solid LIVE / READY / OFF Status Pill + Session Information
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Dedicated Status Pill (Properly positioned, prominent, always visible)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            !isMasterEnabled -> StudioTrackBg
                            isPlaying -> EmeraldNeon.copy(alpha = 0.18f)
                            else -> CyanNeon.copy(alpha = 0.12f)
                        },
                        border = BorderStroke(
                            1.dp,
                            when {
                                !isMasterEnabled -> StudioBorder
                                isPlaying -> EmeraldNeon
                                else -> CyanNeon.copy(alpha = 0.5f)
                            }
                        ),
                        modifier = Modifier.testTag("status_live_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            !isMasterEnabled -> TextMuted
                                            isPlaying -> EmeraldNeon
                                            else -> CyanNeon
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    !isMasterEnabled -> "OFF"
                                    isPlaying -> "LIVE"
                                    else -> "READY"
                                },
                                color = when {
                                    !isMasterEnabled -> TextMuted
                                    isPlaying -> EmeraldNeon
                                    else -> CyanNeon
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Session Name & Hardware Specifications Column
                    Column(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = sessionTitle,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (sessionTag.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = StudioCardElevated,
                                    border = BorderStroke(0.5.dp, StudioBorder)
                                ) {
                                    Text(
                                        text = sessionTag,
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = if (activeSessionName.contains("System")) {
                                "Direct DSP • 48 kHz • Hardware Mix"
                            } else {
                                "Dedicated Thread • Ultra-Low Latency (<12ms)"
                            },
                            color = TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right: Output Level Badge + Quick Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Numerical Output Level Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            !isMasterEnabled -> StudioTrackBg
                            outputDb > -1.5f && isPlaying -> RedClip.copy(alpha = 0.2f)
                            isPlaying -> EmeraldNeon.copy(alpha = 0.15f)
                            else -> StudioCardElevated
                        },
                        border = BorderStroke(
                            0.5.dp,
                            when {
                                !isMasterEnabled -> StudioBorder
                                outputDb > -1.5f && isPlaying -> RedClip
                                isPlaying -> EmeraldNeon
                                else -> StudioBorder
                            }
                        ),
                        modifier = Modifier.testTag("output_db_badge")
                    ) {
                        Text(
                            text = when {
                                !isMasterEnabled -> "DSP OFF"
                                isPlaying -> if (outputDb > -1.5f) "CLIP 0dB" else String.format("%.1f dB", outputDb)
                                else -> "MONITOR"
                            },
                            color = when {
                                !isMasterEnabled -> TextMuted
                                outputDb > -1.5f && isPlaying -> RedClip
                                isPlaying -> EmeraldNeon
                                else -> CyanNeon
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Quick Session Switch Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StudioCardElevated,
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (activeSessionName.contains("System")) {
                                    onSwitchToAudition()
                                } else {
                                    onSwitchToSystem()
                                }
                            }
                            .testTag("session_switch_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Switch Session",
                                tint = CyanNeon,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (activeSessionName.contains("System")) "Audition" else "System",
                                color = CyanNeon,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Audition Play/Stop Action Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isPlaying) RedClip.copy(alpha = 0.2f) else CyanNeon.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (isPlaying) RedClip else CyanNeon.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleAudition() }
                            .testTag("visualizer_audition_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Stop" else "Play",
                                tint = if (isPlaying) RedClip else CyanNeon,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Visualizer Screen: Real-Time Oscilloscope Waveform & Dual Stereo VU Meters
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianDark)
                    .border(BorderStroke(1.dp, StudioBorder), RoundedCornerShape(10.dp))
                    .padding(6.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Canvas: Oscilloscope Waveform & EQ Contour Grid
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val midY = h * 0.5f

                            // 1. Oscilloscope Grid lines & reference marks
                            val gridColor = StudioBorder.copy(alpha = 0.35f)
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                            // Horizontal dB grid lines: +3dB, 0dB, -6dB, -18dB
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, h * 0.15f),
                                end = Offset(w, h * 0.15f),
                                strokeWidth = 1f,
                                pathEffect = dashEffect
                            )
                            drawLine(
                                color = gridColor.copy(alpha = 0.55f),
                                start = Offset(0f, midY),
                                end = Offset(w, midY),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, h * 0.85f),
                                end = Offset(w, h * 0.85f),
                                strokeWidth = 1f,
                                pathEffect = dashEffect
                            )

                            // Vertical subtle grid divisions
                            for (c in 1..5) {
                                val gx = w * (c / 6f)
                                drawLine(
                                    color = gridColor.copy(alpha = 0.25f),
                                    start = Offset(gx, 0f),
                                    end = Offset(gx, h),
                                    strokeWidth = 1f,
                                    pathEffect = dashEffect
                                )
                            }

                            if (isPlaying && isMasterEnabled && waveformPoints.isNotEmpty()) {
                                // 2. Real-Time Oscilloscope Waveform (Live Audio Synthesis)
                                val points = waveformPoints
                                val numPoints = points.size
                                val stepX = w / (numPoints - 1)

                                val wavePath = Path()
                                val fillPath = Path()

                                fillPath.moveTo(0f, midY)

                                for (i in 0 until numPoints) {
                                    val x = i * stepX
                                    val amp = points[i].coerceIn(-1f, 1f)
                                    val y = midY - amp * (h * 0.45f)

                                    if (i == 0) {
                                        wavePath.moveTo(x, y)
                                        fillPath.lineTo(x, y)
                                    } else {
                                        val prevX = (i - 1) * stepX
                                        val prevAmp = points[i - 1].coerceIn(-1f, 1f)
                                        val prevY = midY - prevAmp * (h * 0.45f)
                                        val cx = (prevX + x) / 2f
                                        wavePath.cubicTo(cx, prevY, cx, y, x, y)
                                        fillPath.cubicTo(cx, prevY, cx, y, x, y)
                                    }
                                }

                                fillPath.lineTo(w, midY)
                                fillPath.close()

                                // Glowing filled area underneath the waveform
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            CyanNeon.copy(alpha = 0.35f),
                                            VioletNeon.copy(alpha = 0.15f),
                                            Color.Transparent
                                        ),
                                        startY = 0f,
                                        endY = h
                                    )
                                )

                                // Crisp neon waveform line
                                val strokeColor = if (outputDb > -1.5f) RedClip else CyanNeon
                                drawPath(
                                    path = wavePath,
                                    color = strokeColor,
                                    style = Stroke(width = 2.2f)
                                )

                                // Peak glowing dots at extrema
                                for (i in 0 until numPoints step 4) {
                                    val x = i * stepX
                                    val amp = points[i]
                                    val y = midY - amp * (h * 0.45f)
                                    if (kotlin.math.abs(amp) > 0.35f) {
                                        drawCircle(
                                            color = if (amp > 0.8f || amp < -0.8f) RedClip else EmeraldNeon,
                                            radius = 2.5f,
                                            center = Offset(x, y)
                                        )
                                    }
                                }
                            } else if (isMasterEnabled) {
                                // 3. Calibrated System Mix Monitoring Curve (Reflects Active 10-Band EQ Curve & Headroom)
                                val bandCount = bands.size
                                val contourPath = Path()
                                val fillPath = Path()

                                fillPath.moveTo(0f, h)

                                if (bandCount > 1) {
                                    val stepX = w / (bandCount - 1)
                                    for (i in 0 until bandCount) {
                                        val x = i * stepX
                                        val db = bands[i].levelDb.coerceIn(-15f, 15f)
                                        // Map dB level to height: 0dB is at 60% height
                                        val y = (h * 0.58f) - (db / 15f) * (h * 0.32f)

                                        if (i == 0) {
                                            contourPath.moveTo(x, y)
                                            fillPath.lineTo(x, y)
                                        } else {
                                            val prevX = (i - 1) * stepX
                                            val prevDb = bands[i - 1].levelDb.coerceIn(-15f, 15f)
                                            val prevY = (h * 0.58f) - (prevDb / 15f) * (h * 0.32f)
                                            val cx = (prevX + x) / 2f
                                            contourPath.cubicTo(cx, prevY, cx, y, x, y)
                                            fillPath.cubicTo(cx, prevY, cx, y, x, y)
                                        }
                                    }
                                    fillPath.lineTo(w, h)
                                    fillPath.close()

                                    // Translucent fill for active EQ frequency contour
                                    drawPath(
                                        path = fillPath,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                CyanNeon.copy(alpha = 0.22f),
                                                VioletNeon.copy(alpha = 0.08f),
                                                Color.Transparent
                                            ),
                                            startY = 0f,
                                            endY = h
                                        )
                                    )

                                    drawPath(
                                        path = contourPath,
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(EmeraldNeon, CyanNeon, VioletNeon)
                                        ),
                                        style = Stroke(width = 1.8f)
                                    )
                                } else {
                                    // Steady resting baseline
                                    drawLine(
                                        color = CyanNeon.copy(alpha = 0.5f),
                                        start = Offset(0f, midY),
                                        end = Offset(w, midY),
                                        strokeWidth = 1.5f
                                    )
                                }

                                // Active radar scanner line across the waveform
                                val scanX = w * scanPhase
                                drawLine(
                                    color = CyanNeon.copy(alpha = 0.6f),
                                    start = Offset(scanX, 0f),
                                    end = Offset(scanX, h),
                                    strokeWidth = 1.5f
                                )
                            } else {
                                // Master Bypassed: Flat Dim Baseline
                                drawLine(
                                    color = TextMuted.copy(alpha = 0.4f),
                                    start = Offset(0f, midY),
                                    end = Offset(w, midY),
                                    strokeWidth = 1.5f,
                                    pathEffect = dashEffect
                                )
                            }
                        }

                        // Scope Header Watermarks (Aligned left & right)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopStart)
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPlaying) "OSCILLOSCOPE • 48 kHz REAL-TIME" else "EQ FREQUENCY RESPONSE",
                                color = TextSecondary.copy(alpha = 0.65f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = "0 dBFS REF",
                                color = TextSecondary.copy(alpha = 0.45f),
                                fontSize = 7.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right Column: Stereo L / R Output Level Meter (VU Ladder)
                    StereoVuLevelMeter(
                        leftLevel = if (isMasterEnabled) leftOutputLevel else 0f,
                        rightLevel = if (isMasterEnabled) rightOutputLevel else 0f,
                        isPlaying = isPlaying,
                        modifier = Modifier
                            .width(46.dp)
                            .fillMaxHeight()
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Footer: Frequency Distribution Micro-Bars (16 bands)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val ampList = if (isPlaying && isMasterEnabled) amplitudes else List(16) { 0.08f }
                for (b in 0 until 16) {
                    val amp = ampList.getOrElse(b) { 0.08f }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height((amp * 8).coerceIn(1.5f, 8f).dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(
                                when {
                                    !isMasterEnabled -> StudioTrackBg
                                    amp > 0.8f -> RedClip
                                    amp > 0.5f -> AmberGlow
                                    b < 5 -> EmeraldNeon
                                    b < 11 -> CyanNeon
                                    else -> VioletNeon
                                }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Frequency Scale Reference Labels for RTA Spectrum
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("32Hz", color = TextMuted, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                Text("125Hz", color = TextMuted, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                Text("500Hz", color = TextMuted, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                Text("2kHz", color = TextMuted, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                Text("8kHz", color = TextMuted, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                Text("16kHz", color = TextMuted, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

/**
 * Stereo Output Level Meter (Dual L / R Segmented VU Ladder).
 */
@Composable
private fun StereoVuLevelMeter(
    leftLevel: Float,
    rightLevel: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = StudioCardElevated.copy(alpha = 0.6f),
        border = BorderStroke(0.5.dp, StudioBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Channel
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                VuBarSegments(
                    level = leftLevel,
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "L",
                    color = if (isPlaying) CyanNeon else TextSecondary,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.width(3.dp))

            // Right Channel
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                VuBarSegments(
                    level = rightLevel,
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "R",
                    color = if (isPlaying) CyanNeon else TextSecondary,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/**
 * 8-Segment Vertical LED Bar for VU Meter.
 */
@Composable
private fun VuBarSegments(
    level: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val totalSegments = 8
    val activeCount = if (isPlaying) {
        (level * totalSegments).toInt().coerceIn(0, totalSegments)
    } else {
        1 // Calm resting single LED
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(1.5.dp, Alignment.Bottom)
    ) {
        for (i in totalSegments downTo 1) {
            val isActive = i <= activeCount
            val segmentColor = when {
                !isActive -> StudioTrackBg.copy(alpha = 0.5f)
                i >= 7 -> RedClip // Peak / Overload
                i >= 5 -> AmberGlow // Hot signal
                i >= 3 -> CyanNeon // Mid signal
                else -> EmeraldNeon // Normal level
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(0.8.dp))
                    .background(segmentColor)
            )
        }
    }
}
