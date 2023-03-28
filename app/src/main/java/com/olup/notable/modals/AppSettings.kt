package com.olup.notable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.olup.notable.components.SelectMenu
import com.olup.notable.db.Kv
import com.olup.notable.db.KvProxy
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@kotlinx.serialization.Serializable
data class AppSettings (
    val version: Int,
    val defaultNativeTemplate : String = "blank",
    val quickNavPages : List<String> = listOf()
)


@Composable
fun AppSettingsModal(onClose: () -> Unit) {
    val kv = KvProxy(LocalContext.current)
    val settings by kv.observeKv("APP_SETTINGS", AppSettings.serializer(), AppSettings(version = 1)).observeAsState()

    if(settings == null) return
    Dialog(
        onDismissRequest = { onClose() }
    ) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .fillMaxWidth()
                .border(2.dp, Color.Black, RectangleShape)
        ) {
            Column(
                Modifier.padding(20.dp, 10.dp)
            ) {
                Text(text = "App setting")
            }
            Box(
                Modifier
                    .height(0.5.dp)
                    .fillMaxWidth()
                    .background(Color.Black)
            )


            Column(
                Modifier.padding(20.dp, 10.dp)
            ) {

                Row() {
                    Text(text = "Default Page Background Template")
                    Spacer(Modifier.width(10.dp))
                    SelectMenu(
                        options = listOf(
                            "blank" to "Blank page",
                            "dotted" to "Dot grid",
                            "lined" to "Lines",
                            "squared" to "Small squares grid"
                        ),
                        onChange = {
                            kv.setKv("APP_SETTINGS", settings!!.copy(defaultNativeTemplate = it), AppSettings.serializer())
                        },
                        value = settings?.defaultNativeTemplate ?: "blank"
                    )

                }
                Spacer(Modifier.height(10.dp))
            }

        }

    }
}