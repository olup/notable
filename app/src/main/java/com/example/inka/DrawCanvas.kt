package com.example.inka

import android.content.Context
import android.graphics.*
import android.view.SurfaceHolder
import android.view.SurfaceView
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
    pageId: Int
) : SurfaceView(context) {

    // State. This replicates the app's state
    var scroll = 0
    var strokeSize = 10f
    var pen = Pen.BALLPEN
    var restartCount = 0
    var pageId: Int = pageId
    var breakBarPosition = -1f
    var forceUpdate = 0
    var isToolbarOpen = true
    var isDrawing = true

    val appRepository = appRepository

    var offScreenBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    var offScreenCanvas = Canvas(offScreenBitmap)

    val coroutineScope = coroutineScope


    fun getActualPageId(): Int {
        return this.pageId
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
                    size = strokeSize, pen = pen, pageId = getActualPageId()
                )
                val strokeId = appRepository.strokeRepository.create(
                    stroke
                )
                val points = plist.points.map {
                    com.example.inka.db.Point(
                        x = it.x,
                        y = it.y + scroll,
                        pressure = it.pressure,
                        size = it.size,
                        tiltX = it.tiltX,
                        tiltY = it.tiltY,
                        timestamp = it.timestamp,
                        pageId = getActualPageId(),
                        strokeId = strokeId.toInt()
                    )
                }
                appRepository.pointRepository.create(points)

                // add stroke to cache. TODO refacto in central cache
                if (StrokeCache.pageId == getActualPageId()) {
                    StrokeCache.strokes += StrokeWithPoints(stroke = stroke, points = points)
                }

                drawStroke(
                    offScreenCanvas, stroke.pen, stroke.size, plist.points
                )

                // add cancel operation to the undo list
                addOperationsToHistory(listOf(Operation.DeleteStroke(strokeId.toInt())))

                // emit to the snapshot debounced flow
                coroutineScope.launch { debouncedSavedBitmapFlow.emit(Unit)}
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

    fun init(pageId: Int, scroll: Int) {
        println("Initializing")
        this.pageId = pageId
        this.scroll = scroll

        val surfaceView = this

        val surfaceCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                println("surface created")
                touchHelper.setLimitRect(
                    mutableListOf(
                        android.graphics.Rect(
                            0, 0, surfaceView.width, surfaceView.height
                        )
                    ),
                    mutableListOf(
                        android.graphics.Rect(
                            0,
                            0,
                            surfaceView.width,
                            convertDpToPixel(40.dp, context).toInt()
                        )
                    )
                ).openRawDrawing()

                // setting up offscreen canvas
                surfaceView.offScreenBitmap = Bitmap.createBitmap(
                    surfaceView.width,
                    surfaceView.height,
                    Bitmap.Config.ARGB_8888
                )
                surfaceView.offScreenCanvas = Canvas(surfaceView.offScreenBitmap)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder, format: Int, width: Int, height: Int
            ) {
                println("surface changed")
                updatePageId(getActualPageId(), scroll)
                updatePenAndStroke(pen, strokeSize)
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

    fun refreshUi() {
        if(isDrawing){
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
                pageId = pageId,
                pageSection = Rect(
                    0,
                    scroll,
                    offScreenCanvas.width,
                    scroll + offScreenCanvas.height
                ),
                canvasSection = Rect(0, 0, offScreenCanvas.width, offScreenCanvas.height),
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

    fun updateIsDrawing(isDrawing: Boolean) {
        this.isDrawing = isDrawing
        if (isDrawing) {
            touchHelper.setRawDrawingEnabled(true)
        } else {
            touchHelper.setRawDrawingEnabled(false)
        }
    }

    fun updatePageId(pageId: Int, scroll: Int) {
        if (pageId != this.pageId) {
            // save bitmmap to disk before changing page
            val targetPageId = this.pageId
            val targetBitmap = offScreenBitmap.copy(offScreenBitmap.config, false)
            coroutineScope.launch {
                pageBitmapToFile(
                    context,
                    targetBitmap,
                    targetPageId
                )
            }
        }
        this.pageId = pageId
        this.scroll = scroll

        println("Update page id")

        clearPage(offScreenCanvas)
        // try to render cache or render from db
        if (!renderCachedPageToCanvas(context, offScreenCanvas, pageId)) {
            renderPageFromDbToCanvas(
                context = context,
                canvas = offScreenCanvas,
                pageId = pageId,
                pageSection = Rect(
                    0,
                    scroll,
                    offScreenCanvas.width,
                    scroll + offScreenCanvas.height
                ),
                canvasSection = Rect(0, 0, offScreenCanvas.width, offScreenCanvas.height),
            )
            coroutineScope.launch { debouncedSavedBitmapFlow.emit(Unit) }
        }

        refreshUi()
    }

    fun updateScroll(scroll: Int) {
        // scroll delta
        val delta = scroll - this.scroll

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
            pageId = pageId,
            pageSection = Rect(
                0,
                scroll + canvasOffset,
                offScreenCanvas.width,
                scroll + canvasOffset + Math.abs(delta)
            ),
            canvasSection = Rect(
                0,
                canvasOffset,
                offScreenCanvas.width,
                canvasOffset + Math.abs(delta)
            ),
        )

        // display update
        refreshUi()

        this.scroll = scroll

        coroutineScope.launch { debouncedSavedBitmapFlow.emit(Unit) }
    }

    fun updateForceUpdate(forceUpdate: Int) {
        this.forceUpdate = forceUpdate
        pageFullRefresh()
    }

    fun updatePenAndStroke(pen: Pen, strokeSize: Float) {
        this.pen = pen
        this.strokeSize = strokeSize
        touchHelper.setStrokeStyle(penToStroke(pen))?.setStrokeWidth(strokeSize)
    }

    fun updateIsToolbarOpen(isToolbarOpen: Boolean) {
        this.isToolbarOpen = isToolbarOpen
        val exclusionWidth =
            if (isToolbarOpen) this.width else convertDpToPixel(40.dp, context).toInt()

      //  touchHelper.setRawDrawingEnabled(false)
       // touchHelper.closeRawDrawing()

        touchHelper.setLimitRect(
            mutableListOf(
                android.graphics.Rect(
                    0, 0, this.width, this.height
                )
            ),
            mutableListOf(
                android.graphics.Rect(
                    0,
                    0,
                    exclusionWidth,
                    convertDpToPixel(40.dp, context).toInt()
                )
            )
        )

        refreshUi()

    }
}