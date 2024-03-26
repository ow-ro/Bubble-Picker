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

    private var selectedItem: Item? = null
    private var standardIncreasedGravity = interpolate(500f, 800f, 0.5f)
    private var bubbleRadius = 0.17f
    private var world = World(Vec2(0f, 0f), false)
    private val step = 0.0009f
    private val circleBodies: ArrayList<CircleBody> = ArrayList()
    private var worldBorders: ArrayList<Border> = ArrayList()
    private val resizeStep = 0.009f
    private var scaleX = 0f
    private var scaleY = 0f
    private var touch = false
    private var increasedGravity = 55f
    private var gravityCenter = Vec2(0f, 0f)
    private var stepsCount = 0
    private val currentGravity: Float get() = if (touch) increasedGravity else speedToCenter
    private val toBeResized = synchronizedSet<Item>(mutableSetOf())
    private val startX get() = if (centerImmediately) 0.5f else 2.2f
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
                    }
                }
                toBeResized.addAll(allItems)
            }
        }
    var mainPickerItems: HashSet<PickerItem> = HashSet()
    var secondaryPickerItems: HashSet<PickerItem> = HashSet()
    var radius = 50
        set(value) {
            bubbleRadius = interpolate(0.1f, 0.25f, value / 100f)
            speedToCenter = interpolate(20f, 80f, value / 100f)
            standardIncreasedGravity = interpolate(500f, 800f, value / 100f)
            field = value
        }
    var centerImmediately = false
    var speedToCenter = 16f
    var horizontalSwipeOnly = false
    var margin = 0.001f
    lateinit var allItems: List<Item>

    fun build(pickerItems: List<PickerItem>, scaleX: Float, scaleY: Float): List<CircleBody> {
        pickerItems.forEach {
            val density = getDensity(it)
            val bubbleRadius = getRadius(it)
            val x = if (Random().nextBoolean()) -startX else startX
            val y = if (Random().nextBoolean()) -0.5f / scaleY else 0.5f / scaleY
            circleBodies.add(
                CircleBody(
                    world,
                    Vec2(x, y),
                    bubbleRadius * if (scaleY > scaleX) scaleY else scaleX,
                    (bubbleRadius * if (scaleY > scaleX) scaleY else scaleX) * 1.2f,
                    density = density,
                    shouldShow = shouldShowPickerItem(it),
                    margin = margin
                )
            )
        }
        Engine.scaleX = scaleX
        Engine.scaleY = scaleY
        createBorders()

        return circleBodies
    }

    fun move() {
        synchronized(toBeResized) {
            toBeResized.forEach { it.circleBody.resize(resizeStep) }
            world.step(step, 11, 11)
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
        touch = false
        increasedGravity = standardIncreasedGravity
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

    private fun shouldShowPickerItem(item: PickerItem): Boolean {
        return when {
            mode == Mode.MAIN && mainPickerItems.contains(item) -> true
            mode == Mode.SECONDARY && secondaryPickerItems.contains(item) -> true
            else -> false
        }
    }

    private fun getRadius(item: PickerItem): Float {
        return if (item.radius != 0f) {
            interpolate(0.1f, 0.25f, item.radius / 100f)
        } else {
            bubbleRadius
        }
    }

    private fun getDensity(item: PickerItem): Float {
        return if (item.radius != 0f) {
            interpolate(0.8f, 0.2f, item.radius / 100f)
        } else {
            interpolate(0.8f, 0.2f, bubbleRadius / 100f)
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
            body.isVisible = centerImmediately.not()
            val direction = gravityCenter.sub(position)
            val distance = direction.length()
            val gravity = if (body.increased) 1.2f * currentGravity else currentGravity
            if (distance > step * 200 && body != selectedItem?.circleBody) {
                applyForce(direction.mul(gravity * 5 / distance.sqr()), position)
            }
            if (body == selectedItem?.circleBody) {
                applyForce(direction.mul(6f * increasedGravity), worldCenter)
            }
        }
    }

    private fun interpolate(start: Float, end: Float, f: Float) = start + f * (end - start)
}