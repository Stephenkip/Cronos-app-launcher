package com.cronos.util

import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

object WheelMath {
    fun normalizedAngle(angleDegrees: Double): Double {
        if (!angleDegrees.isFinite()) return 0.0

        val wrapped = angleDegrees % 360.0
        return if (wrapped < 0.0) wrapped + 360.0 else wrapped
    }

    fun opacityForRingAngle(angleDegrees: Double): Float {
        if (!angleDegrees.isFinite()) return 0.0f

        val normalized = normalizedAngle(angleDegrees)
        return when {
            normalized <= 75.0 -> ((75.0 - normalized) / 75.0).toFloat()
            normalized <= 150.0 -> 0.0f
            normalized <= 180.0 -> ((normalized - 150.0) / 30.0).toFloat()
            else -> 1.0f
        }.coerceIn(0f, 1f)
    }

    fun scaleForRingAngle(angleDegrees: Double): Float {
        if (!angleDegrees.isFinite()) return 0.6f

        val normalized = normalizedAngle(angleDegrees).toFloat()
        return when {
            normalized <= 75.0f -> (0.6f + ((75.0f - normalized) / 75.0f) * 0.4f)
            normalized <= 150.0f -> 0.6f
            normalized <= 180.0f -> (0.6f + ((normalized - 150.0f) / 30.0f) * 0.4f)
            else -> 1.0f
        }.coerceIn(0.6f, 1.0f)
    }

    fun snapToNearestStep(rawAngle: Double, step: Double): Double {
        if (!rawAngle.isFinite() || !step.isFinite() || step <= 0.0) {
            return normalizedAngle(rawAngle)
        }

        val snapped = round(rawAngle / step) * step
        return normalizedAngle(snapped)
    }

    fun angleToCartesian(angleDegrees: Double, radius: Double): Pair<Double, Double> {
        if (!angleDegrees.isFinite() || !radius.isFinite()) {
            return 0.0 to 0.0
        }

        val radians = Math.toRadians(angleDegrees - 90.0)
        return cos(radians) * radius to sin(radians) * radius
    }
}
