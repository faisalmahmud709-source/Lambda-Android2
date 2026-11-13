package com.lambda.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.lambdaPrefs by preferencesDataStore("lambda_preferences")
class Preferences(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val nameKey = stringPreferencesKey("display_name")
    private val focusKey = intPreferencesKey("focus_minutes")
    private val shortKey = intPreferencesKey("short_break_minutes")
    private val longKey = intPreferencesKey("long_break_minutes")
    private val intervalKey = intPreferencesKey("long_break_every")
    private val goalKey = intPreferencesKey("daily_goal_hours")
    private val soundKey = booleanPreferencesKey("sound_enabled")
    val values = context.lambdaPrefs.data.map { p -> PrefState(p[themeKey] ?: "dark", p[nameKey] ?: "", p[focusKey] ?: 25, p[shortKey] ?: 5, p[longKey] ?: 15, p[goalKey] ?: 6, p[soundKey] ?: true, p[intervalKey] ?: 4) }
    suspend fun update(block: (MutablePreferences) -> Unit) { context.lambdaPrefs.edit { block(it) } }
    suspend fun setTheme(v: String) = update { it[themeKey] = v }
    suspend fun setName(v: String) = update { it[nameKey] = v }
    suspend fun setTimer(focus: Int, short: Int, long: Int, interval: Int = 4) = update { it[focusKey] = focus; it[shortKey] = short; it[longKey] = long; it[intervalKey] = interval.coerceIn(2,8) }
    suspend fun setGoal(v: Int) = update { it[goalKey] = v }
    suspend fun setSound(v: Boolean) = update { it[soundKey] = v }
}
data class PrefState(val theme: String, val name: String, val focus: Int, val short: Int, val long: Int, val goal: Int, val sound: Boolean, val longBreakEvery: Int = 4)
