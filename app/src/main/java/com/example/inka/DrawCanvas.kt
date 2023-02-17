package com.example.inka

import android.content.Context
import android.graphics.*
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.ui.platform.LocalContext
import com.example.inka.db.*
import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.api.device.epd.UpdateMode
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.*
import com.onyx.android.sdk.pen.data.TouchPointList
import com.onyx.android.sdk.pen.style.StrokeStyle
import java.io.File
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

fun drawBallPenStroke(
    canvas: Canvas, paint: Paint, strokeSize: Float, points: List<TouchPoint>
) {
    val copyPaint = Paint(paint).apply {
        this.strokeWidth = strokeSize
        this.style = Paint.Style.STROKE
        this.isAntiAlias = true
    }

    val path = Path()
    val prePoint = PointF(points[0].x, points[0].y)
    path.moveTo(prePoint.x, prePoint.y)

    for (point in points) {
        // skip strange jump point.
        if (abs(prePoint.y - point.y) >= 30) continue
        path.quadTo(prePoint.x, prePoint.y, point.x, point.y)
        prePoint.x = point.x
        prePoint.y = point.y
    }

    canvas.drawPath(path, copyPaint)
}

fun drawStroke(canvas: Canvas, pen: Pen, strokeSize: Float, points: List<TouchPoint>) {
    val paint = Paint()
    paint.color = Color.BLACK

    when (pen) {
        Pen.BALLPEN -> drawBallPenStroke(canvas, paint, strokeSize, points)
        Pen.PENCIL -> NeoCharcoalPen.drawNormalStroke(
            null, canvas, paint, points, -16777216, strokeSize, pressure, 90, false
        )
        Pen.BRUSH -> NeoBrushPen.drawStroke(canvas, paint, points, strokeSize, pressure, false)
        Pen.MARKER -> NeoMarkerPen.drawStroke(canvas, paint, points, strokeSize, false)
        Pen.FOUNTAIN -> NeoFountainPen.drawStroke(
            canvas, paint, points, 1f, strokeSize, pressure, false
        )
    }
}


class DrawCanvas(
    context: Context,
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

    var strokeCache = listOf<StrokeWithPoints>()




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

                drawStroke(
                    offScreenCanvas, stroke.pen, stroke.size, plist.points
                )

                // TODO clean undo ?
                AppRepository.clearUndo()

                thread(true) {
                    pageBitmapToFile(context, offScreenBitmap, getActualPageId())
                }

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

    fun init(pageId: Int, scroll:Int) {
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
                    ), mutableListOf(android.graphics.Rect(0, 0, surfaceView.width, 40))
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
                thread(true) {
                    strokeCache = appRepository.pageRepository.getWithStrokeById(pageId).strokes
                }
                drawDottedBg(offScreenCanvas, scroll)
                if(!renderCachedPageToCanvas(context, offScreenCanvas,pageId)) {
                    drawPage(context, offScreenCanvas,0,pageId, scroll, offScreenCanvas.height, null)
                }

                refreshUi()
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
        updateIsDrawing(false)
        drawCanvasToView()
        updateIsDrawing(true)
    }

    fun pageFullRefresh() {
        thread(start = true) {
            clearPage(offScreenCanvas)
            refreshUi()
            drawPage(context, offScreenCanvas, 0, pageId, scroll, offScreenCanvas.height, strokeCache)
            refreshUi()

            // cache bitmap to file
            pageBitmapToFile(context, offScreenBitmap, getActualPageId())
       }
    }

    fun drawCanvasToView() {
        val canvas = this.holder.lockCanvas() ?: return
        canvas.drawBitmap(offScreenBitmap, 0f, 0f, Paint());
        this.holder.unlockCanvasAndPost(canvas)
    }

    fun updateIsDrawing(isDrawingBool: Boolean) {
        isDrawing = isDrawingBool
        touchHelper.setRawDrawingEnabled(isDrawingBool)
    }

    fun updatePageId(pageId: Int, scroll: Int) {
        this.pageId = pageId
        this.scroll = scroll

        clearPage(offScreenCanvas)
        // try to render cache or render from db
        if(!renderCachedPageToCanvas(context, offScreenCanvas,pageId)) {
            drawPage(context, offScreenCanvas,0,pageId, scroll, offScreenCanvas.height, null)
        }

        refreshUi()

        thread(true) {
            strokeCache = appRepository.pageRepository.getWithStrokeById(pageId).strokes
        }

    }

    fun updateScroll(scroll: Int) {
        val delta = scroll - this.scroll

        val tmp = offScreenBitmap.copy(offScreenBitmap.config, false)
        drawDottedBg(offScreenCanvas, scroll)
        offScreenCanvas.drawBitmap(tmp, 0f, -delta.toFloat(), Paint())
        tmp.recycle()

        val canvasOffset = if(delta > 0) offScreenCanvas.height-delta else 0
        //drawCanvasToView()

        drawPage(context, offScreenCanvas, canvasOffset, pageId, scroll+canvasOffset, Math.abs(delta), strokeCache)
        drawCanvasToView()

        this.scroll = scroll

        // cache bitmap to file
        pageBitmapToFile(context, offScreenBitmap, getActualPageId())

        refreshUi()
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
}