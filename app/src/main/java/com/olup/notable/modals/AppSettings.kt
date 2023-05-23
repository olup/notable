package com.olup.notable

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.olup.notable.components.SelectMenu
import com.olup.notable.db.Kv
import com.olup.notable.db.KvProxy
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.concurrent.thread

@kotlinx.serialization.Serializable
data class AppSettings(
    val version: Int,
    val defaultNativeTemplate: String = "blank",
    val quickNavPages: List<String> = listOf()
)


@Composable
fun AppSettingsModal(onClose: () -> Unit) {
    val context = LocalContext.current
    val kv = KvProxy(context)

    var isLatestVersion by remember {
        mutableStateOf(true)
    }
    LaunchedEffect(key1 = Unit, block = {
        thread {
            isLatestVersion = isLatestVersion(context)
        }
    })

    val settings by kv.observeKv("APP_SETTINGS", AppSettings.serializer(), AppSettings(version = 1))
        .observeAsState()

    if (settings == null) return

    Dialog(
        onDismissRequest = { onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false),

        ) {
        Column(
            modifier = Modifier
                .padding(40.dp)
                .background(Color.White)
                .fillMaxWidth()
                .border(2.dp, Color.Black, RectangleShape)
        ) {
            Column(
                Modifier.padding(20.dp, 10.dp)
            ) {
                Text(text = "App setting - v${BuildConfig.VERSION_NAME}${if(isNext) " [NEXT]" else ""}")
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
                            kv.setKv(
                                "APP_SETTINGS",
                                settings!!.copy(defaultNativeTemplate = it),
                                AppSettings.serializer()
                            )
                        },
                        value = settings?.defaultNativeTemplate ?: "blank"
                    )

                }
                Spacer(Modifier.height(10.dp))

                if(!isLatestVersion){
                    Text(text = "It seems a new version of Notable is available on github.", fontStyle = FontStyle.Italic)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "See release in browser",
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.noRippleClickable {
                            val urlIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/olup/notable/releases")
                            )
                            context.startActivity(urlIntent)
                        })
                    Spacer(Modifier.height(10.dp))
                }
            }

        }

    }
}