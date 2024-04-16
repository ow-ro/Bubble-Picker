package com.dongnh.bubblepicker.physics

import com.dongnh.bubblepicker.model.PickerItem
import com.dongnh.bubblepicker.rendering.Item
import com.dongnh.bubblepicker.sqr
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.World
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
                        if (it.pickerItem.secondaryValue != null) {
                            if (value == Mode.MAIN) {
                                // Main mode, we want the main sizings/densities
                                val mainRadius = getRadius(it.pickerItem.value)
                                density = getDensity(it.pickerItem.value)
                                defaultRadius = mainRadius * getScale()
                                increasedRadius = mainRadius * getScale() * 1.2f
                            } else {
                                // Secondary mode, we want the secondary sizings/densities
                                val secondaryRadius = getRadius(it.pickerItem.secondaryValue!!)
                                density = getDensity(it.pickerItem.secondaryValue!!)
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
    var speedToCenter = 16f
    var horizontalSwipeOnly = false
    var margin = 0.001f
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
            mode == Mode.SECONDARY && (item.isSecondary || item.secondaryValue != null) -> true
            else -> false
        }
    }

    private fun getRadius(value: Float): Float {
        // Get interpolated area, return radius from it
        val scaledArea = interpolate(minArea, maxArea, value)

        return sqrt(scaledArea / Math.PI).toFloat()
    }

    private fun getDensity(value: Float): Float {
        return interpolate(0.8f, 0.4f, value)
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
            if (distance > 1f && body != selectedItem?.circleBody) {
                applyForce(direction.mul(gravity * 3 * distance.sqr()), position)
            } else if (distance > 0.5f && body != selectedItem?.circleBody) {
                applyForce(direction.mul(gravity * 3 / distance.sqr()), position)
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

    /**
     * Generates a list of coordinates for the bubbles to be placed at in a rectangular
     * alternating pattern, this helps with putting larger items in the center which are
     * at the start of the list in the build function.
     * For example: 8 4 0 2 6
     *              9 5 1 3 7
     * @param numPoints The number of bubbles to generate coordinates for
     * @return A list of coordinates for the bubbles
     */
    private fun generateCoordinates(numPoints: Int): List<Pair<Float, Float>> {
        val coordinates = mutableListOf<Pair<Float, Float>>()
        var x = 0f
        var y = 0.5f
        for (i in 0 until numPoints) {
            // Subtract 2 because the first 2 coordinates are (0, 0.5) and (0, -0.5)
            if ((i - 2) % 2 == 0) {
                x *= -1
            }
            if ((i - 2) % 4 == 0) {
                x += 0.5f
            }
            y *= -1
            coordinates.add(Pair(x, y))
        }
        return coordinates
    }

    fun build(pickerItems: List<PickerItem>, scaleX: Float, scaleY: Float): List<CircleBody> {
        this.scaleX = scaleX
        this.scaleY = scaleY
        val coords = generateCoordinates(pickerItems.size)
        pickerItems.filter { !it.isSecondary }.forEachIndexed { i, it ->
            val density = getDensity(it.value)
            val bubbleRadius = getRadius(it.value)
            val x = coords[i].first
            val y = coords[i].second
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
        pickerItems.filter { it.isSecondary }.forEachIndexed { i, it ->
            val density = getDensity(it.value)
            val bubbleRadius = getRadius(it.value)
            val x = coords[i].first
            val y = coords[i].second
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