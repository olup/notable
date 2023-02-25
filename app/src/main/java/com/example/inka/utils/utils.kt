package com.example.inka

import android.content.Context
import android.graphics.*
import android.util.DisplayMetrics
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.Dp
import com.example.inka.db.Stroke
import com.example.inka.db.StrokePoint
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.NeoBrushPen
import com.onyx.android.sdk.pen.NeoCharcoalPen
import com.onyx.android.sdk.pen.NeoFountainPen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.math.abs
import kotlin.system.measureTimeMillis


// TODO remove in profit of the page class
object StrokeCache {
    var pageId: String? = null
    var strokes: List<Stroke> = listOf()
}

// TODO remove in profit of the page class
fun loadStrokeCache(context: Context) {
    if (StrokeCache.pageId == null) return
    val appRepository = AppRepository(context)
    StrokeCache.strokes =
        appRepository.pageRepository.getWithStrokeById(StrokeCache.pageId!!).strokes
}

// TODO remove in profit of the page class
fun loadStrokeCache(context: Context, pageId: String) {
    val appRepository = AppRepository(context)
    StrokeCache.pageId = pageId
    StrokeCache.strokes = appRepository.pageRepository.getWithStrokeById(pageId).strokes
}

// TODO remove in profit of the page class
fun renderPageFromDbToCanvas(
    context: Context, canvas: Canvas, pageId: String, pageSection: RectF, canvasSection: RectF
) {
    canvas.save();
    canvas.clipRect(canvasSection);
    val timeToBg = measureTimeMillis {
        drawDottedBg(canvas, (pageSection.top - canvasSection.top).toInt())
    }
    println("Took $timeToBg to draw the BG")

    if (StrokeCache.pageId != pageId) {
        val timeToQuery = measureTimeMillis {
            loadStrokeCache(context, pageId)
        }
        println("Took $timeToQuery to fetch all page's points")
    }

    val timeToDraw = measureTimeMillis {
        StrokeCache.strokes.forEach { stroke ->

            val bounds = RectF(stroke.left, stroke.top, stroke.right, stroke.bottom)

            // if stroke is inside page section
            if (bounds.intersect(pageSection)) {
                println("Stroke identifed for rendering")

                val points = stroke.points.map {
                    TouchPoint(
                        it.x - pageSection.left + canvasSection.left,
                        it.y - pageSection.top + canvasSection.top,
                        it.pressure,
                        stroke.size,
                        it.tiltX,
                        it.tiltY,
                        it.timestamp
                    )
                }
                drawStroke(
                    canvas, stroke.pen, stroke.size, points
                )
            }
        }
    }

    println("Took $timeToDraw to draw all page's points")

    canvas.restore();
}

// TODO remove in profit of the page class
fun pageBitmapToFile(context: Context, bitmap: Bitmap, pageId: String) {
    println("Saving page bitmap to disk")
    runBlocking {
        launch {
            // full size
            val file = File(context.filesDir, "pages/previews/full/$pageId")
            Files.createDirectories(Path(file.absolutePath).parent)
            val os = BufferedOutputStream(FileOutputStream(file))
            bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 50, os);
            os.close()
        }

        launch {
            // thumbs
            val file = File(context.filesDir, "pages/previews/thumbs/$pageId")
            Files.createDirectories(Path(file.absolutePath).parent)
            val os = BufferedOutputStream(FileOutputStream(file))
            val ratio = bitmap.height.toFloat() / bitmap.width.toFloat()
            Bitmap.createScaledBitmap(bitmap, 500, (500 * ratio).toInt(), false)
                .compress(Bitmap.CompressFormat.WEBP_LOSSY, 50, os);
            os.close()
        }

    }
}

// TODO remove in profit of the page class
fun renderCachedPageToCanvas(context: Context, canvas: Canvas, pageId: String): Boolean {
    val imgFile = File(context.filesDir, "pages/previews/full/$pageId")
    var imgBitmap: Bitmap? = null
    if (imgFile.exists()) {
        imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
        if (imgBitmap != null) {
            canvas.drawBitmap(imgBitmap, 0f, 0f, Paint());
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

val debouncedSavedBitmapFlow = MutableSharedFlow<Unit>()

fun Modifier.noRippleClickable(
    onClick: () -> Unit
): Modifier = composed {
    clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
        onClick()
    }
}


fun convertDpToPixel(dp: Dp, context: Context): Float {
    val resources = context.resources
    val metrics: DisplayMetrics = resources.getDisplayMetrics()
    return dp.value * (metrics.densityDpi / 160f)
}

// TODO move this to repository
fun deletePage(context: Context, pageId: String) {
    val appRepository = AppRepository(context)
    val page = appRepository.pageRepository.getById(pageId) ?: return

    runBlocking {
        launch {
            appRepository.pageRepository.delete(page)
        }
        launch {
            val imgFile = File(context.filesDir, "pages/previews/thumbs/$pageId")
            if (imgFile.exists()) {
                imgFile.delete()
            }
        }
        launch {
            val imgFile = File(context.filesDir, "pages/previews/full/$pageId")
            if (imgFile.exists()) {
                imgFile.delete()
            }
        }

    }
}

// TODO move this to repository
fun deleteBook(context: Context, bookId: String) {
    val appRepository = AppRepository(context)
    val book = appRepository.bookRepository.getById(bookId) ?: return

    runBlocking {
        launch {
            appRepository.bookRepository.delete(bookId)
        }
        for (pageId in book.pageIds) {
            launch {
                deletePage(context, pageId)
            }
        }
    }
}

fun <T : Any> Flow<T>.withPrevious(): Flow<Pair<T?, T>> = flow {
    var prev: T? = null
    this@withPrevious.collect {
        emit(prev to it)
        prev = it
    }
}

fun pointsToPath(points: List<Pair<Float, Float>>): Path {
    val path = Path()
    val prePoint = PointF(points[0].first, points[0].second)
    path.moveTo(prePoint.x, prePoint.y)

    for (point in points) {
        // skip strange jump point.
        if (abs(prePoint.y - point.second) >= 30) continue
        path.quadTo(prePoint.x, prePoint.y, point.first, point.second)
        prePoint.x = point.first
        prePoint.y = point.second
    }
    return path
}

fun handleErase(
    context: Context,
    canvas: Canvas,
    pageId: String,
    scroll: Int,
    points: List<Pair<Float, Float>>
) {
    val paint = Paint().apply {
        this.strokeWidth = 10f
        this.style = Paint.Style.STROKE
        this.strokeCap = Paint.Cap.ROUND
        this.strokeJoin = Paint.Join.ROUND
        this.isAntiAlias = true
    }
    val path = pointsToPath(points)

    // lasso eraser
    path.close()
    val outPath = path


// stroke erasaer
//    val outPath = Path()
//    paint.getFillPath(path, outPath)

    val bounds = RectF()
    outPath.computeBounds(bounds, true)

    val region = Region()
    region.setPath(
        outPath,
        Region(
            bounds.left.toInt(),
            bounds.top.toInt(),
            bounds.right.toInt(),
            bounds.bottom.toInt()
        )
    )

    val affectedZone = RectF()
    var deletedStrokes = listOf<Stroke>()
    val appRepository = AppRepository(context)
    StrokeCache.strokes.filter {
        RectF(
            it.left,
            it.top,
            it.right,
            it.bottom
        ).intersect(bounds)
    }.forEach() {
        for (point in it.points) {
            if (region.contains(point.x.toInt(), point.y.toInt())) {
                affectedZone.union(
                    RectF(
                        it.left,
                        it.top,
                        it.right,
                        it.bottom
                    )
                )
                deletedStrokes += it
                break
            }
        }
    }


    val deletedStrokeIds = deletedStrokes.map { it.id }
    StrokeCache.strokes = StrokeCache.strokes.filter {
        !deletedStrokeIds.contains(it.id)
    }
    appRepository.strokeRepository.deleteAll(deletedStrokeIds)

    addOperationsToHistory(deletedStrokes.map { Operation.AddStroke(it) })

    renderPageFromDbToCanvas(
        context = context,
        canvas = canvas,
        pageId = pageId,
        pageSection = RectF(
            affectedZone.left, affectedZone.top, affectedZone.right, affectedZone.bottom
        ),
        canvasSection = RectF(
            affectedZone.left,
            affectedZone.top - scroll,
            affectedZone.right,
            affectedZone.bottom - scroll
        ),
    )
}

fun handleDraw(
    context: Context,
    canvas: Canvas,
    pageId: String,
    scroll: Int,
    strokeSize: Float,
    pen: Pen,
    touchPoints: List<TouchPoint>
) {

    val boundingBox = RectF()

    val points = touchPoints.map {
        boundingBox.union(it.x, it.y)

        StrokePoint(
            x = it.x,
            y = it.y + scroll,
            pressure = it.pressure,
            size = it.size,
            tiltX = it.tiltX,
            tiltY = it.tiltY,
            timestamp = it.timestamp,
        )
    }

    boundingBox.inset(-strokeSize, -strokeSize)

    val stroke = Stroke(
        size = strokeSize,
        pen = pen,
        pageId = pageId,
        top = boundingBox.top,
        bottom = boundingBox.bottom,
        left = boundingBox.left,
        right = boundingBox.right,
        points = points
    )

    // add stroke to DB and cache
    AppRepository(context).strokeRepository.create(
        stroke
    )
    if (StrokeCache.pageId == pageId) {
        StrokeCache.strokes += stroke
    }

    // draw to page (could also call refresh page from region)
    drawStroke(
        canvas, stroke.pen, stroke.size, touchPoints
    )
    addOperationsToHistory(listOf(Operation.DeleteStroke(stroke.id)))

}


inline fun Modifier.ifTrue(predicate: Boolean, builder: () -> Modifier) =
    then(if (predicate) builder() else Modifier)
