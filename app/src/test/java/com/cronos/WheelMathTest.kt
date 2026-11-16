package com.cronos

import com.cronos.util.WheelMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelMathTest {
    @Test
    fun normalizesAngleAcrossZero() {
        assertEquals(315.0, WheelMath.normalizedAngle(-45.0), 0.001)
        assertEquals(90.0, WheelMath.normalizedAngle(90.0), 0.001)
    }

    @Test
    fun opacityTransitionsWithoutBreakingBounds() {
        assertEquals(1.0f, WheelMath.opacityForRingAngle(180.0), 0.001f)
        assertEquals(0.0f, WheelMath.opacityForRingAngle(120.0), 0.001f)
        assertTrue(WheelMath.opacityForRingAngle(330.0) in 0.0f..1.0f)
    }

    @Test
    fun snapsToNearestStep() {
        assertEquals(90.0, WheelMath.snapToNearestStep(95.0, 90.0), 0.001)
        assertEquals(270.0, WheelMath.snapToNearestStep(280.0, 90.0), 0.001)
    }

    @Test
    fun guardsInvalidInputs() {
        assertEquals(0.0, WheelMath.normalizedAngle(Double.NaN), 0.001)
        assertEquals(0.0, WheelMath.snapToNearestStep(Double.NaN, 90.0), 0.001)
        assertEquals(45.0, WheelMath.snapToNearestStep(45.0, 0.0), 0.001)
        assertEquals(0.0f, WheelMath.opacityForRingAngle(Double.NaN), 0.001f)
    }
}
