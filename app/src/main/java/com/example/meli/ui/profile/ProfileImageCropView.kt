package com.example.meli.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.max

class ProfileImageCropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    private val drawMatrix = Matrix()
    private val matrixValues = FloatArray(9)
    private var minScale = 1f
    private var maxScale = 5f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val drawable = drawable ?: return false
            val currentScale = currentScale()
            val targetScale = (currentScale * detector.scaleFactor).coerceIn(minScale, maxScale)
            val factor = targetScale / currentScale
            drawMatrix.postScale(factor, factor, detector.focusX, detector.focusY)
            constrainMatrix(drawable)
            imageMatrix = drawMatrix
            return true
        }
    })

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        post { resetImageTransform() }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handledByScale = scaleDetector.onTouchEvent(event)
        val drawable = drawable ?: return handledByScale || super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = true
                parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress && isDragging) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    drawMatrix.postTranslate(dx, dy)
                    constrainMatrix(drawable)
                    imageMatrix = drawMatrix
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }

        return true
    }

    fun exportCroppedBitmap(outputSize: Int = 512): Bitmap? {
        val drawable = drawable as? BitmapDrawable ?: return null
        val sourceBitmap = drawable.bitmap ?: return null
        val cropRect = cropRect()
        if (cropRect.width() <= 0f || cropRect.height() <= 0f) return null

        return Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888).also { outBitmap ->
            val canvas = Canvas(outBitmap)
            val scale = outputSize / cropRect.width()
            canvas.scale(scale, scale)
            canvas.translate(-cropRect.left, -cropRect.top)
            canvas.concat(drawMatrix)
            canvas.drawBitmap(sourceBitmap, 0f, 0f, null)
        }
    }

    private fun resetImageTransform() {
        val drawable = drawable ?: return
        if (width == 0 || height == 0) return

        val cropRect = cropRect()
        val drawableWidth = drawable.intrinsicWidth.toFloat().takeIf { it > 0f } ?: return
        val drawableHeight = drawable.intrinsicHeight.toFloat().takeIf { it > 0f } ?: return
        val scale = max(cropRect.width() / drawableWidth, cropRect.height() / drawableHeight)
        minScale = scale
        maxScale = scale * 5f

        drawMatrix.reset()
        drawMatrix.postScale(scale, scale)
        val scaledWidth = drawableWidth * scale
        val scaledHeight = drawableHeight * scale
        val dx = cropRect.centerX() - scaledWidth / 2f
        val dy = cropRect.centerY() - scaledHeight / 2f
        drawMatrix.postTranslate(dx, dy)
        constrainMatrix(drawable)
        imageMatrix = drawMatrix
    }

    private fun constrainMatrix(drawable: Drawable) {
        val cropRect = cropRect()
        val bounds = drawableRect(drawable)
        var dx = 0f
        var dy = 0f

        if (bounds.left > cropRect.left) dx = cropRect.left - bounds.left
        if (bounds.top > cropRect.top) dy = cropRect.top - bounds.top
        if (bounds.right < cropRect.right) dx = cropRect.right - bounds.right
        if (bounds.bottom < cropRect.bottom) dy = cropRect.bottom - bounds.bottom

        drawMatrix.postTranslate(dx, dy)
    }

    private fun drawableRect(drawable: Drawable): RectF {
        val rect = RectF(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        drawMatrix.mapRect(rect)
        return rect
    }

    private fun cropRect(): RectF {
        val size = minOf(width, height).toFloat()
        val inset = size * 0.08f
        val cropSize = size - inset * 2f
        val left = (width - cropSize) / 2f
        val top = (height - cropSize) / 2f
        return RectF(left, top, left + cropSize, top + cropSize)
    }

    private fun currentScale(): Float {
        drawMatrix.getValues(matrixValues)
        return matrixValues[Matrix.MSCALE_X]
    }
}
