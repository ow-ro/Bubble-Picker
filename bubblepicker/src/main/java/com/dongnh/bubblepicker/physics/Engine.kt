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
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Created by irinagalata on 1/26/17.
 */
class Engine {
    enum class Mode {
        MAIN, SECONDARY
    }

    private val STEP = 0.0009f
    private val RESIZE_STEP = 0.009f
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
    private var maxArea = 0f
    private var minArea = 0f
    var allItems: ArrayList<Item> = arrayListOf()
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
                            if (value == Mode.MAIN) {
                                // Main mode, we want the main sizings/densities
                                val mainRadius = getRadius(it.pickerItem.value, false)
                                density = getDensity(it.pickerItem.value, false)
                                defaultRadius = mainRadius * getScale()
                                increasedRadius = mainRadius * getScale() * 1.2f
                            } else {
                                // Secondary mode, we want the secondary sizings/densities
                                val secondaryRadius = getRadius(it.pickerItem.secondaryValue, true)
                                density = getDensity(it.pickerItem.secondaryValue, true)
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
        set(value) {
            field = value
            maxArea = getArea(value)
        }
    var minBubbleSize = 0.1f
        set(value) {
            field = value
            minArea = getArea(value)
        }

    private fun shouldShowPickerItem(item: PickerItem): Boolean {
        return when {
            mode == Mode.MAIN && !item.isSecondary -> true
            mode == Mode.SECONDARY && (item.isSecondary || item.secondaryValue != 0f) -> true
            else -> false
        }
    }

    private fun getRadius(value: Float, isSecondary: Boolean): Float {
        // Get interpolated area, return radius from it
        val scaledArea = if (!isSecondary) {
            interpolate(minArea, maxArea, value / mainMaxScale)
        } else {
            interpolate(minArea, maxArea, value / secondaryMaxScale)
        }
        return sqrt(scaledArea / Math.PI).toFloat()
    }

    private fun getDensity(value: Float, isSecondary: Boolean): Float {
        return if (!isSecondary) {
            interpolate(0.8f, 0.2f, value / mainMaxScale)
        } else {
            interpolate(0.8f, 0.2f, value / secondaryMaxScale)
        }
    }

    private fun getArea(radius: Float): Float {
        return (Math.PI * radius.pow(2)).toFloat()
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
            if (distance > STEP * 100 && body != selectedItem?.circleBody) {
                applyForce(direction.mul(gravity * 3 * distance.sqr()), position)
            }
            if (body == selectedItem?.circleBody && centerDirection.length() > STEP * 50) {
                applyForce(centerDirection.mul(7f * increasedGravity), gravityCenterFixed)
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
            val isSecondary = it.isSecondary
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