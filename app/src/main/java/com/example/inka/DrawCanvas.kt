package com.example.inka

import android.content.Context
import android.graphics.*
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.dp
import com.example.inka.db.Stroke
import com.example.inka.db.StrokeWithPoints
import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.api.device.epd.UpdateMode
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.*
import com.onyx.android.sdk.pen.data.TouchPointList
import com.onyx.android.sdk.pen.style.StrokeStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.concurrent.thread
import kotlin.math.abs

val pressure = EpdController.getMaxTouchPressure()

fun penToStroke(pen: Pen): Int {
    return when (pen) {
        Pen.BALLPEN -> StrokeStyle.PENCIL
        Pen.PENCIL -> StrokeStyle.CHARCOAL
        Pen.BRUSH -> StrokeStyle.NEO_BRUSH
        Pen.MARKER -> StrokeStyle.MARKER
        Pen.FOUNTAIN -> StrokeStyle.BRUSH
    }
}


class DrawCanvas(
    context: Context,
    coroutineScope: CoroutineScope,
    appRepository: AppRepository,
    state: EditorState
) : SurfaceView(context) {

    // State. This replicates the app's state
    val state = state
    var restartCount = 0

    var offScreenBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    var offScreenCanvas = Canvas(offScreenBitmap)

    val coroutineScope = coroutineScope


    fun getActualState(): EditorState {
        return this.state
    }

    private val inputCallback: RawInputCallback = object : RawInputCallback() {
        override fun onBeginRawDrawing(p0: Boolean, p1: TouchPoint?) {
        }

        override fun onEndRawDrawing(p0: Boolean, p1: TouchPoint?) {
            // page.saveToDisk(context) // TODO
        }

        override fun onRawDrawingTouchPointMoveReceived(p0: TouchPoint?) {
        }

        override fun onRawDrawingTouchPointListReceived(plist: TouchPointList) {
            // TODO refactor to use single addStrokeToPage function - requires passing a point type without strokeId or pageId
            thread(start = true) {
                val stroke = Stroke(
                    size = getActualState().strokeSize,
                    pen = getActualState().pen,
                    pageId = getActualState().pageId,
                    top = plist.points[0].y + state.scroll,
                    bottom = plist.points[0].y + state.scroll,
                    left = 0f,
                    right = 0f,
                )
                val boundingBox = RectF(plist.points[0].x, plist.points[0].y, plist.points[0].x, plist.points[0].y)
                val strokeId = appRepository.strokeRepository.create(
                    stroke
                )

                val points = plist.points.map {
                    boundingBox.union(it.x, it.y)

                    com.example.inka.db.Point(
                        x = it.x,
                        y = it.y + state.scroll,
                        pressure = it.pressure,
                        size = it.size,
                        tiltX = it.tiltX,
                        tiltY = it.tiltY,
                        timestamp = it.timestamp,
                        pageId = getActualState().pageId,
                        strokeId = strokeId.toInt()
                    )
                }
                appRepository.pointRepository.create(points)

                // update stroke to include its bounding box
                stroke.apply {
                    boundingBox.inset(-this.size, -this.size)

                    id = strokeId.toInt()
                    top = boundingBox.top
                    left = boundingBox.left
                    bottom = boundingBox.bottom
                    right = boundingBox.right
                }
                println(stroke)
                appRepository.strokeRepository.update(stroke)

                // add stroke to cache. TODO refacto in central cache
                if (StrokeCache.pageId == getActualState().pageId) {
                    StrokeCache.strokes += StrokeWithPoints(stroke = stroke, points = points)
                }

                drawStroke(
                    offScreenCanvas, stroke.pen, stroke.size, plist.points
                )

                // add cancel operation to the undo list
                addOperationsToHistory(listOf(Operation.DeleteStroke(strokeId.toInt())))

                // emit to the snapshot debounced flow
                coroutineScope.launch { debouncedSavedBitmapFlow.emit(Unit) }
            }
        }

        override fun onBeginRawErasing(p0: Boolean, p1: TouchPoint?) {
        }

        override fun onEndRawErasing(p0: Boolean, p1: TouchPoint?) {
        }

        override fun onRawErasingTouchPointListReceived(p0: TouchPointList?) {
        }

        override fun onRawErasingTouchPointMoveReceived(p0: TouchPoint?) {
        }
    }

    private val touchHelper by lazy { TouchHelper.create(this, inputCallback) }

    fun clearPage(canvas: Canvas) {
        // white bg
        canvas.drawColor(android.graphics.Color.WHITE)
    }

    fun init() {
        println("Initializing")

        val surfaceView = this

        val surfaceCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                println("surface created")
                touchHelper.setLimitRect(
                    mutableListOf(
                        android.graphics.Rect(
                            0, 0, surfaceView.width, surfaceView.height
                        )
                    ), mutableListOf(
                        android.graphics.Rect(
                            0, 0, surfaceView.width, convertDpToPixel(40.dp, context).toInt()
                        )
                    )
                ).openRawDrawing()

                // setting up offscreen canvas
                surfaceView.offScreenBitmap = Bitmap.createBitmap(
                    surfaceView.width, surfaceView.height, Bitmap.Config.ARGB_8888
                )
                surfaceView.offScreenCanvas = Canvas(surfaceView.offScreenBitmap)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder, format: Int, width: Int, height: Int
            ) {
                println("surface changed")
                renderPageFromCacheOdDb()
                updatePenAndStroke()
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                println("surface destroyed")
                holder.removeCallback(this)
                touchHelper.closeRawDrawing()
            }
        }

        this.holder.addCallback(surfaceCallback)
        EpdController.repaintEveryThing(UpdateMode.GC)

    }

    fun registerObservers() {
        // observe cache save
        coroutineScope.launch {
            // debaounce
            debouncedSavedBitmapFlow.debounce(1000).collect {
                println("Saving page to disk")
                thread(true) {
                    pageBitmapToFile(
                        context, offScreenBitmap, getActualState().pageId
                    )
                }
            }
        }

        // observe forceUpdate
        coroutineScope.launch {
            snapshotFlow { state.forceUpdate }.collect {
                val zoneAffected = it.second
                println("Force update zone ${zoneAffected}")

                renderPageFromDbToCanvas(
                    context = context,
                    canvas = offScreenCanvas,
                    pageId = state.pageId,
                    pageSection = RectF(
                        zoneAffected.left, zoneAffected.top, zoneAffected.right, zoneAffected.bottom
                    ),
                    canvasSection = RectF(
                        zoneAffected.left, zoneAffected.top - state.scroll, zoneAffected.right, zoneAffected.bottom - state.scroll
                    ),
                )
                refreshUi()
                coroutineScope.launch { debouncedSavedBitmapFlow.emit(Unit) }
            }
        }

        // observe scroll
        coroutineScope.launch {
            snapshotFlow { state.scroll }.withPrevious().collect {
                if (it.first != null) updateScroll(it.first!!, it.second)
            }
        }

        // observe pageId
        coroutineScope.launch {
            snapshotFlow { state.pageId }.collect {
                renderPageFromCacheOdDb()
            }
        }

        // observe paen and stroke size
        coroutineScope.launch {
            snapshotFlow { state.pen to state.strokeSize }.collect {
                updatePenAndStroke()
            }
        }

        // observe is drawing
        coroutineScope.launch {
            snapshotFlow { state.isDrawing }.collect {
                updateIsDrawing()
            }
        }

        // observe toolbar open
        coroutineScope.launch {
            snapshotFlow { state.isToolbarOpen }.collect {
                updateIsToolbarOpen()
            }
        }
    }

    fun refreshUi() {
        if (state.isDrawing) {
            touchHelper.setRawDrawingEnabled(false)
            drawCanvasToView()
            touchHelper.setRawDrawingEnabled(true)
        }
    }

    fun pageFullRefresh() {
        thread(start = true) {
            clearPage(offScreenCanvas)
            renderPageFromDbToCanvas(
                context = context,
                canvas = offScreenCanvas,
                pageId = state.pageId,
                pageSection = RectF(
                    0f,
                    state.scroll.toFloat(),
                    offScreenCanvas.width.toFloat(),
                    state.scroll + offScreenCanvas.height.toFloat()
                ),
                canvasSection = RectF(
                    0f, 0f, offScreenCanvas.width.toFloat(), offScreenCanvas.height.toFloat()
                ),
            )
            refreshUi()
            coroutineScope.launch { debouncedSavedBitmapFlow.emit(Unit) }
        }
    }

    fun drawCanvasToView() {
        val canvas = this.holder.lockCanvas() ?: return
        canvas.drawBitmap(offScreenBitmap, 0f, 0f, Paint());
        this.holder.unlockCanvasAndPost(canvas)
    }

    fun updateIsDrawing() {
        if (state.isDrawing) {
            touchHelper.setRawDrawingEnabled(true)
        } else {
            touchHelper.setRawDrawingEnabled(false)
        }
    }

    fun renderPageFromCacheOdDb() {
        println("Update page id")
        clearPage(offScreenCanvas)
        // try to render cache or render from db
        if (!renderCachedPageToCanvas(context, offScreenCanvas, state.pageId)) {
            renderPageFromDbToCanvas(
                context = context,
                canvas = offScreenCanvas,
                pageId = state.pageId,
                pageSection = RectF(
                    0f,
                    state.scroll.toFloat(),
                    offScreenCanvas.width.toFloat(),
                    state.scroll + offScreenCanvas.height.toFloat()
                ),
                canvasSection = RectF(
                    0f, 0f, offScreenCanvas.width.toFloat(), offScreenCanvas.height.toFloat()
                ),
            )
            coroutineScope.launch { debouncedSavedBitmapFlow.emit(Unit) }
        }

        refreshUi()
    }

    fun updateScroll(previous: Int, scroll: Int) {
        println("scroll update")
        val delta = scroll - previous
        // scroll bitmap
        val tmp = offScreenBitmap.copy(offScreenBitmap.config, false)
        drawDottedBg(offScreenCanvas, scroll)
        offScreenCanvas.drawBitmap(tmp, 0f, -delta.toFloat(), Paint())
        tmp.recycle()

        // where is the new rendering area starting ?
        val canvasOffset = if (delta > 0) offScreenCanvas.height - delta else 0

        // do we want to show progressive updates ?
        //refreshUi()

        renderPageFromDbToCanvas(
            context = context,
            canvas = offScreenCanvas,
            pageId = state.pageId,
            pageSection = RectF(
                0f,
                scroll + canvasOffset.toFloat(),
                offScreenCanvas.width.toFloat(),
                scroll + canvasOffset + Math.abs(delta).toFloat()
            ),
            canvasSection = RectF(
                0f,
                canvasOffset.toFloat(),
                offScreenCanvas.width.toFloat(),
                canvasOffset + Math.abs(delta).toFloat()
            ),
        )

        // display update
        refreshUi()

        coroutineScope.launch { debouncedSavedBitmapFlow.emit(Unit) }
    }

    fun updatePenAndStroke() {
        touchHelper.setStrokeStyle(penToStroke(state.pen))?.setStrokeWidth(state.strokeSize)
    }

    fun updateIsToolbarOpen() {
        val exclusionWidth =
            if (state.isToolbarOpen) this.width else convertDpToPixel(40.dp, context).toInt()
        //  touchHelper.setRawDrawingEnabled(false)
        // touchHelper.closeRawDrawing()
        touchHelper.setLimitRect(
            mutableListOf(
                android.graphics.Rect(
                    0, 0, this.width, this.height
                )
            ), mutableListOf(
                android.graphics.Rect(
                    0, 0, exclusionWidth, convertDpToPixel(40.dp, context).toInt()
                )
            )
        )
        refreshUi()
    }
}