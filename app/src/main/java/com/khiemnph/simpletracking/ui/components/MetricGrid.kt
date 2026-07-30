package com.khiemnph.simpletracking.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** One labelled number. [value] carries its own unit already, since units are locale-dependent. */
data class Metric(
    val label: String,
    val value: String,
)

object MetricGridTestTags {
    fun metricFor(label: String) = "metric_$label"
}

/**
 * Metrics laid out in rows of [columns], the number given the weight and the label kept quiet
 * beneath it.
 *
 * The brief's Editorial mode treats the numbers as the graphic element, so this deliberately has no
 * card, border or fill. Separation comes from space and type weight alone.
 *
 * A final row with fewer metrics than [columns] is padded with empty cells, so the columns stay
 * aligned instead of the last row spreading itself across the width.
 */
@Composable
fun MetricGrid(
    metrics: List<Metric>,
    modifier: Modifier = Modifier,
    columns: Int = 3,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        metrics.chunked(columns).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { metric ->
                    MetricCell(metric, Modifier.weight(1f))
                }
                repeat(columns - row.size) {
                    Column(Modifier.weight(1f)) {}
                }
            }
        }
    }
}

@Composable
private fun MetricCell(metric: Metric, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.testTag(MetricGridTestTags.metricFor(metric.label)),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        Text(
            text = metric.value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = metric.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
