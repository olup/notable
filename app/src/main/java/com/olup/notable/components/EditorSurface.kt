package com.olup.notable

import io.shipbook.shipbooksdk.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
@ExperimentalComposeUiApi
fun EditorSurface(
    state: EditorState, page : PageView, history: History
) {
    val couroutineScope = rememberCoroutineScope()
    Log.i(TAG, "recompose surface")

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
        })
    }
}