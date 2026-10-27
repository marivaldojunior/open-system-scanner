package com.scannervciarfid.vision

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect

class BoundingBoxGraphic(
    overlay: GraphicOverlay,
    val boundingBox: Rect,
    val color: Int = Color.GREEN
) : Graphic(overlay) {

    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH_PX
        color = this@BoundingBoxGraphic.color
    }

    override fun draw(canvas: Canvas) {
        val left = overlay.translateX(boundingBox.left.toFloat())
        val top = overlay.translateY(boundingBox.top.toFloat())
        val right = overlay.translateX(boundingBox.right.toFloat())
        val bottom = overlay.translateY(boundingBox.bottom.toFloat())

        canvas.drawRect(left, top, right, bottom, boxPaint)
    }

    private companion object {
        const val STROKE_WIDTH_PX = 6f
    }
}
