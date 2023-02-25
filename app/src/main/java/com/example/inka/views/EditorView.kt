package com.example.inka

import android.graphics.RectF
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.inka.db.*
import com.example.inka.ui.theme.InkaTheme
import com.onyx.android.sdk.pen.*

enum class Mode {
    DRAW,
    ERASE,
    SELECT
}

class PageEditorState(pageId : String, scroll : Int, bookId: String?) {
    var pen by mutableStateOf(Pen.BALLPEN) // should save
    var strokeSize by mutableStateOf( 10f) // should save
    var isDrawing by mutableStateOf(true)
    var isToolbarOpen by mutableStateOf(false) // should save
    var scroll by mutableStateOf(scroll)
    var forceUpdate by mutableStateOf(0 to RectF())
    var mode by mutableStateOf(Mode.DRAW) // should save
    var pageId by mutableStateOf(pageId)
    var bookId by mutableStateOf(bookId)
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BookUi(navController: NavController, restartCount: Int, _bookId: String?, _pageId: String) {

    val appRepository = AppRepository(LocalContext.current)
    val page = appRepository.pageRepository.getById(_pageId)

    val state = remember {
        PageEditorState(_pageId, page?.scroll!!, _bookId)
    }


    // update opened page
    LaunchedEffect(Unit) {
        println("Initializing state")
        if (_bookId != null) {
            appRepository.bookRepository.setOpenPageId(_bookId, state.pageId)
            println("Open page updated")

        }
        // clean history
        clearHistory()
    }

    LaunchedEffect(state.scroll) {
        appRepository.pageRepository.updateScroll(state.pageId, state.scroll)
    }

    val lastRoute = navController.previousBackStackEntry
    println(lastRoute?.destination)

    fun goToNextPage() {
        if (state.bookId != null) {
            val newPageId = appRepository.getNextPageIdFromBookAndPage(
                pageId = state.pageId,
                notebookId = state.bookId!!
            )
            navController.navigate("books/${_bookId}/pages/${newPageId}"){
                popUpTo(lastRoute!!.destination.id ){
                    inclusive= false
                }
            }
        }
    }

    fun goToPreviousPage() {
        if (state.bookId != null) {
            val newPageId = appRepository.getPreviousPageIdFromBookAndPage(
                pageId = state.pageId,
                notebookId = state.bookId!!
            )
            if (newPageId != null) navController.navigate("books/${_bookId}/pages/${newPageId}")
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


