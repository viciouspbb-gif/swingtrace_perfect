package com.golftrajectory.app.logic

import kotlin.math.PI
import kotlin.math.abs

class OneEuroFilter(
    private var minCutoff: Float = 1.0f,
    private var beta: Float = 0.0f,
    private var dCutoff: Float = 1.0f
) {
    private var xPrev: Float? = null
    private var dxPrev: Float = 0.0f
    private var tPrev: Long? = null

    fun filter(x: Float, t: Long): Float {
        if (tPrev == null || xPrev == null) {
            xPrev = x
            dxPrev = 0.0f
            tPrev = t
            return x
        }

        val dt = (t - tPrev!!) / 1000.0f
        if (dt <= 0.0f) return x

        val alphaD = smoothingFactor(dt, dCutoff)
        val dx = (x - xPrev!!) / dt
        val dxHat = exponentialSmoothing(alphaD, dx, dxPrev)

        val cutoff = minCutoff + beta * abs(dxHat)
        val alpha = smoothingFactor(dt, cutoff)
        val xHat = exponentialSmoothing(alpha, x, xPrev!!)

        xPrev = xHat
        dxPrev = dxHat
        tPrev = t

        return xHat
    }

    private fun smoothingFactor(dt: Float, cutoff: Float): Float {
        val r = 2.0 * PI * cutoff * dt
        return (r / (r + 1.0)).toFloat()
    }

    private fun exponentialSmoothing(alpha: Float, x: Float, xPrev: Float): Float {
        return alpha * x + (1.0f - alpha) * xPrev
    }
}
