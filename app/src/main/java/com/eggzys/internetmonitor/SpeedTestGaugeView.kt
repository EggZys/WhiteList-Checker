package com.eggzys.internetmonitor

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat

class SpeedTestGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val neonCyan = ContextCompat.getColor(context, R.color.neon_cyan)
    private val neonCyan40 = ContextCompat.getColor(context, R.color.neon_cyan_40)
    private val neonCyan20 = ContextCompat.getColor(context, R.color.neon_cyan_20)
    private val neonGreen = ContextCompat.getColor(context, R.color.neon_green)
    private val glassBorder = ContextCompat.getColor(context, R.color.glass_border)

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 12f
        color = glassBorder
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 20f
        strokeCap = Paint.Cap.ROUND
        color = neonCyan
        maskFilter = android.graphics.BlurMaskFilter(30f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = neonCyan40
    }

    private val rect = RectF()
    private var progress = 0f
    private var maxSpeed = 100f
    private var currentSpeed = 0f
    private var progressAnimator: ValueAnimator? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padding = 20f
        rect.set(padding, padding, w - padding, h - padding)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background arc
        canvas.drawArc(rect, 135f, 270f, false, bgPaint)

        // Draw tick marks
        for (i in 0..10) {
            val angle = 135f + (i * 27f)
            val rad = Math.toRadians(angle.toDouble())
            val cx = rect.centerX()
            val cy = rect.centerY()
            val r = rect.width() / 2f
            val inner = r - 18f
            val outer = r - 8f
            canvas.drawLine(
                cx + (inner * Math.cos(rad)).toFloat(),
                cy + (inner * Math.sin(rad)).toFloat(),
                cx + (outer * Math.cos(rad)).toFloat(),
                cy + (outer * Math.sin(rad)).toFloat(),
                tickPaint
            )
        }

        // Draw glow
        if (progress > 0f) {
            val sweepAngle = progress * 270f
            glowPaint.alpha = (progress * 80).toInt().coerceIn(0, 255)
            canvas.drawArc(rect, 135f, sweepAngle, false, glowPaint)
        }

        // Draw progress arc
        if (progress > 0f) {
            val sweepAngle = progress * 270f
            val gradient = SweepGradient(
                rect.centerX(), rect.centerY(),
                intArrayOf(neonCyan, neonGreen, neonCyan),
                floatArrayOf(0f, 0.5f, 1f)
            )
            val matrix = android.graphics.Matrix()
            matrix.setRotate(135f, rect.centerX(), rect.centerY())
            gradient.setLocalMatrix(matrix)
            progressPaint.shader = gradient
            canvas.drawArc(rect, 135f, sweepAngle, false, progressPaint)
        }
    }

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        invalidate()
    }

    fun setSpeed(speed: Float) {
        currentSpeed = speed
        val targetProgress = (speed / maxSpeed).coerceIn(0f, 1f)
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(progress, targetProgress).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun setMaxSpeed(max: Float) {
        maxSpeed = max
        invalidate()
    }

    fun reset() {
        progressAnimator?.cancel()
        progress = 0f
        currentSpeed = 0f
        invalidate()
    }
}