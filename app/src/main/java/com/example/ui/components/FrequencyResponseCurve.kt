package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BandUiModel
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioTrackBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.VioletNeon
import kotlin.math.abs

@Composable
fun FrequencyResponseCurve(
    bands: List<BandUiModel>,
    isEnabled: Boolean,
    onBandAdjusted: (Short, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    var draggingBandIndex by remember { mutableStateOf<Short?>(null) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .testTag("frequency_response_curve"),
        shape = RoundedCornerShape(16.dp),
        color = StudioCardElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(bands, isEnabled) {
                        if (!isEnabled || bands.isEmpty()) return@pointerInput
                        detectDragGestures(
                            onDragStart = { offset ->
                                val w = size.width
                                val stepX = w / (bands.size + 1)
                                // Find closest band to touch
                                var closestIndex: Short? = null
                                var minDistance = Float.MAX_VALUE
                                bands.forEachIndexed { i, band ->
                                    val bx = stepX * (i + 1)
                                    val dist = abs(offset.x - bx)
                                    if (dist < minDistance && dist < stepX * 0.9f) {
                                        minDistance = dist
                                        closestIndex = band.index
                                    }
                                }
                                draggingBandIndex = closestIndex
                            },
                            onDragEnd = {
                                draggingBandIndex = null
                            },
                            onDragCancel = {
                                draggingBandIndex = null
                            },
                            onDrag = { change, _ ->
                                draggingBandIndex?.let { index ->
                                    change.consume()
                                    val band = bands.find { it.index == index } ?: return@let
                                    val h = size.height
                                    val topMargin = 12f
                                    val bottomMargin = 16f
                                    val effectiveH = h - topMargin - bottomMargin
                                    val normalizedY = ((change.position.y - topMargin) / effectiveH).coerceIn(0f, 1f)
                                    // 0 = maxDb (+15), 1 = minDb (-15)
                                    val newDb = band.maxDb - normalizedY * (band.maxDb - band.minDb)
                                    onBandAdjusted(index, newDb)
                                }
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height
                val topMargin = 12f
                val bottomMargin = 18f
                val effectiveH = h - topMargin - bottomMargin
                val zeroY = topMargin + effectiveH * 0.5f

                // Draw dB reference grid lines
                val dbLevels = listOf(15f, 10f, 5f, 0f, -5f, -10f, -15f)
                dbLevels.forEach { db ->
                    val y = topMargin + effectiveH * (1f - (db + 15f) / 30f)
                    val isZero = db == 0f
                    val strokeColor = if (isZero) StudioBorder.copy(alpha = 0.8f) else StudioTrackBg.copy(alpha = 0.45f)
                    val strokeWidth = if (isZero) 1.5f else 0.8f

                    drawLine(
                        color = strokeColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = strokeWidth
                    )

                    // Draw dB text label on the left margin
                    if (db % 5f == 0f) {
                        val labelText = if (db > 0) "+${db.toInt()}" else "${db.toInt()}"
                        val textLayoutResult = textMeasurer.measure(
                            text = labelText,
                            style = TextStyle(
                                color = if (isZero) CyanNeon.copy(alpha = 0.7f) else TextMuted,
                                fontSize = 9.sp
                            )
                        )
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(4f, y - textLayoutResult.size.height * 0.5f)
                        )
                    }
                }

                if (bands.isEmpty()) return@Canvas

                val stepX = w / (bands.size + 1)
                val points = mutableListOf<Offset>()

                // Anchor start at zero
                points.add(Offset(0f, zeroY))

                bands.forEachIndexed { i, band ->
                    val x = stepX * (i + 1)
                    val effectiveLevel = if (isEnabled) band.levelDb else 0f
                    val normLevel = (effectiveLevel - band.minDb) / (band.maxDb - band.minDb)
                    val y = topMargin + effectiveH * (1f - normLevel.coerceIn(0f, 1f))
                    points.add(Offset(x, y))
                }

                // Anchor end at zero
                points.add(Offset(w, zeroY))

                // Build smooth cubic spline path
                val curvePath = Path()
                val fillPath = Path()

                curvePath.moveTo(points[0].x, points[0].y)
                fillPath.moveTo(points[0].x, zeroY)
                fillPath.lineTo(points[0].x, points[0].y)

                for (i in 0 until points.size - 1) {
                    val p0 = if (i > 0) points[i - 1] else points[i]
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val p3 = if (i < points.size - 2) points[i + 2] else p2

                    val controlX1 = p1.x + (p2.x - p0.x) * 0.22f
                    val controlY1 = p1.y + (p2.y - p0.y) * 0.22f
                    val controlX2 = p2.x - (p3.x - p1.x) * 0.22f
                    val controlY2 = p2.y - (p3.y - p1.y) * 0.22f

                    curvePath.cubicTo(controlX1, controlY1, controlX2, controlY2, p2.x, p2.y)
                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p2.x, p2.y)
                }

                fillPath.lineTo(w, zeroY)
                fillPath.close()

                // Draw gradient under curve
                val gradientBrush = Brush.verticalGradient(
                    colors = if (isEnabled) listOf(
                        CyanNeon.copy(alpha = 0.35f),
                        VioletNeon.copy(alpha = 0.15f),
                        Color.Transparent
                    ) else listOf(
                        TextMuted.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h
                )
                drawPath(path = fillPath, brush = gradientBrush)

                // Glow path
                if (isEnabled) {
                    drawPath(
                        path = curvePath,
                        color = CyanNeon.copy(alpha = 0.3f),
                        style = Stroke(width = 8f, cap = StrokeCap.Round)
                    )
                }

                // Main stroke
                drawPath(
                    path = curvePath,
                    color = if (isEnabled) CyanNeon else TextMuted,
                    style = Stroke(width = 2.8f, cap = StrokeCap.Round)
                )

                // Draw node control points
                bands.forEachIndexed { i, band ->
                    val point = points[i + 1]
                    val isDragging = draggingBandIndex == band.index

                    // Vertical guide line
                    drawLine(
                        color = if (isEnabled) CyanNeon.copy(alpha = 0.2f) else StudioTrackBg,
                        start = Offset(point.x, topMargin),
                        end = Offset(point.x, h - bottomMargin),
                        strokeWidth = 1f
                    )

                    // Node outer ring
                    drawCircle(
                        color = if (isEnabled) {
                            if (isDragging) CyanNeon else VioletNeon
                        } else TextMuted,
                        radius = if (isDragging) 8.dp.toPx() else 5.dp.toPx(),
                        center = point
                    )
                    // Node inner center
                    drawCircle(
                        color = Color.White,
                        radius = if (isDragging) 4.dp.toPx() else 2.5.dp.toPx(),
                        center = point
                    )

                    // Bottom frequency tick label
                    val freqText = if (band.centerFreqHz >= 1000) "${band.centerFreqHz / 1000}k" else "${band.centerFreqHz}"
                    val labelResult = textMeasurer.measure(
                        text = freqText,
                        style = TextStyle(
                            color = if (isDragging) CyanNeon else TextMuted,
                            fontSize = 8.sp
                        )
                    )
                    drawText(
                        textLayoutResult = labelResult,
                        topLeft = Offset(point.x - labelResult.size.width * 0.5f, h - bottomMargin + 2f)
                    )
                }
            }
        }
    }
}
