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
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.NeoBrushPen
import com.onyx.android.sdk.pen.NeoCharcoalPen
import com.onyx.android.sdk.pen.NeoFountainPen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
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


const val padding = 50
const val lineHeight = 50
const val dotSize = 4f

fun drawLinedBg(canvas: Canvas, scroll: Int) {
    val height = canvas.height
    val width = canvas.width

    // white bg
    canvas.drawColor(android.graphics.Color.WHITE)

    // paint
    val paint = Paint().apply {
        this.color = Color.GRAY
        this.strokeWidth = 1f
    }

    // lines
    for (y in 0..height) {
        val line = scroll + y
        if (line % lineHeight == 0) {
            canvas.drawLine(
                padding.toFloat(), y.toFloat(), (width - padding).toFloat(), y.toFloat(), paint
            )
        }
    }
}

fun drawDottedBg(canvas: Canvas, offset: Int) {
    val height = canvas.height
    val width = canvas.width

    // white bg
    canvas.drawColor(android.graphics.Color.WHITE)

    // paint
    val paint = Paint().apply {
        this.color = Color.GRAY
        this.strokeWidth = 1f
    }

    // dots
    for (y in 0..height) {
        val line = offset + y
        if (line % lineHeight == 0 && line >= padding) {
            for (x in padding..width - padding step lineHeight) {
                canvas.drawOval(
                    x.toFloat() - dotSize / 2,
                    y.toFloat() - dotSize / 2,
                    x.toFloat() + dotSize / 2,
                    y.toFloat() + dotSize / 2,
                    paint
                )
            }
        }
    }
}

fun drawSquaredBg(canvas: Canvas, scroll: Int) {
    println("Drawing BG")
    val height = canvas.height
    val width = canvas.width

    // white bg
    canvas.drawColor(android.graphics.Color.WHITE)

    // paint
    val paint = Paint().apply {
        this.color = Color.GRAY
        this.strokeWidth = 1f
    }

    // lines
    for (y in 0..height) {
        val line = scroll + y
        if (line % lineHeight == 0) {
            canvas.drawLine(
                padding.toFloat(), y.toFloat(), (width - padding).toFloat(), y.toFloat(), paint
            )
        }
    }

    for (x in padding..width - padding step lineHeight) {
        canvas.drawLine(
            x.toFloat(), padding.toFloat(), x.toFloat(), height.toFloat(), paint
        )
    }
}

object StrokeCache {
    var pageId: Int? = null
    var strokes: List<Stroke> = listOf()
}

fun loadStrokeCache(context: Context) {
    if(StrokeCache.pageId == null) return
    val appRepository = AppRepository(context)
    StrokeCache.strokes = appRepository.pageRepository.getWithStrokeById(StrokeCache.pageId!!).strokes
}

fun loadStrokeCache(context: Context, pageId: Int) {
    val appRepository = AppRepository(context)
    StrokeCache.pageId = pageId
    StrokeCache.strokes = appRepository.pageRepository.getWithStrokeById(pageId).strokes
}

fun renderPageFromDbToCanvas(
    context: Context, canvas: Canvas, pageId: Int, pageSection: RectF, canvasSection: RectF
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

            // if stroke is inside page section
            if (stroke.top <= pageSection.bottom &&
                stroke.bottom >= pageSection.top &&
                stroke.left <= pageSection.right &&
                stroke.right >= pageSection.left) {
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

fun pageBitmapToFile(context: Context, bitmap: Bitmap, pageId: Int) {
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

fun renderCachedPageToCanvas(context: Context, canvas: Canvas, pageId: Int): Boolean {
    // Optim - load cached page on disk as jpeg
    val imgFile = File(context.filesDir, "pages/previews/full/$pageId")
    // on below line we are checking if the image file exist or not.
    var imgBitmap: Bitmap? = null
    if (imgFile.exists()) {
        // TODO decode straight to given bitmap ?
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

fun drawBallPenStroke(
    canvas: Canvas, paint: Paint, strokeSize: Float, points: List<TouchPoint>
) {
    val copyPaint = Paint(paint).apply {
        this.strokeWidth = strokeSize
        this.style = Paint.Style.STROKE
        this.strokeCap = Paint.Cap.ROUND
        this.strokeJoin = Paint.Join.ROUND

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

fun drawMarkerStroke(
    canvas: Canvas, paint: Paint, strokeSize: Float, points: List<TouchPoint>
) {
    val copyPaint = Paint(paint).apply {
        this.strokeWidth = strokeSize
        this.style = Paint.Style.STROKE
        this.strokeCap = Paint.Cap.ROUND
        this.strokeJoin = Paint.Join.ROUND
        this.isAntiAlias = true
        this.alpha = 100
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
    val paint = Paint().apply {
        color = Color.BLACK
        this.strokeWidth = strokeSize
    }

    when (pen) {
        Pen.BALLPEN -> drawBallPenStroke(canvas, paint, strokeSize, points)
        Pen.PENCIL -> NeoCharcoalPen.drawNormalStroke(
            null, canvas, paint, points, -16777216, strokeSize, pressure, 90, false
        )
        Pen.BRUSH -> NeoBrushPen.drawStroke(canvas, paint, points, strokeSize, pressure, false)
        Pen.MARKER -> drawMarkerStroke(canvas, paint, strokeSize, points)
        Pen.FOUNTAIN -> NeoFountainPen.drawStroke(
            canvas, paint, points, 1f, strokeSize, pressure, false
        )
    }
}

fun convertDpToPixel(dp: Dp, context: Context): Float {
    val resources = context.resources
    val metrics: DisplayMetrics = resources.getDisplayMetrics()
    return dp.value * (metrics.densityDpi / 160f)
}

// TODO move this to repository
fun deletePage(context: Context, pageId: Int) {
    val appRepository = AppRepository(context)
    val page = appRepository.pageRepository.getById(pageId)?: return

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
fun deleteBook(context: Context, bookId: Int) {
    val appRepository = AppRepository(context)
    val book = appRepository.bookRepository.getById(bookId)?:return

    runBlocking {
        launch {
            appRepository.bookRepository.delete(bookId)
        }
        for(pageId in book.pageIds){
            launch {
                deletePage(context, pageId)
            }
        }
    }
}
fun <T: Any> Flow<T>.withPrevious(): Flow<Pair<T?, T>> = flow {
    var prev: T? = null
    this@withPrevious.collect {
        emit(prev to it)
        prev = it
    }
}