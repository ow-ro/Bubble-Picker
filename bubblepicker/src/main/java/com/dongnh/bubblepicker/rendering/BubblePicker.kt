package com.dongnh.bubblepicker.rendering

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.annotation.ColorInt
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.dongnh.bubblepicker.BubblePickerListener
import com.dongnh.bubblepicker.BubblePickerOnTouchListener
import com.dongnh.bubblepicker.R
import com.dongnh.bubblepicker.adapter.BubblePickerAdapter
import com.dongnh.bubblepicker.model.Color
import com.dongnh.bubblepicker.model.PickerItem
import com.dongnh.bubblepicker.physics.Engine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Created by irinagalata on 1/19/17.
 */
class BubblePicker(startMode: Engine.Mode, private val resizeOnDeselect: Boolean, context: Context?, attrs: AttributeSet?, private val touchListener: BubblePickerOnTouchListener? = null) : GLSurfaceView(context, attrs) {

    constructor(context: Context?, attrs: AttributeSet? = null) : this(Engine.Mode.MAIN, true, context, attrs)

    private val engine: Engine = Engine()
    private val renderer: PickerRenderer = PickerRenderer(this, engine, touchListener, startMode)
    private var startX = 0f
    private var startY = 0f
    private var previousX = 0f
    private var previousY = 0f
    private var isLongPress = false
    private var longPressJob: Job? = null
    private val coroutineScope by lazy {
        this.findViewTreeLifecycleOwner()?.lifecycleScope ?: CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

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

                val secondaryPickerItems = mutableListOf<PickerItem>()
                value.secondaryItemCount?.let { secondaryCount ->
                    secondaryPickerItems.addAll((0 until secondaryCount)
                        .map { value.getSecondaryItem(it) }
                        .sortedByDescending { it.value }
                    )

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
                startX = event.x
                startY = event.y
                renderer.setCurrentTouchedItem(startX, startY)
                previousX = event.x
                previousY = event.y
                isLongPress = false

                longPressJob = coroutineScope.launch {
                    delay(300)
                    renderer.longClick()
                    isLongPress = true
                }
            }
            MotionEvent.ACTION_UP -> {
                longPressJob?.cancel()
                if (isClick(event) && !isLongPress) renderer.resize(resizeOnDeselect)
                touchListener?.onTouchUp(event)
                renderer.release()
            }
            MotionEvent.ACTION_MOVE -> {
                if (isSwipe(event)) {
                    longPressJob?.cancel()
                    touchListener?.onTouchMove(event)
                    renderer.swipe(event.x, event.y)
                    previousX = event.x
                    previousY = event.y
                }
            }
            else -> {
                renderer.release()
                longPressJob?.cancel()
            }
        }

        return true
    }

    private fun isClick(event: MotionEvent) =
        abs(event.x - startX) < 10 && abs(event.y - startY) < 10

    private fun isSwipe(event: MotionEvent) =
        abs(event.x - previousX) > 10 || abs(event.y - previousY) > 10

    private fun retrieveAttributes(attrs: AttributeSet) {
        val array = context.obtainStyledAttributes(attrs, R.styleable.BubblePicker)

        if (array.hasValue(R.styleable.BubblePicker_backgroundColor)) {
            background = array.getColor(R.styleable.BubblePicker_backgroundColor, -1)
        }

        array.recycle()
    }

    fun showMainItems() {
        engine.mode = Engine.Mode.MAIN
    }

    fun showSecondaryItems() {
        engine.mode = Engine.Mode.SECONDARY
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
