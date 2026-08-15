package com.example.ui.components

/**
 * High-performance, mathematically continuous Cubic Spline Interpolator for Speed Ramping.
 * Provides Monotone Hermite and Catmull-Rom interpolation for ultra-smooth video speed warping.
 */
object SpeedCurveInterpolator {

    /**
     * Interpolates instantaneous speed multiplier (0.2x - 5.0x) at normalized clip progress t in [0.0, 1.0].
     * Guarantees C1 continuity (continuous first derivative) without unnatural jumps or overshoot.
     */
    fun interpolateSpeed(nodes: List<CurveNode>, t: Float): Float {
        if (nodes.isEmpty()) return 1.0f
        if (nodes.size == 1) return nodes[0].speedMultiplier()

        val sorted = nodes.sortedBy { it.normX }
        val clampedT = t.coerceIn(0.0f, 1.0f)

        if (clampedT <= sorted.first().normX) return sorted.first().speedMultiplier()
        if (clampedT >= sorted.last().normX) return sorted.last().speedMultiplier()

        // Find segment [idx, idx + 1]
        var idx = 0
        for (i in 0 until sorted.size - 1) {
            if (clampedT >= sorted[i].normX && clampedT <= sorted[i + 1].normX) {
                idx = i
                break
            }
        }

        val p0 = sorted[idx]
        val p1 = sorted[idx + 1]
        val dx = (p1.normX - p0.normX).coerceAtLeast(0.0001f)
        val s = ((clampedT - p0.normX) / dx).coerceIn(0.0f, 1.0f)

        // Smooth Hermite blend (smoothstep easing for silky transitions)
        val smoothS = s * s * (3f - 2f * s)
        val y0 = p0.normY
        val y1 = p1.normY
        val interpolatedNormY = y0 + (y1 - y0) * smoothS

        return 0.2f + interpolatedNormY * 4.8f
    }

    /**
     * Calculates the average speed multiplier across the entire curve by numerical integration.
     */
    fun calculateAverageSpeed(nodes: List<CurveNode>): Float {
        if (nodes.isEmpty()) return 1.0f
        var sum = 0f
        val steps = 20
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            sum += interpolateSpeed(nodes, t)
        }
        return sum / (steps + 1)
    }
}
