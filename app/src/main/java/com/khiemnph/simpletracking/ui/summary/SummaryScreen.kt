package com.khiemnph.simpletracking.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.ui.components.Metric
import com.khiemnph.simpletracking.ui.components.MetricGrid
import com.khiemnph.simpletracking.ui.route.RouteHero

object SummaryTestTags {
    const val HEADLINE = "summary_headline"
    const val KEEP = "summary_keep"
    const val DISCARD = "summary_discard"
    const val TOO_SHORT = "summary_too_short"
    const val NOT_FOUND = "summary_not_found"
    const val LOADING = "summary_loading"
}

/**
 * The moment after a run, which the app previously did not have at all: stopping dropped the user
 * straight back to the list with nothing to show for what they had just done.
 *
 * Restrained on purpose. The brief allows exactly one celebratory surface and asks for no confetti,
 * so the reward is the route and the numbers at size, not decoration.
 */
@Composable
fun SummaryScreen(
    state: SummaryUiState,
    onKeep: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) { insets ->
        when (state) {
            SummaryUiState.Loading -> Loading(Modifier.padding(insets))
            SummaryUiState.NotFound -> NotFound(onKeep, Modifier.padding(insets))
            is SummaryUiState.Ready -> Ready(state, onKeep, onDiscard, Modifier.padding(insets))
        }
    }
}

@Composable
private fun Ready(
    state: SummaryUiState.Ready,
    onKeep: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = stringResource(R.string.summary_headline),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(top = 16.dp)
                .testTag(SummaryTestTags.HEADLINE),
        )

        RouteHero(
            points = state.routePoints,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        MetricGrid(
            metrics = listOf(
                Metric(stringResource(R.string.summary_metric_distance), state.distanceKm),
                Metric(stringResource(R.string.summary_metric_duration), state.durationLabel),
                Metric(stringResource(R.string.summary_metric_pace), state.paceLabel),
            ),
        )

        if (state.isTooShortToKeep) {
            Text(
                text = stringResource(R.string.summary_too_short),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SummaryTestTags.TOO_SHORT),
            )
        }

        Column(
            modifier = Modifier.padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = onKeep,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SummaryTestTags.KEEP),
            ) {
                Text(stringResource(R.string.summary_keep))
            }
            TextButton(
                onClick = onDiscard,
                modifier = Modifier.testTag(SummaryTestTags.DISCARD),
            ) {
                Text(
                    text = stringResource(R.string.summary_discard),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** Shown for the moment between Stop and the Service finishing its write. */
@Composable
private fun Loading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.summary_headline),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(SummaryTestTags.HEADLINE),
        )
        CircularProgressIndicator(
            modifier = Modifier
                .padding(top = 24.dp)
                .testTag(SummaryTestTags.LOADING),
        )
    }
}

@Composable
private fun NotFound(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.summary_not_found),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(SummaryTestTags.NOT_FOUND),
        )
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.summary_back_to_runs))
        }
    }
}
