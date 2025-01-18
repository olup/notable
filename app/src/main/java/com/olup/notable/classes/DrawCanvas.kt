package com.olup.notable

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.olup.notable.db.Image
import com.olup.notable.db.ImageRepository
import com.olup.notable.db.handleSelect
import com.olup.notable.db.selectImage
import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.RawInputCallback
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.android.sdk.pen.data.TouchPointList
import io.shipbook.shipbooksdk.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis


val pressure = EpdController.getMaxTouchPressure()

// keep reference of the surface view presently associated to the singleton touchhelper
var referencedSurfaceView: String = ""


class DrawCanvas(
    _context: Context,
    val coroutineScope: CoroutineScope,
    val state: EditorState,
    val page: PageView,
    val history: History
) : SurfaceView(_context) {

    private val strokeHistoryBatch = mutableListOf<String>()
//    private val commitHistorySignal = MutableSharedFlow<Unit>()


    companion object {
        var forceUpdate = MutableSharedFlow<Rect?>()
        var refreshUi = MutableSharedFlow<Unit>()
        var isDrawing = MutableSharedFlow<Boolean>()
        var restartAfterConfChange = MutableSharedFlow<Unit>()

        // before undo we need to commit changes
        val commitHistorySignal = MutableSharedFlow<Unit>()
        val commitHistorySignalImmediately = MutableSharedFlow<Unit>()

        // used for checking if commit was completed
        var commitCompletion = CompletableDeferred<Unit>()

        // It might be bad idea, but plan is to insert graphic in this, and then take it from it
        // There is probably better way
        var addImageByUri = MutableStateFlow<Uri?>(null)
        var imageCoordinateToSelect = MutableStateFlow<Pair<Int, Int>?>(null)
    }

    fun getActualState(): EditorState {
        return this.state
    }

    private val inputCallback: RawInputCallback = object : RawInputCallback() {

        override fun onBeginRawDrawing(p0: Boolean, p1: TouchPoint?) {
        }

        override fun onEndRawDrawing(p0: Boolean, p1: TouchPoint?) {
        }

        override fun onRawDrawingTouchPointMoveReceived(p0: TouchPoint?) {
        }

        override fun onRawDrawingTouchPointListReceived(plist: TouchPointList) {
            thread(true) {
                if (getActualState().mode == Mode.Erase) {
                    handleErase(
                        this@DrawCanvas.page,
                        history,
                        plist.points.map { SimplePointF(it.x, it.y + page.scroll) },
                        eraser = getActualState().eraser
                    )
                    drawCanvasToView()
                    refreshUi()
                }

                if (getActualState().mode == Mode.Draw) {
                    handleDraw(
                        this@DrawCanvas.page,
                        strokeHistoryBatch,
                        getActualState().penSettings[getActualState().pen.penName]!!.strokeSize,
                        getActualState().penSettings[getActualState().pen.penName]!!.color,
                        getActualState().pen,
                        plist.points
                    )
                    coroutineScope.launch {
                        commitHistorySignal.emit(Unit)
                    }
                }

                if (getActualState().mode == Mode.Select) {
                    handleSelect(coroutineScope,
                        this@DrawCanvas.page,
                        getActualState(),
                        plist.points.map { SimplePointF(it.x, it.y + page.scroll) })
                    drawCanvasToView()
                    refreshUi()
                }

                if (getActualState().mode == Mode.Line) {
                    // draw line
                    handleLine(
                        page = this@DrawCanvas.page,
                        historyBucket = strokeHistoryBatch,
                        strokeSize = getActualState().penSettings[getActualState().pen.penName]!!.strokeSize,
                        color = getActualState().penSettings[getActualState().pen.penName]!!.color,
                        pen = getActualState().pen,
                        touchPoints = plist.points
                    )
                    //make it visible
                    drawCanvasToView()
                    refreshUi()
                }
            }
        }


        override fun onBeginRawErasing(p0: Boolean, p1: TouchPoint?) {
        }

        override fun onEndRawErasing(p0: Boolean, p1: TouchPoint?) {
        }

        override fun onRawErasingTouchPointListReceived(plist: TouchPointList?) {
            if (plist == null) return
            handleErase(
                this@DrawCanvas.page,
                history,
                plist.points.map { SimplePointF(it.x, it.y + page.scroll) },
                eraser = getActualState().eraser
            )
            drawCanvasToView()
            refreshUi()
        }

        override fun onRawErasingTouchPointMoveReceived(p0: TouchPoint?) {
        }
    }

    private val touchHelper by lazy {
        referencedSurfaceView = this.hashCode().toString()
        TouchHelper.create(this, inputCallback)
    }

    fun init() {
        Log.i(TAG, "Initializing Canvas")

        val surfaceView = this

        val surfaceCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.i(TAG, "surface created $holder")
                // set up the drawing surface
                updateActiveSurface()
                // This is supposed to let the ui update while the old surface is being unmounted
                coroutineScope.launch {
                    forceUpdate.emit(null)
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder, format: Int, width: Int, height: Int
            ) {
                Log.i(TAG, "surface changed $holder")
                drawCanvasToView()
                updatePenAndStroke()
                refreshUi()
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.i(
                    TAG,
                    "surface destroyed ${
                        this@DrawCanvas.hashCode()
                    } - ref $referencedSurfaceView"
                )
                holder.removeCallback(this)
                if (referencedSurfaceView == this@DrawCanvas.hashCode().toString()) {
                    touchHelper.closeRawDrawing()
                }
            }
        }

        this.holder.addCallback(surfaceCallback)

    }

    fun registerObservers() {

        // observe forceUpdate
        coroutineScope.launch {
            forceUpdate.collect { zoneAffected ->
                Log.i(TAG + "Observer", "Force update zone $zoneAffected")

                if (zoneAffected != null) page.drawArea(
                    area = Rect(
                        zoneAffected.left,
                        zoneAffected.top - page.scroll,
                        zoneAffected.right,
                        zoneAffected.bottom - page.scroll
                    ),
                )
                refreshUi()
            }
        }

        // observe refreshUi
        coroutineScope.launch {
            refreshUi.collect {
                Log.i(TAG + "Observer", "Refreshing UI!")
                refreshUi()
            }
        }
        coroutineScope.launch {
            isDrawing.collect {
                Log.i(TAG + "Observer", "drawing state changed!")
                state.isDrawing = it
            }
        }


        coroutineScope.launch {
            addImageByUri.drop(1).collect { imageUri ->
                Log.i(TAG + "Observer", "Received image!")

                if (imageUri != null) {
                    handleImage(imageUri)
                } //else
//                    Log.i(TAG, "Image uri is empty")
            }
        }
        coroutineScope.launch {
            imageCoordinateToSelect.drop(1).collect { point ->
                SelectImage(point)
            }
        }


        // observe restartcount
        coroutineScope.launch {
            restartAfterConfChange.collect {
                Log.i(TAG + "Observer", "Configuration changed!")
                init()
                drawCanvasToView()
            }
        }

        // observe pen and stroke size
        coroutineScope.launch {
            snapshotFlow { state.pen }.drop(1).collect {
                Log.i(TAG + "Observer", "pen change: ${state.pen}")
                updatePenAndStroke()
                refreshUi()
            }
        }
        coroutineScope.launch {
            snapshotFlow { state.penSettings.toMap() }.drop(1).collect {
                Log.i(TAG + "Observer", "pen settings change: ${state.penSettings}")
                updatePenAndStroke()
                refreshUi()
            }
        }
        coroutineScope.launch {
            snapshotFlow { state.eraser }.drop(1).collect {
                Log.i(TAG + "Observer", "eraser change: ${state.eraser}")
                updatePenAndStroke()
                refreshUi()
            }
        }

        // observe is drawing
        coroutineScope.launch {
            snapshotFlow { state.isDrawing }.drop(1).collect {
                Log.i(TAG + "Observer", "isDrawing change: ${state.isDrawing}")
                updateIsDrawing()
            }
        }

        // observe toolbar open
        coroutineScope.launch {
            snapshotFlow { state.isToolbarOpen }.drop(1).collect {
                Log.i(TAG + "Observer", "istoolbaropen change: ${state.isToolbarOpen}")
                updateActiveSurface()
            }
        }

        // observe mode
        coroutineScope.launch {
            snapshotFlow { getActualState().mode }.drop(1).collect {
                Log.i(TAG + "Observer", "mode change: ${getActualState().mode}")
                updatePenAndStroke()
                refreshUi()
            }
        }

        coroutineScope.launch {
            //After 500ms add to history strokes
            commitHistorySignal.debounce(500).collect {
                Log.i(TAG + "Observer", "Commiting to history")
                commitToHistory()
            }
        }
        coroutineScope.launch {
            commitHistorySignalImmediately.collect() {
                commitToHistory()
                commitCompletion.complete(Unit)
            }
        }

    }

    private suspend fun SelectImage(point: Pair<Int, Int>?) {
        if (point != null) {
            Log.i(TAG + "Observer", "position of image $point")

            // Query the database to find an image that coincides with the point
            val imageToSelect = withContext(Dispatchers.IO) {
                ImageRepository(context).getImageAtPoint(
                    point.first,
                    point.second + page.scroll,
                    page.id
                )
            }
            imageCoordinateToSelect.value = null
            if (imageToSelect != null) {
                selectImage(coroutineScope, page, state, imageToSelect)
                SnackState.globalSnackFlow.emit(
                    SnackConf(
                        text = "Image selected!",
                        duration = 3000,
                    )
                )
            } else {
                SnackState.globalSnackFlow.emit(
                    SnackConf(
                        text = "There is no image at this position",
                        duration = 3000,
                    )
                )
            }
        }
    }

    private fun commitToHistory() {
        if (strokeHistoryBatch.size > 0) history.addOperationsToHistory(
            operations = listOf(
                Operation.DeleteStroke(strokeHistoryBatch.map { it })
            )
        )
        strokeHistoryBatch.clear()
        //testing if it will help with undo hiding strokes.
        drawCanvasToView()
    }

    fun refreshUi() {
        Log.i(TAG, "Refreshing ui. isDrawing : ${state.isDrawing}")
        drawCanvasToView()

        if (state.isDrawing) {
            // reset screen freeze
            touchHelper.setRawDrawingEnabled(false)
            touchHelper.setRawDrawingEnabled(true) // screen won't freeze until you actually stoke
        }
    }

    private fun handleImage(imageUri: Uri) {
        // Convert the image to a software-backed bitmap
        val imageBitmap = uriToBitmap(context, imageUri)?.asImageBitmap()

        val softwareBitmap =
            imageBitmap?.asAndroidBitmap()?.copy(Bitmap.Config.ARGB_8888, true)
        if (softwareBitmap != null) {
            addImageByUri.value = null

            // Get the image dimensions
            val imageWidth = softwareBitmap.width
            val imageHeight = softwareBitmap.height

            // Calculate the center position for the image relative to the page dimensions
            val centerX = (page.viewWidth - imageWidth) / 2
            val centerY = (page.viewHeight - imageHeight) / 2+ page.scroll
            val imageToSave = Image(
                x = centerX,
                y = centerY,
                height = imageHeight,
                width = imageWidth,
                uri = imageUri.toString(),
                pageId = page.id
            )
            // Add the image to the page
            page.addImage(imageToSave)
            drawImage(
                context, page.windowedCanvas, imageToSave, IntOffset(0, -page.scroll)
            )
            selectImage(coroutineScope, page, state, imageToSave)
        } else {
            // Handle cases where the bitmap could not be created
            Log.e("ImageProcessing", "Failed to create software bitmap from URI.")
        }
    }


    fun drawCanvasToView() {
        val canvas = this.holder.lockCanvas() ?: return
        canvas.drawBitmap(page.windowedBitmap, 0f, 0f, Paint())
        val timeToDraw = measureTimeMillis {
            if (getActualState().mode == Mode.Select) {
                // render selection
                if (getActualState().selectionState.firstPageCut != null) {
                    Log.i(TAG, "render cut")
                    val path = pointsToPath(getActualState().selectionState.firstPageCut!!.map {
                        SimplePointF(
                            it.x, it.y - page.scroll
                        )
                    })
                    canvas.drawPath(path, selectPaint)
                }
            }
        }
        Log.i(TAG, "drawCanvasToView: Took ${timeToDraw}ms.")
        // finish rendering
        this.holder.unlockCanvasAndPost(canvas)
    }

    private fun updateIsDrawing() {
        Log.i(TAG, "Update is drawing : ${state.isDrawing}")
        if (state.isDrawing) {
            touchHelper.setRawDrawingEnabled(true)
        } else {
            drawCanvasToView()
            touchHelper.setRawDrawingEnabled(false)
        }
    }

    fun updatePenAndStroke() {
        Log.i(TAG, "Update pen and stroke")
        when (state.mode) {
            Mode.Draw -> touchHelper.setStrokeStyle(penToStroke(state.pen))
                ?.setStrokeWidth(state.penSettings[state.pen.penName]!!.strokeSize)
                ?.setStrokeColor(state.penSettings[state.pen.penName]!!.color)

            Mode.Erase -> {
                when (state.eraser) {
                    Eraser.PEN -> touchHelper.setStrokeStyle(penToStroke(Pen.MARKER))
                        ?.setStrokeWidth(30f)
                        ?.setStrokeColor(Color.GRAY)

                    Eraser.SELECT -> touchHelper.setStrokeStyle(penToStroke(Pen.BALLPEN))
                        ?.setStrokeWidth(3f)
                        ?.setStrokeColor(Color.GRAY)
                }
            }

            Mode.Select -> touchHelper.setStrokeStyle(penToStroke(Pen.BALLPEN))?.setStrokeWidth(3f)
                ?.setStrokeColor(Color.GRAY)

            Mode.Line -> {
            }
        }
    }

    fun updateActiveSurface() {
        Log.i(TAG, "Update editable surface")

        val exclusionHeight =
            if (state.isToolbarOpen) convertDpToPixel(40.dp, context).toInt() else 0

        touchHelper.setRawDrawingEnabled(false)
        touchHelper.closeRawDrawing()

        touchHelper.setLimitRect(
            mutableListOf(
                Rect(
                    0, 0, this.width, this.height
                )
            )
        ).setExcludeRect(listOf(Rect(0, 0, this.width, exclusionHeight)))
            .openRawDrawing()

        touchHelper.setRawDrawingEnabled(true)
        updatePenAndStroke()

        refreshUi()
    }

}