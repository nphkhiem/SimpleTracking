package com.khiemnph.simpletracking.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.dp
import com.khiemnph.domain.model.LatLngPoint

/**
 * Draws a recorded route as a line, with no map underneath it.
 *
 * This replaces a captured `GoogleMap` snapshot, and the differences are the reason for it: this
 * renders identically every time, needs no network, no Play Services and no rendered map, is sharp
 * at any size because it is a path rather than a bitmap, and takes its colour from the theme so it
 * belongs in dark mode instead of being a bright rectangle of map tiles.
 *
 * The shape is normalised to fill the box while preserving its aspect ratio, so an out-and-back and
 * a wide loop both read correctly rather than being stretched to the frame.
 */
@Composable
fun RouteThumbnail(
    points: List<LatLngPoint>,
    modifier: Modifier = Modifier,
    strokeWidth: androidx.compose.ui.unit.Dp = 2.dp,
) {
    val routeColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Canvas(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(emptyColor),
    ) {
        if (points.size < 2) return@Canvas
        drawRoute(points, routeColor, strokeWidth.toPx())
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoute(
    points: List<LatLngPoint>,
    color: Color,
    strokeWidthPx: Float,
) {
    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLng = points.minOf { it.longitude }
    val maxLng = points.maxOf { it.longitude }

    val spanLat = (maxLat - minLat).takeIf { it > 0.0 } ?: 1.0
    val spanLng = (maxLng - minLng).takeIf { it > 0.0 } ?: 1.0

    // Inset by the stroke so the line is never clipped in half at the edge of the box.
    val inset = strokeWidthPx
    val usable = Size(size.width - inset * 2, size.height - inset * 2)

    // One scale for both axes keeps the route's proportions; the smaller axis is then centred.
    val scale = minOf(usable.width / spanLng, usable.height / spanLat).toFloat()
    val drawnWidth = (spanLng * scale).toFloat()
    val drawnHeight = (spanLat * scale).toFloat()
    val originX = inset + (usable.width - drawnWidth) / 2f
    val originY = inset + (usable.height - drawnHeight) / 2f

    fun offsetOf(point: LatLngPoint) = Offset(
        x = originX + ((point.longitude - minLng) * scale).toFloat(),
        // Latitude increases northward, y increases downward, so this axis is flipped.
        y = originY + drawnHeight - ((point.latitude - minLat) * scale).toFloat(),
    )

    val path = Path().apply {
        val first = offsetOf(points.first())
        moveTo(first.x, first.y)
        points.drop(1).forEach { point ->
            val offset = offsetOf(point)
            lineTo(offset.x, offset.y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}
