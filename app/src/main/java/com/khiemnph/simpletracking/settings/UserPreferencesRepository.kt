package com.khiemnph.simpletracking.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val THEME = stringPreferencesKey("theme")
private val DYNAMIC_COLOUR = booleanPreferencesKey("dynamic_colour")

/**
 * Reads and writes the user's display preferences.
 *
 * A stored theme that no longer maps to a known value falls back to the default rather than
 * throwing. Preferences are not worth crashing over, and a value can go stale across an update.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    val preferences: Flow<UserPreferences> = dataStore.data.map { stored ->
        UserPreferences(
            theme = stored[THEME]?.let { name ->
                ThemeChoice.entries.firstOrNull { it.name == name }
            } ?: ThemeChoice.System,
            dynamicColour = stored[DYNAMIC_COLOUR] ?: false,
        )
    }

    suspend fun setTheme(theme: ThemeChoice) {
        dataStore.edit { it[THEME] = theme.name }
    }

    suspend fun setDynamicColour(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOUR] = enabled }
    }
}
