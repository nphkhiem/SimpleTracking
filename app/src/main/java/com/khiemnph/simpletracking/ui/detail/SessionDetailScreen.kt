package com.khiemnph.simpletracking.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.ui.components.Metric
import com.khiemnph.simpletracking.ui.components.MetricGrid
import com.khiemnph.simpletracking.ui.route.RouteHero

object SessionDetailTestTags {
    const val TITLE = "detail_title"
    const val OVERFLOW = "detail_overflow"
    const val RENAME = "detail_rename"
    const val RENAME_FIELD = "detail_rename_field"
    const val RENAME_CONFIRM = "detail_rename_confirm"
    const val SHARE = "detail_share"
    const val EXPORT = "detail_export"
    const val SPLITS = "detail_splits"
    const val NOT_FOUND = "detail_not_found"
    const val DELETE = "detail_delete"
    fun splitFor(label: String) = "detail_split_$label"
}

/**
 * A recorded run in full: the route as the hero, the metrics that describe it, and the splits that
 * explain how it was run.
 *
 * Until this existed a finished run was a single line of text in a list and nothing more, which is
 * what the brief meant by recorded data being a dead end.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    state: SessionDetailUiState,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onShare: () -> Unit,
    onExportGpx: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    if (state is SessionDetailUiState.Ready) {
                        Text(
                            text = state.titleLabel,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.testTag(SessionDetailTestTags.TITLE),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.detail_back),
                        )
                    }
                },
                actions = {
                    if (state is SessionDetailUiState.Ready) {
                        IconButton(
                            onClick = { menuOpen = true },
                            modifier = Modifier.testTag(SessionDetailTestTags.OVERFLOW),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert),
                                contentDescription = stringResource(R.string.detail_more_actions),
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.detail_rename)) },
                                onClick = { menuOpen = false; renaming = true },
                                modifier = Modifier.testTag(SessionDetailTestTags.RENAME),
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.detail_share)) },
                                onClick = { menuOpen = false; onShare() },
                                modifier = Modifier.testTag(SessionDetailTestTags.SHARE),
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.detail_export_gpx)) },
                                onClick = { menuOpen = false; onExportGpx() },
                                modifier = Modifier.testTag(SessionDetailTestTags.EXPORT),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { insets ->
        if (renaming && state is SessionDetailUiState.Ready) {
            RenameDialog(
                initial = if (state.hasCustomTitle) state.titleLabel else "",
                onConfirm = { renaming = false; onRename(it) },
                onDismiss = { renaming = false },
            )
        }
        when (state) {
            SessionDetailUiState.Loading -> Box(Modifier.fillMaxSize())
            SessionDetailUiState.NotFound -> NotFound(onBack, Modifier.padding(insets))
            is SessionDetailUiState.Ready -> Ready(state, onDelete, Modifier.padding(insets))
        }
    }
}

@Composable
private fun Ready(
    state: SessionDetailUiState.Ready,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(SessionDetailTestTags.SPLITS),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            RouteHero(
                points = state.routePoints,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
            )
        }

        item {
            MetricGrid(
                metrics = listOf(
                    Metric(stringResource(R.string.detail_metric_distance), state.distanceKm),
                    Metric(stringResource(R.string.detail_metric_duration), state.durationLabel),
                    Metric(stringResource(R.string.detail_metric_avg_pace), state.averagePaceLabel),
                    Metric(stringResource(R.string.detail_metric_best_pace), state.bestPaceLabel),
                ),
                columns = 2,
            )
        }

        if (state.splits.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.detail_splits_heading),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(state.splits, key = { it.label }) { split -> SplitRow(split) }
        }

        item {
            TextButton(
                onClick = onDelete,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .testTag(SessionDetailTestTags.DELETE),
            ) {
                Text(
                    text = stringResource(R.string.detail_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** One kilometre: its number, a bar whose length is the time it took, and its pace. */
@Composable
private fun SplitRow(split: SplitUiModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .testTag(SessionDetailTestTags.splitFor(split.label)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = split.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 7.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(split.barFraction)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(
                        if (split.isFastest) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                    ),
            )
        }
        Text(
            text = split.paceLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (split.isFastest) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier
                .padding(start = 12.dp)
                .width(56.dp),
        )
    }
}

/** Pre-filled only when the run already has a name, so the date is never offered as one to edit. */
@Composable
private fun RenameDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_rename)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text(stringResource(R.string.detail_rename_hint)) },
                modifier = Modifier.testTag(SessionDetailTestTags.RENAME_FIELD),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                modifier = Modifier.testTag(SessionDetailTestTags.RENAME_CONFIRM),
            ) {
                Text(stringResource(R.string.detail_rename_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.detail_rename_cancel)) }
        },
    )
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
            text = stringResource(R.string.detail_not_found),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(SessionDetailTestTags.NOT_FOUND),
        )
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.summary_back_to_runs))
        }
    }
}
