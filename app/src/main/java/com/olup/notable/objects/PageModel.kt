package com.olup.notable

import android.content.Context
import android.graphics.*
import androidx.compose.ui.unit.IntOffset
import androidx.core.graphics.toRect
import com.olup.notable.AppRepository
import com.olup.notable.db.AppDatabase
import com.olup.notable.db.Stroke
import com.olup.notable.drawDottedBg
import com.olup.notable.drawStroke
import com.olup.notable.strokeBounds
import com.onyx.android.sdk.api.device.epd.EpdController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.math.max
import kotlin.system.measureTimeMillis

class PageModel(
    val context: Context,
    val coroutineScope: CoroutineScope,
    val pageId: String,
) {

    val windowedBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
    val windowedCanvas = Canvas(windowedBitmap)
    var strokes = listOf<Stroke>()
    var strokesById: HashMap<String, Stroke> = hashMapOf()
    var scroll = 0
    val saveTopic = MutableSharedFlow<Unit>()
    val pageWidth = EpdController.getEpdHeight()
    var pageHeight = EpdController.getEpdWidth()

    var db = AppDatabase.getDatabase(context)?.strokeDao()!!

    init {
        coroutineScope.launch {
            saveTopic.debounce(1000).collect {
                launch { persistBitmap() }
                launch { persistBitmapThumbnail() }
            }
        }

        val isCacehd = loadBitmap()
        initFromPersistLayer(isCacehd)
    }

    fun indexStrokes() {
        coroutineScope.launch {
            strokesById = hashMapOf(*strokes.map { s -> s.id to s }.toTypedArray())
        }
    }

    private fun initFromPersistLayer(isCached: Boolean) {
        // pageInfos
        // TODO page might not exists yet
        val page = AppRepository(context).pageRepository.getById(pageId)
        scroll = page!!.scroll

        if (!isCached) {
            // if not cached we work synchronously
            val pageWithStrokes = AppRepository(context).pageRepository.getWithStrokeById(pageId)
            strokes = pageWithStrokes.strokes
            indexStrokes()

            drawDottedBg(windowedCanvas, scroll)
            drawArea(Rect(0, 0, windowedCanvas.width, windowedCanvas.height))
            persistBitmap()
            persistBitmapThumbnail()
        }

        // otherwise we can fetch this in the backgrond
        coroutineScope.launch {
            val pageWithStrokes = AppRepository(context).pageRepository.getWithStrokeById(pageId)
            strokes = pageWithStrokes.strokes
            indexStrokes()
        }
    }

    fun addStrokes(strokesToAdd: List<Stroke>) {
        strokes += strokesToAdd
        strokesToAdd.forEach {
            val bottomPlusPadding = it.bottom + 50
            if (bottomPlusPadding > pageHeight) pageHeight = bottomPlusPadding.toInt()
        }

        saveStrokesToPersistLayer(strokesToAdd)
        indexStrokes()

        persistBitmapDebounced()
    }

    fun removeStrokes(strokeIds: List<String>) {
        strokes = strokes.filter { s -> !strokeIds.contains(s.id) }
        removeStrokesFromPersistLayer(strokeIds)
        indexStrokes()
        computeHeight()

        persistBitmapDebounced()
    }

    fun getStrokes(strokeIds: List<String>): List<Stroke?> {
        return strokeIds.map { s -> strokesById[s] }
    }

    private fun saveStrokesToPersistLayer(strokes: List<Stroke>) {
        db.create(strokes)
    }

    fun computeHeight() {
        if (strokes.isEmpty()) {
            pageHeight = viewHeight
            return
        }
        val maxStrokeBottom = strokes.maxOf { it.bottom }.plus(50) ?: 0
        pageHeight = max(maxStrokeBottom.toInt(), viewHeight)
    }

    private fun removeStrokesFromPersistLayer(strokeIds: List<String>) {
        AppRepository(context).strokeRepository.deleteAll(strokeIds)
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

    fun drawArea(canvasArea: Rect, ignoredStrokeIds: List<String> = listOf(), canvas : Canvas? = null) {
        val activeCanvas = canvas ?: windowedCanvas
        val pageArea = Rect(
            canvasArea.left,
            canvasArea.top + scroll,
            canvasArea.right,
            canvasArea.bottom + scroll
        )

        activeCanvas.save();
        activeCanvas.clipRect(canvasArea);
        activeCanvas.drawColor(Color.BLACK)

        val timeToBg = measureTimeMillis {
            drawDottedBg(activeCanvas, scroll)
        }
        println("Took $timeToBg to draw the BG")

        val timeToDraw = measureTimeMillis {
            strokes.forEach { stroke ->
                if (ignoredStrokeIds.contains(stroke.id)) return@forEach
                val bounds = strokeBounds(stroke)
                // if stroke is not inside page section
                if (!bounds.toRect().intersect(pageArea)) return@forEach

                drawStroke(
                    activeCanvas, stroke, IntOffset(0, -scroll)
                )

            }
        }
        println("Drew area in ${timeToDraw}ms")
        activeCanvas.restore();
    }

    fun updateScroll(_delta: Int) {
        var delta = _delta
        if (scroll + delta < 0) delta = 0 - scroll

        scroll += delta

        // scroll bitmap
        val tmp = windowedBitmap.copy(windowedBitmap.config, false)
        drawDottedBg(windowedCanvas, scroll)

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

        persistBitmapDebounced()
        saveToPersistLayer()
    }

    private fun persistBitmapDebounced() {
        coroutineScope.launch {
            saveTopic.emit(Unit)
        }
    }

    private fun saveToPersistLayer() {
        coroutineScope.launch {
            AppRepository(context).pageRepository.updateScroll(pageId, scroll)
        }
    }
}

