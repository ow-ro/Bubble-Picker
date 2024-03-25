package com.dongnh.bubblepicker.rendering

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.opengl.GLES20.*
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

    val x: Float
        get() = circleBody.physicalBody.position.x

    val y: Float
        get() = circleBody.physicalBody.position.y

    val radius: Float
        get() = circleBody.actualRadius

    val initialPosition: Vec2
        get() = circleBody.position

    private val currentPosition: Vec2
        get() = circleBody.physicalBody.position

    private val isVisible
        get() = circleBody.isVisible


    private var imageTexture: Int = 0

    private val currentTexture: Int get () = imageTexture

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

    fun drawItself(programId: Int, index: Int, scaleX: Float, scaleY: Float) {
        glActiveTexture(GL_TEXTURE)
        glBindTexture(GL_TEXTURE_2D, currentTexture)
        glUniform1i(glGetUniformLocation(programId, BubbleShader.U_TEXT), 0)
        glUniform1i(glGetUniformLocation(programId, BubbleShader.U_VISIBILITY), if (isVisible) 1 else -1)
        glUniformMatrix4fv(glGetUniformLocation(programId, U_MATRIX), 1, false, calculateMatrix(scaleX, scaleY), 0)
        glDrawArrays(GL_TRIANGLE_STRIP, index * 4, 4)
    }

    fun bindTextures(textureIds: IntArray, index: Int) {
        imageTexture = bindTexture(textureIds, index * 2 + 1)
    }

    private fun createBitmap(): Bitmap {
        var bitmap: Bitmap =
            Bitmap.createBitmap(widthImage.toInt(), heightImage.toInt(), Bitmap.Config.ARGB_8888)

        val bitmapConfig: Bitmap.Config = bitmap.config ?: Bitmap.Config.ARGB_8888

        bitmap = bitmap.copy(bitmapConfig, true)

        val canvas = bitmap?.let { Canvas(it) }

        canvas?.let {
            drawImage(it)
            if (pickerItem.isViewBorderSelected) drawStrokeSelect(it)
            drawBackground(it)
            drawIcon(it)
            drawText(it)
        }

        return bitmap
    }

    private fun drawStrokeSelect(canvas: Canvas) {
        val strokePaint = Paint()
        strokePaint.style = Paint.Style.STROKE
        if (pickerItem.colorBorderSelected != null) {
            strokePaint.color = pickerItem.colorBorderSelected!!
        } else {
            strokePaint.color = Color.BLACK
        }

        strokePaint.strokeWidth = pickerItem.strokeWidthBorder
        canvas.drawCircle(
            widthImage / 2,
            heightImage / 2,
            widthImage / 2 - (pickerItem.strokeWidthBorder / 2f),
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

    private fun drawImage(canvas: Canvas) {
        pickerItem.imgDrawable?.let {
            val height = (it as BitmapDrawable).bitmap.height.toFloat()
            val width = it.bitmap.width.toFloat()
            val ratio = height.coerceAtLeast(width) / height.coerceAtMost(width)
            val bitmapHeight = if (height < width) widthImage else heightImage * ratio
            val bitmapWidth = if (height < width) widthImage * ratio else heightImage
            it.bounds = Rect(0, 0, bitmapWidth.toInt(), bitmapHeight.toInt())
            it.draw(canvas)
        }
    }

    private fun bindTexture(textureIds: IntArray, index: Int): Int {
        glGenTextures(1, textureIds, index)
        createBitmap().toTexture(textureIds[index])
        return textureIds[index]
    }

    private fun calculateMatrix(scaleX: Float, scaleY: Float) = FloatArray(16).apply {
        Matrix.setIdentityM(this, 0)
        Matrix.translateM(this, 0, currentPosition.x * scaleX - initialPosition.x,
            currentPosition.y * scaleY - initialPosition.y, 0f)
    }

}