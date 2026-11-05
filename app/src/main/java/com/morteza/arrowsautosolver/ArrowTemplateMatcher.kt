package com.morteza.arrowsautosolver

import android.graphics.Bitmap
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** A detected arrowhead. Coordinates refer to the original screenshot. */
data class ArrowHeadDetection(
    val direction: ArrowDirection,
    val centerX: Int,
    val centerY: Int,
    val boundsLeft: Int,
    val boundsTop: Int,
    val boundsRight: Int,
    val boundsBottom: Int,
    val confidence: Double
)

/**
 * Lightweight normalized cross-correlation matcher.
 *
 * This first detector locates arrowhead templates only; it does not yet
 * reconstruct complete arrow bodies or capture the screen.
 */
object ArrowTemplateMatcher {
    private data class GrayTemplate(
        val direction: ArrowDirection,
        val width: Int,
        val height: Int,
        val pixels: DoubleArray,
        val mean: Double,
        val norm: Double
    )

    private data class Candidate(
        val direction: ArrowDirection,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val score: Double
    )

    fun detect(
        screenshot: Bitmap,
        templates: Map<ArrowDirection, Bitmap>,
        threshold: Double = 0.76,
        maxDetections: Int = 300,
        searchRegion: Rect? = null,
        scanStep: Int? = null,
        maxCandidates: Int = 6000
    ): List<ArrowHeadDetection> {
        if (screenshot.width < 3 || screenshot.height < 3 || templates.isEmpty()) {
            return emptyList()
        }

        val region = (searchRegion ?: Rect(0, 0, screenshot.width, screenshot.height)).let {
            Rect(
                it.left.coerceIn(0, screenshot.width),
                it.top.coerceIn(0, screenshot.height),
                it.right.coerceIn(0, screenshot.width),
                it.bottom.coerceIn(0, screenshot.height)
            )
        }
        if (region.width() < 3 || region.height() < 3) return emptyList()

        val screen = grayscale(screenshot)
        val candidates = mutableListOf<Candidate>()
        val scales = doubleArrayOf(0.65, 0.8, 1.0, 1.2, 1.4)

        for ((direction, source) in templates) {
            for (scale in scales) {
                val w = (source.width * scale).toInt().coerceAtLeast(3)
                val h = (source.height * scale).toInt().coerceAtLeast(3)
                if (w > region.width() || h > region.height) continue
                val scaled = if (w == source.width && h == source.height) source
                    else Bitmap.createScaledBitmap(source, w, h, true)
                val tpl = makeTemplate(direction, scaled)
                if (tpl.norm <= 1e-8) continue

                // A two-pixel scan stride limits work on full-resolution phone captures.
                val automaticStep = if (region.width().toLong() * region.height() > 1_500_000L) 4 else 2
                val step = (scanStep ?: automaticStep).coerceAtLeast(1)
                var y = region.top
                while (y <= region.bottom - h) {
                    var x = region.left
                    while (x <= region.right - w) {
                        val score = correlation(screen, screenshot.width, x, y, tpl)
                        if (score >= threshold) {
                            candidates += Candidate(direction, x, y, w, h, score)
                            if (candidates.size >= maxCandidates.coerceAtLeast(1)) break
                        }
                        x += step
                    }
                    if (candidates.size >= maxCandidates.coerceAtLeast(1)) break
                    y += step
                }
                if (scaled !== source) scaled.recycle()
                if (candidates.size >= maxCandidates.coerceAtLeast(1)) break
            }
            if (candidates.size >= maxCandidates.coerceAtLeast(1)) break
        }

        // Highest-confidence matches win; overlapping boxes are suppressed.
        val accepted = mutableListOf<Candidate>()
        for (candidate in candidates.sortedByDescending { it.score }) {
            if (accepted.none { overlapRatio(it, candidate) > 0.35 }) {
                accepted += candidate
                if (accepted.size >= maxDetections) break
            }
        }
        return accepted.map {
            ArrowHeadDetection(
                direction = it.direction,
                centerX = it.x + it.width / 2,
                centerY = it.y + it.height / 2,
                boundsLeft = it.x,
                boundsTop = it.y,
                boundsRight = it.x + it.width,
                boundsBottom = it.y + it.height,
                confidence = it.score
            )
        }
    }

    private fun grayscale(bitmap: Bitmap): DoubleArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return DoubleArray(pixels.size) { i ->
            val p = pixels[i]
            0.299 * ((p shr 16) and 255) +
                0.587 * ((p shr 8) and 255) +
                0.114 * (p and 255)
        }
    }

    private fun makeTemplate(direction: ArrowDirection, bitmap: Bitmap): GrayTemplate {
        val values = grayscale(bitmap)
        val mean = values.average()
        var sum = 0.0
        for (v in values) {
            val d = v - mean
            sum += d * d
        }
        return GrayTemplate(direction, bitmap.width, bitmap.height, values, mean, sqrt(sum))
    }

    private fun correlation(
        screen: DoubleArray,
        screenWidth: Int,
        x: Int,
        y: Int,
        template: GrayTemplate
    ): Double {
        val count = template.width * template.height
        var sum = 0.0
        var sumSquares = 0.0
        var cross = 0.0
        var index = 0
        for (ty in 0 until template.height) {
            var si = (y + ty) * screenWidth + x
            for (tx in 0 until template.width) {
                val value = screen[si++]
                sum += value
                sumSquares += value * value
                cross += value * template.pixels[index++]
            }
        }
        val variance = max(0.0, sumSquares - sum * sum / count)
        val norm = sqrt(variance) * template.norm
        if (norm <= 1e-8) return 0.0
        return ((cross - sum * template.mean) / norm).coerceIn(-1.0, 1.0)
    }

    private fun overlapRatio(a: Candidate, b: Candidate): Double {
        val left = max(a.x, b.x)
        val top = max(a.y, b.y)
        val right = min(a.x + a.width, b.x + b.width)
        val bottom = min(a.y + a.height, b.y + b.height)
        if (right <= left || bottom <= top) return 0.0
        val intersection = (right - left).toDouble() * (bottom - top)
        val smaller = min(a.width * a.height, b.width * b.height).toDouble()
        return intersection / smaller
    }
}
