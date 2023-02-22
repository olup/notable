package com.example.inka

import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.inka.db.*
import com.example.inka.ui.theme.InkaTheme
import com.onyx.android.sdk.pen.*
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.concurrent.thread


enum class Pen {
    BALLPEN,
    PENCIL,
    BRUSH,
    MARKER,
    FOUNTAIN
}

class EditorState {

}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BookUi(navController: NavController, restartCount: Int, bookId: Int?, pageId: Int) {
    var pen by remember { mutableStateOf(Pen.BALLPEN) }
    var strokeSize by remember { mutableStateOf(penSizes[pen] ?: 10f) }
    var isDrawing by remember { mutableStateOf(true) }
    var isToolbarOpen by remember { mutableStateOf(false) }
    var scroll by remember { mutableStateOf(0) }
    var forceUpdate by remember { mutableStateOf(0) }

    val appRepository = AppRepository(LocalContext.current)
    var pageId by remember { mutableStateOf(pageId) }

    // update opened page
    LaunchedEffect(pageId) {
        println("PageId chnaged")
        if (bookId != null) {
            appRepository.bookRepository.setOpenPageId(bookId, pageId)
        }
        // clean history
        clearHistory()
    }

    LaunchedEffect(scroll) {
        appRepository.pageRepository.updateScroll(pageId, scroll)
    }

    fun goToNextPage() {
        if (bookId != null) {
            pageId = appRepository.getNextPageIdFromBookAndPage(
                pageId = pageId,
                notebookId = bookId
            )
        }
    }

    fun goToPreviousPage() {
        if (bookId != null) {
            val newPageId = appRepository.getPreviousPageIdFromBookAndPage(
                pageId = pageId,
                notebookId = bookId
            )
            if (newPageId != null) pageId = newPageId
        }
    }

    InkaTheme {
        EditorSurface(
            pen = pen,
            strokeSize = strokeSize,
            restartCount = restartCount,
            isDrawing = isDrawing,
            pageId = pageId,
            isToolbarOpen = isToolbarOpen,
            scroll = scroll,
            forceUpdate = forceUpdate
        )
        EditorGestureReceiver(
            pageId = pageId,
            goToNextPage = ::goToNextPage,
            goToPreviousPage = ::goToPreviousPage,
            scroll = scroll,
            updateScroll = { scroll = it },
            forceUpdate = { forceUpdate++ }
        )
        Toolbar(
            navController = navController,
            pen = pen,
            onChangePen = { pen = it },
            _strokeSize = strokeSize,
            onChangeStrokeSize = { strokeSize = it },
            onChangeIsDrawing = { isDrawing = it },
            onChangeIsToolbarOpen = { isToolbarOpen = it },
            bookId = bookId,
            isToolbarOpen = isToolbarOpen
        )
    }
}


