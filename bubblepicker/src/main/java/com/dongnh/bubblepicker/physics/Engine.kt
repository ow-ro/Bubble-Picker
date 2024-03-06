package com.dongnh.bubblepicker.physics

import com.dongnh.bubblepicker.model.PickerItem
import com.dongnh.bubblepicker.rendering.Item
import com.dongnh.bubblepicker.sqr
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.World
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

/**
 * Created by irinagalata on 1/26/17.
 */
object Engine {
//    val selectedBodies: List<CircleBody>
//        get() = bodies.filter { it.increased || it.toBeIncreased || it.isIncreasing }

    var selectedItem: Item? = null

    var radius = 50
        set(value) {
            bubbleRadius = interpolate(0.1f, 0.25f, value / 100f)
            speedToCenter = interpolate(20f, 80f, value / 100f)
            standardIncreasedGravity = interpolate(500f, 800f, value / 100f)
            field = value
        }
    var centerImmediately = false
    private var standardIncreasedGravity = interpolate(500f, 800f, 0.5f)
    private var bubbleRadius = 0.17f

    private var world = World(Vec2(0f, 0f), false)
    private val step = 0.0009f
    private val bodies: ArrayList<CircleBody> = ArrayList()
    private var borders: ArrayList<Border> = ArrayList()
    private val resizeStep = 0.009f
    private var scaleX = 0f
    private var scaleY = 0f
    private var touch = false
    var speedToCenter = 16f
    private var increasedGravity = 55f
    private var gravityCenter = Vec2(0f, 0f)
    var horizontalSwipeOnly = false
    private val currentGravity: Float
        get() = if (touch) increasedGravity else speedToCenter
    private val toBeResized = mutableSetOf<Item>()
    private val startX
        get() = if (centerImmediately) 0.5f else 2.2f
    private var stepsCount = 0
    var margin = 0.001f

    fun build(pickerItems: List<PickerItem>, scaleX: Float, scaleY: Float): List<CircleBody> {
        pickerItems.forEach {
            val density = getDensity(it)
            val bubbleRadius = getRadius(it)
            val x = if (Random().nextBoolean()) -startX else startX
            val y = if (Random().nextBoolean()) -0.5f / scaleY else 0.5f / scaleY
            bodies.add(
                CircleBody(
                    world,
                    Vec2(x, y),
                    bubbleRadius * if (scaleY > scaleX) scaleY else scaleX,
                    (bubbleRadius * if (scaleY > scaleX) scaleY else scaleX) * 1.2f,
                    density = density,
                    margin = margin
                )
            )
        }
        Engine.scaleX = scaleX
        Engine.scaleY = scaleY
        createBorders()

        return bodies
    }

    fun move() {
        toBeResized.forEach { it.circleBody.resize(resizeStep) }
        world.step(step, 11, 11)
        bodies.forEach { move(it) }
        toBeResized.removeAll(toBeResized.filter { it.circleBody.finished }.toSet())
        stepsCount++
        if (stepsCount >= 10) {
            centerImmediately = false
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
        borders.forEach { world.destroyBody(it.itemBody) }
        bodies.forEach { world.destroyBody(it.physicalBody) }
        world = World(Vec2(0f, 0f), false)
        borders.clear()
        bodies.clear()
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
        borders = arrayListOf(
            Border(world, Vec2(0f, 0.5f / scaleY), Border.HORIZONTAL),
            Border(world, Vec2(0f, -0.5f / scaleY), Border.HORIZONTAL),
//            Border(world, Vec2(-0.5f / scaleX, 0f), Border.VERTICAL),
//            Border(world, Vec2(0.5f / scaleX, 0f), Border.VERTICAL)
        )
    }

    private fun move(body: CircleBody) {
        body.physicalBody.apply {
            body.isVisible = centerImmediately.not()
            val direction = gravityCenter.sub(position)
            val distance = direction.length()
            val gravity = if (body.increased) 1.2f * currentGravity else currentGravity
            if (distance > step * 200 && body != selectedItem?.circleBody) {
                applyForce(direction.mul(gravity * 5 / distance.sqr()), position)
            }
            if (body == selectedItem?.circleBody) {
                applyForce(direction.mul(6f * increasedGravity), this.worldCenter)
            }
        }
    }

    private fun interpolate(start: Float, end: Float, f: Float) = start + f * (end - start)

}