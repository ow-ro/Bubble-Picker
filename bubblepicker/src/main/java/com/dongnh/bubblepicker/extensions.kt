package com.dongnh.bubblepicker

import android.content.res.Resources
import android.graphics.Bitmap
import android.opengl.GLES20.*
import android.opengl.GLUtils
import com.dongnh.bubblepicker.Constant.FLOAT_SIZE
import com.dongnh.bubblepicker.Constant.TEXTURE_VERTICES
import com.dongnh.bubblepicker.model.Item
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Created by irinagalata on 3/8/17.
 */

fun Float.sqr() = this * this

fun FloatBuffer.passToShader(programId: Int, name: String) {
    position(0)
    glGetAttribLocation(programId, name).let {
        glVertexAttribPointer(it, 2, GL_FLOAT, false, 2 * FLOAT_SIZE, this)
        glEnableVertexAttribArray(it)
    }
}

fun FloatArray.toFloatBuffer() = ByteBuffer
    .allocateDirect(size * FLOAT_SIZE)
    .order(ByteOrder.nativeOrder())
    .asFloatBuffer()?.put(this)

fun FloatArray.passTextureVertices(index: Int) = put(index * 8, TEXTURE_VERTICES)

fun FloatArray.put(index: Int, another: FloatArray) = another.forEachIndexed { i, float -> this[index + i] = float }

fun Float.convertPoint(size: Int, scale: Float) = (2f * (this / size.toFloat()) - 1f) / scale

fun Bitmap.toTexture(textureUnit: Int) {
    glActiveTexture(GL_TEXTURE0)
    glBindTexture(GL_TEXTURE_2D, textureUnit)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
    GLUtils.texImage2D(GL_TEXTURE_2D, 0, this, 0)
    if (!isRecycled) {
        recycle()
    }
    glBindTexture(GL_TEXTURE_2D, 0)
}

fun getPixelRadii(items: List<Item>, lesserDimension: Int, containerArea: Float): List<Float> {
    val sum = items.sumOf { it.value.toDouble() }.toFloat()
    // Prevent division by zero
    val totalValue = if (sum > items.size) sum else items.size.toFloat()
    // Increasing the area allows the bubbles to grow closer to the bubble picker view height
    val totalArea = containerArea * 2.5f
    val maxArea = (Math.PI * (0.8f * lesserDimension).pow(2)).toFloat()
    val minArea = (Math.PI * (0.1f * lesserDimension).pow(2)).toFloat()
    // Make each radius based on area
    return items.map {
        val value = if (it.value == 0f) 1f else it.value
        val ratio = value / totalValue
        val area = totalArea * ratio
        val finalArea = max(minArea, min(maxArea, area))
        sqrt(finalArea / Math.PI).toFloat()
    }
}

fun getScreenWidth() = Resources.getSystem().displayMetrics.widthPixels
fun getScreenHeight() = Resources.getSystem().displayMetrics.heightPixels
