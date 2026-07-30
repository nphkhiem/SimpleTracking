package com.khiemnph.simpletracking.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.settings.ThemeChoice

object SettingsTestTags {
    const val BACK = "settings_back"
    const val DYNAMIC_COLOUR = "settings_dynamic_colour"
    const val VERSION = "settings_version"

    fun themeOption(theme: ThemeChoice) = "settings_theme_${theme.name}"
}

/**
 * Units are deliberately absent. The brief defers an imperial toggle to Settings, but changing it
 * reaches every formatter and every screen, so it is its own slice rather than a checkbox added
 * here in passing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onThemeChosen: (ThemeChoice) -> Unit,
    onDynamicColourChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag(SettingsTestTags.BACK)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .fillMaxSize()
                // Scrollable because the content grows with font scale, and About is the last
                // thing on the screen: without this it becomes unreachable rather than just tight.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionHeading(stringResource(R.string.settings_theme_heading))

            Column(Modifier.selectableGroup()) {
                ThemeChoice.entries.forEach { choice ->
                    ThemeRow(
                        choice = choice,
                        selected = choice == state.theme,
                        onSelected = { onThemeChosen(choice) },
                    )
                }
            }

            SectionHeading(stringResource(R.string.settings_dynamic_colour))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_dynamic_colour_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(end = 16.dp),
                )
                Switch(
                    checked = state.dynamicColour,
                    onCheckedChange = onDynamicColourChanged,
                    modifier = Modifier.testTag(SettingsTestTags.DYNAMIC_COLOUR),
                )
            }

            SectionHeading(stringResource(R.string.settings_about_heading))

            Text(
                text = stringResource(R.string.settings_version, state.versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp).testTag(SettingsTestTags.VERSION),
            )
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
    )
}

/**
 * The whole row is the target, not just the radio button. `Role.RadioButton` on the row and no
 * click handler on the control itself keeps TalkBack announcing one control rather than two.
 */
@Composable
private fun ThemeRow(choice: ThemeChoice, selected: Boolean, onSelected: () -> Unit) {
    val label = when (choice) {
        ThemeChoice.System -> R.string.settings_theme_system
        ThemeChoice.Light -> R.string.settings_theme_light
        ThemeChoice.Dark -> R.string.settings_theme_dark
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelected)
            .padding(vertical = 12.dp)
            .testTag(SettingsTestTags.themeOption(choice)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
