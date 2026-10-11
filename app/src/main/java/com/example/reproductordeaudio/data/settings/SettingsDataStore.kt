package com.example.reproductordeaudio.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_prefs")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val DOWNLOAD_FOLDER_URI_KEY = stringPreferencesKey("download_folder_uri")
    }

    val downloadFolderUri: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[DOWNLOAD_FOLDER_URI_KEY]
    }

    suspend fun saveDownloadFolderUri(uriString: String) {
        context.dataStore.edit { prefs ->
            prefs[DOWNLOAD_FOLDER_URI_KEY] = uriString
        }
    }

    suspend fun clearDownloadFolderUri() {
        context.dataStore.edit { prefs ->
            prefs.remove(DOWNLOAD_FOLDER_URI_KEY)
        }
    }
}
