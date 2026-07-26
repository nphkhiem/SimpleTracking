package com.khiemnph.simpletracking.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.simpletracking.ui.route.RouteProjection
import com.khiemnph.simpletracking.ui.route.drawRouteTrace

/**
 * Draws a recorded route as a line, with no map underneath it.
 *
 * This replaces a captured `GoogleMap` snapshot, and the differences are the reason for it: this
 * renders identically every time, needs no network, no Play Services and no rendered map, is sharp
 * at any size because it is a path rather than a bitmap, and takes its colour from the theme so it
 * belongs in dark mode instead of being a bright rectangle of map tiles.
 *
 * [RouteProjection] keeps the shape true: it fits the route to the box preserving aspect ratio, so
 * an out-and-back and a wide loop both read correctly rather than being stretched to the frame.
 */
@Composable
fun RouteThumbnail(
    points: List<LatLngPoint>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp,
) {
    val routeColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Canvas(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(emptyColor),
    ) {
        val strokeWidthPx = strokeWidth.toPx()
        // Inset by the stroke so the line is never clipped in half at the edge of the box.
        val projected = RouteProjection.project(points, size, insetPx = strokeWidthPx) ?: return@Canvas
        drawRouteTrace(projected.offsets, routeColor, strokeWidthPx)
    }
}
