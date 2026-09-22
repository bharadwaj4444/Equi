package com.example.audio

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.max

object SpectrumBitmapRenderer {

    /**
     * Generates a high-fidelity visualizer spectrum bitmap designed for Android system notifications.
     */
    fun renderSpectrumBitmap(
        amplitudes: List<Float>,
        presetName: String,
        isEqEnabled: Boolean,
        bassBoostPercent: Float,
        isPlaying: Boolean,
        width: Int = 640,
        height: Int = 220
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background: Deep Obsidian studio dark
        val bgPaint = Paint().apply {
            color = 0xFF0D1017.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Subtle Grid Lines
        val gridPaint = Paint().apply {
            color = 0x22FFFFFF
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }
        for (i in 1..4) {
            val y = (height / 5f) * i
            canvas.drawLine(16f, y, (width - 16).toFloat(), y, gridPaint)
        }

        // Draw 16 Spectrum Bars
        val barCount = 16
        val barAmps = if (amplitudes.size >= barCount) amplitudes.take(barCount) else List(barCount) { 0.1f }
        val paddingHorizontal = 24f
        val availableWidth = width - (paddingHorizontal * 2)
        val barSpacing = 6f
        val barWidth = (availableWidth - (barSpacing * (barCount - 1))) / barCount
        val chartBottom = height - 36f
        val chartTop = 50f
        val maxBarHeight = chartBottom - chartTop

        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isPlaying) 0xFF00E5FF.toInt() else 0xFF64748B.toInt()
            style = Paint.Style.FILL
        }

        for (i in 0 until barCount) {
            val rawAmp = barAmps.getOrElse(i) { 0.05f }
            // Apply slight boost curve for visualization aesthetics
            val normAmp = (rawAmp * if (isPlaying) 1.2f else 0.2f).coerceIn(0.06f, 1.0f)
            val barH = max(8f, normAmp * maxBarHeight)
            val left = paddingHorizontal + i * (barWidth + barSpacing)
            val right = left + barWidth
            val top = chartBottom - barH

            // Neon Gradient Shader for each bar (Cyan -> Emerald / Violet)
            val topColor = if (isEqEnabled) 0xFF00E676.toInt() else 0xFF94A3B8.toInt()
            val bottomColor = if (isEqEnabled) 0xFF00E5FF.toInt() else 0xFF475569.toInt()
            barPaint.shader = LinearGradient(
                left, top, left, chartBottom,
                topColor, bottomColor,
                Shader.TileMode.CLAMP
            )

            // Draw bar
            val rect = RectF(left, top, right, chartBottom)
            canvas.drawRoundRect(rect, 4f, 4f, barPaint)

            // Draw Peak Cap
            val capRect = RectF(left, max(chartTop, top - 4f), right, top)
            canvas.drawRoundRect(capRect, 2f, 2f, capPaint)
        }

        // Text & Status Indicators
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF1F5F9.toInt()
            textSize = 22f
            isFakeBoldText = true
        }

        val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF00E5FF.toInt()
            textSize = 20f
            isFakeBoldText = true
        }

        // Draw Status Beacon
        val beaconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isPlaying) 0xFF00E676.toInt() else 0xFF64748B.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(32f, 30f, 6f, beaconPaint)

        val headerText = if (isPlaying) "REAL-TIME DSP SPECTRUM • 48 kHz" else "AUDIO ENGINE STANDBY"
        canvas.drawText(headerText, 46f, 36f, textPaint)

        // Draw Right Metadata: EQ Mode & Preset
        val eqTag = if (isEqEnabled) "EQ: ON" else "EQ: BYPASS"
        val presetTag = "$presetName | BASS: ${bassBoostPercent.toInt()}% | $eqTag"
        val presetWidth = subTextPaint.measureText(presetTag)
        canvas.drawText(presetTag, width - presetWidth - 24f, 36f, subTextPaint)

        // Bottom Frequency Labels
        val freqPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF64748B.toInt()
            textSize = 16f
        }
        canvas.drawText("31Hz", 24f, height - 12f, freqPaint)
        canvas.drawText("250Hz", width * 0.28f, height - 12f, freqPaint)
        canvas.drawText("1kHz", width * 0.50f, height - 12f, freqPaint)
        canvas.drawText("4kHz", width * 0.72f, height - 12f, freqPaint)
        canvas.drawText("16kHz", width - 70f, height - 12f, freqPaint)

        return bitmap
    }
}
