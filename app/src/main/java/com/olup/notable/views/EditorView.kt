package com.olup.notable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.olup.notable.db.*
import com.olup.notable.ui.theme.InkaTheme
import com.onyx.android.sdk.pen.*


@OptIn(ExperimentalComposeUiApi::class)
@Composable
@ExperimentalFoundationApi
fun EditorView(
    navController: NavController, _bookId: String?, _pageId: String
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // control if we do have a page
    if(AppRepository(context).pageRepository.getById(_pageId) == null) {
        if(_bookId != null){
            // clean the book
            println("Cleaning book")
            AppRepository(context).bookRepository.removePage(_bookId, _pageId)
        }
        navController.navigate("library")
        return
    }


    val page = remember {
        PageModel(
            context = context,
            coroutineScope = scope,
            id = _pageId,
            width = SCREEN_WIDTH,
            viewWidth = SCREEN_WIDTH,
            viewHeight = SCREEN_HEIGHT
        )
    }

    val editorState = remember { EditorState(bookId = _bookId, pageId = _pageId, pageModel = page) }

    val history = remember {
        History(scope, page)
    }
    val editorControlTower = remember {
        EditorControlTower(scope, page, history, editorState)
    }

    val appRepository = AppRepository(context)

    // update opened page
    LaunchedEffect(Unit) {
        if (_bookId != null) {
            appRepository.bookRepository.setOpenPageId(_bookId, _pageId)
        }
    }

    // TODO put in editorSetting class
    LaunchedEffect(
        editorState.isToolbarOpen,
        editorState.pen,
        editorState.penSettings,
        editorState.mode
    ) {
        println("saving")
        DataStoreManager.setEditorSettings(
            context,
            DataStoreManager.EditorSettings(
                isToolbarOpen = editorState.isToolbarOpen,
                mode = editorState.mode,
                pen = editorState.pen,
                penSettings = editorState.penSettings
            )
        )
    }

    val lastRoute = navController.previousBackStackEntry

    fun goToNextPage() {
        if (_bookId != null) {
            val newPageId = appRepository.getNextPageIdFromBookAndPage(
                pageId = _pageId, notebookId = _bookId!!
            )
            navController.navigate("books/${_bookId}/pages/${newPageId}") {
                popUpTo(lastRoute!!.destination.id) {
                    inclusive = false
                }
            }
        }
    }

    fun goToPreviousPage() {
        if (_bookId != null) {
            val newPageId = appRepository.getPreviousPageIdFromBookAndPage(
                pageId = _pageId, notebookId = _bookId!!
            )
            if (newPageId != null) navController.navigate("books/${_bookId}/pages/${newPageId}")
        }
    }

    InkaTheme {
        EditorSurface(
            state = editorState, page = page, history = history
        )
        EditorGestureReceiver(
            goToNextPage = ::goToNextPage,
            goToPreviousPage = ::goToPreviousPage,
            controlTower = editorControlTower,
            state = editorState
        )
        SelectedBitmap(editorState = editorState, controlTower = editorControlTower)
        ScrollIndicator(context = context, state = editorState)
        Toolbar(
            navController = navController, state = editorState
        )

    }
}


