package com.olup.notable

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toOffset
import com.olup.notable.db.Image
import com.olup.notable.db.selectImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class EditorControlTower(
    val scope: CoroutineScope, val page: PageView, val history: History, val state: EditorState
) {

    fun onSingleFingerVerticalSwipe(startPosition: SimplePointF, delta: Int) {
        if (state.mode == Mode.Select) {
            if (state.selectionState.firstPageCut != null) {
                onOpenPageCut(delta)
            } else {
                onPageScroll(-delta)
            }
        } else {
            onPageScroll(-delta)
        }

        scope.launch { DrawCanvas.refreshUi.emit(Unit) }

    }

    fun onOpenPageCut(offset: Int) {
        if (offset < 0) return
        var cutLine = state.selectionState.firstPageCut!!

        val (_, previousStrokes) = divideStrokesFromCut(page.strokes, cutLine)

        // calculate new strokes to add to the page
        val nextStrokes = previousStrokes.map {
            it.copy(points = it.points.map {
                it.copy(x = it.x, y = it.y + offset)
            }, top = it.top + offset, bottom = it.bottom + offset)
        }

        // remove and paste
        page.removeStrokes(strokeIds = previousStrokes.map { it.id })
        page.addStrokes(nextStrokes)

        // commit to history
        history.addOperationsToHistory(
            listOf(
                Operation.DeleteStroke(nextStrokes.map { it.id }),
                Operation.AddStroke(previousStrokes)
            )
        )

        state.selectionState.reset()
        page.drawArea(
            pageAreaToCanvasArea(
                strokeBounds(previousStrokes + nextStrokes), page.scroll
            )
        )
    }

    fun onPageScroll(delta: Int) {
        page!!.updateScroll(delta)
    }


    //Now we can have selected images or selected strokes
    fun applySelectionDisplace() {
        val selectedStrokes = state.selectionState.selectedStrokes
        val selectedImages = state.selectionState.selectedImages
        val offset = state.selectionState.selectionDisplaceOffset!!
        val finalZone = Rect(state.selectionState.selectionRect!!)
        finalZone.offset(offset.x, offset.y)

        if (selectedStrokes != null) {

            val displacedStrokes = selectedStrokes.map {
                offsetStroke(it, offset = offset.toOffset())
            }

            if (state.selectionState.placementMode == PlacementMode.Move)
                page.removeStrokes(selectedStrokes.map { it.id })

            page.addStrokes(displacedStrokes)
            page.drawArea(finalZone)


            if (offset.x > 0 || offset.y > 0) {
                // A displacement happened, we can create a history for this
                var operationList =
                    listOf<Operation>(Operation.DeleteStroke(displacedStrokes.map { it.id }))
                // in case we are on a move operation, this history point re-adds the original strokes
                if (state.selectionState.placementMode == PlacementMode.Move)
                    operationList += Operation.AddStroke(selectedStrokes)
                history.addOperationsToHistory(operationList)
            }
        }
        if (selectedImages != null) {
            val displacedImages = selectedImages.map {
                offsetImage(it, offset = offset.toOffset())
            }
            if (state.selectionState.placementMode == PlacementMode.Move)
                page.removeImage(selectedImages.map { it.id })

            page.addImage(displacedImages)
            page.drawArea(finalZone)


            if (offset.x > 0 || offset.y > 0) {
                // A displacement happened, we can create a history for this
                var operationList =
                    listOf<Operation>(Operation.DeleteStroke(displacedImages.map { it.id }))
                // TODO: in case we are on a move operation, this history point re-adds the original strokes
                // if (state.selectionState.placementMode == PlacementMode.Move)
                //    operationList += Operation.AddImage(selectedImages)
                //history.addOperationsToHistory(operationList)
            }

        }
        scope.launch {
            DrawCanvas.refreshUi.emit(Unit)
        }
    }

    fun deleteSelection() {
        val selectedImages = state.selectionState.selectedImages
        if (!selectedImages.isNullOrEmpty()) {
            val imageIds: List<String> = selectedImages.map { it.id }
            Log.i(TAG, "removing images")
            page.removeImage(imageIds)
        }
        val selectedStrokes = state.selectionState.selectedStrokes
        if (!selectedStrokes.isNullOrEmpty()) {
            val imageIds: List<String> = selectedStrokes.map { it.id }
            Log.i(TAG, "removing strokes")
            page.removeStrokes(imageIds)
        }
        state.selectionState.reset()
        state.isDrawing = true
        scope.launch {
            DrawCanvas.refreshUi.emit(Unit)
        }
    }

    fun changeSizeOfSelection(scale: Int) {
        val selectedImages = state.selectionState.selectedImages

        // Ensure selected images are not null or empty
        if (!selectedImages.isNullOrEmpty()) {
            state.selectionState.selectedImages = selectedImages.map { image ->
                image.copy(
                    height = image.height + (image.height * scale / 100),
                    width = image.width + (image.width * scale / 100)
                )
            }

            // Adjust displacement offset by half the size change
            val sizeChange = selectedImages.firstOrNull()?.let { image ->
                IntOffset(
                    x = (image.width * scale / 200),
                    y = (image.height * scale / 200)
                )
            } ?: IntOffset.Zero

            val pageBounds = imageBoundsInt(selectedImages)
            state.selectionState.selectionRect = pageAreaToCanvasArea(pageBounds, page.scroll)

            state.selectionState.selectionDisplaceOffset =
                state.selectionState.selectionDisplaceOffset?.let { it - sizeChange } ?: IntOffset.Zero

            val selectedBitmap = Bitmap.createBitmap(
                pageBounds.width(), pageBounds.height(),
                Bitmap.Config.ARGB_8888
            )
            val selectedCanvas = Canvas(selectedBitmap)
            selectedImages.forEach() {
                drawImage(
                    page.context,
                    selectedCanvas,
                    it,
                    IntOffset(-it.x, -it.y)
                )
            }

            // set state
            state.selectionState.selectedBitmap = selectedBitmap

            // Emit a refresh signal to update UI
            scope.launch {
                DrawCanvas.refreshUi.emit(Unit)
            }
        }
    }


    fun copySelection() {
        // finish ongoing movement
        applySelectionDisplace()
        // set operation to paste only
        state.selectionState.placementMode = PlacementMode.Paste
        // change the selected stokes' ids - it's a copy
        state.selectionState.selectedStrokes = state.selectionState.selectedStrokes!!.map {
            it.copy(
                id = UUID
                    .randomUUID()
                    .toString(),
                createdAt = Date()
            )
        }
        // move the selection a bit, to show the copy
        state.selectionState.selectionDisplaceOffset = IntOffset(
            x = state.selectionState.selectionDisplaceOffset!!.x + 50,
            y = state.selectionState.selectionDisplaceOffset!!.y + 50,
        )
        //TODO: implement coping for images
    }

}