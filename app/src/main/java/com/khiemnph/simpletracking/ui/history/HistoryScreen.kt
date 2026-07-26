package com.khiemnph.simpletracking.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.khiemnph.simpletracking.R

/** Stable handles for tests, so assertions do not depend on user-visible copy. */
object HistoryTestTags {
    const val LIST = "history_list"
    const val RECORD_BUTTON = "history_record_button"
    const val DIVIDER = "history_divider"
}

/**
 * The session list. Stateless by design: it renders exactly the pre-formatted models
 * [HistoryViewModel] emits and reports clicks upward, so it can be previewed and tested without a
 * ViewModel, a database or a Fragment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    sessions: List<HistorySummaryUiModel>,
    onRecordClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        // Centred to match the reviewed mockup, not Scaffold's bottom-end default: the Record
        // action is the screen's single primary action and sits under the thumb on both hands.
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRecordClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag(HistoryTestTags.RECORD_BUTTON),
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary),
                )
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.history_record_button_text))
            }
        },
    ) { insets ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(HistoryTestTags.LIST),
            contentPadding = PaddingValues(
                top = insets.calculateTopPadding() + 4.dp,
                // Clears the floating action button so the last row is never trapped underneath it.
                bottom = insets.calculateBottomPadding() + 88.dp,
            ),
        ) {
            itemsIndexed(sessions, key = { _, session -> session.id }) { index, session ->
                SessionRow(session)
                // No rule after the last row: it would fence off the empty space below rather than
                // separate two things, which is what a divider is for.
                if (index < sessions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .testTag(HistoryTestTags.DIVIDER),
                        thickness = dimensionResource(R.dimen.divider_thickness),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: HistorySummaryUiModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = session.recordedAtLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RouteThumbnail(
                points = session.routePoints,
                // The labels beside it already state distance, time and pace, so announcing the
                // route as well would just make TalkBack read the row twice.
                modifier = Modifier
                    .size(56.dp)
                    .clearAndSetSemantics {},
            )
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = session.distanceLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = session.durationLabel +
                        stringResource(R.string.history_item_stats_separator) +
                        session.averageSpeedLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
