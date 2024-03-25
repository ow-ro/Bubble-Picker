package com.dongnh.bubblepicker.physics

import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.*
import kotlin.math.abs

/**
 * Created by irinagalata on 1/26/17.
 */
class CircleBody(
    val world: World,
    var position: Vec2,
    var defaultRadius: Float,
    var increasedRadius: Float,
    var density: Float,
    var shouldShow: Boolean = true,
    private val margin: Float = 0.001f
) {
    lateinit var physicalBody: Body
    private var decreasedRadius: Float = defaultRadius
    private var isIncreasing = false
    private var isDecreasing = false
    private var toBeDecreased = false
    private val damping = 25f
    private val shape: CircleShape
        get() = CircleShape().apply {
            m_radius = actualRadius
            m_p.setZero()
        }
    private val fixture: FixtureDef
        get() = FixtureDef().apply {
            this.shape = this@CircleBody.shape
            this.density = this@CircleBody.density
        }
    private val bodyDef: BodyDef
        get() = BodyDef().apply {
            type = BodyType.DYNAMIC
            this.position = this@CircleBody.position
        }
    var isVisible: Boolean = true
    var toBeIncreased: Boolean = false
    var actualRadius: Float = if (shouldShow) {
        defaultRadius + margin
    } else {
        0f
    }
    val finished: Boolean
        get() = !toBeIncreased && !toBeDecreased && !isIncreasing && !isDecreasing
    val isBusy: Boolean
        get() = isIncreasing || isDecreasing
    var increased = false

    init {
        while (true) {
            if (world.isLocked.not()) {
                initializeBody()
                break
            }
        }
    }

    private fun initializeBody() {
        physicalBody = world.createBody(bodyDef).apply {
            createFixture(fixture)
            linearDamping = damping
        }
    }

    fun resize(step: Float) {
        when {
            actualRadius < defaultRadius && shouldShow -> inflate(step)
            actualRadius > 0f && !shouldShow -> deflate(step)
            shouldShow -> if (increased) {
                decrease(step)
            } else {
                increase(step)
            }
        }
    }

    private fun decrease(step: Float) {
        isDecreasing = true
        actualRadius -= step
        reset()

        if (abs(actualRadius - decreasedRadius) < step) {
            increased = false
            clear()
        }
    }

    private fun increase(step: Float) {
        isIncreasing = true
        actualRadius += step
        reset()

        if (abs(actualRadius - increasedRadius) < step) {
            increased = true
            clear()
        }
    }

    private fun inflate(step: Float) {
        isVisible = true
        isIncreasing = true
        actualRadius = if (actualRadius + step < defaultRadius) {
            actualRadius + step
        } else {
            defaultRadius
        }
        reset()

        if (actualRadius == defaultRadius) {
            clear()
        }
    }

    private fun deflate(step: Float) {
        isDecreasing = true
        actualRadius = if (actualRadius - step > 0) {
            actualRadius - step
        } else {
            0f
        }
        reset()

        if (actualRadius == 0f) {
            isVisible = false
            clear()
        }
    }

    private fun reset() {
        physicalBody.fixtureList?.shape?.m_radius = actualRadius + margin
    }

    fun defineState() {
        toBeIncreased = !increased
        toBeDecreased = increased
    }

    private fun clear() {
        toBeIncreased = false
        toBeDecreased = false
        isIncreasing = false
        isDecreasing = false
    }

}