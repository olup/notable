package com.example.inka

import android.graphics.Rect
import android.graphics.RectF
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

class EditorState(pageId : String, bookId: String?) {
    var pen by mutableStateOf(Pen.BALLPEN)
    var strokeSize by mutableStateOf( 10f)
    var isDrawing by mutableStateOf(true)
    var isToolbarOpen by mutableStateOf(false)
    var scroll by mutableStateOf(0)
    var forceUpdate by mutableStateOf(0 to RectF())
    var pageId by mutableStateOf(pageId)
    var bookId by mutableStateOf(bookId)
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BookUi(navController: NavController, restartCount: Int, bookId: String?, pageId: String) {

    val state = remember {
        EditorState(pageId, bookId)
    }

    val appRepository = AppRepository(LocalContext.current)

    // update opened page
    LaunchedEffect(state.pageId) {
        println("PageId changed")
        if (bookId != null) {
            appRepository.bookRepository.setOpenPageId(bookId, pageId)
        }
        // clean history
        clearHistory()
    }

    LaunchedEffect(state.scroll) {
        appRepository.pageRepository.updateScroll(pageId, state.scroll)
    }

    fun goToNextPage() {
        if (state.bookId != null) {
            state.pageId = appRepository.getNextPageIdFromBookAndPage(
                pageId = state.pageId,
                notebookId = state.bookId!!
            )
        }
    }

    fun goToPreviousPage() {
        if (state.bookId != null) {
            val newPageId = appRepository.getPreviousPageIdFromBookAndPage(
                pageId = state.pageId,
                notebookId = state.bookId!!
            )
            if (newPageId != null) state.pageId = newPageId
        }
    }

    InkaTheme {
        EditorSurface(
            restartCount = restartCount,
            state= state
        )
        EditorGestureReceiver(
            state = state,
            goToNextPage = ::goToNextPage,
            goToPreviousPage = ::goToPreviousPage,
        )
        Toolbar(
            navController = navController,
            state = state
        )
    }
}


