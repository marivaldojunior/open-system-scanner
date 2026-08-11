package com.opensystemscanner.vision

import android.graphics.Canvas

abstract class Graphic(val overlay: GraphicOverlay) {
    abstract fun draw(canvas: Canvas)
}
