package com.example.meli.ui.profile

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class ProfileImageCropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#99000000")
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cropRect = cropRect()
        val path = Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            addOval(cropRect, Path.Direction.CCW)
        }
        canvas.drawPath(path, scrimPaint)
        canvas.drawOval(cropRect, strokePaint)
    }

    private fun cropRect(): RectF {
        val size = minOf(width, height).toFloat()
        val inset = size * 0.08f
        val cropSize = size - inset * 2f
        val left = (width - cropSize) / 2f
        val top = (height - cropSize) / 2f
        return RectF(left, top, left + cropSize, top + cropSize)
    }
}
