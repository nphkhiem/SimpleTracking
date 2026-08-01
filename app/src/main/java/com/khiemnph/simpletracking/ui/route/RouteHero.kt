package com.khiemnph.simpletracking.ui.route

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.simpletracking.R

private val EDGE_INSET = 24.dp
private val START_MARKER_RADIUS = 6.dp
private val END_MARKER_RADIUS = 8.dp
private const val MINIMUM_POINTS_TO_DRAW = 2

object RouteHeroTestTags {
    const val TRACE = "route_hero_trace"
    const val SPARSE = "route_hero_sparse"
}

/**
 * A finished route at display size, framed to its own bounds.
 *
 * Built on [RouteProjection], so it needs no map, no network and no Play Services, and it renders
 * identically every time. That is what lets the post-run screen show the user their route the
 * instant they stop, including indoors and on a device that has never fetched the Maps renderer.
 *
 * A route with fewer than two points says so rather than drawing an empty box. That is a real
 * state: a session stopped within a second of starting, or one that never got a GPS fix, still has
 * numbers worth showing even though it has no shape.
 */
@Composable
fun RouteHero(
    points: List<LatLngPoint>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 4.dp,
) {
    val routeColor = MaterialTheme.colorScheme.primary
    val startColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        // Two distinct nothings, and they mean different things to the person reading them. Too
        // few points is a recording that never got going. Enough points with no extent is a
        // recording that worked and a run that did not move, which drew as a lone dot in a large
        // empty frame and read as a failed render rather than as a fact about the run.
        val hasExtent = points.size >= MINIMUM_POINTS_TO_DRAW &&
            (points.minOf { it.latitude } != points.maxOf { it.latitude } ||
                points.minOf { it.longitude } != points.maxOf { it.longitude })

        if (points.size < MINIMUM_POINTS_TO_DRAW || !hasExtent) {
            Text(
                text = stringResource(
                    if (points.size < MINIMUM_POINTS_TO_DRAW) {
                        R.string.route_not_enough_points
                    } else {
                        R.string.route_no_movement
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(RouteHeroTestTags.SPARSE),
            )
            return@Box
        }

        Canvas(modifier = Modifier.fillMaxSize().testTag(RouteHeroTestTags.TRACE)) {
            val strokePx = strokeWidth.toPx()
            val projected = RouteProjection.project(points, size, insetPx = EDGE_INSET.toPx())
                ?: return@Canvas

            drawRouteTrace(projected.offsets, routeColor, strokePx)
            drawCircle(
                color = startColor,
                radius = START_MARKER_RADIUS.toPx(),
                center = projected.offsets.first(),
                style = Stroke(width = strokePx / 2f),
            )
            drawCircle(
                color = routeColor,
                radius = END_MARKER_RADIUS.toPx(),
                center = projected.offsets.last(),
            )
        }
    }
}
