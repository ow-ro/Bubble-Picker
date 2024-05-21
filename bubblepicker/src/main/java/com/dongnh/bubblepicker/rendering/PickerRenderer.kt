package com.dongnh.bubblepicker.rendering

import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.view.View
import com.dongnh.bubblepicker.*
import com.dongnh.bubblepicker.model.Color
import com.dongnh.bubblepicker.model.PickerItem
import com.dongnh.bubblepicker.physics.Engine
import com.dongnh.bubblepicker.rendering.BubbleShader.A_POSITION
import com.dongnh.bubblepicker.rendering.BubbleShader.A_UV
import com.dongnh.bubblepicker.rendering.BubbleShader.U_BACKGROUND
import com.dongnh.bubblepicker.rendering.BubbleShader.fragmentShader
import com.dongnh.bubblepicker.rendering.BubbleShader.vertexShader
import org.jbox2d.common.Vec2
import java.lang.ref.WeakReference
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sqrt

/**
 * Created by irinagalata on 1/19/17.
 */
class PickerRenderer(private val glView: View, private val engine: Engine, private val startMode: Engine.Mode) : GLSurfaceView.Renderer {

    private val scaleX: Float get() = if (glView.width < glView.height) {
        1f
    } else {
        glView.height.toFloat() / glView.width.toFloat()
    }
    private val scaleY: Float get() = if (glView.width < glView.height) {
        glView.width.toFloat() / glView.height.toFloat()
    } else {
        1f
    }
    private val circles = ArrayList<Item>()
    private var programId = 0
    private var verticesBuffer: FloatBuffer? = null
    private var uvBuffer: FloatBuffer? = null
    private var vertices: FloatArray? = null
    private var textureVertices: FloatArray? = null
    private var textureIds: IntArray? = null
    private var widthImage = 256f
    private var heightImage = 256f
    var backgroundColor: Color? = null
    var listener: BubblePickerListener? = null
    var allPickerItems: List<PickerItem> = ArrayList()

    // Gravity
    var speedBackToCenter = 50f
        set(value) {
            field = value
            engine.speedToCenter = value
        }
    var marginBetweenItems = 0.001f
        set(value) {
            field = value
            engine.margin = value
        }
    var selectedBorderColor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)
    var selectedBorderWidth: Float = 0.01f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        glClearColor(
            backgroundColor?.red ?: 1f, backgroundColor?.green ?: 1f,
            backgroundColor?.blue ?: 1f, backgroundColor?.alpha ?: 1f
        )
        enableTransparency()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)
        initialize()
    }

    override fun onDrawFrame(gl: GL10?) {
        calculateVertices()
        engine.move()
        drawFrame()
    }

    private fun initialize() {
        if (allPickerItems.isEmpty()) {
            return
        }

        // If items aren't already generated, create them
        if (circles.size == 0) {
            engine.build(allPickerItems, scaleX, scaleY)
                .forEachIndexed { index, body ->
                    circles.add(
                        Item(
                            WeakReference(glView.context),
                            allPickerItems[index],
                            body,
                            widthImage,
                            heightImage
                        )
                    )
                }
            engine.allItems = circles

            allPickerItems.forEach {
                if (circles.isNotEmpty() && it.isSelected) {
                    engine.resize(circles.first { circle -> circle.pickerItem == it }, true)
                }
            }
            engine.mode = startMode
        }

        if (textureIds == null) {
            textureIds = IntArray(circles.size * 2)
        }
        initializeArrays()
    }

    private fun initializeArrays() {
        vertices = FloatArray(circles.size * 8)
        textureVertices = FloatArray(circles.size * 8)
        circles.forEachIndexed { i, item -> initializeItem(item, i) }
        verticesBuffer = vertices?.toFloatBuffer()
        uvBuffer = textureVertices?.toFloatBuffer()
    }

    private fun initializeItem(item: Item, index: Int) {
        item.programId = programId
        initializeVertices(item, index)
        textureVertices?.passTextureVertices(index)
        item.bindTextures(textureIds ?: IntArray(0), index)
    }

    private fun calculateVertices() {
        circles.forEachIndexed { i, item -> initializeVertices(item, i) }
        vertices?.forEachIndexed { i, float -> verticesBuffer?.put(i, float) }
    }

    private fun initializeVertices(body: Item, index: Int) {
        val radius = body.radius
        val radiusX = radius * scaleX
        val radiusY = radius * scaleY

        body.initialPosition.apply {
            vertices?.put(
                8 * index,
                floatArrayOf(
                    x - radiusX,
                    y + radiusY,
                    x - radiusX,
                    y - radiusY,
                    x + radiusX,
                    y + radiusY,
                    x + radiusX,
                    y - radiusY
                )
            )
        }
    }

    private fun drawFrame() {
        glClear(GL_COLOR_BUFFER_BIT)
        glUniform4f(glGetUniformLocation(programId, U_BACKGROUND), 1f, 1f, 1f, 0f)
        verticesBuffer?.passToShader(programId, A_POSITION)
        uvBuffer?.passToShader(programId, A_UV)
        circles.forEachIndexed { i, circle ->
            if (!circle.isBodyDestroyed) {
                circle.drawItself(i, scaleX, scaleY, selectedBorderColor, selectedBorderWidth, engine.selectedItem == circle)
            }
        }
    }

    private fun enableTransparency() {
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        attachShaders()
    }

    private fun attachShaders() {
        programId = createProgram(
            createShader(GL_VERTEX_SHADER, vertexShader),
            createShader(GL_FRAGMENT_SHADER, fragmentShader)
        )
        glUseProgram(programId)
    }

    private fun createProgram(vertexShader: Int, fragmentShader: Int) = glCreateProgram().apply {
        glAttachShader(this, vertexShader)
        glAttachShader(this, fragmentShader)
        glLinkProgram(this)
    }

    private fun createShader(type: Int, shader: String) = glCreateShader(type).apply {
        glShaderSource(this, shader)
        glCompileShader(this)
    }

    fun swipe(x: Float, y: Float) = engine.swipe(
        x.convertPoint(glView.width, scaleX),
        (glView.height - y).convertPoint(glView.height, scaleY),
        getItem(Vec2(x, glView.height - y))
    )

    private fun getItem(position: Vec2) = position.let { vec2 ->
        val x = vec2.x.convertPoint(glView.width, scaleX)
        val y = vec2.y.convertPoint(glView.height, scaleY)
        circles.find { !it.isBodyDestroyed && sqrt(((x - it.x!!).sqr() + (y - it.y!!).sqr()).toDouble()) <= it.radius }
    }

    fun resize(x: Float, y: Float, resizeOnDeselect: Boolean) = getItem(Vec2(x, glView.height - y))?.apply {
        if (engine.resize(this, resizeOnDeselect)) {
            listener?.let {
                if (circleBody.increased) {
                    it.onBubbleDeselected(pickerItem)
                } else {
                    it.onBubbleSelected(pickerItem)
                }
            }
        } else if (this == engine.selectedItem && !resizeOnDeselect) {
            listener?.onBubbleDeselected(pickerItem)
        }
    }

    fun release() = engine.release()
}