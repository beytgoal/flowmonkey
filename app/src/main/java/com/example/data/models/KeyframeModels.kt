package com.example.data.models

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.sin
import kotlin.math.PI

/**
 * Represents an individual keyframe point on an overlay/clip timeline.
 */
data class KeyframePoint(
    val timeOffsetMs: Long,     // Milliseconds from start of clip
    val posX: Float = 0f,       // X offset in DP (-300f .. 300f)
    val posY: Float = 0f,       // Y offset in DP (-300f .. 300f)
    val scale: Float = 1.0f,    // Scale multiplier (0.1f .. 4.0f)
    val rotation: Float = 0f,   // Rotation degrees (-360f .. 360f)
    val opacity: Float = 1.0f,  // Alpha opacity (0.0f .. 1.0f)
    val easing: String = "Linear" // "Linear", "Ease In", "Ease Out", "Ease In Out"
)

/**
 * Result of evaluating interpolated transforms at an arbitrary playback timestamp.
 */
data class KeyframeTransform(
    val posX: Float = 0f,
    val posY: Float = 0f,
    val scale: Float = 1.0f,
    val rotation: Float = 0f,
    val opacity: Float = 1.0f
)

object KeyframeHelper {

    /**
     * Parses keyframes from stored string (supports JSON array or legacy JSON object).
     */
    fun parseKeyframes(data: String): List<KeyframePoint> {
        if (data.isBlank()) return emptyList()
        val list = mutableListOf<KeyframePoint>()
        try {
            val trimmed = data.trim()
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        KeyframePoint(
                            timeOffsetMs = obj.optLong("timeOffsetMs", obj.optLong("t", 0L)),
                            posX = obj.optDouble("posX", obj.optDouble("x", 0.0)).toFloat(),
                            posY = obj.optDouble("posY", obj.optDouble("y", 0.0)).toFloat(),
                            scale = obj.optDouble("scale", obj.optDouble("s", 1.0)).toFloat(),
                            rotation = obj.optDouble("rotation", obj.optDouble("r", 0.0)).toFloat(),
                            opacity = obj.optDouble("opacity", obj.optDouble("o", 1.0)).toFloat(),
                            easing = obj.optString("easing", "Linear")
                        )
                    )
                }
            } else if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                list.add(
                    KeyframePoint(
                        timeOffsetMs = obj.optLong("timeOffsetMs", 0L),
                        posX = obj.optDouble("posX", 0.0).toFloat(),
                        posY = obj.optDouble("posY", 0.0).toFloat(),
                        scale = obj.optDouble("scale", 1.0).toFloat(),
                        rotation = obj.optDouble("rotation", 0.0).toFloat(),
                        opacity = obj.optDouble("opacity", 1.0).toFloat(),
                        easing = obj.optString("ease", "Linear")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedBy { it.timeOffsetMs }
    }

    /**
     * Serializes keyframe list to a compact JSON array string.
     */
    fun serializeKeyframes(keyframes: List<KeyframePoint>): String {
        if (keyframes.isEmpty()) return ""
        val array = JSONArray()
        keyframes.sortedBy { it.timeOffsetMs }.forEach { kf ->
            val obj = JSONObject()
            obj.put("t", kf.timeOffsetMs)
            obj.put("x", Math.round(kf.posX * 10) / 10.0)
            obj.put("y", Math.round(kf.posY * 10) / 10.0)
            obj.put("s", Math.round(kf.scale * 100) / 100.0)
            obj.put("r", Math.round(kf.rotation * 10) / 10.0)
            obj.put("o", Math.round(kf.opacity * 100) / 100.0)
            obj.put("easing", kf.easing)
            array.put(obj)
        }
        return array.toString()
    }

    /**
     * Interpolates transforms at a specific clip-relative time offset in milliseconds.
     */
    fun evaluateTransform(
        keyframes: List<KeyframePoint>,
        clipTimeOffsetMs: Long,
        defaultPosX: Float = 0f,
        defaultPosY: Float = 0f,
        defaultScale: Float = 1.0f,
        defaultRotation: Float = 0f,
        defaultOpacity: Float = 1.0f
    ): KeyframeTransform {
        if (keyframes.isEmpty()) {
            return KeyframeTransform(
                posX = defaultPosX,
                posY = defaultPosY,
                scale = defaultScale,
                rotation = defaultRotation,
                opacity = defaultOpacity
            )
        }

        val sorted = keyframes.sortedBy { it.timeOffsetMs }

        // If before first keyframe
        if (clipTimeOffsetMs <= sorted.first().timeOffsetMs) {
            val first = sorted.first()
            return KeyframeTransform(
                posX = first.posX,
                posY = first.posY,
                scale = first.scale,
                rotation = first.rotation,
                opacity = first.opacity
            )
        }

        // If after last keyframe
        if (clipTimeOffsetMs >= sorted.last().timeOffsetMs) {
            val last = sorted.last()
            return KeyframeTransform(
                posX = last.posX,
                posY = last.posY,
                scale = last.scale,
                rotation = last.rotation,
                opacity = last.opacity
            )
        }

        // Find surrounding keyframe interval
        for (i in 0 until sorted.size - 1) {
            val k1 = sorted[i]
            val k2 = sorted[i + 1]
            if (clipTimeOffsetMs in k1.timeOffsetMs..k2.timeOffsetMs) {
                val intervalDuration = (k2.timeOffsetMs - k1.timeOffsetMs).coerceAtLeast(1L)
                val linearFraction = ((clipTimeOffsetMs - k1.timeOffsetMs).toFloat() / intervalDuration.toFloat()).coerceIn(0f, 1f)
                val easedFraction = applyEasing(linearFraction, k1.easing)

                return KeyframeTransform(
                    posX = lerp(k1.posX, k2.posX, easedFraction),
                    posY = lerp(k1.posY, k2.posY, easedFraction),
                    scale = lerp(k1.scale, k2.scale, easedFraction),
                    rotation = lerp(k1.rotation, k2.rotation, easedFraction),
                    opacity = lerp(k1.opacity, k2.opacity, easedFraction)
                )
            }
        }

        val last = sorted.last()
        return KeyframeTransform(
            posX = last.posX,
            posY = last.posY,
            scale = last.scale,
            rotation = last.rotation,
            opacity = last.opacity
        )
    }

    private fun lerp(start: Float, stop: Float, fraction: Float): Float {
        return start + (stop - start) * fraction
    }

    private fun applyEasing(fraction: Float, easing: String): Float {
        return when (easing) {
            "Ease In" -> fraction * fraction
            "Ease Out" -> fraction * (2f - fraction)
            "Ease In Out" -> if (fraction < 0.5f) {
                2f * fraction * fraction
            } else {
                -1f + (4f - 2f * fraction) * fraction
            }
            "Sine Wave" -> ((sin((fraction - 0.5) * PI) + 1) / 2.0).toFloat()
            else -> fraction // Linear
        }
    }
}
