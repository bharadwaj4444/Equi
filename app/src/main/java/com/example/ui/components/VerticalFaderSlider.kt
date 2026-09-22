package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BandUiModel
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.RedClip
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioTrackBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon
import kotlin.math.roundToInt

@Composable
fun VerticalFaderSlider(
    band: BandUiModel,
    isEnabled: Boolean,
    onValueChange: (Float) -> Unit,
    onStepValue: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var trackHeightPx by remember { mutableFloatStateOf(1f) }

    Surface(
        modifier = modifier
            .width(66.dp)
            .clip(RoundedCornerShape(14.dp))
            .testTag("fader_band_${band.index}"),
        shape = RoundedCornerShape(14.dp),
        color = StudioCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: + Stepper button
            IconButton(
                onClick = { if (isEnabled) onStepValue(0.5f) },
                enabled = isEnabled,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(StudioCardElevated)
                    .testTag("fader_plus_${band.index}")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase frequency",
                    tint = if (isEnabled) CyanNeon else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            // dB Readout value
            val dbFormatted = if (band.levelDb > 0) "+${String.format("%.1f", band.levelDb)}"
            else String.format("%.1f", band.levelDb)

            val dbColor = when {
                !isEnabled -> TextMuted
                band.levelDb > 10f -> RedClip
                band.levelDb > 0f -> CyanNeon
                band.levelDb < -10f -> VioletNeon
                else -> TextPrimary
            }

            Text(
                text = "$dbFormatted\ndB",
                color = dbColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // Vertical Fader Track Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(42.dp)
                    .padding(vertical = 6.dp)
                    .onGloballyPositioned { coordinates ->
                        trackHeightPx = coordinates.size.height.toFloat()
                    }
                    .pointerInput(isEnabled, band) {
                        if (!isEnabled) return@pointerInput
                        detectTapGestures(
                            onDoubleTap = {
                                onValueChange(0f)
                            },
                            onTap = { offset ->
                                val norm = (offset.y / trackHeightPx).coerceIn(0f, 1f)
                                val newDb = band.maxDb - norm * (band.maxDb - band.minDb)
                                onValueChange(newDb)
                            }
                        )
                    }
                    .pointerInput(isEnabled, band) {
                        if (!isEnabled) return@pointerInput
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, _ ->
                                change.consume()
                                val norm = (change.position.y / trackHeightPx).coerceIn(0f, 1f)
                                val newDb = band.maxDb - norm * (band.maxDb - band.minDb)
                                onValueChange(newDb)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Background Track slot
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(StudioTrackBg)
                )

                // Zero Center Mark
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(2.dp)
                        .background(StudioBorder)
                )

                // Dynamic filled level indicator
                val normLevel = (band.levelDb - band.minDb) / (band.maxDb - band.minDb)
                val thumbYFraction = (1f - normLevel.coerceIn(0f, 1f))
                val zeroFraction = 0.5f

                // Level fill bar from zero to current position
                val fillHeightFraction = kotlin.math.abs(thumbYFraction - zeroFraction)
                val fillTopFraction = kotlin.math.min(thumbYFraction, zeroFraction)

                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight(fillHeightFraction)
                        .offset {
                            IntOffset(
                                x = 0,
                                y = (trackHeightPx * (fillTopFraction - 0.5f)).roundToInt()
                            )
                        }
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isEnabled) {
                                Brush.verticalGradient(
                                    listOf(CyanNeon, VioletNeon)
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(TextMuted, TextMuted)
                                )
                            }
                        )
                )

                // Tactile Fader Thumb Knob
                val thumbOffsetPx = (thumbYFraction - 0.5f) * (trackHeightPx - 32.dp.value)
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, thumbOffsetPx.roundToInt()) }
                        .size(width = 38.dp, height = 24.dp)
                        .shadow(4.dp, RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isEnabled) StudioCardElevated else StudioCardBg
                        )
                        .border(
                            width = 1.2.dp,
                            color = if (isEnabled) CyanNeon else StudioBorder,
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Center LED marker on the knob
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(if (isEnabled) CyanNeon else TextMuted)
                    )
                }
            }

            // Bottom: - Stepper button
            IconButton(
                onClick = { if (isEnabled) onStepValue(-0.5f) },
                enabled = isEnabled,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(StudioCardElevated)
                    .testTag("fader_minus_${band.index}")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease frequency",
                    tint = if (isEnabled) VioletNeon else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Frequency label (e.g. 60 Hz, 14 kHz)
            Text(
                text = band.freqLabel,
                color = if (isEnabled) TextPrimary else TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // Category tag (e.g. Sub, Bass, Mid)
            Text(
                text = band.bandCategory,
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
