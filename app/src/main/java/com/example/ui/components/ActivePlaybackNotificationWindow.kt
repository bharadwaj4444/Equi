package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.TestAudioMode
import com.example.data.model.PresetEntity
import com.example.ui.EqualizerUiState
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.RedClip
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioTrackBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon
import kotlin.math.max

/**
 * An interactive notification window showing real-time audio spectrum
 * and changeable equalizer effects that activates whenever there is audio playback.
 */
@Composable
fun ActivePlaybackNotificationWindow(
    uiState: EqualizerUiState,
    presets: List<PresetEntity>,
    onTogglePlayback: () -> Unit,
    onToggleMasterEq: () -> Unit,
    onApplyPreset: (PresetEntity) -> Unit,
    onCycleBass: () -> Unit,
    onCycleVirtualizer: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible = uiState.isAuditionPlaying,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "beacon_transition")
        val beaconAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "beacon_alpha"
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioCardElevated.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f)),
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .animateContentSize()
                .testTag("notification_window")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Header Bar with Live Indicator, Track Info, Expand/Collapse & Dismiss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Live Beacon Dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(EmeraldNeon.copy(alpha = beaconAlpha))
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AUDIO PLAYBACK ACTIVE",
                                    color = EmeraldNeon,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = StudioTrackBg,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = "48 kHz DSP",
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Source: ${uiState.auditionMode.name.replace('_', ' ')} • Preset: ${uiState.selectedPresetName}",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }

                    // Window Action Buttons: Expand/Collapse & Close/Stop
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("notification_window_toggle_expand")
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse window" else "Expand window",
                                tint = CyanNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onTogglePlayback,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(StudioTrackBg)
                                .testTag("notification_window_close")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Stop playback",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real-Time Animated Micro-Spectrum Visualizer
                LiveMicroSpectrum(
                    amplitudes = uiState.visualizerAmplitudes,
                    isEqEnabled = uiState.isMasterEnabled,
                    heightDp = if (isExpanded) 48 else 28,
                    modifier = Modifier.testTag("notification_window_spectrum")
                )

                if (isExpanded) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Changeable Equalizer Preset Chips Carousel
                    Text(
                        text = "CHANGE EQUALIZER PRESET",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { preset ->
                            val isSelected = uiState.selectedPresetName == preset.name
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CyanNeon.copy(alpha = 0.2f) else StudioTrackBg,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) CyanNeon else StudioBorder
                                ),
                                modifier = Modifier
                                    .clickable { onApplyPreset(preset) }
                                    .testTag("notification_window_preset_chip_${preset.name}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = CyanNeon,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = preset.name,
                                        color = if (isSelected) CyanNeon else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Changeable Effect Toggles (EQ Bypass, Bass Boost, 3D Virtualizer) & Audio Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Effect Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Master EQ Toggle Button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (uiState.isMasterEnabled) EmeraldNeon.copy(alpha = 0.15f) else StudioTrackBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (uiState.isMasterEnabled) EmeraldNeon else StudioBorder
                                ),
                                modifier = Modifier
                                    .clickable { onToggleMasterEq() }
                                    .testTag("notification_window_eq_toggle")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = if (uiState.isMasterEnabled) EmeraldNeon else TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (uiState.isMasterEnabled) "EQ: ON" else "BYPASS",
                                        color = if (uiState.isMasterEnabled) EmeraldNeon else TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Quick Bass Boost Stepper Chip
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (uiState.bassBoostPercent > 0) CyanNeon.copy(alpha = 0.15f) else StudioTrackBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (uiState.bassBoostPercent > 0) CyanNeon else StudioBorder
                                ),
                                modifier = Modifier
                                    .clickable { onCycleBass() }
                                    .testTag("notification_window_bass_cycle")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speaker,
                                        contentDescription = null,
                                        tint = if (uiState.bassBoostPercent > 0) CyanNeon else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Bass: ${uiState.bassBoostPercent.toInt()}%",
                                        color = if (uiState.bassBoostPercent > 0) CyanNeon else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Quick Virtualizer Stepper Chip
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (uiState.virtualizerPercent > 0) VioletNeon.copy(alpha = 0.2f) else StudioTrackBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (uiState.virtualizerPercent > 0) VioletNeon else StudioBorder
                                ),
                                modifier = Modifier
                                    .clickable { onCycleVirtualizer() }
                                    .testTag("notification_window_virtualizer_cycle")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SpatialAudio,
                                        contentDescription = null,
                                        tint = if (uiState.virtualizerPercent > 0) VioletNeon else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "3D: ${uiState.virtualizerPercent.toInt()}%",
                                        color = if (uiState.virtualizerPercent > 0) VioletNeon else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Compact Audio Transport Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = onSkipPrevious,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(StudioTrackBg)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous Mode",
                                    tint = CyanNeon,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Button(
                                onClick = onTogglePlayback,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RedClip,
                                    contentColor = ObsidianDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("notification_window_play_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Stop",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            IconButton(
                                onClick = onSkipNext,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(StudioTrackBg)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Mode",
                                    tint = CyanNeon,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Compact Mode Quick Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Preset: ${uiState.selectedPresetName} • Bass: ${uiState.bassBoostPercent.toInt()}% • EQ: ${if (uiState.isMasterEnabled) "ON" else "OFF"}",
                            color = CyanNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = onTogglePlayback,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RedClip,
                                    contentColor = ObsidianDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Stop", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * High-performance 16-band animated micro-spectrum canvas.
 */
@Composable
fun LiveMicroSpectrum(
    amplitudes: List<Float>,
    isEqEnabled: Boolean,
    heightDp: Int,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(StudioTrackBg.copy(alpha = 0.6f))
    ) {
        val barCount = 16
        val barAmps = if (amplitudes.size >= barCount) amplitudes.take(barCount) else List(barCount) { 0.05f }
        val spacing = 4.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = (size.width - totalSpacing) / barCount
        val maxH = size.height

        for (i in 0 until barCount) {
            val rawAmp = barAmps.getOrElse(i) { 0.05f }
            val normAmp = (rawAmp * 1.3f).coerceIn(0.08f, 1.0f)
            val barH = max(4.dp.toPx(), normAmp * maxH)
            val left = i * (barWidth + spacing)
            val top = size.height - barH

            val brush = Brush.verticalGradient(
                colors = if (isEqEnabled) {
                    listOf(EmeraldNeon, CyanNeon)
                } else {
                    listOf(TextSecondary, TextMuted)
                },
                startY = top,
                endY = size.height
            )

            // Draw Bar
            drawRoundRect(
                brush = brush,
                topLeft = Offset(left, top),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )

            // Draw Peak Cap
            val capColor = if (isEqEnabled) CyanNeon else TextSecondary
            drawRoundRect(
                color = capColor,
                topLeft = Offset(left, max(0f, top - 2.dp.toPx())),
                size = Size(barWidth, 2.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}
