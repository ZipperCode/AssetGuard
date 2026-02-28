package com.zipper.compose.assetguard.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zipper.compose.assetguard.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val REMINDER_ADVANCE_DAYS = intPreferencesKey("reminder_advance_days")
        val ONLY_OVERDUE = booleanPreferencesKey("only_overdue")
        val SILENT_START_HOUR = intPreferencesKey("silent_start_hour")
        val SILENT_END_HOUR = intPreferencesKey("silent_end_hour")
        val NOTIFICATION_PERMISSION_ASKED = booleanPreferencesKey("notification_permission_asked")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 9,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            reminderAdvanceDays = prefs[Keys.REMINDER_ADVANCE_DAYS] ?: 1,
            onlyOverdue = prefs[Keys.ONLY_OVERDUE] ?: false,
            silentStartHour = prefs[Keys.SILENT_START_HOUR] ?: 22,
            silentEndHour = prefs[Keys.SILENT_END_HOUR] ?: 8,
            notificationPermissionAsked = prefs[Keys.NOTIFICATION_PERMISSION_ASKED] ?: false,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            themeMode = try {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            } catch (_: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
        )
    }

    suspend fun updateReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REMINDER_HOUR] = hour
            prefs[Keys.REMINDER_MINUTE] = minute
        }
    }

    suspend fun updateReminderAdvanceDays(days: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REMINDER_ADVANCE_DAYS] = days
        }
    }

    suspend fun updateOnlyOverdue(onlyOverdue: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONLY_OVERDUE] = onlyOverdue
        }
    }

    suspend fun updateSilentHours(startHour: Int, endHour: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SILENT_START_HOUR] = startHour
            prefs[Keys.SILENT_END_HOUR] = endHour
        }
    }

    suspend fun setNotificationPermissionAsked(asked: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATION_PERMISSION_ASKED] = asked
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_LOCK_ENABLED] = enabled
        }
    }

    suspend fun saveThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    fun observeThemeMode(): Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        try {
            ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }
}
