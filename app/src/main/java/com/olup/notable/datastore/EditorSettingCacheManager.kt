package com.olup.notable

import android.content.Context
import com.olup.notable.AppRepository
import com.olup.notable.db.Kv
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val persistVersion = 2

object EditorSettingCacheManager {

    @kotlinx.serialization.Serializable
    data class EditorSettings(
        val version: Int = persistVersion,
        val isToolbarOpen: Boolean,
        val pen: Pen,
        val eraser: Eraser? = Eraser.PEN,
        val penSettings: NamedSettings,
        val mode: Mode
    )

    fun init(context: Context){
        val settingsJSon = AppRepository(context).kvRepository.get("EDITOR_SETTINGS")
        if(settingsJSon != null) {
            val settings = Json.decodeFromString<EditorSettings>(settingsJSon.value)
            if(settings.version == persistVersion) setEditorSettings(context, settings, false)
        }
    }

    fun persist(context: Context, settings : EditorSettings){
        val settingsJson = Json.encodeToString(settings)
        AppRepository(context).kvRepository.set(Kv("EDITOR_SETTINGS", settingsJson))
    }

    private var editorSettings: EditorSettings? = null
    fun getEditorSettings(): EditorSettings? {
        return editorSettings
    }

    fun setEditorSettings(context : Context, newEditorSettings: EditorSettings, shouldPersist : Boolean = true) {
        editorSettings = newEditorSettings
        if(shouldPersist) persist(context, newEditorSettings)
    }
}
