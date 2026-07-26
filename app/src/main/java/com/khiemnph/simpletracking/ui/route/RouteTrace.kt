package com.khiemnph.simpletracking.ui.route

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Draws already-projected route points as a single continuous stroke.
 *
 * Shared by the history thumbnail and the live fallback so the two always render the same route
 * the same way: one path, rounded joins, no per-point decoration.
 */
fun DrawScope.drawRouteTrace(offsets: List<Offset>, color: Color, strokeWidthPx: Float) {
    if (offsets.size < 2) return

    val path = Path().apply {
        moveTo(offsets.first().x, offsets.first().y)
        offsets.drop(1).forEach { lineTo(it.x, it.y) }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}
