package com.example.audio

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object EqualizerNotificationManager {

    const val CHANNEL_ID = "equalizer_playback_channel"
    private const val CHANNEL_NAME = "Audio Playback & Equalizer"
    const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time spectrum visualizer and changeable equalizer effects during audio playback"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showOrUpdateNotification(
        context: Context,
        isPlaying: Boolean,
        presetName: String,
        isEqEnabled: Boolean,
        bassPercent: Float,
        amplitudes: List<Float>,
        modeName: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        createNotificationChannel(context)

        // Generate spectrum bitmap for the notification window
        val spectrumBitmap = SpectrumBitmapRenderer.renderSpectrumBitmap(
            amplitudes = amplitudes,
            presetName = presetName,
            isEqEnabled = isEqEnabled,
            bassBoostPercent = bassPercent,
            isPlaying = isPlaying
        )

        // Activity Intent to open the Equalizer screen when notification is tapped
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPI = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // PendingIntents for changeable equalizer effects
        val toggleEqIntent = Intent(context, EqualizerNotificationReceiver::class.java).apply {
            action = EqualizerNotificationReceiver.ACTION_TOGGLE_EQ
        }
        val toggleEqPI = PendingIntent.getBroadcast(
            context,
            1,
            toggleEqIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextPresetIntent = Intent(context, EqualizerNotificationReceiver::class.java).apply {
            action = EqualizerNotificationReceiver.ACTION_NEXT_PRESET
        }
        val nextPresetPI = PendingIntent.getBroadcast(
            context,
            2,
            nextPresetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cycleBassIntent = Intent(context, EqualizerNotificationReceiver::class.java).apply {
            action = EqualizerNotificationReceiver.ACTION_CYCLE_BASS
        }
        val cycleBassPI = PendingIntent.getBroadcast(
            context,
            3,
            cycleBassIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val togglePlayIntent = Intent(context, EqualizerNotificationReceiver::class.java).apply {
            action = EqualizerNotificationReceiver.ACTION_TOGGLE_PLAY
        }
        val togglePlayPI = PendingIntent.getBroadcast(
            context,
            4,
            togglePlayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedMode = modeName.replace('_', ' ')
        val notificationTitle = if (isPlaying) "Equalizer Studio • Active Playback" else "Equalizer Studio • Paused"
        val notificationSubtitle = "Preset: $presetName • $formattedMode • EQ: ${if (isEqEnabled) "ON" else "BYPASS"}"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_eq_notification)
            .setContentTitle(notificationTitle)
            .setContentText(notificationSubtitle)
            .setContentIntent(contentPI)
            .setLargeIcon(spectrumBitmap)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(spectrumBitmap)
                    .setSummaryText(notificationSubtitle)
            )
            .setOngoing(isPlaying)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Changeable Equalizer Effects action buttons
            .addAction(
                R.drawable.ic_eq_notification,
                if (isEqEnabled) "EQ: ON" else "EQ: BYPASS",
                toggleEqPI
            )
            .addAction(
                R.drawable.ic_eq_notification,
                "Preset: $presetName",
                nextPresetPI
            )
            .addAction(
                R.drawable.ic_eq_notification,
                "Bass: ${bassPercent.toInt()}%",
                cycleBassPI
            )
            .addAction(
                if (isPlaying) R.drawable.ic_pause_notif else R.drawable.ic_play_notif,
                if (isPlaying) "Pause" else "Play",
                togglePlayPI
            )

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, builder.build())
    }

    fun dismissNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }
}
