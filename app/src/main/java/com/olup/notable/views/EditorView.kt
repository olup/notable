package com.olup.notable

import android.graphics.Bitmap
import android.graphics.Rect
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset

enum class Mode {
    Draw, Erase, Select
}

class EditorState(val bookId: String? = null, val pageId: String, val pageModel: PageModel) {

    val persistedEditorSettings = DataStoreManager.getEditorSettings()

    var mode by mutableStateOf(persistedEditorSettings?.mode ?: Mode.Draw) // should save
    var pen by mutableStateOf(persistedEditorSettings?.pen ?: Pen.BALLPEN) // should save
    var isDrawing by mutableStateOf(true)
    var isToolbarOpen by mutableStateOf(
        persistedEditorSettings?.isToolbarOpen ?: false
    ) // should save
    var penSettings by mutableStateOf<NamedSettings>(
        persistedEditorSettings?.penSettings ?: mapOf<String, PenSetting>(
            Pen.BALLPEN.penName to PenSetting(5f, android.graphics.Color.BLACK),
            Pen.PENCIL.penName to PenSetting(5f, android.graphics.Color.BLACK),
            Pen.BRUSH.penName to PenSetting(5f, android.graphics.Color.BLACK),
            Pen.MARKER.penName to PenSetting(20f, android.graphics.Color.LTGRAY),
            Pen.FOUNTAIN.penName to PenSetting(5f, android.graphics.Color.BLACK)
        )
    )

    val selectionState = SelectionState()
}

enum class PlacementMode {
    Move,
    Paste
}

class SelectionState {
    var firstPageCut by mutableStateOf<List<SimplePointF>?>(null)
    var secondPageCut by mutableStateOf<List<SimplePointF>?>(null)
    var selectedStrokes by mutableStateOf<List<Stroke>?>(null)
    var selectedBitmap by mutableStateOf<Bitmap?>(null)
    var selectionStartOffset by mutableStateOf<IntOffset?>(null)
    var selectionDisplaceOffset by mutableStateOf<IntOffset?>(null)
    var selectionRect by mutableStateOf<Rect?>(null)
    var placementMode by mutableStateOf<PlacementMode?>(null)

    fun reset() {
        selectedStrokes = null
        secondPageCut = null
        firstPageCut = null
        selectedBitmap = null
        selectionStartOffset = null
        selectionRect = null
        selectionDisplaceOffset = null
        placementMode = null
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@ExperimentalFoundationApi
fun BookUi(
    navController: NavController, _bookId: String?, _pageId: String
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()


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


