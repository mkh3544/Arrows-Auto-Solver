package com.morteza.arrowsautosolver

import android.graphics.Bitmap
import android.graphics.Rect

data class ArrowScanReport(
    val detections: List<ArrowHeadDetection>,
    val paths: List<ArrowPathResult>
) {
    val clearCount: Int get() = paths.count { it.status == ArrowPathStatus.CLEAR }
    val blockedCount: Int get() = paths.count { it.status == ArrowPathStatus.BLOCKED }
    val unknownCount: Int get() = paths.count { it.status == ArrowPathStatus.UNKNOWN }
}

/** Runs template detection followed by a pixel-level exit-path check. */
object ArrowDetectionPipeline {
    fun scan(
        screenshot: Bitmap,
        templates: Map<ArrowDirection, Bitmap>,
        searchRegion: Rect? = null,
        matchThreshold: Double = 0.76,
        darkThreshold: Int = 150
    ): ArrowScanReport {
        val detections = ArrowTemplateMatcher.detect(
            screenshot = screenshot,
            templates = templates,
            threshold = matchThreshold,
            searchRegion = searchRegion
        )
        val paths = ArrowPathAnalyzer.analyze(
            screenshot = screenshot,
            detections = detections,
            darkThreshold = darkThreshold
        )
        return ArrowScanReport(detections, paths)
    }
}
