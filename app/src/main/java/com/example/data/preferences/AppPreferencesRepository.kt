package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.DigestFrequency
import com.example.data.model.ListDensity
import com.example.data.model.SwipeAction
import com.example.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.kuryrDataStore: DataStore<Preferences> by preferencesDataStore(name = "kuryr_settings")

data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useMaterialYou: Boolean = false,
    val accentColorHex: Long = 0xFF1E40AF, // Královská modrá (výchozí)
    val listDensity: ListDensity = ListDensity.DEFAULT,
    val swipeLeftAction: SwipeAction = SwipeAction.ARCHIVE,
    val swipeRightAction: SwipeAction = SwipeAction.TOGGLE_READ,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 22,
    val quietHoursEndHour: Int = 7,
    val digestFrequency: DigestFrequency = DigestFrequency.DAILY,
    val globalSyncIntervalMinutes: Int = 15,
    val telemetryOptIn: Boolean = false // Žádná telemetrie bez výslovného opt-inu
)

@Singleton
class AppPreferencesRepository @Inject constructor(
    private val context: Context
) {
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_MATERIAL_YOU = booleanPreferencesKey("use_material_you")
        val ACCENT_COLOR_HEX = longPreferencesKey("accent_color_hex")
        val LIST_DENSITY = stringPreferencesKey("list_density")
        val SWIPE_LEFT = stringPreferencesKey("swipe_left_action")
        val SWIPE_RIGHT = stringPreferencesKey("swipe_right_action")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_HOURS_START = intPreferencesKey("quiet_hours_start")
        val QUIET_HOURS_END = intPreferencesKey("quiet_hours_end")
        val DIGEST_FREQUENCY = stringPreferencesKey("digest_frequency")
        val SYNC_INTERVAL_MINUTES = intPreferencesKey("sync_interval_minutes")
        val TELEMETRY_OPT_IN = booleanPreferencesKey("telemetry_opt_in")
    }

    val preferencesFlow: Flow<UserPreferences> = context.kuryrDataStore.data.map { prefs ->
        UserPreferences(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            useMaterialYou = prefs[Keys.USE_MATERIAL_YOU] ?: false,
            accentColorHex = prefs[Keys.ACCENT_COLOR_HEX] ?: 0xFF1E40AF,
            listDensity = prefs[Keys.LIST_DENSITY]?.let { runCatching { ListDensity.valueOf(it) }.getOrNull() } ?: ListDensity.DEFAULT,
            swipeLeftAction = prefs[Keys.SWIPE_LEFT]?.let { runCatching { SwipeAction.valueOf(it) }.getOrNull() } ?: SwipeAction.ARCHIVE,
            swipeRightAction = prefs[Keys.SWIPE_RIGHT]?.let { runCatching { SwipeAction.valueOf(it) }.getOrNull() } ?: SwipeAction.TOGGLE_READ,
            quietHoursEnabled = prefs[Keys.QUIET_HOURS_ENABLED] ?: false,
            quietHoursStartHour = prefs[Keys.QUIET_HOURS_START] ?: 22,
            quietHoursEndHour = prefs[Keys.QUIET_HOURS_END] ?: 7,
            digestFrequency = prefs[Keys.DIGEST_FREQUENCY]?.let { runCatching { DigestFrequency.valueOf(it) }.getOrNull() } ?: DigestFrequency.DAILY,
            globalSyncIntervalMinutes = prefs[Keys.SYNC_INTERVAL_MINUTES] ?: 15,
            telemetryOptIn = prefs[Keys.TELEMETRY_OPT_IN] ?: false
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.kuryrDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.kuryrDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setUseMaterialYou(enabled: Boolean) {
        context.kuryrDataStore.edit { it[Keys.USE_MATERIAL_YOU] = enabled }
    }

    suspend fun setAccentColor(colorHex: Long) {
        context.kuryrDataStore.edit { it[Keys.ACCENT_COLOR_HEX] = colorHex }
    }

    suspend fun setListDensity(density: ListDensity) {
        context.kuryrDataStore.edit { it[Keys.LIST_DENSITY] = density.name }
    }

    suspend fun setSwipeLeftAction(action: SwipeAction) {
        context.kuryrDataStore.edit { it[Keys.SWIPE_LEFT] = action.name }
    }

    suspend fun setSwipeRightAction(action: SwipeAction) {
        context.kuryrDataStore.edit { it[Keys.SWIPE_RIGHT] = action.name }
    }

    suspend fun setQuietHours(enabled: Boolean, startHour: Int, endHour: Int) {
        context.kuryrDataStore.edit {
            it[Keys.QUIET_HOURS_ENABLED] = enabled
            it[Keys.QUIET_HOURS_START] = startHour.coerceIn(0, 23)
            it[Keys.QUIET_HOURS_END] = endHour.coerceIn(0, 23)
        }
    }

    suspend fun setDigestFrequency(frequency: DigestFrequency) {
        context.kuryrDataStore.edit { it[Keys.DIGEST_FREQUENCY] = frequency.name }
    }

    suspend fun setSyncIntervalMinutes(minutes: Int) {
        context.kuryrDataStore.edit { it[Keys.SYNC_INTERVAL_MINUTES] = minutes }
    }

    suspend fun setTelemetryOptIn(optIn: Boolean) {
        context.kuryrDataStore.edit { it[Keys.TELEMETRY_OPT_IN] = optIn }
    }
}
