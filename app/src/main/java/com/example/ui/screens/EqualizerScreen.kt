package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.TestAudioMode
import com.example.data.model.PresetEntity
import com.example.ui.EqualizerUiState
import com.example.ui.EqualizerViewModel
import com.example.ui.components.ActivePlaybackNotificationWindow
import com.example.ui.components.EffectKnobCard
import com.example.ui.components.FrequencyResponseCurve
import com.example.ui.components.RealtimeAudioVisualizerHeader
import com.example.ui.components.SavePresetDialog
import com.example.ui.components.VerticalFaderSlider
import com.example.ui.widget.WidgetActivity
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: EqualizerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val presets by viewModel.allPresets.collectAsStateWithLifecycle()

    var showSaveDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("equalizer_main_screen"),
        containerColor = ObsidianDark,
        topBar = {
            TopStudioAppBar(
                isEnabled = uiState.isMasterEnabled,
                onToggleEnabled = { viewModel.setMasterEnabled(!uiState.isMasterEnabled) },
                onResetFlat = { viewModel.resetToFlat() },
                onOpenSave = { showSaveDialog = true }
            )
        },
        bottomBar = {
            StudioBottomNavigationBar(
                selectedIndex = selectedTabIndex,
                onSelectTab = { selectedTabIndex = it }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Real-Time Waveform Visualizer & Audio Level Meter Header
            RealtimeAudioVisualizerHeader(
                activeSessionName = uiState.activeSessionName,
                isLowLatency = uiState.lowLatencyMode,
                bufferFrames = uiState.lowLatencyBufferFrames,
                isMasterEnabled = uiState.isMasterEnabled,
                isPlaying = uiState.isAuditionPlaying,
                amplitudes = uiState.visualizerAmplitudes,
                waveformPoints = uiState.waveformPoints,
                leftOutputLevel = uiState.leftOutputLevel,
                rightOutputLevel = uiState.rightOutputLevel,
                outputDb = uiState.outputDb,
                bands = uiState.bands,
                onSwitchToSystem = { viewModel.switchToSystemSession() },
                onSwitchToAudition = { viewModel.switchToAuditionSession() },
                onToggleAudition = { viewModel.toggleAuditionPlayback() }
            )

            // Preset Chip Carousel
            PresetChipCarousel(
                presets = presets,
                selectedPresetName = uiState.selectedPresetName,
                isEnabled = uiState.isMasterEnabled,
                onSelectPreset = { viewModel.applyPreset(it) },
                onDeletePreset = { viewModel.deletePreset(it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Main Content Tabs
            when (selectedTabIndex) {
                0 -> {
                    EqualizerTabContent(
                        uiState = uiState,
                        onBandLevelChanged = { index, db -> viewModel.setBandLevelDb(index, db) },
                        onStepBand = { index, delta -> viewModel.stepBandLevel(index, delta) }
                    )
                }
                1 -> {
                    EffectsTabContent(
                        uiState = uiState,
                        onBassBoostChanged = { viewModel.setBassBoost(it) },
                        onVirtualizerChanged = { viewModel.setVirtualizer(it) },
                        onLoudnessChanged = { viewModel.setLoudness(it) },
                        onStereoBalanceChanged = { viewModel.setStereoBalance(it) }
                    )
                }
                2 -> {
                    AuditionTabContent(
                        uiState = uiState,
                        onTogglePlayback = { viewModel.toggleAuditionPlayback() },
                        onSetMode = { viewModel.setAuditionMode(it) },
                        onSetSineFreq = { viewModel.setSineFrequency(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Playback Notification Window (Real-time spectrum & changeable equalizer effects)
            ActivePlaybackNotificationWindow(
                uiState = uiState,
                presets = presets,
                onTogglePlayback = { viewModel.toggleAuditionPlayback() },
                onToggleMasterEq = { viewModel.setMasterEnabled(!uiState.isMasterEnabled) },
                onApplyPreset = { viewModel.applyPreset(it) },
                onCycleBass = { viewModel.cycleBassBoost() },
                onCycleVirtualizer = { viewModel.cycleVirtualizer() },
                onSkipPrevious = { viewModel.skipPreviousAuditionMode() },
                onSkipNext = { viewModel.skipNextAuditionMode() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Floating Audition Test Bar (Quick Access)
            AuditionFloatingBar(
                isPlaying = uiState.isAuditionPlaying,
                mode = uiState.auditionMode,
                onTogglePlay = { viewModel.toggleAuditionPlayback() },
                onSkipPrevious = { viewModel.skipPreviousAuditionMode() },
                onSkipNext = { viewModel.skipNextAuditionMode() }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showSaveDialog) {
        SavePresetDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                viewModel.saveCustomPreset(name)
                showSaveDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopStudioAppBar(
    isEnabled: Boolean,
    onToggleEnabled: () -> Unit,
    onResetFlat: () -> Unit,
    onOpenSave: () -> Unit
) {
    Surface(
        color = ObsidianDark,
        border = BorderStroke(0.dp, Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StudioCardElevated,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Equalizer",
                            tint = if (isEnabled) CyanNeon else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "EQUALIZER",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isEnabled) CyanNeon.copy(alpha = 0.15f) else StudioTrackBg
                        ) {
                            Text(
                                text = "DSP",
                                color = if (isEnabled) CyanNeon else TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = if (isEnabled) "Active Studio Engine" else "Bypassed",
                        color = if (isEnabled) EmeraldNeon else TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Right Action Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Reset to Flat
                IconButton(
                    onClick = onResetFlat,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(StudioCardElevated)
                        .testTag("reset_flat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Flat",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Save Preset
                IconButton(
                    onClick = onOpenSave,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(StudioCardElevated)
                        .testTag("open_save_preset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Save Preset",
                        tint = CyanNeon,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Launch Floating Widget Activity
                val context = LocalContext.current
                IconButton(
                    onClick = {
                        val intent = Intent(context, WidgetActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(StudioCardElevated)
                        .testTag("launch_widget_activity_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureInPictureAlt,
                        contentDescription = "Launch Widget Activity",
                        tint = AmberGlow,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Master Power Bypass Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isEnabled) CyanNeon.copy(alpha = 0.15f) else StudioCardElevated,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isEnabled) CyanNeon else StudioBorder
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleEnabled() }
                        .testTag("master_bypass_switch")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Power",
                            tint = if (isEnabled) CyanNeon else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEnabled) "ON" else "OFF",
                            color = if (isEnabled) CyanNeon else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PresetChipCarousel(
    presets: List<PresetEntity>,
    selectedPresetName: String,
    isEnabled: Boolean,
    onSelectPreset: (PresetEntity) -> Unit,
    onDeletePreset: (PresetEntity) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(presets, key = { it.id }) { preset ->
            val isSelected = preset.name.equals(selectedPresetName, ignoreCase = true)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) CyanNeon.copy(alpha = 0.18f) else StudioCardBg,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) CyanNeon else StudioBorder
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = isEnabled) { onSelectPreset(preset) }
                    .testTag("preset_chip_${preset.name}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CyanNeon)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = preset.name,
                        color = if (isSelected) CyanNeon else if (isEnabled) TextPrimary else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )

                    // If custom preset, show small delete icon
                    if (!preset.isDefault) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete preset",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onDeletePreset(preset) }
                                .testTag("delete_preset_${preset.name}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EqualizerTabContent(
    uiState: EqualizerUiState,
    onBandLevelChanged: (Short, Float) -> Unit,
    onStepBand: (Short, Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Dynamic Frequency Response Curve Graph
        FrequencyResponseCurve(
            bands = uiState.bands,
            isEnabled = uiState.isMasterEnabled,
            onBandAdjusted = onBandLevelChanged
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Multi-Band Vertical Faders Row Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "10-BAND ISO PRECISION EQUALIZER",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = CyanNeon.copy(alpha = 0.12f),
                border = BorderStroke(0.5.dp, CyanNeon.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "Refined 1/1 Octave",
                    color = CyanNeon,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.bands.forEach { band ->
                VerticalFaderSlider(
                    band = band,
                    isEnabled = uiState.isMasterEnabled,
                    onValueChange = { newDb -> onBandLevelChanged(band.index, newDb) },
                    onStepValue = { delta -> onStepBand(band.index, delta) },
                    modifier = Modifier.height(290.dp)
                )
            }
        }
    }
}

@Composable
fun EffectsTabContent(
    uiState: EqualizerUiState,
    onBassBoostChanged: (Float) -> Unit,
    onVirtualizerChanged: (Float) -> Unit,
    onLoudnessChanged: (Float) -> Unit,
    onStereoBalanceChanged: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "DSP AUDIO ENHANCEMENTS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )

        // Bass Boost
        EffectKnobCard(
            title = "Sub-Bass & Punch",
            subtitle = "Dynamic low-frequency harmonic boost",
            value = uiState.bassBoostPercent,
            valueRange = 0f..100f,
            valueFormatted = "${uiState.bassBoostPercent.toInt()}%",
            accentColor = CyanNeon,
            isEnabled = uiState.isMasterEnabled,
            onValueChange = onBassBoostChanged,
            testTag = "bass_boost_card"
        )

        // 3D Virtualizer
        EffectKnobCard(
            title = "3D Surround Virtualizer",
            subtitle = "Spatial acoustic stereo widening",
            value = uiState.virtualizerPercent,
            valueRange = 0f..100f,
            valueFormatted = "${uiState.virtualizerPercent.toInt()}%",
            accentColor = VioletNeon,
            isEnabled = uiState.isMasterEnabled,
            onValueChange = onVirtualizerChanged,
            testTag = "virtualizer_card"
        )

        // Loudness Enhancer
        EffectKnobCard(
            title = "Loudness Maximizer",
            subtitle = "Hardware-accelerated output gain",
            value = uiState.loudnessGainDb,
            valueRange = 0f..15f,
            valueFormatted = "+${String.format("%.1f", uiState.loudnessGainDb)} dB",
            accentColor = EmeraldNeon,
            isEnabled = uiState.isMasterEnabled,
            onValueChange = onLoudnessChanged,
            testTag = "loudness_card"
        )

        // Stereo Balance
        EffectKnobCard(
            title = "Stereo Pan Balance",
            subtitle = "Left / Right channel balance",
            value = uiState.stereoBalance,
            valueRange = -1f..1f,
            valueFormatted = when {
                uiState.stereoBalance < -0.05f -> "L ${(uiState.stereoBalance * -100).toInt()}%"
                uiState.stereoBalance > 0.05f -> "R ${(uiState.stereoBalance * 100).toInt()}%"
                else -> "Center"
            },
            accentColor = AmberGlow,
            isEnabled = uiState.isMasterEnabled,
            onValueChange = onStereoBalanceChanged,
            testTag = "stereo_balance_card"
        )
    }
}

@Composable
fun AuditionTabContent(
    uiState: EqualizerUiState,
    onTogglePlayback: () -> Unit,
    onSetMode: (TestAudioMode) -> Unit,
    onSetSineFreq: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "LOW-LATENCY AUDITION ENGINE",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )

        // Mode Selector Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = StudioCardElevated,
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Audition Audio Source",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select an audio pattern to immediately audition frequency adjustments in real-time with sub-10ms output latency.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        Triple(TestAudioMode.GROOVE_BEAT, "Synth Beat", "Drums & Chords"),
                        Triple(TestAudioMode.SPECTRUM_SWEEP, "Sweep", "20Hz - 20kHz"),
                        Triple(TestAudioMode.SINE_TONE, "Sine Wave", "Test Tone")
                    )

                    modes.forEach { (mode, title, sub) ->
                        val isSelected = uiState.auditionMode == mode
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CyanNeon.copy(alpha = 0.15f) else StudioCardBg,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) CyanNeon else StudioBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSetMode(mode) }
                                .testTag("audition_mode_${mode.name}")
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSelected) CyanNeon else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = sub,
                                    color = TextSecondary,
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // If Sine Wave is chosen, show interactive frequency slider
                if (uiState.auditionMode == TestAudioMode.SINE_TONE) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tone Frequency",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${uiState.sineFrequency.toInt()} Hz",
                            color = CyanNeon,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = uiState.sineFrequency,
                        onValueChange = onSetSineFreq,
                        valueRange = 40f..16000f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanNeon,
                            activeTrackColor = CyanNeon,
                            inactiveTrackColor = StudioTrackBg
                        ),
                        modifier = Modifier.testTag("sine_frequency_slider")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Play / Pause Master Audition Button
                Button(
                    onClick = onTogglePlayback,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isAuditionPlaying) RedClip else CyanNeon,
                        contentColor = ObsidianDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("audition_play_button")
                ) {
                    Icon(
                        imageVector = if (uiState.isAuditionPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (uiState.isAuditionPlaying) "Pause" else "Play",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isAuditionPlaying) "STOP AUDITION" else "START LOW-LATENCY AUDITION",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AuditionFloatingBar(
    isPlaying: Boolean,
    mode: TestAudioMode,
    onTogglePlay: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        color = StudioCardBg,
        border = BorderStroke(1.dp, if (isPlaying) CyanNeon.copy(alpha = 0.5f) else StudioBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isPlaying) EmeraldNeon.copy(alpha = 0.2f) else StudioCardElevated,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = "Audition",
                            tint = if (isPlaying) EmeraldNeon else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isPlaying) "Real-Time DSP Active" else "Audition Test Audio",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = "Mode: ${mode.name.replace('_', ' ')}",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }

            // Transport Control Buttons (Skip Prev, Play/Pause, Skip Next)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(StudioCardElevated)
                        .testTag("floating_skip_previous_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Skip Previous",
                        tint = CyanNeon,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Button(
                    onClick = onTogglePlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) RedClip else CyanNeon,
                        contentColor = ObsidianDark
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("floating_play_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPlaying) "Stop" else "Play",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(StudioCardElevated)
                        .testTag("floating_skip_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip Next",
                        tint = CyanNeon,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StudioBottomNavigationBar(
    selectedIndex: Int,
    onSelectTab: (Int) -> Unit
) {
    NavigationBar(
        containerColor = StudioCardBg,
        contentColor = TextPrimary,
        tonalElevation = 6.dp
    ) {
        val items = listOf(
            Triple(0, "Equalizer", Icons.Default.Tune),
            Triple(1, "Effects", Icons.Default.SettingsInputComponent),
            Triple(2, "Audition", Icons.Default.Hearing)
        )

        items.forEach { (index, label, icon) ->
            val isSelected = selectedIndex == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectTab(index) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) CyanNeon else TextSecondary
                    )
                },
                label = {
                    Text(
                        text = label,
                        color = if (isSelected) CyanNeon else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = CyanNeon.copy(alpha = 0.15f)
                ),
                modifier = Modifier.testTag("bottom_nav_$label")
            )
        }
    }
}
