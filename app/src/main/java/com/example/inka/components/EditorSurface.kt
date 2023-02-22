package com.example.inka

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

@Composable
@ExperimentalComposeUiApi
fun EditorSurface(
    restartCount: Int, state: EditorState
) {
    val appRepository = AppRepository(LocalContext.current)
    val couroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()

    ) {
        AndroidView(factory = { ctx ->
            DrawCanvas(ctx, couroutineScope, appRepository, state).apply {
                init()
                registerObservers()
            }
        }, update = {
            if (it.restartCount !== restartCount) {
                println("restart count triggered")
                it.restartCount = restartCount
                it.init()
                it.pageFullRefresh()
            }
        })
    }
}