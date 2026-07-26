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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.khiemnph.simpletracking.R
import kotlinx.coroutines.launch

/** A rest day is drawn as a sliver rather than nothing, so the strip always reads as seven days. */
private const val MINIMUM_BAR_FRACTION = 0.08f
private const val SKELETON_ROW_COUNT = 3

/** Stable handles for tests, so assertions do not depend on user-visible copy. */
object HistoryTestTags {
    const val LIST = "history_list"
    const val EMPTY = "history_empty"
    const val RECORD_BUTTON = "history_record_button"
    const val DIVIDER = "history_divider"
    const val SKELETON = "history_skeleton"
    const val WEEK_SUMMARY = "history_week_summary"

    fun groupHeaderFor(label: String) = "history_group_$label"

    fun rowFor(sessionId: String) = "history_row_$sessionId"
}

/**
 * The session list. Stateless by design: it renders exactly the pre-formatted models
 * [HistoryViewModel] emits and reports clicks upward, so it can be previewed and tested without a
 * ViewModel, a database or a Fragment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onRecordClick: () -> Unit,
    onSessionSwipedAway: (String) -> Unit,
    onUndoDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.history_session_deleted)
    val undoLabel = stringResource(R.string.history_undo)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
        val contentPadding = PaddingValues(
            top = insets.calculateTopPadding() + 4.dp,
            // Clears the floating action button so the last row is never trapped underneath it.
            bottom = insets.calculateBottomPadding() + 88.dp,
        )
        when (state) {
            // Nothing is drawn while the database answers. Showing the empty state here would tell
            // a returning user they have no runs, a moment before their runs appear.
            // A skeleton rather than nothing: the list has a known shape, so showing it settles
            // the layout instead of letting rows appear against a blank screen.
            HistoryUiState.Loading -> LoadingSkeleton(Modifier.padding(contentPadding))
            HistoryUiState.Empty -> EmptyHistory(Modifier.padding(contentPadding))
            is HistoryUiState.Sessions -> SessionList(
                week = state.week,
                groups = state.groups,
                contentPadding = contentPadding,
                onSwipedAway = { sessionId ->
                    onSessionSwipedAway(sessionId)
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = deletedMessage,
                            actionLabel = undoLabel,
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) onUndoDelete(sessionId)
                    }
                },
            )
        }
    }
}

@Composable
private fun SessionList(
    week: WeekSummaryUiModel,
    groups: List<SessionGroupUiModel>,
    contentPadding: PaddingValues,
    onSwipedAway: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(HistoryTestTags.LIST),
        contentPadding = contentPadding,
    ) {
        item(key = "week") { WeekSummary(week) }

        groups.forEach { group ->
            item(key = "header-" + group.label) { GroupHeader(group.label) }

            itemsIndexed(group.sessions, key = { _, session -> session.id }) { index, session ->
                SwipeToDeleteRow(session = session, onSwipedAway = { onSwipedAway(session.id) })
                // No rule after a group's last row: the next heading already separates them, and a
                // rule there would fence off empty space rather than divide two rows.
                if (index < group.sessions.lastIndex) {
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
private fun GroupHeader(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
            .testTag(HistoryTestTags.groupHeaderFor(label)),
    )
}

/** The week at a glance: three totals over a seven-day bar strip, today rightmost. */
@Composable
private fun WeekSummary(week: WeekSummaryUiModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(HistoryTestTags.WEEK_SUMMARY),
    ) {
        Text(
            text = stringResource(R.string.history_week_heading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = week.distanceLabel,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = week.runCountLabel + stringResource(R.string.history_item_stats_separator) + week.durationLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        WeekStrip(week.dailyDistanceFractions)
    }
}

@Composable
private fun WeekStrip(fractions: List<Float>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        fractions.forEach { fraction ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction.coerceAtLeast(MINIMUM_BAR_FRACTION))
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(
                        // A rest day still gets a visible trace, so the strip reads as seven days
                        // rather than as however many days happened to have a run.
                        if (fraction > 0f) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                    ),
            )
        }
    }
}

@Composable
private fun LoadingSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(HistoryTestTags.SKELETON),
    ) {
        repeat(SKELETON_ROW_COUNT) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Box(
                        Modifier
                            .width(120.dp)
                            .height(20.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .width(180.dp)
                            .height(14.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteRow(
    session: HistorySummaryUiModel,
    onSwipedAway: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            val dismissed = value == SwipeToDismissBoxValue.EndToStart
            if (dismissed) onSwipedAway()
            dismissed
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.testTag(HistoryTestTags.rowFor(session.id)),
        // One direction only. A two-way swipe on a list whose only destructive action is delete
        // makes it twice as easy to lose a run by accident.
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    text = stringResource(R.string.history_delete),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        Box(Modifier.background(MaterialTheme.colorScheme.surface)) { SessionRow(session) }
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp)
            .testTag(HistoryTestTags.EMPTY),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.history_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.history_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
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
