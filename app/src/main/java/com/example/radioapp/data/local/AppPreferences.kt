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
        val THEME_MODE_KEY = androidx.datastore.preferences.core.intPreferencesKey("theme_mode")
        val AUTO_PLAY_KEY = booleanPreferencesKey("auto_play")
        val GRID_COLUMNS_KEY = androidx.datastore.preferences.core.intPreferencesKey("grid_columns")
    }

    val themeModeFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_MODE_KEY] ?: 0 // 0 = System, 1 = Light, 2 = Dark
        }
        
    val autoPlayFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AUTO_PLAY_KEY] ?: true
        }

    val gridColumnsFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[GRID_COLUMNS_KEY] ?: 3
        }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode
        }
    }
    
    suspend fun setAutoPlay(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_PLAY_KEY] = enabled
        }
    }

    suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { preferences ->
            preferences[GRID_COLUMNS_KEY] = columns
        }
    }
}
