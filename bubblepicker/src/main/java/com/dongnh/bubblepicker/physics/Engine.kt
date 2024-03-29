package com.dongnh.bubblepicker.physics

import com.dongnh.bubblepicker.model.PickerItem
import com.dongnh.bubblepicker.rendering.Item
import com.dongnh.bubblepicker.sqr
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.World
import java.util.*
import java.util.Collections.synchronizedSet
import kotlin.collections.ArrayList
import kotlin.math.abs

/**
 * Created by irinagalata on 1/26/17.
 */
object Engine {
    enum class Mode {
        MAIN, SECONDARY
    }

    private const val STEP = 0.0009f
    private const val RESIZE_STEP = 0.009f
    private val circleBodies: ArrayList<CircleBody> = ArrayList()
    private val gravityCenterFixed = Vec2(0f, 0f)
    private val toBeResized = synchronizedSet<Item>(mutableSetOf())
    private val startX get() = if (centerImmediately) 0.5f else 2.2f
    private val currentGravity: Float get() = if (touch) increasedGravity else speedToCenter
    private var selectedItem: Item? = null
    private var standardIncreasedGravity = interpolate(500f, 800f, 0.5f)
    private var world = World(Vec2(0f, 0f), false)
    private var worldBorders: ArrayList<Border> = ArrayList()
    private var scaleX = 0f
    private var scaleY = 0f
    private var touch = false
    private var increasedGravity = 55f
    private var gravityCenter = Vec2(0f, 0f)
    private var stepsCount = 0
    private var didModeChange = false
    lateinit var allItems: List<Item>
    var mainPickerItems: HashSet<PickerItem> = HashSet()
    var secondaryPickerItems: HashSet<PickerItem> = HashSet()
    var mode: Mode = Mode.MAIN
        set(value) {
            // Don't do anything if the mode is the same
            if (value != field) {
                field = value
                selectedItem = null
                allItems.forEach {
                    it.circleBody.apply {
                        increased = false
                        shouldShow = shouldShowPickerItem(it.pickerItem)

                        // Only need to do this for duplicate items
                        if (it.pickerItem.secondaryValue != 0f) {
                            val isSecondary = it.pickerItem.value > mainMaxScale
                            if (value == Mode.MAIN) {
                                val mainRadius = getRadius(it.pickerItem.value, isSecondary)
                                density = getDensity(it.pickerItem.value, isSecondary)
                                defaultRadius = mainRadius * getScale()
                                increasedRadius = mainRadius * getScale() * 1.2f
                            } else {
                                val secondaryRadius = getRadius(it.pickerItem.secondaryValue, isSecondary)
                                density = getDensity(it.pickerItem.secondaryValue, isSecondary)
                                defaultRadius = secondaryRadius * getScale()
                                increasedRadius = secondaryRadius * getScale() * 1.2f
                            }
                        }
                    }
                }
                toBeResized.addAll(allItems)
                didModeChange = true
            }
        }
    var centerImmediately = false
    var speedToCenter = 16f
    var horizontalSwipeOnly = false
    var margin = 0.001f
    var mainMaxScale: Float = 0f
    var secondaryMaxScale: Float = 0f
    var maxBubbleSize = 0.4f
    var minBubbleSize = 0.1f


    private fun shouldShowPickerItem(item: PickerItem): Boolean {
        return when {
            mode == Mode.MAIN && mainPickerItems.any { it.title == item.title } -> true
            mode == Mode.SECONDARY && secondaryPickerItems.any { it.title == item.title } -> true
            else -> false
        }
    }

    private fun getRadius(value: Float, isSecondary: Boolean): Float {
        return if (!isSecondary) {
            interpolate(minBubbleSize, maxBubbleSize, value / mainMaxScale)
        } else {
            interpolate(minBubbleSize, maxBubbleSize, value / secondaryMaxScale)
        }
    }

    private fun getDensity(value: Float, isSecondary: Boolean): Float {
        return if (!isSecondary) {
            interpolate(0.2f, 0.4f, value / mainMaxScale)
        } else {
            interpolate(0.2f, 0.4f, value / secondaryMaxScale)
        }
    }


    private fun createBorders() {
        worldBorders = arrayListOf(
            Border(world, Vec2(0f, 0.5f / scaleY), Border.HORIZONTAL),
            Border(world, Vec2(0f, -0.5f / scaleY), Border.HORIZONTAL),
        )
    }

    private fun move(body: CircleBody) {
        body.physicalBody?.apply {
            body.position = position
            val centerDirection = gravityCenterFixed.sub(position)
            val direction = gravityCenter.sub(position)
            val distance = direction.length()
            val gravity = if (body.increased) 1.2f * currentGravity else currentGravity
            if (distance > STEP * 200 && body != selectedItem?.circleBody) {
                applyForce(direction.mul(gravity * 5 / distance.sqr()), position)
            }
            if (body == selectedItem?.circleBody && centerDirection.length() > STEP * 50) {
                applyForce(centerDirection.mul(6f * increasedGravity), gravityCenterFixed)
            }
        }
    }

    private fun interpolate(start: Float, end: Float, f: Float) = start + f * (end - start)

    private fun getScale(): Float {
        return if (scaleY > scaleX) {
            scaleY
        } else {
            scaleX
        }
    }

    fun build(pickerItems: List<PickerItem>, scaleX: Float, scaleY: Float): List<CircleBody> {
        this.scaleX = scaleX
        this.scaleY = scaleY
        pickerItems.forEach {
            val isSecondary = it.value > mainMaxScale
            val density = getDensity(it.value, isSecondary)
            val bubbleRadius = getRadius(it.value, isSecondary)
            val x = if (Random().nextBoolean()) -startX else startX
            val y = if (Random().nextBoolean()) -0.5f / scaleY else 0.5f / scaleY
            circleBodies.add(
                CircleBody(
                    world,
                    Vec2(x, y),
                    bubbleRadius * getScale(),
                    bubbleRadius * getScale() * 1.2f,
                    density = density,
                    shouldShow = shouldShowPickerItem(it),
                    margin = margin
                )
            )
        }

        createBorders()

        return circleBodies
    }

    fun move() {
        synchronized(toBeResized) {
            toBeResized.forEach { it.circleBody.resize(RESIZE_STEP) }
            world.step(STEP, 11, 11)
            circleBodies.forEach { move(it) }
            toBeResized.removeAll(toBeResized.filter { it.circleBody.finished }.toSet())
            stepsCount++
            if (stepsCount >= 10) {
                centerImmediately = false
            }
        }
    }

    fun swipe(x: Float, y: Float) {
        gravityCenter.x += -x
        if (!horizontalSwipeOnly) {
            gravityCenter.y += y
        } else {
            gravityCenter.y = 0f
        }
        increasedGravity = standardIncreasedGravity * abs(x * 13) * abs(y * 13)
        touch = true
    }

    fun release() {
        touch = false
        increasedGravity = standardIncreasedGravity
    }

    fun releaseWithReset() {
        gravityCenter.setZero()
    }

    fun clear() {
        worldBorders.forEach { world.destroyBody(it.itemBody) }
        circleBodies.forEach {
            it.physicalBody?.let { body -> world.destroyBody(body) }
        }
        world = World(Vec2(0f, 0f), false)
        worldBorders.clear()
        circleBodies.clear()
    }

    fun resize(item: Item): Boolean {
        if (item != selectedItem && selectedItem?.circleBody?.isBusy == false) {
            selectedItem?.also {
                it.circleBody.defineState()
                toBeResized.add(it)
            }
        }

        if (item.circleBody.isBusy) return false
        item.circleBody.defineState()
        toBeResized.add(item)
        selectedItem = if (item.circleBody.toBeIncreased) {
            item
        } else {
            null
        }

        return true
    }
}