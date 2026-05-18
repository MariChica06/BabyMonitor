package com.example.babymonitor  // ← reemplaza con tu paquete real

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }

    private val amplitudes = mutableListOf<Float>()
    private var isPlaying = false
    private val runnable = Runnable { updateWave() }

    fun addAmplitude(amp: Float) {
        val normalized = amp.coerceIn(0f, 1f)
        amplitudes.add(normalized)
        if (amplitudes.size > 40) amplitudes.removeAt(0)
        invalidate()
    }

    fun startWave() {
        isPlaying = true
        updateWave()
    }

    fun stopWave() {
        isPlaying = false
        removeCallbacks(runnable)
        amplitudes.clear()
        invalidate()
    }

    private fun updateWave() {
        if (!isPlaying) return
        addAmplitude(Random.nextFloat())
        postDelayed(runnable, 100)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (amplitudes.isEmpty()) return

        val barWidth = 6f
        val gap = 4f
        val totalBars = amplitudes.size
        val totalWidth = totalBars * (barWidth + gap)
        val startX = (width - totalWidth) / 2f
        val centerY = height / 2f

        amplitudes.forEachIndexed { i, amp ->
            val barHeight = amp * (height * 0.45f) + 8f
            val x = startX + i * (barWidth + gap)
            canvas.drawLine(x, centerY - barHeight, x, centerY + barHeight, paint)
        }
    }
}