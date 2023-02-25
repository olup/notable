package com.example.inka

import android.content.Context
import android.graphics.*
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.dp
import com.example.inka.db.Stroke
import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.api.device.epd.UpdateMode
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.*
import com.onyx.android.sdk.pen.data.TouchPointList
import com.onyx.android.sdk.pen.style.StrokeStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.concurrent.thread
import kotlin.io.path.Path
import kotlin.system.measureTimeMillis

val pressure = EpdController.getMaxTouchPressure()

// keep reference of the surface view presently associated to the singleton touchhelper
var referencedSurfaceView: String = ""

class PageModel(context: Context, coroutineScope: CoroutineScope, pageId: String, windowWidth: Int, windowsHeight: Int) {
    val context = context
    val coroutineScope = coroutineScope
    val pageId = pageId
    val windowedBitmap = Bitmap.createBitmap(windowWidth, windowsHeight, Bitmap.Config.ARGB_8888)
    val windowedCanvas = Canvas(windowedBitmap)
    private var strokes = listOf<Stroke>()
    var scroll = 0

    init {
        loadBitmap()
        initFromPersistLayer()
    }

    private fun initFromPersistLayer() {
        // pageInfos
        val page = AppRepository(context).pageRepository.getById(pageId)
        scroll = page.scroll

        // Strokes TODO in corountine
        val pageWithStrokes = AppRepository(context).pageRepository.getWithStrokeById(pageId)
        strokes = pageWithStrokes.strokes
    }

    fun addStroke(stroke: Stroke) {
        strokes += stroke
        saveStrokeToPersistLayer(stroke)

        // TODO in coroutine, and debounced
        persistBitmap()
        persistBitmapThumbnail()
    }

    fun removeStroke(strokeId: String) {
        strokes = strokes.filter { s -> s.id != strokeId }
        removeStrokeFromPersistLayer(strokeId)

        // TODO in coroutine, and debounced
        persistBitmap()
        persistBitmapThumbnail()
    }
    private fun saveStrokeToPersistLayer(stroke: Stroke) {
        AppRepository(context).strokeRepository.create(stroke)
    }
   private fun removeStrokeFromPersistLayer(strokeId: String) {
        AppRepository(context).strokeRepository.deleteAll(listOf(strokeId))
    }
    private fun loadBitmap(): Boolean {
        val imgFile = File(context.filesDir, "pages/previews/full/$pageId")
        var imgBitmap: Bitmap? = null
        if (imgFile.exists()) {
            imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            if (imgBitmap != null) {
                windowedCanvas.drawBitmap(imgBitmap, 0f, 0f, Paint());
                println("Page rendered from cache")
                return true
            } else {
                println("Cannot read cache image")
            }
        } else {
            println("Cannot find cache image")
        }
        return false
    }

    private fun persistBitmap() {
        val file = File(context.filesDir, "pages/previews/full/$pageId")
        Files.createDirectories(Path(file.absolutePath).parent)
        val os = BufferedOutputStream(FileOutputStream(file))
        windowedBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 50, os);
        os.close()
    }

    private fun persistBitmapThumbnail() {
        val file = File(context.filesDir, "pages/previews/thumbs/$pageId")
        Files.createDirectories(Path(file.absolutePath).parent)
        val os = BufferedOutputStream(FileOutputStream(file))
        val ratio = windowedBitmap.height.toFloat() / windowedBitmap.width.toFloat()
        Bitmap.createScaledBitmap(windowedBitmap, 500, (500 * ratio).toInt(), false)
            .compress(Bitmap.CompressFormat.WEBP_LOSSY, 50, os);
        os.close()
    }

    private fun drawArea(canvasArea: Rect) {
        val pageArea = Rect(
            canvasArea.left,
            canvasArea.top + scroll,
            canvasArea.right,
            canvasArea.bottom + scroll
        )

        windowedCanvas.save();
        windowedCanvas.clipRect(canvasArea);

        val timeToBg = measureTimeMillis {
            drawDottedBg(windowedCanvas, (scroll).toInt())
        }
        println("Took $timeToBg to draw the BG")

        val timeToDraw = measureTimeMillis {
            strokes.forEach { stroke ->

                val bounds = Rect(
                    stroke.left.toInt(),
                    stroke.top.toInt(),
                    stroke.right.toInt(),
                    stroke.bottom.toInt()
                )

                // if stroke is inside page section
                if (bounds.intersect(pageArea)) {
                    println("Stroke identifed for rendering")

                    val points = stroke.points.map {
                        TouchPoint(
                            it.x - pageArea.left + canvasArea.left,
                            it.y - pageArea.top + canvasArea.top,
                            it.pressure,
                            stroke.size,
                            it.tiltX,
                            it.tiltY,
                            it.timestamp
                        )
                    }
                    drawStroke(
                        windowedCanvas, stroke.pen, stroke.size, points
                    )
                }
            }
        }
        println("Drew area in ${timeToDraw}ms")
    }

    fun updateScroll(updatedScroll: Int) {
        val delta = updatedScroll - scroll
        // scroll bitmap
        val tmp = windowedBitmap.copy(windowedBitmap.config, false)
        drawDottedBg(windowedCanvas, updatedScroll)

        windowedCanvas.drawBitmap(tmp, 0f, -delta.toFloat(), Paint())
        tmp.recycle()

        // where is the new rendering area starting ?
        val canvasOffset = if (delta > 0) windowedCanvas.height - delta else 0

        drawArea(
            canvasArea = Rect(
                0,
                canvasOffset,
                windowedCanvas.width,
                canvasOffset + Math.abs(delta)
            ),
        )

        // TODO in coroutine, and debounced
        persistBitmap()
        persistBitmapThumbnail()
    }

}


class DrawCanvas(
    context: Context,
    coroutineScope: CoroutineScope,
    appRepository: AppRepository,
    state: PageEditorState
) : SurfaceView(context) {
    val state = state
    var restartCount = 0
    var offScreenBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    var offScreenCanvas = Canvas(offScreenBitmap)
    val coroutineScope = coroutineScope

    fun getActualState(): PageEditorState {
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
            thread(true) {
                if (state.mode == Mode.ERASE) {
                    handleErase(
                        context,
                        offScreenCanvas,
                        getActualState().pageId,
                        getActualState().scroll,
                        plist.points.map { it.x to it.y })
                    drawCanvasToView()
                    refreshUi()
                    coroutineScope.launch { debouncedSavedBitmapFlow.emit(Unit) }
                }

                if (state.mode == Mode.DRAW) {

                    handleDraw(
                        context,
                        offScreenCanvas,
                        getActualState().pageId,
                        getActualState().scroll,
                        getActualState().strokeSize,
                        getActualState().pen,
                        plist.points
                    )
                    coroutineScope.launch { debouncedSavedBitmapFlow.emit(Unit) }
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

    private val touchHelper by lazy {
        referencedSurfaceView = this.hashCode().toString()
        TouchHelper.create(this, inputCallback)
    }

    fun clearPage(canvas: Canvas) {
        // white bg
        canvas.drawColor(android.graphics.Color.WHITE)
    }

    fun init() {
        println("Initializing")

        val surfaceView = this

        val surfaceCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                println("surface created ${holder}")
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
                println("surface changed ${holder}")
                renderPageFromCacheOdDb()
                updatePenAndStroke()
                refreshUi()
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                println(
                    "surface destroyed ${
                        this@DrawCanvas.hashCode().toString()
                    } - ref ${referencedSurfaceView}"
                )
                holder.removeCallback(this)
                if (referencedSurfaceView == this@DrawCanvas.hashCode().toString()) {
                    touchHelper.closeRawDrawing()
                }
            }
        }

        this.holder.addCallback(surfaceCallback)
        // EpdController.repaintEveryThing(UpdateMode.GC)

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
            snapshotFlow { state.forceUpdate }.drop(1).collect {
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
                        zoneAffected.left,
                        zoneAffected.top - state.scroll,
                        zoneAffected.right,
                        zoneAffected.bottom - state.scroll
                    ),
                )
                refreshUi()
                coroutineScope.launch { debouncedSavedBitmapFlow.emit(Unit) }
            }
        }

        // observe scroll
        coroutineScope.launch {
            snapshotFlow { state.scroll }.withPrevious().drop(1).collect {
                println("scroll change: ${state.scroll}")
                if (it.first != null) updateScroll(it.first!!, it.second)
            }
        }

        /*    // observe pageId
            coroutineScope.launch {
                snapshotFlow { state.pageId }.collect {
                    renderPageFromCacheOdDb()
                }
            }*/

        // observe paen and stroke size
        coroutineScope.launch {
            snapshotFlow { state.pen to state.strokeSize }.drop(1).collect {
                println("pen change: ${state.pen}")
                updatePenAndStroke()
                refreshUi()
            }
        }

        // observe is drawing
        coroutineScope.launch {
            snapshotFlow { state.isDrawing }.drop(1).collect {
                println("isDrawing change: ${state.isDrawing}")
                updateIsDrawing()
            }
        }

        // observe toolbar open
        coroutineScope.launch {
            snapshotFlow { state.isToolbarOpen }.drop(1).collect {
                println("istoolbaropen change: ${state.isToolbarOpen}")
                updateIsToolbarOpen()
            }
        }

        // observe mode
        coroutineScope.launch {
            snapshotFlow { state.mode }.drop(1).collect {
                println("mode change: ${state.mode}")
                refreshUi()
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