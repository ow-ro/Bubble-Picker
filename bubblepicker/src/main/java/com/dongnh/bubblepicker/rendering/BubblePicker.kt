package com.dongnh.bubblepicker.rendering

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.annotation.ColorInt
import com.dongnh.bubblepicker.BubblePickerListener
import com.dongnh.bubblepicker.R
import com.dongnh.bubblepicker.adapter.BubblePickerAdapter
import com.dongnh.bubblepicker.model.Color
import com.dongnh.bubblepicker.model.PickerItem
import kotlin.math.abs
import kotlin.math.pow

/**
 * Created by irinagalata on 1/19/17.
 */
class BubblePicker(context: Context?, attrs: AttributeSet?) : GLSurfaceView(context, attrs) {
    private lateinit var renderer: PickerRenderer

    @ColorInt
    var background: Int = 0
        set(value) {
            field = value
            renderer.backgroundColor = Color(value)
        }

    var adapter: BubblePickerAdapter? = null
        set(value) {
            field = value
            if (value != null) {
                var area = width * height / if (isTablet()) 24 else 2  // in pixels
                renderer.items = ArrayList((0 until value.totalCount)
                        .map { value.getItem(it) }
                        .filter {
                            val diameter = it.radius!! * 2 * 1.3f
                            val oldArea = area
                            area -= diameter.pow(2)
                            return@filter diameter < width
                                    && diameter < height
                                    && diameter.pow(2) < oldArea
                                    && area > 0
                        }
                        .dropLast(1)
                        .toList())
            }
            super.onResume()
        }

    var swipeMoveSpeed = 1.5f

    private var width = 0f
    private var height = 0f
    private var startX = 0f
    private var startY = 0f
    private var previousX = 0f
    private var previousY = 0f

    init {
        init()
        attrs?.let { retrieveAttributes(attrs) }
    }

    private fun init() {
        renderer = PickerRenderer(this)

        setZOrderOnTop(true)
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.RGBA_8888)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                previousX = event.x
                previousY = event.y
            }

            MotionEvent.ACTION_UP -> {
                if (isClick(event)) renderer.resize(event.x, event.y)
                renderer.release()
            }

            MotionEvent.ACTION_MOVE -> {
                if (isSwipe(event)) {
                    renderer.swipe((previousX - event.x) * swipeMoveSpeed, (previousY - event.y) * swipeMoveSpeed)
                    previousX = event.x
                    previousY = event.y
                } else {
                    release()
                }
            }

            else -> release()
        }

        return true
    }

    private fun release() = postDelayed({ renderer.release() }, 0)

    private fun isClick(event: MotionEvent) =
            abs(event.x - startX) < 20 && abs(event.y - startY) < 20

    private fun isSwipe(event: MotionEvent) =
            abs(event.x - previousX) > 20 && abs(event.y - previousY) > 20

    private fun retrieveAttributes(attrs: AttributeSet) {
        val array = context.obtainStyledAttributes(attrs, R.styleable.BubblePicker)

        if (array.hasValue(R.styleable.BubblePicker_maxSelectedCount)) {
            renderer.maxSelectedCount = array.getInt(R.styleable.BubblePicker_maxSelectedCount, -1)
        }

        if (array.hasValue(R.styleable.BubblePicker_backgroundColor)) {
            background = array.getColor(R.styleable.BubblePicker_backgroundColor, -1)
        }

        array.recycle()
    }

    private fun isTablet(): Boolean {
        return resources.getBoolean(R.bool.isTablet)
    }

    fun configArea(width: Float, height: Float) {
        this.width = width
        this.height = height
    }

    // Config default gravity
    fun configDefaultGravity(gravity: Float) {
        renderer.gravity = gravity
    }

    // Config listener
    fun configListenerForBubble(listener: BubblePickerListener) {
        renderer.listener = listener
    }

    // Max select allow select
    fun configMaxSelectedCount(maxSelect: Int) {
        renderer.maxSelectedCount = maxSelect
    }

    // List Item selected
    fun selectedItems(): List<PickerItem?> = renderer.selectedItems

    // Margin of item
    fun configMargin(margin: Float) {
        renderer.marginBetweenItem = margin
    }

    // Config Center Immediately
    fun configCenterImmediately(center: Boolean) {
        renderer.centerImmediately = center
    }

    // Config size of bubble
    fun configBubbleSize(bubbleSize: Int) {
        if (bubbleSize in 1..100) {
            renderer.bubbleSize = bubbleSize
        } else {
            renderer.bubbleSize = 50
        }
    }

    // Config size of image
    fun configSizeOfImage(width: Float, height: Float) {
        renderer.widthImage = width
        renderer.heightImage = height
    }

    override fun onResume() {
        if (renderer.items.isNotEmpty()) {
            super.onResume()
        }
    }

    override fun onPause() {
        if (renderer.items.isNotEmpty()) {
            super.onPause()
            renderer.clear()
        }
    }

}
