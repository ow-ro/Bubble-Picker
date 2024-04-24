package com.dongnh.bubblepicker.physics

import com.dongnh.bubblepicker.model.PickerItem
import com.dongnh.bubblepicker.rendering.Item
import com.dongnh.bubblepicker.sqr
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.World
import java.util.Collections.synchronizedSet
import kotlin.collections.ArrayList
import kotlin.math.abs

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
    var selectedItem: Item? = null
    var allItems: ArrayList<Item> = arrayListOf()
    var mode: Mode = Mode.MAIN
        set(newMode) {
            // Don't do anything if the mode is the same
            if (newMode != field) {
                field = newMode
                selectedItem = null
                allItems.forEach {
                    it.circleBody.apply {
                        increased = false
                        shouldShow = shouldShowPickerItem(it.pickerItem)

                        // Only need to do this for duplicate items
                        if (it.pickerItem.secondaryValue != null) {
                            if (newMode == Mode.MAIN) {
                                // Main mode, we want the main sizings/densities
                                density = getDensity(it.pickerItem.value)
                                defaultRadius = it.pickerItem.value * getScale()
                                increasedRadius = it.pickerItem.value * getScale() * 1.2f
                                value = it.pickerItem.value
                            } else {
                                // Secondary mode, we want the secondary sizings/densities
                                density = getDensity(it.pickerItem.secondaryValue!!)
                                defaultRadius = it.pickerItem.secondaryValue!! * getScale()
                                increasedRadius = it.pickerItem.secondaryValue!! * getScale() * 1.2f
                                value = it.pickerItem.secondaryValue!!
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

    private fun shouldShowPickerItem(item: PickerItem): Boolean {
        return when {
            mode == Mode.MAIN && !item.isSecondary -> true
            mode == Mode.SECONDARY && (item.isSecondary || item.secondaryValue != null) -> true
            else -> false
        }
    }

    private fun getDensity(value: Float): Float {
        return interpolate(0.8f, 0.4f, value)
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
            if (distance > STEP * 300 && body != selectedItem?.circleBody) {
                // apply more force to top 30% of bubbles
                if (body.value >= 0.7f) {
                    // pull in bubbles that are far away
                    if (distance > 1.5f) {
                        applyForce(direction.mul(gravity * 3 * distance.sqr()), position)
                    } else {
                        applyForce(direction.mul(gravity * 5 / distance.sqr()), position)
                    }
                } else {
                    if (distance > 1.5f) {
                        applyForce(direction.mul(gravity * distance.sqr()), position)
                    } else {
                        applyForce(direction.mul(gravity / distance.sqr()), position)
                    }
                }
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
     * alternating pattern.
     * For example:
     * ```
     * 8 4 0 2 6
     * 9 5 1 3 7
     * ```
     * Where each number is the index of the item in the list
     * @param numPoints The number of bubbles to generate coordinates for
     * @return A list of coordinates for the bubbles
     */
    private fun getCoordinates(numPoints: Int): List<Pair<Float, Float>> {
        val coordinates = mutableListOf<Pair<Float, Float>>()
        var x = 0f
        var y = 0f
        for (i in 0 until numPoints) {
            if (i == 2) y = 0.15f
            // Subtract 2 because the first 2 coordinates are (0, 0.15) and (0, -0.15)
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
        val coords = getCoordinates(pickerItems.size)
        var secondaryIndex = 0
        pickerItems.forEachIndexed { i, it ->
            val density = getDensity(it.value)
            val bubbleRadius = it.value

            val x = if (it.isSecondary) {
                coords[secondaryIndex].first
            } else {
                coords[i].first
            }
            val y = if (it.isSecondary) {
                val index = secondaryIndex
                secondaryIndex++
                coords[index].second
            } else {
                coords[i].second
            }

            circleBodies.add(
                CircleBody(
                    world,
                    Vec2(x, y),
                    bubbleRadius * getScale(),
                    bubbleRadius * getScale() * 1.2f,
                    density = density,
                    shouldShow = shouldShowPickerItem(it),
                    value = it.value,
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

    fun resize(item: Item, resizeOnDeselect: Boolean): Boolean {
        /** If an item different than currently selected item was clicked,
         * we queue up the currently selected item to be downsized */
        if (item != selectedItem && selectedItem?.circleBody?.isBusy == false) {
            selectedItem?.also {
                it.circleBody.defineState()
                toBeResized.add(it)
            }
        }

        if (item.circleBody.isBusy) return false
        if (!resizeOnDeselect && selectedItem == item) return false
        item.circleBody.defineState()
        toBeResized.add(item)

        // If a default radius item is clicked, we queue up the item to be upsized
        selectedItem = if (item.circleBody.toBeIncreased) {
            item
        } else {
            null
        }

        return true
    }
}