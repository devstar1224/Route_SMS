package com.routesms.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
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
}

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
