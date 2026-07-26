package com.khiemnph.simpletracking.ui.route

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.simpletracking.R

private val ROUTE_STROKE = 5.dp
private val EDGE_INSET = 28.dp
private val CHROME_MARGIN = 20.dp
private val START_MARKER_RADIUS = 5.dp
private val CURRENT_MARKER_RADIUS = 7.dp
private val CURRENT_MARKER_HALO_RADIUS = 15.dp
private const val HALO_ALPHA = 0.18f

/** How much of the width the scale bar may take before it stops being chrome. */
private const val SCALE_BAR_WIDTH_FRACTION = 0.4f
private const val METERS_PER_KILOMETER = 1000
private val SCALE_BAR_HEIGHT = 8.dp

object OfflineRouteTestTags {
    const val TRACE = "offline_route_trace"
    const val SCALE_BAR = "offline_route_scale_bar"
}

/**
 * The route on its own, for when there is no map to draw it on.
 *
 * The Maps renderer ships as a Play Services dynamic module rather than inside the APK, so on a
 * device that has never fetched it and has no connection it cannot initialise at all - and it does
 * not retry when the network comes back within the same map instance. Without this the Record
 * screen offline is a blank rectangle: the session records correctly but shows the user nothing.
 *
 * What is drawn here is real. The points are projected to true ground distances by
 * [RouteProjection], so the shape matches the one a map would show, and [ScaleBar] states the size
 * outright because a view that refits itself as the route grows has no fixed zoom to infer it
 * from. What is missing is only the map underneath.
 */
@Composable
fun OfflineRouteCanvas(
    points: List<LatLngPoint>,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val routeColor = MaterialTheme.colorScheme.primary
    val startColor = MaterialTheme.colorScheme.onSurfaceVariant
    val background = MaterialTheme.colorScheme.surfaceContainerLowest

    BoxWithConstraints(modifier = modifier.background(background)) {
        val density = LocalDensity.current
        val strokePx = with(density) { ROUTE_STROKE.toPx() }
        val insetPx = with(density) { EDGE_INSET.toPx() }
        val canvasSize = with(density) {
            Size(maxWidth.toPx(), (maxHeight - bottomInset).coerceAtLeast(0.dp).toPx())
        }
        val projected = RouteProjection.project(points, canvasSize, insetPx)

        if (projected != null) {
            Canvas(modifier = Modifier.fillMaxSize().testTag(OfflineRouteTestTags.TRACE)) {
                drawRouteTrace(projected.offsets, routeColor, strokePx)

                drawCircle(
                    color = startColor,
                    radius = START_MARKER_RADIUS.toPx(),
                    center = projected.offsets.first(),
                    style = Stroke(width = strokePx / 2f),
                )
                drawCircle(
                    color = routeColor.copy(alpha = HALO_ALPHA),
                    radius = CURRENT_MARKER_HALO_RADIUS.toPx(),
                    center = projected.offsets.last(),
                )
                drawCircle(
                    color = routeColor,
                    radius = CURRENT_MARKER_RADIUS.toPx(),
                    center = projected.offsets.last(),
                )
            }
        }

        val maxBarPx = with(density) { (maxWidth * SCALE_BAR_WIDTH_FRACTION).toPx() }
        val scaleBar = projected?.metersPerPixel?.let { ScaleBar.fit(it, maxBarPx) }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = CHROME_MARGIN, end = CHROME_MARGIN)
                .padding(bottom = bottomInset + CHROME_MARGIN),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            scaleBar?.let { ScaleBarChrome(it) }

            Text(
                text = stringResource(R.string.record_map_offline_caption),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The bar itself plus the distance it stands for, which is the only part that carries meaning. */
@Composable
private fun ScaleBarChrome(spec: ScaleBarSpec) {
    val density = LocalDensity.current
    val chromeColor = MaterialTheme.colorScheme.onSurfaceVariant
    val label = if (spec.meters >= METERS_PER_KILOMETER) {
        stringResource(R.string.route_scale_kilometers, spec.meters / METERS_PER_KILOMETER)
    } else {
        stringResource(R.string.route_scale_meters, spec.meters)
    }

    Column(
        modifier = Modifier.testTag(OfflineRouteTestTags.SCALE_BAR),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = chromeColor)
        Box(
            modifier = Modifier
                .width(with(density) { spec.lengthPx.toDp() })
                .height(SCALE_BAR_HEIGHT),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val thickness = 1.5.dp.toPx()
                val tick = 5.dp.toPx()
                val baseline = size.height - thickness
                drawLine(
                    color = chromeColor,
                    start = Offset(0f, baseline),
                    end = Offset(size.width, baseline),
                    strokeWidth = thickness,
                )
                listOf(0f, size.width).forEach { x ->
                    drawLine(
                        color = chromeColor,
                        start = Offset(x, baseline - tick),
                        end = Offset(x, baseline),
                        strokeWidth = thickness,
                    )
                }
            }
        }
    }
}
