package com.morteza.arrowsautosolver

/**
 * Platform-independent dependency solver.
 *
 * Coordinates are screenshot pixels. Each arrow's body contains sampled
 * pixels belonging to that arrow (excluding its own tip).
 */
data class ArrowPoint(val x: Int, val y: Int)

enum class ArrowDirection { UP, DOWN, LEFT, RIGHT }

data class DetectedArrow(
    val id: Int,
    val tip: ArrowPoint,
    val direction: ArrowDirection,
    val body: List<ArrowPoint>
)

data class SolveResult(
    val orderedArrowIds: List<Int>,
    val blockedPairs: List<Pair<Int, Int>>,
    val hasCycle: Boolean
)

object ArrowPuzzleSolver {

    /**
     * Returns true when the body of [blocker] lies in front of [moving]'s tip
     * on the same row/column. A small clearance avoids treating the arrowhead
     * itself as an obstacle.
     */
    private fun blocks(moving: DetectedArrow, blocker: DetectedArrow): Boolean {
        return blocker.body.any { p ->
            when (moving.direction) {
                ArrowDirection.UP ->
                    kotlin.math.abs(p.x - moving.tip.x) <= 2 && p.y < moving.tip.y - 4
                ArrowDirection.DOWN ->
                    kotlin.math.abs(p.x - moving.tip.x) <= 2 && p.y > moving.tip.y + 4
                ArrowDirection.LEFT ->
                    kotlin.math.abs(p.y - moving.tip.y) <= 2 && p.x < moving.tip.x - 4
                ArrowDirection.RIGHT ->
                    kotlin.math.abs(p.y - moving.tip.y) <= 2 && p.x > moving.tip.x + 4
            }
        }
    }

    /**
     * Edge direction is blocker -> blocked arrow, so topological order
     * removes every blocker before the arrow whose path it obstructs.
     */
    fun solve(arrows: List<DetectedArrow>): SolveResult {
        if (arrows.isEmpty()) return SolveResult(emptyList(), emptyList(), false)

        val byId = arrows.associateBy { it.id }
        require(byId.size == arrows.size) { "Arrow IDs must be unique" }

        val outgoing = arrows.associate { it.id to mutableSetOf<Int>() }
        val indegree = arrows.associate { it.id to 0 }.toMutableMap()
        val pairs = mutableListOf<Pair<Int, Int>>()

        for (moving in arrows) {
            for (blocker in arrows) {
                if (moving.id == blocker.id || !blocks(moving, blocker)) continue
                // blocker must be cleared before moving arrow
                if (outgoing.getValue(blocker.id).add(moving.id)) {
                    indegree[moving.id] = indegree.getValue(moving.id) + 1
                    pairs += blocker.id to moving.id
                }
            }
        }

        // Stable ordering makes test runs and tap plans reproducible.
        val ready = java.util.PriorityQueue<Int>()
        indegree.filterValues { it == 0 }.keys.forEach(ready::add)
        val order = mutableListOf<Int>()

        while (ready.isNotEmpty()) {
            val id = ready.remove()
            order += id
            for (next in outgoing.getValue(id)) {
                val degree = indegree.getValue(next) - 1
                indegree[next] = degree
                if (degree == 0) ready.add(next)
            }
        }

        return SolveResult(
            orderedArrowIds = order,
            blockedPairs = pairs,
            hasCycle = order.size != arrows.size
        )
    }
}
