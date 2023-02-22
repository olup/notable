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
    pen: Pen,
    strokeSize: Float,
    restartCount: Int,
    isDrawing: Boolean,
    pageId: Int,
    isToolbarOpen: Boolean,
    scroll: Int,
    forceUpdate: Int
) {
    val appRepository = AppRepository(LocalContext.current)
    val couroutineScope = rememberCoroutineScope()


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()

    ) {
        AndroidView(factory = { ctx ->
            DrawCanvas(ctx, couroutineScope, appRepository, pageId).apply {
                init(pageId, scroll)
                val thisClass = this
                couroutineScope.launch {
                    println("listening")
                    debouncedSavedBitmapFlow.debounce(1000).collect {
                        println("Saving page to disk")
                        thread(true) {
                            pageBitmapToFile(
                                context,
                                thisClass.offScreenBitmap,
                                thisClass.pageId
                            )
                        }
                    }
                }

            }
        },
            update = {

                // restart
                if (it.restartCount !== restartCount) {
                    println("restart count triggered")
                    it.restartCount = restartCount
                    it.init(pageId, scroll)// TODO fix
                    it.pageFullRefresh()
                }


                if (it.pageId != pageId) {
                    println("pageId update")
                    it.updatePageId(pageId, scroll)
                } else if (scroll != it.scroll) {
                    it.updateScroll(scroll)
                }

                if (it.forceUpdate != forceUpdate) {
                    println("force update")
                    it.updateForceUpdate(forceUpdate)
                }

                if (pen != it.pen || strokeSize != it.strokeSize) {
                    println("pen chnaged")
                    it.updatePenAndStroke(pen, strokeSize)
                    it.refreshUi()
                }

                if (isDrawing != it.isDrawing) {
                    println("isDrawing $isDrawing")
                    it.updateIsDrawing(isDrawing)
                    it.refreshUi()
                }

                if (isToolbarOpen != it.isToolbarOpen) {
                    println("isToolBarOpen $isToolbarOpen")
                    it.updateIsToolbarOpen(isToolbarOpen)
                    it.refreshUi()
                }
            })
    }
}