package com.olup.notable

import android.content.Context
import android.graphics.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import androidx.core.graphics.toRect
import com.olup.notable.db.AppDatabase
import com.olup.notable.db.Page
import com.olup.notable.db.Stroke
import com.olup.notable.db.Image
import io.shipbook.shipbooksdk.Log
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


import android.graphics.Bitmap
import android.graphics.BitmapFactory


class PageView(
    val context: Context,
    val coroutineScope: CoroutineScope,
    val id: String,
    val width: Int,
    var viewWidth: Int,
    var viewHeight: Int
) {

    var windowedBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
    var windowedCanvas = Canvas(windowedBitmap)
    var strokes = listOf<Stroke>()
    var strokesById: HashMap<String, Stroke> = hashMapOf()
    var images = listOf<Image>()
    var imagesById: HashMap<String, Image> = hashMapOf()
    var scroll by mutableStateOf(0) // is observed by ui
    val saveTopic = MutableSharedFlow<Unit>()

    var height by mutableStateOf(viewHeight) // is observed by ui

    var pageFromDb = AppRepository(context).pageRepository.getById(id)

    var dbStrokes = AppDatabase.getDatabase(context)?.strokeDao()!!
    var dbImages = AppDatabase.getDatabase(context)?.ImageDao()!!


    init {
        coroutineScope.launch {
            saveTopic.debounce(1000).collect {
                launch { persistBitmap() }
                launch { persistBitmapThumbnail() }
            }
        }

        windowedCanvas.drawColor(Color.WHITE)
        drawBg(windowedCanvas, pageFromDb?.nativeTemplate!!, scroll)

        val isCached = loadBitmap()
        initFromPersistLayer(isCached)
    }

    fun indexStrokes() {
        coroutineScope.launch {
            strokesById = hashMapOf(*strokes.map { s -> s.id to s }.toTypedArray())
        }
    }

    fun indexImages() {
        coroutineScope.launch {
            imagesById = hashMapOf(*images.map { img -> img.id to img }.toTypedArray())
        }
    }

    private fun initFromPersistLayer(isCached: Boolean) {
        // pageInfos
        // TODO page might not exists yet
        val page = AppRepository(context).pageRepository.getById(id)
        scroll = page!!.scroll

        coroutineScope.launch {
            val timeToLoad = measureTimeMillis {
                val pageWithStrokes = AppRepository(context).pageRepository.getWithStrokeById(id)
                val pageWithImages = AppRepository(context).pageRepository.getWithImageById(id)
                strokes = pageWithStrokes.strokes
                images = pageWithImages.images
                indexStrokes()
                indexImages()
                computeHeight()

                if (!isCached) {
                    // we draw and cache
                    drawBg(windowedCanvas, page.nativeTemplate, scroll)
                    drawArea(Rect(0, 0, windowedCanvas.width, windowedCanvas.height))
                    persistBitmap()
                    persistBitmapThumbnail()
                }
            }
            Log.i(TAG, "initializing from persistent layer took ${timeToLoad}ms")
        }

        //TODO: Images loading
    }

    fun addStrokes(strokesToAdd: List<Stroke>) {
        strokes += strokesToAdd
        strokesToAdd.forEach {
            val bottomPlusPadding = it.bottom + 50
            if (bottomPlusPadding > height) height = bottomPlusPadding.toInt()
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
        dbStrokes.create(strokes)
    }

    private fun saveImagesToPersistLayer(image: List<Image>) {
        dbImages.create(image)
    }


    fun addImage(imageToAdd: Image) {
        images += listOf(imageToAdd)
        val bottomPlusPadding = imageToAdd.x + imageToAdd.height + 50
        if (bottomPlusPadding > height) height = bottomPlusPadding.toInt()

        saveImagesToPersistLayer(listOf(imageToAdd))
        indexImages()

        persistBitmapDebounced()
    }

    fun addImage(imageToAdd: List<Image>) {
        images += imageToAdd
        imageToAdd.forEach {
            val bottomPlusPadding = it.x + it.height + 50
            if (bottomPlusPadding > height) height = bottomPlusPadding.toInt()
        }
        saveImagesToPersistLayer(imageToAdd)
        indexImages()

        persistBitmapDebounced()
    }

    fun removeImage(imageIds: List<String>) {
        images = images.filter { s -> !imageIds.contains(s.id) }
        removeImagesFromPersistLayer(imageIds)
        indexImages()
        computeHeight()

        persistBitmapDebounced()
    }

    fun getImage(imageId: String): Image? {
        return imagesById[imageId]
    }


    fun computeHeight() {
        if (strokes.isEmpty()) {
            height = viewHeight
            return
        }
        val maxStrokeBottom = strokes.maxOf { it.bottom }.plus(50) ?: 0
        height = max(maxStrokeBottom.toInt(), viewHeight)
    }

    fun computeWidth(): Int {
        if (strokes.isEmpty()) {
            return viewWidth
        }
        val maxStrokeRight = strokes.maxOf { it.right }.plus(50) ?: 0
        return max(maxStrokeRight.toInt(), viewWidth)
    }

    private fun removeStrokesFromPersistLayer(strokeIds: List<String>) {
        AppRepository(context).strokeRepository.deleteAll(strokeIds)
    }

    private fun removeImagesFromPersistLayer(imageIds: List<String>) {
        AppRepository(context).imageRepository.deleteAll(imageIds)
    }

    private fun loadBitmap(): Boolean {
        val imgFile = File(context.filesDir, "pages/previews/full/$id")
        var imgBitmap: Bitmap? = null
        if (imgFile.exists()) {
            imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            if (imgBitmap != null) {
                windowedCanvas.drawBitmap(imgBitmap, 0f, 0f, Paint());
                Log.i(TAG, "Page rendered from cache")
                // let's control that the last preview fits the present orientation. Otherwise we'll ask for a redraw.
                if (imgBitmap.height == windowedCanvas.height && imgBitmap.width == windowedCanvas.width) {
                    return true
                } else {
                    Log.i(TAG, "Image preview does not fit canvas area - redrawing")
                }
            } else {
                Log.i(TAG, "Cannot read cache image")
            }
        } else {
            Log.i(TAG, "Cannot find cache image")
        }
        return false
    }

    private fun persistBitmap() {
        val file = File(context.filesDir, "pages/previews/full/$id")
        Files.createDirectories(Path(file.absolutePath).parent)
        val os = BufferedOutputStream(FileOutputStream(file))
        windowedBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
        os.close()
    }

    private fun persistBitmapThumbnail() {
        val file = File(context.filesDir, "pages/previews/thumbs/$id")
        Files.createDirectories(Path(file.absolutePath).parent)
        val os = BufferedOutputStream(FileOutputStream(file))
        val ratio = windowedBitmap.height.toFloat() / windowedBitmap.width.toFloat()
        Bitmap.createScaledBitmap(windowedBitmap, 500, (500 * ratio).toInt(), false)
            .compress(Bitmap.CompressFormat.JPEG, 80, os);
        os.close()
    }

    // ignored strokes are used in handleSelect
    // TODO: find way for selecting images
    fun drawArea(
        area: Rect,
        ignoredStrokeIds: List<String> = listOf(),
        ignoredImageIds: List<String> = listOf(),
        canvas: Canvas? = null
    ) {
        val activeCanvas = canvas ?: windowedCanvas
        val pageArea = Rect(
            area.left,
            area.top + scroll,
            area.right,
            area.bottom + scroll
        )

        activeCanvas.save();
        activeCanvas.clipRect(area);
        activeCanvas.drawColor(Color.BLACK)

        val timeToBg = measureTimeMillis {
            drawBg(activeCanvas, pageFromDb?.nativeTemplate ?: "blank", scroll)
        }
        Log.i(TAG, "Took $timeToBg to draw the BG")

        val timeToDraw = measureTimeMillis {
            // Trying to find what throws error when drawing quickly
            try {
                strokes.forEach { stroke ->
                    if (ignoredStrokeIds.contains(stroke.id)) return@forEach
                    val bounds = strokeBounds(stroke)
                    // if stroke is not inside page section
                    if (!bounds.toRect().intersect(pageArea)) return@forEach

                    drawStroke(
                        activeCanvas, stroke, IntOffset(0, -scroll)
                    )
                }

                images.forEach { image ->
                    if (ignoredImageIds.contains(image.id)) return@forEach
                    Log.i(TAG, "PageView.kt: drawing image!")
                    val bounds = imageBounds(image)
                    // if stroke is not inside page section
                    if (!bounds.toRect().intersect(pageArea)) return@forEach
                    drawImage(context, activeCanvas, image, IntOffset(0, -scroll))

                }
            } catch (e: Exception) {
                Log.e(TAG, "PageView.kt: Drawing strokes failed: ${e.message}")
            }

        }
        Log.i(TAG, "Drew area in ${timeToDraw}ms")
        activeCanvas.restore();
    }

    fun updateScroll(_delta: Int) {
        var delta = _delta
        if (scroll + delta < 0) delta = 0 - scroll

        scroll += delta

        // scroll bitmap
        val tmp = windowedBitmap.copy(windowedBitmap.config, false)
        drawBg(windowedCanvas, pageFromDb?.nativeTemplate ?: "blank", scroll)

        windowedCanvas.drawBitmap(tmp, 0f, -delta.toFloat(), Paint())
        tmp.recycle()

        // where is the new rendering area starting ?
        val canvasOffset = if (delta > 0) windowedCanvas.height - delta else 0

        drawArea(
            area = Rect(
                0,
                canvasOffset,
                windowedCanvas.width,
                canvasOffset + Math.abs(delta)
            ),
        )

        persistBitmapDebounced()
        saveToPersistLayer()
    }

    // updates page setting in db, (for instance type of background)
    // and redraws page to vew.
    fun updatePageSettings(page: Page) {
        AppRepository(context).pageRepository.update(page)
        pageFromDb = AppRepository(context).pageRepository.getById(id)
        drawArea(Rect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT))
        persistBitmapDebounced()
    }

    fun updateDimensions(newWidth: Int, newHeight: Int) {
        if (newWidth != viewWidth || newHeight != viewHeight) {
            viewWidth = newWidth
            viewHeight = newHeight

            // Recreate bitmap and canvas with new dimensions
            windowedBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
            windowedCanvas = Canvas(windowedBitmap)
            drawArea(Rect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT))
            persistBitmapDebounced()
        }
    }

    private fun persistBitmapDebounced() {
        coroutineScope.launch {
            saveTopic.emit(Unit)
        }
    }

    private fun saveToPersistLayer() {
        coroutineScope.launch {
            AppRepository(context).pageRepository.updateScroll(id, scroll)
            pageFromDb = AppRepository(context).pageRepository.getById(id)
        }
    }
}

