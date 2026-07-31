package com.example.radioapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppPreferences(private val context: Context) {
    companion object {
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        val AUTO_PLAY_KEY = booleanPreferencesKey("auto_play")
        val VIEW_MODE_KEY = stringPreferencesKey("view_mode")
        val FONT_SIZE_KEY = stringPreferencesKey("font_size")
    }

    val darkModeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }
        
    val autoPlayFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AUTO_PLAY_KEY] ?: true
        }

    val viewModeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[VIEW_MODE_KEY] ?: "List"
        }

    val fontSizeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[FONT_SIZE_KEY] ?: "Default"
        }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }
    
    suspend fun setAutoPlay(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_PLAY_KEY] = enabled
        }
    }

    suspend fun setViewMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[VIEW_MODE_KEY] = mode
        }
    }

    suspend fun setFontSize(size: String) {
        context.dataStore.edit { preferences ->
            preferences[FONT_SIZE_KEY] = size
        }
    }
}
