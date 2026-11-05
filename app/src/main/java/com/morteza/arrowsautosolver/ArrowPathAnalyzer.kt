package com.morteza.arrowsautosolver

import android.graphics.Bitmap
import kotlin.math.max

enum class ArrowPathStatus { CLEAR, BLOCKED, UNKNOWN }

data class ArrowPathResult(
    val detection: ArrowHeadDetection,
    val status: ArrowPathStatus,
    val firstObstacle: ArrowPoint? = null,
    val sampledPixels: Int = 0
)

/**
 * Checks the forward ray from each detected arrowhead against dark pixels.
 * The arrow shaft extends behind its head, so the forward ray is the exit path.
 *
 * This is an initial visibility/path test. It does not yet identify which
 * arrow owns an obstacle pixel; ownership is needed for a full move simulation.
 */
object ArrowPathAnalyzer {
    fun analyze(
        screenshot: Bitmap,
        detections: List<ArrowHeadDetection>,
        darkThreshold: Int = 150,
        clearanceAfterHead: Int = 7,
        sampleStep: Int = 2,
        maxSamples: Int = 5000
    ): List<ArrowPathResult> {
        if (screenshot.width <= 0 || screenshot.height <= 0) return emptyList()
        val step = sampleStep.coerceAtLeast(1)
        val pixels = IntArray(screenshot.width * screenshot.height)
        screenshot.getPixels(pixels, 0, screenshot.width, 0, 0, screenshot.width, screenshot.height)

        return detections.map { arrow ->
            val (dx, dy) = when (arrow.direction) {
                ArrowDirection.UP -> 0 to -1
                ArrowDirection.DOWN -> 0 to 1
                ArrowDirection.LEFT -> -1 to 0
                ArrowDirection.RIGHT -> 1 to 0
            }
            val tipX = when (arrow.direction) {
                ArrowDirection.LEFT -> arrow.boundsLeft
                ArrowDirection.RIGHT -> arrow.boundsRight - 1
                else -> arrow.centerX
            }.coerceIn(0, screenshot.width - 1)
            val tipY = when (arrow.direction) {
                ArrowDirection.UP -> arrow.boundsTop
                ArrowDirection.DOWN -> arrow.boundsBottom - 1
                else -> arrow.centerY
            }.coerceIn(0, screenshot.height - 1)

            var x = tipX + dx * clearanceAfterHead.coerceAtLeast(0)
            var y = tipY + dy * clearanceAfterHead.coerceAtLeast(0)
            var count = 0
            var obstacle: ArrowPoint? = null

            while (x in 0 until screenshot.width &&
                y in 0 until screenshot.height &&
                count < maxSamples.coerceAtLeast(1)
            ) {
                val color = pixels[y * screenshot.width + x]
                val gray = (0.299 * ((color shr 16) and 255) +
                    0.587 * ((color shr 8) and 255) +
                    0.114 * (color and 255)).toInt()
                count++
                if (gray < darkThreshold.coerceIn(0, 255)) {
                    obstacle = ArrowPoint(x, y)
                    break
                }
                x += dx * step
                y += dy * step
            }

            val reachedEdge = x !in 0 until screenshot.width || y !in 0 until screenshot.height
            val status = when {
                obstacle != null -> ArrowPathStatus.BLOCKED
                reachedEdge -> ArrowPathStatus.CLEAR
                else -> ArrowPathStatus.UNKNOWN
            }
            ArrowPathResult(arrow, status, obstacle, count)
        }
    }
}
