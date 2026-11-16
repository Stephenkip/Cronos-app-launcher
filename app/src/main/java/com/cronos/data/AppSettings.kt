package com.cronos.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.appSettingsStore: DataStore<Preferences> by preferencesDataStore(name = "cronos_settings")

data class AppSettings(
    val iconSize: Float = 40f,
    val accentMode: AccentMode = AccentMode.SYSTEM,
    val followSystemFontSize: Boolean = true,
)

enum class AccentMode {
    SYSTEM,
    FIXED
}

class AppSettingsRepository(private val context: Context) {
    private val iconSizeKey = floatPreferencesKey("icon_size")
    private val accentModeKey = stringPreferencesKey("accent_mode")
    private val followSystemFontSizeKey = booleanPreferencesKey("follow_system_font_size")

    val settingsFlow: Flow<AppSettings> = context.appSettingsStore.data.map { prefs ->
        AppSettings(
            iconSize = prefs[iconSizeKey] ?: 40f,
            accentMode = runCatching {
                prefs[accentModeKey]?.let { AccentMode.valueOf(it) } ?: AccentMode.SYSTEM
            }.getOrDefault(AccentMode.SYSTEM),
            followSystemFontSize = prefs[followSystemFontSizeKey] ?: true,
        )
    }

    suspend fun setIconSize(size: Float) {
        context.appSettingsStore.edit { prefs ->
            prefs[iconSizeKey] = size.coerceIn(28f, 56f)
        }
    }

    suspend fun setAccentMode(mode: AccentMode) {
        context.appSettingsStore.edit { prefs ->
            prefs[accentModeKey] = mode.name
        }
    }

    suspend fun setFollowSystemFontSize(enabled: Boolean) {
        context.appSettingsStore.edit { prefs ->
            prefs[followSystemFontSizeKey] = enabled
        }
    }
}
