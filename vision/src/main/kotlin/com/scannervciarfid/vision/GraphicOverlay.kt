package com.scannervciarfid.vision

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Canvas transparente sobreposto ao PreviewView. Recebe Graphic's dos
 * VisionEngineAnalyzer (rodando na thread de analise do CameraX) e os
 * desenha na thread de UI em onDraw(). CopyOnWriteArrayList evita
 * ConcurrentModificationException nesse cenario de escrita em background /
 * leitura na UI thread, sem precisar de lock explicito na iteracao de desenho.
 */
class GraphicOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val graphics: MutableList<Graphic> = CopyOnWriteArrayList()

    @Volatile
    private var imageWidth: Int = 0

    @Volatile
    private var imageHeight: Int = 0

    /**
     * Dimensoes (em pixels) da imagem que o ML Kit efetivamente analisou -
     * geralmente diferente do tamanho renderizado desta View. Usado por
     * translateX/translateY para converter coordenadas do espaco da imagem
     * para o espaco de tela.
     */
    fun setImageSourceInfo(imageWidth: Int, imageHeight: Int) {
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
    }

    /**
     * Variante para frames do CameraX: o ImageProxy informa largura/altura do
     * sensor (sem rotacao), mas o ML Kit devolve as coordenadas na imagem ja
     * rotacionada para a orientacao "em pe". Com rotacao de 90/270 graus
     * (aparelho em retrato), largura e altura precisam ser trocadas - senao
     * os bounding boxes ficam deslocados e esticados.
     */
    fun setImageSourceInfo(imageWidth: Int, imageHeight: Int, rotationDegrees: Int) {
        if (rotationDegrees == 90 || rotationDegrees == 270) {
            setImageSourceInfo(imageHeight, imageWidth)
        } else {
            setImageSourceInfo(imageWidth, imageHeight)
        }
    }

    fun add(graphic: Graphic) {
        graphics.add(graphic)
    }

    fun clear() {
        graphics.clear()
    }

    /**
     * scaleFactor unico (nao X/Y independentes) reproduzindo o comportamento
     * padrao FILL_CENTER do PreviewView do CameraX: a imagem eh escalada
     * preservando a proporcao ate cobrir a View inteira, cortando o excesso
     * nas bordas. Usar o maior dos dois ratios - em vez de um por eixo - e
     * o que evita distorcer os bounding boxes.
     */
    private fun scaleFactor(): Float = maxOf(
        width.toFloat() / imageWidth.toFloat(),
        height.toFloat() / imageHeight.toFloat()
    )

    private fun isImageSourceInfoReady(): Boolean =
        imageWidth != 0 && imageHeight != 0 && width != 0 && height != 0

    fun translateX(x: Float): Float {
        if (!isImageSourceInfoReady()) {
            return x
        }

        val scaleFactor = scaleFactor()
        val postScaleWidth = imageWidth * scaleFactor
        val xOffset = (width - postScaleWidth) / 2f

        return (x * scaleFactor) + xOffset
    }

    fun translateY(y: Float): Float {
        if (!isImageSourceInfoReady()) {
            return y
        }

        val scaleFactor = scaleFactor()
        val postScaleHeight = imageHeight * scaleFactor
        val yOffset = (height - postScaleHeight) / 2f

        return (y * scaleFactor) + yOffset
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (graphic in graphics) {
            graphic.draw(canvas)
        }
    }
}
