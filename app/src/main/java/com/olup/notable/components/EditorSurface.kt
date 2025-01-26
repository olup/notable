package com.olup.notable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.shipbook.shipbooksdk.Log

@Composable
@ExperimentalComposeUiApi
fun EditorSurface(
    state: EditorState, page: PageView, history: History
) {
    val coroutineScope = rememberCoroutineScope()
    Log.i(TAG, "recompose surface")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()

    ) {
        AndroidView(factory = { ctx ->
            DrawCanvas(ctx, coroutineScope, state, page, history).apply {
                init()
                registerObservers()
            }
        })
    }
}