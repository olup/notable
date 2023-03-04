package com.example.inka

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class EditorControlTower(val scope : CoroutineScope, val page : PageModel, val history: History, val state: EditorState) {

    fun resetSelectionState() {
        state.selectionState.selectedStrokes = null
        state.selectionState.firstPageCut = null
        state.selectionState.secondPageCut = null
    }

    fun onSingleFingerVerticalSwipe(startPosition : SimplePointF, delta : Int){
        if(state.mode == Mode.SELECT){
            if(state.selectionState.firstPageCut != null){
                onOpenPageCut(delta)
            }else {
                onPageScroll(-delta)
            }
        }else {
            onPageScroll(-delta)
        }

        scope.launch{ DrawCanvas.refreshUi.emit(Unit) }

    }

    fun onOpenPageCut(offset : Int){
        if(offset < 0) return
        var cutLine = state.selectionState.firstPageCut!!

        val (_, previousStrokes) = divideStrokesFromCut(page.strokes, cutLine)

        // calculate new strokes to add to the page
        val nextStrokes = previousStrokes.map {
            it.copy(points = it.points.map {
                it.copy(x = it.x, y = it.y + offset)
            }, top = it.top +offset, bottom = it.bottom +offset)
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

        resetSelectionState()
        page.drawArea(pageAreaToCanvasArea(strokeBounds(previousStrokes+nextStrokes), page.scroll))
    }

    fun onPageScroll(delta : Int){
        page!!.updateScroll(delta)
    }

}