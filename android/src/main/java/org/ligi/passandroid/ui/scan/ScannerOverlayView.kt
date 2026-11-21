package org.ligi.passandroid.ui.scan

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.View

/** darkens the camera picture around the square scan frame and draws the frame */
class ScannerOverlayView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val shadePaint = Paint().apply { color = 0xA6000000.toInt() }
    private val clearPaint = Paint().apply {
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x55FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFD8A657.toInt()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 5f * density
    }
    private val scanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCCD8A657.toInt()
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2f * density
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val frameSize = width * 0.72f
        val left = (width - frameSize) / 2f
        val top = (height - frameSize) / 2f
        val right = left + frameSize
        val bottom = top + frameSize
        val radius = 28f * density
        val corner = 42f * density
        val scanY = top + frameSize * 0.58f

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shadePaint)
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, clearPaint)
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, borderPaint)

        canvas.drawLine(left, top + corner, left, top + radius, cornerPaint)
        canvas.drawLine(left + radius, top, left + corner, top, cornerPaint)
        canvas.drawLine(right - corner, top, right - radius, top, cornerPaint)
        canvas.drawLine(right, top + radius, right, top + corner, cornerPaint)
        canvas.drawLine(left, bottom - corner, left, bottom - radius, cornerPaint)
        canvas.drawLine(left + radius, bottom, left + corner, bottom, cornerPaint)
        canvas.drawLine(right - corner, bottom, right - radius, bottom, cornerPaint)
        canvas.drawLine(right, bottom - corner, right, bottom - radius, cornerPaint)

        canvas.drawLine(left + 24f * density, scanY, right - 24f * density, scanY, scanPaint)
    }
}
