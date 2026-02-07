package com.jnmsse.cockpithealthmonitor.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesManager(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        // For the main app's selected server
        val SERVER_URL_KEY = stringPreferencesKey("server_url")
        // For widgets, keyed by appWidgetId
        fun getServerUrlKey(appWidgetId: Int) = stringPreferencesKey("server_url_$appWidgetId")
    }

    // Main app server URL
    val serverUrl: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[SERVER_URL_KEY]
        }

    suspend fun saveServerUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[SERVER_URL_KEY] = url
        }
    }

    // Widget-specific server URL
    fun getWidgetServerUrl(appWidgetId: Int): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[getServerUrlKey(appWidgetId)]
        }
    }

    suspend fun saveWidgetServerUrl(appWidgetId: Int, url: String) {
        dataStore.edit { preferences ->
            preferences[getServerUrlKey(appWidgetId)] = url
        }
    }

    suspend fun deleteWidgetServerUrl(appWidgetId: Int) {
        dataStore.edit { preferences ->
            preferences.remove(getServerUrlKey(appWidgetId))
        }
    }
}