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
import com.dongnh.bubblepicker.BubblePickerOnTouchListener
import com.dongnh.bubblepicker.R
import com.dongnh.bubblepicker.adapter.BubblePickerAdapter
import com.dongnh.bubblepicker.model.Color
import com.dongnh.bubblepicker.model.PickerItem
import com.dongnh.bubblepicker.physics.Engine
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Created by irinagalata on 1/19/17.
 */
class BubblePicker(startMode: Engine.Mode, private val resizeOnDeselect: Boolean, context: Context?, attrs: AttributeSet?, private val touchListener: BubblePickerOnTouchListener? = null) : GLSurfaceView(context, attrs) {

    constructor(context: Context?, attrs: AttributeSet? = null) : this(Engine.Mode.MAIN, true, context, attrs)

    private val engine: Engine = Engine(touchListener)
    private val renderer: PickerRenderer = PickerRenderer(this, engine, startMode)
    private var startX = 0f
    private var startY = 0f
    private var previousX = 0f
    private var previousY = 0f
    private var minBubbleSize: Float = 0.1f
    private var maxBubbleSize: Float = 0.8f
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
                val mainPickerItems = (0 until value.mainItemCount)
                    .map { value.getMainItem(it) }
                    .sortedByDescending { it.value }

                // Transition values into radii
                setRadii(mainPickerItems)

                val secondaryPickerItems = mutableListOf<PickerItem>()
                value.secondaryItemCount?.let { secondaryCount ->
                    secondaryPickerItems.addAll((0 until secondaryCount)
                        .map { value.getSecondaryItem(it) }
                        .sortedByDescending { it.value }
                    )

                    // Transition values into radii
                    setRadii(secondaryPickerItems)

                    // Add secondaryRadius if item exists in both lists
                    mainPickerItems.forEach { mainItem ->
                        secondaryPickerItems.firstOrNull { it.id == mainItem.id }?.let { duplicate ->
                            mainItem.secondaryValue = duplicate.value
                        }
                    }
                }

                // Combine mainPickerItems and secondaryPickerItems, excluding duplicates based on title
                renderer.allPickerItems = mainPickerItems + secondaryPickerItems.filterNot { secondaryItem ->
                    mainPickerItems.any { it.id == secondaryItem.id }
                }
            }
            super.onResume()
        }
    var swipeMoveSpeed = 1.5f

    init {
        try {
            setZOrderOnTop(true)
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            holder.setFormat(PixelFormat.RGBA_8888)
            setRenderer(renderer)
            renderMode = RENDERMODE_CONTINUOUSLY
            attrs?.let { retrieveAttributes(attrs) }
        } catch (e: Exception) {
            Log.e("BubblePicker", "Error creating BubblePicker: ${e.message}", e)
        }
    }

    override fun onResume() {
        try {
            if (renderer.allPickerItems.isNotEmpty()) {
                super.onResume()
            }
        } catch (e: Exception) {
            Log.e("BubblePicker", "Error resuming BubblePicker: ${e.message}", e)
        }
    }

    override fun onPause() {
        try {
            if (renderer.allPickerItems.isNotEmpty()) {
                super.onPause()
            }
        } catch (e: Exception) {
            Log.e("BubblePicker", "Error pausing BubblePicker: ${e.message}", e)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                Log.i("BubblePicker", "ACTION_DOWN")
                startX = event.x
                startY = event.y
                previousX = event.x
                previousY = event.y
            }
            MotionEvent.ACTION_UP -> {
                Log.i("BubblePicker", "ACTION_UP")
                if (isClick(event)) renderer.resize(event.x, event.y, resizeOnDeselect)
                touchListener?.onTouchUp(event)
                renderer.release()
            }
            MotionEvent.ACTION_MOVE -> {
                Log.i("BubblePicker", "ACTION_MOVE")
                if (isSwipe(event)) {
                    touchListener?.onTouchMove(event)
                    renderer.swipe(event.x, event.y)
                    previousX = event.x
                    previousY = event.y
                }
            }
            else -> {
                renderer.release()
            }
        }

        return true
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

    private fun setRadii(items: List<PickerItem>) {
        val sum = items.sumOf { it.value.toDouble() }.toFloat()
        // Prevent division by zero
        val totalValue = if (sum > items.size) sum else items.size.toFloat()
        val lesserDimension = min(adapter?.width!!, adapter?.height!!)
        // Increasing the area allows the bubbles to grow closer to the bubble picker view height
        val totalArea = (adapter?.width?.times(adapter?.height ?: 0) ?: 0) * 2.5f
        val maxArea = (Math.PI * (maxBubbleSize * lesserDimension).pow(2)).toFloat()
        val minArea = (Math.PI * (minBubbleSize * lesserDimension).pow(2)).toFloat()
        // Make each radius based on area
        items.forEach {
            val value = if (it.value == 0f) 1f else it.value
            val ratio = value / totalValue
            val area = totalArea * ratio
            val finalArea = max(minArea, min(maxArea, area))
            val radius = sqrt(finalArea / Math.PI).toFloat()
            it.value = radius / lesserDimension
        }
    }

    fun showMainItems() {
        engine.mode = Engine.Mode.MAIN
    }

    fun showSecondaryItems() {
        engine.mode = Engine.Mode.SECONDARY
    }

    /**
     * This function sets the max size of the bubble as a percentage
     * of the lesser dimension of the view the BubblePicker is displayed in.
     *
     * @param size The maximum size of the bubble, default is 0.4f (40% of smaller dimension).
     */
    fun setMaxBubbleSize(size: Float) {
        maxBubbleSize = size
    }

    /**
     * This function sets the min size of the bubble as a percentage
     * of the lesser dimension of the view the BubblePicker is displayed in.
     *
     * @param size The minimum size of the bubble, default is 0.1f (10% of smaller dimension).
     */
    fun setMinBubbleSize(size: Float) {
        minBubbleSize = size
    }

    fun setSelectedBorderColor(color: FloatArray) {
        renderer.selectedBorderColor = color
    }

    fun setSelectedBorderWidth(width: Float) {
        renderer.selectedBorderWidth = width
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
}
