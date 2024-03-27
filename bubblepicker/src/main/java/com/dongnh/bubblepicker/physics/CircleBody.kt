package com.dongnh.bubblepicker.physics

import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.*
import kotlin.math.abs

/**
 * Created by irinagalata on 1/26/17.
 */
class CircleBody(
    private val world: World,
    var position: Vec2,
    private var defaultRadius: Float,
    private var increasedRadius: Float,
    var density: Float,
    var shouldShow: Boolean = true,
    private val margin: Float = 0.001f
) {
    var physicalBody: Body? = null
    private var isIncreasing = false
    private var isDecreasing = false
    private var toBeDecreased = false
    private val damping = 25f
    private val shape: CircleShape
        get() = CircleShape().apply {
            m_radius = defaultRadius + margin
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
    val finished: Boolean
        get() = !toBeIncreased && !toBeDecreased && !isIncreasing && !isDecreasing
    val isBusy: Boolean
        get() = isIncreasing || isDecreasing
    var isVisible: Boolean = true
    var isDestroyed = true
    var toBeIncreased: Boolean = false
    var actualRadius: Float = if (shouldShow) {
        defaultRadius + margin
    } else {
        0f
    }
    var increased = false

    init {
        while (shouldShow) {
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
        isDestroyed = false
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

        if (abs(actualRadius - defaultRadius) < step) {
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
        if (isDestroyed) {
            initializeBody()
        }

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
            world.destroyBody(physicalBody)
            isDestroyed = true
            clear()
        }
    }

    private fun reset() {
        physicalBody?.fixtureList?.shape?.m_radius = actualRadius + margin
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