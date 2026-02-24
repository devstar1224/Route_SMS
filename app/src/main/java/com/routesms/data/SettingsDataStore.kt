package com.routesms.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = "Application",
                keysToMigrate = setOf("api_url")
            )
        )
    }
)

object SettingsKeys {
    val WEBHOOK_URL = stringPreferencesKey("api_url")
    val FILTER_RULES_JSON = stringPreferencesKey("filter_rules_json")
    val DAILY_MESSAGE_COUNT = intPreferencesKey("daily_message_count")
    val LAST_COUNT_RESET_DATE = stringPreferencesKey("last_count_reset_date")
    val FORWARDED_TOTAL_COUNT = intPreferencesKey("forwarded_total_count")
    val FORWARDING_ENABLED = booleanPreferencesKey("forwarding_enabled")
}

// Webhook URL
fun DataStore<Preferences>.webhookUrlFlow(): Flow<String> {
    return data.map { preferences ->
        preferences[SettingsKeys.WEBHOOK_URL] ?: ""
    }
}

suspend fun DataStore<Preferences>.saveWebhookUrl(url: String) {
    edit { preferences ->
        preferences[SettingsKeys.WEBHOOK_URL] = url
    }
}

// Filter Rules JSON
fun DataStore<Preferences>.filterRulesJsonFlow(): Flow<String> {
    return data.map { preferences ->
        preferences[SettingsKeys.FILTER_RULES_JSON] ?: "[]"
    }
}

suspend fun DataStore<Preferences>.saveFilterRulesJson(json: String) {
    edit { preferences ->
        preferences[SettingsKeys.FILTER_RULES_JSON] = json
    }
}

// Daily Message Count
fun DataStore<Preferences>.dailyMessageCountFlow(): Flow<Int> {
    return data.map { preferences ->
        preferences[SettingsKeys.DAILY_MESSAGE_COUNT] ?: 0
    }
}

suspend fun DataStore<Preferences>.incrementDailyMessageCount() {
    edit { preferences ->
        val current = preferences[SettingsKeys.DAILY_MESSAGE_COUNT] ?: 0
        preferences[SettingsKeys.DAILY_MESSAGE_COUNT] = current + 1
    }
}

suspend fun DataStore<Preferences>.resetDailyMessageCount() {
    edit { preferences ->
        preferences[SettingsKeys.DAILY_MESSAGE_COUNT] = 0
    }
}

// Last Count Reset Date
fun DataStore<Preferences>.lastCountResetDateFlow(): Flow<String> {
    return data.map { preferences ->
        preferences[SettingsKeys.LAST_COUNT_RESET_DATE] ?: ""
    }
}

suspend fun DataStore<Preferences>.saveLastCountResetDate(date: String) {
    edit { preferences ->
        preferences[SettingsKeys.LAST_COUNT_RESET_DATE] = date
    }
}

// Forwarded Total Count
fun DataStore<Preferences>.forwardedTotalCountFlow(): Flow<Int> {
    return data.map { preferences ->
        preferences[SettingsKeys.FORWARDED_TOTAL_COUNT] ?: 0
    }
}

suspend fun DataStore<Preferences>.incrementForwardedTotalCount() {
    edit { preferences ->
        val current = preferences[SettingsKeys.FORWARDED_TOTAL_COUNT] ?: 0
        preferences[SettingsKeys.FORWARDED_TOTAL_COUNT] = current + 1
    }
}

// Forwarding Enabled
fun DataStore<Preferences>.forwardingEnabledFlow(): Flow<Boolean> {
    return data.map { preferences ->
        preferences[SettingsKeys.FORWARDING_ENABLED] ?: true
    }
}

suspend fun DataStore<Preferences>.saveForwardingEnabled(enabled: Boolean) {
    edit { preferences ->
        preferences[SettingsKeys.FORWARDING_ENABLED] = enabled
    }
}

/**
 * 전달 활성화 여부를 동기적으로 조회 (BroadcastReceiver 등에서 사용)
 */
fun DataStore<Preferences>.isForwardingEnabledSync(): Boolean {
    return kotlinx.coroutines.runBlocking {
        data.map { it[SettingsKeys.FORWARDING_ENABLED] ?: true }.first()
    }
}

