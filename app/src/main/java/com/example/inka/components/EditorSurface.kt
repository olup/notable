package com.example.inka

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
@ExperimentalComposeUiApi
fun EditorSurface(
    restartCount: Int, state: EditorState, page : PageModel, history: History
) {
    val couroutineScope = rememberCoroutineScope()
    println("recompose surface")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()

    ) {
        AndroidView(factory = { ctx ->
            DrawCanvas(ctx, couroutineScope, state, page, history ).apply {
                init()
                registerObservers()
            }
        }, update = {
            if (it.restartCount !== restartCount) {
                println("restart count triggered")
                it.restartCount = restartCount
                it.init()
                it.drawCanvasToView()
            }
        })
    }
}