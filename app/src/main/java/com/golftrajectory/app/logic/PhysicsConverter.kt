package com.golftrajectory.app.logic

import com.google.mediapipe.tasks.components.containers.Landmark
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

object PhysicsConverter {
    fun calculateDistance3D(p1: Landmark, p2: Landmark): Float {
        return sqrt(
            (p1.x() - p2.x()).pow(2) +
            (p1.y() - p2.y()).pow(2) +
            (p1.z() - p2.z()).pow(2)
        )
    }

    fun calculateAngle3D(a: Landmark, b: Landmark, c: Landmark): Float {
        val ba = floatArrayOf(a.x() - b.x(), a.y() - b.y(), a.z() - b.z())
        val bc = floatArrayOf(c.x() - b.x(), c.y() - b.y(), c.z() - b.z())

        val dotProduct = ba[0] * bc[0] + ba[1] * bc[1] + ba[2] * bc[2]
        val magBA = sqrt(ba[0].pow(2) + ba[1].pow(2) + ba[2].pow(2))
        val magBC = sqrt(bc[0].pow(2) + bc[1].pow(2) + bc[2].pow(2))

        if (magBA == 0f || magBC == 0f) return 0f

        val cosTheta = (dotProduct / (magBA * magBC)).coerceIn(-1.0f, 1.0f)
        return Math.toDegrees(acos(cosTheta.toDouble())).toFloat()
    }

    fun getMidPoint(p1: Landmark, p2: Landmark): Landmark {
        return Landmark.create(
            (p1.x() + p2.x()) / 2f,
            (p1.y() + p2.y()) / 2f,
            (p1.z() + p2.z()) / 2f
        )
    }
}
