package com.olup.notable

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import com.olup.notable.db.Image
import com.olup.notable.db.Stroke

enum class Mode {
    Draw, Erase, Select, Line
}

class EditorState(val bookId: String? = null, val pageId: String, val pageView: PageView) {

    val persistedEditorSettings = EditorSettingCacheManager.getEditorSettings()

    var mode by mutableStateOf(persistedEditorSettings?.mode ?: Mode.Draw) // should save
    var pen by mutableStateOf(persistedEditorSettings?.pen ?: Pen.BALLPEN) // should save
    var eraser by mutableStateOf(persistedEditorSettings?.eraser ?: Eraser.PEN) // should save
    var isDrawing by mutableStateOf(true)
    var isToolbarOpen by mutableStateOf(
        persistedEditorSettings?.isToolbarOpen ?: false
    ) // should save
    var penSettings by mutableStateOf<NamedSettings>(
        persistedEditorSettings?.penSettings ?: mapOf<String, PenSetting>(
            Pen.BALLPEN.penName to PenSetting(5f, Color.BLACK),
            Pen.REDBALLPEN.penName to PenSetting(5f, Color.RED),
            Pen.BLUEBALLPEN.penName to PenSetting(5f, Color.BLUE),
            Pen.GREENBALLPEN.penName to PenSetting(5f, Color.GREEN),
            Pen.PENCIL.penName to PenSetting(5f, Color.BLACK),
            Pen.BRUSH.penName to PenSetting(5f, Color.BLACK),
            Pen.MARKER.penName to PenSetting(40f, Color.LTGRAY),
            Pen.FOUNTAIN.penName to PenSetting(5f, Color.BLACK)
        )
    )
    // needed for images, should be removed
    var isDialogOpen by mutableStateOf(false)


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
    var selectedImages by mutableStateOf<List<Image>?>(null)
    var selectedBitmap by mutableStateOf<Bitmap?>(null)
    var selectionStartOffset by mutableStateOf<IntOffset?>(null)
    var selectionDisplaceOffset by mutableStateOf<IntOffset?>(null)
    var selectionRect by mutableStateOf<Rect?>(null)
    var placementMode by mutableStateOf<PlacementMode?>(null)

    fun reset() {
        selectedStrokes = null
        selectedImages = null
        secondPageCut = null
        firstPageCut = null
        selectedBitmap = null
        selectionStartOffset = null
        selectionRect = null
        selectionDisplaceOffset = null
        placementMode = null
    }
}