package com.dongnh.bubblepicker.rendering

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.opengl.GLES20.*
import android.opengl.GLUtils
import android.opengl.Matrix
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.dongnh.bubblepicker.model.BubbleGradient
import com.dongnh.bubblepicker.model.PickerItem
import com.dongnh.bubblepicker.physics.CircleBody
import com.dongnh.bubblepicker.rendering.BubbleShader.U_MATRIX
import com.dongnh.bubblepicker.toTexture
import org.jbox2d.common.Vec2
import java.lang.ref.WeakReference

/**
 * Created by irinagalata on 1/19/17.
 */
data class Item(
    val context: WeakReference<Context>,
    val pickerItem: PickerItem,
    val circleBody: CircleBody,
    val widthImage: Float,
    val heightImage: Float,
) {

    val isBodyDestroyed: Boolean
        get() = circleBody.isDestroyed

    val x: Float?
        get() = circleBody.physicalBody?.position?.x

    val y: Float?
        get() = circleBody.physicalBody?.position?.y

    val radius: Float
        get() = circleBody.actualRadius

    val initialPosition: Vec2
        get() = circleBody.position

    private val currentPosition: Vec2?
        get() = circleBody.physicalBody?.position

    private val isVisible
        get() = circleBody.isVisible

    var programId: Int = 0
    private var texture: Int = 0
    private var currentFrameIndex: Int = 0
    private var lastUpdateTime = System.currentTimeMillis()
    private val frameDelay: Long get() = pickerItem.animatedFrames?.get(currentFrameIndex)?.duration ?: 40
    private val aspectRatioLocation by lazy { glGetUniformLocation(programId, BubbleShader.U_ASPECT_RATIO) }
    private val borderColorLocation by lazy { glGetUniformLocation(programId, BubbleShader.U_BORDER_COLOR) }
    private val borderThicknessLocation by lazy { glGetUniformLocation(programId, BubbleShader.U_BORDER_THICKNESS) }
    private val textUniformLocation by lazy { glGetUniformLocation(programId, BubbleShader.U_TEXT) }
    private val visibilityLocation by lazy { glGetUniformLocation(programId, BubbleShader.U_VISIBILITY) }
    private val matrixLocation by lazy { glGetUniformLocation(programId, U_MATRIX) }

    private val gradient: LinearGradient?
        get() {
            return pickerItem.gradient?.let {
                val horizontal = it.direction == BubbleGradient.HORIZONTAL
                LinearGradient(
                    if (horizontal) 0f else heightImage / 2f,
                    if (horizontal) widthImage / 2f else 0f,
                    if (horizontal) widthImage else heightImage / 2f,
                    if (horizontal) widthImage / 2f else heightImage,
                    it.startColor, it.endColor, Shader.TileMode.CLAMP
                )
            }
        }

    private fun maybeUpdateAnimatedFrame(): Float {
        pickerItem.animatedFrames?.let { frames ->
            val frame = frames[currentFrameIndex]
            val currentTime = System.currentTimeMillis()
            if (frames.size > 1 && currentTime - lastUpdateTime > frameDelay) {
                currentFrameIndex = (currentFrameIndex + 1) % frames.size
                lastUpdateTime = currentTime
                updateTexture(frame.bitmap)
            } else if (frames.size == 1) {
                updateTexture(frame.bitmap)
            }
            return frame.bitmap.width.toFloat() / frame.bitmap.height.toFloat()
        }
        return 1f
    }

    private fun updateTexture(bitmap: Bitmap) {
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)
    }

    fun drawItself(index: Int, scaleX: Float, scaleY: Float, borderColor: FloatArray, borderThickness: Float, isSelected: Boolean) {
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, texture)
        val aspectRatio = maybeUpdateAnimatedFrame()
        glUniform1i(textUniformLocation, 0)
        glUniform1i(visibilityLocation, if (isVisible) 1 else -1)
        glUniform1f(aspectRatioLocation, aspectRatio)
        if (isSelected) {
            glUniform4fv(borderColorLocation, 1, borderColor, 0)
            glUniform1f(borderThicknessLocation, borderThickness)
        } else {
            glUniform4f(borderColorLocation, 0f, 0f, 0f, 0f)
            glUniform1f(borderThicknessLocation, 0f)
        }
        glUniformMatrix4fv(matrixLocation, 1, false, calculateMatrix(scaleX, scaleY), 0)
        glDrawArrays(GL_TRIANGLE_STRIP, index * 4, 4)
    }

    private fun bindTexture(textureIds: IntArray, index: Int): Int {
        glGenTextures(1, textureIds, index)
        createBitmap().toTexture(textureIds[index])
        return textureIds[index]
    }

    fun bindTextures(textureIds: IntArray, index: Int) {
        texture = bindTexture(textureIds, index * 2 + 1)
    }

    private fun applyStrokeToFrame(sourceImage: Bitmap): Bitmap {
        val modifiedBitmap = Bitmap.createBitmap(widthImage.toInt(), heightImage.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(modifiedBitmap)
        val bitmapDrawable = BitmapDrawable(context.get()?.resources, sourceImage)
        drawImage(bitmapDrawable, canvas)
        drawStrokeSelect(canvas)
        return modifiedBitmap
    }

    private fun createBitmap(): Bitmap {
        var bitmap: Bitmap =
            Bitmap.createBitmap(widthImage.toInt(), heightImage.toInt(), Bitmap.Config.ARGB_8888)

        val bitmapConfig: Bitmap.Config = bitmap.config ?: Bitmap.Config.ARGB_8888

        bitmap = bitmap.copy(bitmapConfig, true)

        val canvas = bitmap?.let { Canvas(it) }

        canvas?.let {
            drawImage(pickerItem.imgDrawable as? BitmapDrawable, it)
            if (pickerItem.isViewBorderSelected) drawStrokeSelect(it)
            drawBackground(it)
            drawIcon(it)
            drawText(it)
        }

        return bitmap
    }

    private fun drawStrokeSelect(canvas: Canvas) {
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = pickerItem.colorBorderSelected ?: Color.WHITE
            strokeWidth = pickerItem.strokeWidthBorder
        }

        canvas.drawCircle(
            widthImage / 2,
            heightImage / 2,
            widthImage / 2,
            strokePaint
        )
    }

    private fun drawBackground(canvas: Canvas) {
        val bgPaint = Paint()
        bgPaint.style = Paint.Style.FILL
        pickerItem.color?.let { bgPaint.color = pickerItem.color!! }
        pickerItem.gradient?.let { bgPaint.shader = gradient }
        bgPaint.alpha = (pickerItem.overlayAlpha * 255).toInt()
        canvas.drawRect(0f, 0f, widthImage, heightImage, bgPaint)
    }

    private fun drawText(canvas: Canvas) {
        if (pickerItem.title == null) return

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {

            color = if (pickerItem.textColor == null) {
                Color.parseColor("#ffffff")
            } else {
                pickerItem.textColor!!
            }

            textSize = pickerItem.textSize
            typeface = pickerItem.typeface
        }

        val maxTextHeight = if (pickerItem.icon == null) heightImage / 2f else heightImage / 2.7f

        var textLayout = placeText(paint)

        while (textLayout.height > maxTextHeight) {
            paint.textSize--
            textLayout = placeText(paint)
        }

        if (pickerItem.icon == null) {
            canvas.translate(
                (widthImage - textLayout.width) / 2f,
                (heightImage - textLayout.height) / 2f
            )
        } else if (pickerItem.iconOnTop) {
            canvas.translate((widthImage - textLayout.width) / 2f, heightImage / 2f)
        } else {
            canvas.translate(
                (widthImage - textLayout.width) / 2f,
                heightImage / 2 - textLayout.height
            )
        }

        textLayout.draw(canvas)
    }

    @Suppress("DEPRECATION")
    private fun placeText(paint: TextPaint): StaticLayout {
        return StaticLayout(
            pickerItem.title, paint, (widthImage * 0.9).toInt(),
            Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false
        )
    }

    private fun drawIcon(canvas: Canvas) {
        pickerItem.icon?.let {
            val width = it.intrinsicWidth
            val height = it.intrinsicHeight

            val left = (widthImage / 2 - width / 2).toInt()
            val right = (widthImage / 2 + width / 2).toInt()

            if (pickerItem.title == null) {
                it.bounds = Rect(
                    left,
                    (widthImage / 2 - height / 2).toInt(),
                    right,
                    (heightImage / 2 + height / 2).toInt()
                )
            } else if (pickerItem.iconOnTop) {
                it.bounds =
                    Rect(left, (widthImage / 2 - height).toInt(), right, (heightImage / 2).toInt())
            } else {
                it.bounds =
                    Rect(left, (widthImage / 2).toInt(), right, (heightImage / 2 + height).toInt())
            }

            it.draw(canvas)
        }
    }

    private fun drawImage(bitmapDrawable: BitmapDrawable?, canvas: Canvas) {
        bitmapDrawable ?: return
        val height = bitmapDrawable.bitmap.height.toFloat()
        val width = bitmapDrawable.bitmap.width.toFloat()
        val ratio = height.coerceAtLeast(width) / height.coerceAtMost(width)
        val bitmapHeight = if (height < width) widthImage else heightImage * ratio
        val bitmapWidth = if (height < width) widthImage * ratio else heightImage
        bitmapDrawable.bounds = Rect(0, 0, bitmapWidth.toInt(), bitmapHeight.toInt())
        bitmapDrawable.draw(canvas)
    }

    private fun calculateMatrix(scaleX: Float, scaleY: Float) = FloatArray(16).apply {
        Matrix.setIdentityM(this, 0)
        Matrix.translateM(this, 0, currentPosition!!.x * scaleX - initialPosition.x,
            currentPosition!!.y * scaleY - initialPosition.y, 0f)
    }

}