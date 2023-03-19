package com.olup.notable

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


const val APP_DATASTORE ="app_datastore"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = APP_DATASTORE)


val persistVersion = 1

class DataStoreManager (val context: Context) {


    companion object {
        val EDITOR_SETTINGS = stringPreferencesKey("EDITOR_SETTINGS")
        private var editorSettings : EditorSettings? = null
        fun getEditorSettings() : EditorSettings?{
            return editorSettings
        }

        fun setEditorSettings(newEditorSettings: EditorSettings){
            editorSettings = newEditorSettings
        }
    }
    suspend fun saveEditorSettings(settings : EditorSettings) {
        context.dataStore.edit {
            it[EDITOR_SETTINGS] = Json.encodeToString(settings)
        }
    }

    fun getEditorSettings() = context.dataStore.data.map {
        val jsonSettings = it[EDITOR_SETTINGS]?:return@map null
        val settings = Json.decodeFromString<EditorSettings>(jsonSettings)
        if(settings.version == persistVersion) return@map settings
        else return@map null
    }

    suspend fun clearDataStore() = context.dataStore.edit {
        it.clear()
    }

    @kotlinx.serialization.Serializable
    data class EditorSettings(val version : Int = persistVersion, val isToolbarOpen : Boolean, val pen : Pen, val penSettings : NamedSettings,val  mode : Mode)

}