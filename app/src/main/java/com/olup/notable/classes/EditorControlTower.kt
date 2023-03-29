package com.olup.notable

import android.graphics.Rect
import androidx.compose.ui.unit.toOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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

    fun applySelectionDisplace() {
        val selectedStrokes = state.selectionState.selectedStrokes!!
        val offset = state.selectionState.selectionDisplaceOffset!!
        val finalZone = Rect(state.selectionState.selectionRect!!)
        finalZone.offset(offset.x, offset.y)

        val displacedStrokes = selectedStrokes.map {
            offsetStroke(it, offset = offset.toOffset())
        }

        if (state.selectionState.placementMode == PlacementMode.Move) page.removeStrokes(selectedStrokes.map{it.id})

        page.addStrokes(displacedStrokes)
        page.drawArea(finalZone)


        if (offset.x > 0 || offset.y > 0) {
            // A displacement happened, we can create a history for this
            var operationList =
                listOf<Operation>(Operation.DeleteStroke(displacedStrokes.map { it.id }))
            // in case we are on a move operation, this history point re-adds the original strokes
            if (state.selectionState.placementMode == PlacementMode.Move) operationList += Operation.AddStroke(
                selectedStrokes
            )
            history.addOperationsToHistory(operationList)
        }


        scope.launch {
            DrawCanvas.refreshUi.emit(Unit)
        }
    }

}