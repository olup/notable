package com.example.inka

import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.inka.ui.theme.InkaTheme
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import com.onyx.android.sdk.pen.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.example.inka.db.*


enum class Pen {
    BALLPEN,
    PENCIL,
    BRUSH,
    MARKER,
    FOUNTAIN
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BookUi(navController: NavController, restartCount: Int, bookId: Int, pageId: Int) {
    var pen by remember { mutableStateOf(Pen.BALLPEN) }
    var strokeSize by remember { mutableStateOf(10f) }
    var isDrawing by remember { mutableStateOf(true) }

    val appRepository = AppRepository(LocalContext.current)
    var pageId by remember { mutableStateOf(pageId) }

    // update opened page
    LaunchedEffect(pageId) {
        appRepository.bookRepository.setOpenPageId(bookId, pageId)
    }

    fun goToNextPage() {
        pageId = appRepository.getNextPageIdFromBookAndPage(
            pageId = pageId,
            notebookId = bookId
        )
    }

    fun goToPreviousPage() {
        val newPageId = appRepository.getPreviousPageIdFromBookAndPage(
            pageId = pageId,
            notebookId = bookId
        )
        if (newPageId != null) pageId = newPageId
    }

    InkaTheme {
        DrawBox(
            pen = pen,
            strokeSize = strokeSize,
            restartCount = restartCount,
            bookId = bookId,
            isDrawing = isDrawing,
            pageId = pageId,
            goToNextPage = ::goToNextPage,
            goToPreviousPage = ::goToPreviousPage,
        )
        Toolbar(
            navController = navController,
            pen = pen,
            onChangePen = { pen = it },
            _strokeSize = strokeSize,
            onChangeStrokeSize = { strokeSize = it },
            onChangeIsDrawing = { isDrawing = it },
            pageId = pageId,
            bookId = bookId,
        )
    }
}


@Composable
@ExperimentalComposeUiApi
fun DrawBox(
    pen: Pen,
    strokeSize: Float,
    restartCount: Int,
    bookId: Int,
    isDrawing: Boolean,
    pageId: Int,
    goToNextPage: () -> Unit,
    goToPreviousPage: () -> Unit,
) {
    val appRepository = AppRepository(LocalContext.current)
    val page = appRepository.pageRepository.getById(pageId)


    var forceUpdate by remember { mutableStateOf(0) }

    var scroll by remember(pageId) { mutableStateOf(page.scroll) }
    println("scroll in main is $scroll")
    var breakBarPosition by remember { mutableStateOf(-1f) }

    var mode = "normal" // normmal / breakBar

    LaunchedEffect(scroll) {
        if (page.scroll != scroll) appRepository.pageRepository.updateScroll(pageId, scroll)
    }

    Box(
        modifier = Modifier
            .pointerInput(pageId) {
                forEachGesture {
                    awaitPointerEventScope {
                        val down = awaitFirstDown(false)
                        if (down.type == PointerType.Stylus) return@awaitPointerEventScope

                        withTimeoutOrNull(100) {
                            waitForUpOrCancellation()
                            true
                        } ?: return@awaitPointerEventScope

                        val secondTap = withTimeoutOrNull(200) {
                            awaitFirstDown(false)
                            withTimeoutOrNull(100) {
                                waitForUpOrCancellation()
                                true
                            } ?: return@withTimeoutOrNull null

                            // Double tap
                            // mode = "breakBar"
                            // breakBarPosition = down.position.y

                            true
                        }

                        if (secondTap != null) return@awaitPointerEventScope
                        // single tap
                        /*if (mode == "breakBar") {
                            mode = "normal"
                            breakBarPosition = -1f
                        }*/

                    }
                }
            }
            .pointerInput(pageId) {
                forEachGesture {
                    awaitPointerEventScope {
                        val down = awaitFirstDown(requireUnconsumed = false)

                        val initialPosition = down.position
                        var lastPosition = initialPosition
                        var isCanceled = false
                        var inputNumber = 0
                        // ignore stylus
                        if (down.type == PointerType.Stylus) return@awaitPointerEventScope

                        do {
                            val event: PointerEvent = awaitPointerEvent()
                            event.changes.forEach { pointerInputChange: PointerInputChange ->
                            }

                            val fingerChange = event.changes.filter { it.type == PointerType.Touch }

                            if (fingerChange.size == 1) {
                                if (inputNumber == 2) isCanceled = true
                                else if (inputNumber == 0) inputNumber = 1
                            } else if (fingerChange.size == 2) {
                                if (inputNumber == 1 || inputNumber == 0) inputNumber = 2
                            }

                            if (fingerChange.size > 0) {
                                lastPosition = fingerChange[0].position
                            }
                        } while (event.changes.any { it.pressed } && !isCanceled)

                        val verticalDrag = lastPosition.y - initialPosition.y
                        val horinzontalDrag = lastPosition.x - initialPosition.x

                        if (mode == "normal") {
                            if (verticalDrag < -200) {
                                if (inputNumber == 1) {
                                    scroll -= verticalDrag.toInt()
                                }
                            }
                            if (verticalDrag > 200) {
                                if (inputNumber == 1) {
                                    println("scroll in gesture is $scroll")
                                    scroll -= verticalDrag.toInt()
                                    if (scroll < 0) scroll = 0
                                }

                            }
                            if (horinzontalDrag < -200) {
                                if (inputNumber == 1) {
                                    goToNextPage()
                                } else if (inputNumber == 2) {
                                    appRepository.redo()
                                    forceUpdate++
                                }


                            }
                            if (horinzontalDrag > 200) {
                                if (inputNumber == 1) {
                                    goToPreviousPage()
                                } else if (inputNumber == 2) {
                                    appRepository.undo(pageId)
                                    forceUpdate++
                                }
                            }
                        }
                        if (mode == "breakBar") {
                            if (verticalDrag != 0f) {
                                appRepository.pointRepository.offsetPoints(
                                    pageId,
                                    scroll + breakBarPosition,
                                    verticalDrag
                                )
                                forceUpdate++
                                mode = "normal"
                                breakBarPosition = -1f
                            }
                        }
                    }

                }
            }
            .fillMaxWidth()
            .fillMaxHeight()

    ) {
        AndroidView(factory = { ctx ->
            DrawCanvas(ctx, appRepository, pageId).apply {
                init(pageId, scroll)

            }
        },
            update = {

                it.breakBarPosition = breakBarPosition

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
                    println("pageId update")
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

            })
    }
}

