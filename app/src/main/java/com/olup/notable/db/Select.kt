package com.olup.notable.db

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.unit.IntOffset
import com.olup.notable.DrawCanvas
import com.olup.notable.EditorState
import com.olup.notable.PageView
import com.olup.notable.PlacementMode
import com.olup.notable.SelectPointPosition
import com.olup.notable.SimplePointF
import com.olup.notable.TAG
import com.olup.notable.divideStrokesFromCut
import com.olup.notable.drawImage
import com.olup.notable.drawStroke
import com.olup.notable.imageBoundsInt
import com.olup.notable.pageAreaToCanvasArea
import com.olup.notable.pointsToPath
import com.olup.notable.selectStrokesFromPath
import com.olup.notable.strokeBounds
import io.shipbook.shipbooksdk.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun selectImage(
    scope: CoroutineScope,
    page: PageView,
    editorState: EditorState,
    imageToSelect: Image
) {
    //handle selection:
    val pageBounds = imageBoundsInt(imageToSelect)
    val padding = 0
    pageBounds.inset(-padding, -padding)
    val bounds = pageAreaToCanvasArea(pageBounds, page.scroll)
    val selectedBitmap = Bitmap.createBitmap(
        imageToSelect.width,
        imageToSelect.height,
        Bitmap.Config.ARGB_8888
    )
    val selectedCanvas = Canvas(selectedBitmap)
    drawImage(
        page.context,
        selectedCanvas,
        imageToSelect,
        IntOffset(-imageToSelect.x, -imageToSelect.y)
    )
    // set state
    editorState.selectionState.selectedImages = listOf<Image>(imageToSelect)
    editorState.selectionState.selectedBitmap = selectedBitmap
    editorState.selectionState.selectionStartOffset = IntOffset(bounds.left, bounds.top)
    editorState.selectionState.selectionRect = bounds
    editorState.selectionState.selectionDisplaceOffset = IntOffset(0, 0)
    editorState.selectionState.placementMode = PlacementMode.Move
    page.drawArea(bounds, ignoredImageIds = listOf<Image>(imageToSelect).map { it.id })

    scope.launch {
        DrawCanvas.refreshUi.emit(Unit)
        editorState.isDrawing = false
    }
}


/** Written by GPT:
 * Handles selection of strokes and areas on a page, enabling either lasso selection or
 * page-cut-based selection for further manipulation or operations.
 *
 * This function performs the following steps:
 *
 * 1. **Page Cut Selection**:
 *    - Identifies if the selection points cross the left or right edge of the page (`Page cut` case).
 *    - Determines the direction of the cut and creates a complete selection area spanning the page.
 *    - For the first page cut, it registers the cut coordinates.
 *    - For the second page cut, it orders the cuts, divides the strokes into sections based on these cuts,
 *      and assigns the strokes in the middle section to `selectedStrokes`.
 *
 * 2. **Lasso Selection**:
 *    - For non-page-cut cases, it performs lasso selection using the provided points.
 *    - Creates a `Path` from the selection points and identifies strokes within this lasso area.
 *    - Computes the bounding box (`pageBounds`) for the selected strokes and expands it with padding.
 *    - Maps the page-relative bounds to the canvas coordinate space.
 *    - Renders the selected strokes onto a new bitmap using the calculated bounds.
 *    - Updates the editor's selection state with:
 *      - The selected strokes.
 *      - The created bitmap and its position on the canvas.
 *      - The selection rectangle and displacement offset.
 *      - Enabling the "Move" placement mode for manipulation.
 *    - Optionally, redraws the affected area without the selected strokes.
 *
 * 3. **UI Refresh**:
 *    - Notifies the UI to refresh and disables the drawing mode.
 *
 * @param scope The `CoroutineScope` used to perform asynchronous operations, such as UI refresh.
 * @param page The `PageView` object representing the current page, including its strokes and dimensions.
 * @param editorState The `EditorState` object storing the current state of the editor, such as selected strokes.
 * @param points A list of `SimplePointF` objects defining the user's selection path in page coordinates.
 * points is in page coodinates
 */
fun handleSelect(
    scope: CoroutineScope,
    page: PageView,
    editorState: EditorState,
    points: List<SimplePointF>
) {
    val state = editorState.selectionState

    val firstPointPosition =
        if (points.first().x < 50) SelectPointPosition.LEFT else if (points.first().x > page.width - 50) SelectPointPosition.RIGHT else SelectPointPosition.CENTER
    val lastPointPosition =
        if (points.last().x < 50) SelectPointPosition.LEFT else if (points.last().x > page.width - 50) SelectPointPosition.RIGHT else SelectPointPosition.CENTER

    if (firstPointPosition != SelectPointPosition.CENTER && lastPointPosition != SelectPointPosition.CENTER && firstPointPosition != lastPointPosition) {
        // Page cut situation
        val correctedPoints =
            if (firstPointPosition === SelectPointPosition.LEFT) points else points.reversed()
        // lets make this end to end
        val completePoints =
            listOf(SimplePointF(0f, correctedPoints.first().y)) + correctedPoints + listOf(
                SimplePointF(page.width.toFloat(), correctedPoints.last().y)
            )
        if (state.firstPageCut == null) {
            // this is the first page cut
            state.firstPageCut = completePoints
            Log.i(TAG, "Registered first curt")
        } else {
            // this is the second page cut, we can also select the strokes
            // first lets have the cuts in the right order
            if (completePoints[0].y > state.firstPageCut!![0].y) state.secondPageCut =
                completePoints
            else {
                state.secondPageCut = state.firstPageCut
                state.firstPageCut = completePoints
            }
            // let's get stroke selection from that
            val (_, after) = divideStrokesFromCut(page.strokes, state.firstPageCut!!)
            val (middle, _) = divideStrokesFromCut(after, state.secondPageCut!!)
            state.selectedStrokes = middle
        }
    } else {
        // lasso selection
        // padding inside the dashed selection square
        val padding = 30

        // rcreate the lasso selection
        val selectionPath = pointsToPath(points)
        selectionPath.close()

        // get the selected strokes
        val selectedStrokes = selectStrokesFromPath(page.strokes, selectionPath)
        if (selectedStrokes.isEmpty()) return

        // TODO collocate with control tower ?

        state.selectedStrokes = selectedStrokes

        // area of implication - in page and view reference
        val pageBounds = strokeBounds(selectedStrokes)
        pageBounds.inset(-padding, -padding)

        val bounds = pageAreaToCanvasArea(pageBounds, page.scroll)

        // create bitmap and draw strokes
        val selectedBitmap =
            Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
        val selectedCanvas = Canvas(selectedBitmap)
        selectedStrokes.forEach {
            drawStroke(
                selectedCanvas,
                it,
                IntOffset(-pageBounds.left, -pageBounds.top)
            )
        }

        // set state
        state.selectedBitmap = selectedBitmap
        state.selectionStartOffset = IntOffset(bounds.left, bounds.top)
        state.selectionRect = bounds
        state.selectionDisplaceOffset = IntOffset(0, 0)
        state.placementMode = PlacementMode.Move

//        page.removeStrokes(selectedStrokes.map{it.id})
        page.drawArea(bounds, ignoredStrokeIds = selectedStrokes.map { it.id })

        scope.launch {
            DrawCanvas.refreshUi.emit(Unit)
            editorState.isDrawing = false
        }
    }
}
