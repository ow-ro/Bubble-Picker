package com.dongnh.bubblepicker.rendering

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.annotation.ColorInt
import com.dongnh.bubblepicker.BubblePickerListener
import com.dongnh.bubblepicker.R
import com.dongnh.bubblepicker.adapter.BubblePickerAdapter
import com.dongnh.bubblepicker.model.Color
import com.dongnh.bubblepicker.model.PickerItem
import com.dongnh.bubblepicker.physics.Engine
import com.dongnh.bubblepicker.physics.Engine.mainMaxScale
import com.dongnh.bubblepicker.physics.Engine.secondaryMaxScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Created by irinagalata on 1/19/17.
 */
class BubblePicker(context: Context?, attrs: AttributeSet?) : GLSurfaceView(context, attrs) {

    private val coroutineScope by lazy { CoroutineScope(Dispatchers.Default) }
    private var renderer: PickerRenderer = PickerRenderer(this)
    private var startX = 0f
    private var startY = 0f
    private var previousX = 0f
    private var previousY = 0f
    private var debounceRelease: Job? = null
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
                val mainPickerItems = HashSet((0 until value.mainItemCount).map { value.getMainItem(it) })
                mainMaxScale = mainPickerItems.maxBy { it.value }.value

                val secondaryPickerItems = hashSetOf<PickerItem>()
                value.secondaryItemCount?.let { secondaryCount ->
                    secondaryPickerItems.addAll((0 until secondaryCount).map { value.getSecondaryItem(it) })
                    secondaryMaxScale = secondaryPickerItems.maxBy { it.value }.value

                    // Add secondaryRadius if item exists in both lists
                    mainPickerItems.forEach { mainItem ->
                        secondaryPickerItems.firstOrNull { it.id == mainItem.id }?.let { duplicate ->
                            mainItem.secondaryValue = duplicate.value
                        }
                    }
                }

                // Combine mainPickerItems and secondaryPickerItems, excluding duplicates based on title
                renderer.allPickerItemsList = ArrayList(mainPickerItems + secondaryPickerItems.filterNot { secondaryItem ->
                    mainPickerItems.any { it.id == secondaryItem.id }
                })
            }
            super.onResume()
        }
    var swipeMoveSpeed = 1.5f

    init {
        setZOrderOnTop(true)
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.RGBA_8888)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        attrs?.let { retrieveAttributes(attrs) }
    }

    override fun onResume() {
        if (renderer.allPickerItemsList.isNotEmpty()) {
            super.onResume()
        }
    }

    override fun onPause() {
        if (renderer.allPickerItemsList.isNotEmpty()) {
            super.onPause()
            renderer.clear()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                Log.i("BubblePicker", "ACTION_DOWN")
                debounceRelease?.cancel()
                startX = event.x
                startY = event.y
                previousX = event.x
                previousY = event.y
            }
            MotionEvent.ACTION_UP -> {
                Log.i("BubblePicker", "ACTION_UP")
                if (isClick(event)) renderer.resize(event.x, event.y)
                renderer.release()
                releaseWithReset()
            }
            MotionEvent.ACTION_MOVE -> {
                Log.i("BubblePicker", "ACTION_MOVE")
                if (isSwipe(event)) {
                    renderer.swipe((previousX - event.x) * swipeMoveSpeed, (previousY - event.y) * swipeMoveSpeed)
                    previousX = event.x
                    previousY = event.y
                } else {
                    release()
                }
            }
            else -> {
                release()
                releaseWithReset()
            }
        }

        return true
    }

    private fun release() = post { renderer.release() }

    private fun releaseWithReset() {
        debounceRelease?.cancel()
        debounceRelease = coroutineScope.launch {
            delay(1000)
            renderer.releaseWithReset()
        }
    }

    private fun isClick(event: MotionEvent) =
        abs(event.x - startX) < 5 && abs(event.y - startY) < 5

    private fun isSwipe(event: MotionEvent) =
        abs(event.x - previousX) > 5 || abs(event.y - previousY) > 5

    private fun retrieveAttributes(attrs: AttributeSet) {
        val array = context.obtainStyledAttributes(attrs, R.styleable.BubblePicker)

        if (array.hasValue(R.styleable.BubblePicker_backgroundColor)) {
            background = array.getColor(R.styleable.BubblePicker_backgroundColor, -1)
        }

        array.recycle()
    }

    fun showMainItems() {
        Engine.mode = Engine.Mode.MAIN
    }

    fun showSecondaryItems() {
        Engine.mode = Engine.Mode.SECONDARY
    }

    /**
     * This function sets the max size of the bubble as a percentage
     * of the lesser dimension of the view the BubblePicker is displayed in.
     *
     * @param size The maximum size of the bubble, default is 0.4f (40% of smaller dimension).
     */
    fun setMaxBubbleSize(size: Float) {
        Engine.maxBubbleSize = size
    }

    /**
     * This function sets the min size of the bubble as a percentage
     * of the lesser dimension of the view the BubblePicker is displayed in.
     *
     * @param size The minimum size of the bubble, default is 0.1f (10% of smaller dimension).
     */
    fun setMinBubbleSize(size: Float) {
        Engine.minBubbleSize = size
    }

    fun configHorizontalSwipeOnly(horizOnly: Boolean) {
        // Default false
        renderer.horizontalSwipeOnly = horizOnly
    }

    // Config listener
    fun configListenerForBubble(listener: BubblePickerListener) {
        renderer.listener = listener
    }

    // Margin of item
    fun configMargin(marginItem: Float) {
        renderer.marginBetweenItems = marginItem
    }

    // Config speed draw and move iem
    fun configSpeedMoveOfItem(speed: Float) {
        renderer.speedBackToCenter = speed
    }

    // Config Center Immediately
    fun configCenterImmediately(center: Boolean) {
        renderer.centerImmediately = center
    }

    // Config size of image
    fun configSizeOfImage(width: Float, height: Float) {
        renderer.widthImage = width
        renderer.heightImage = height
    }

    fun cleanup() {
        renderer.clear()
    }
}
