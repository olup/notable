package com.olup.notable

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Region
import android.util.TypedValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.toRect
import androidx.core.graphics.toRegion
import com.olup.notable.db.Image
import com.olup.notable.db.Stroke
import com.olup.notable.db.StrokePoint
import com.onyx.android.sdk.data.note.TouchPoint
import io.shipbook.shipbooksdk.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


fun Modifier.noRippleClickable(
    onClick: () -> Unit
): Modifier = composed {
    this.then(
        clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
            onClick()
        })
}


fun convertDpToPixel(dp: Dp, context: Context): Float {
    val resources = context.resources
//    val metrics: DisplayMetrics = resources.displayMetrics
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.value,
        context.resources.displayMetrics
    )
}

// TODO move this to repository
fun deletePage(context: Context, pageId: String) {
    val appRepository = AppRepository(context)
    val page = appRepository.pageRepository.getById(pageId) ?: return
    val proxy = appRepository.kvProxy
    val settings = proxy.get("APPS_SETTINGS", AppSettings.serializer())


    runBlocking {
        // remove from book
        if (page.notebookId != null) {
            appRepository.bookRepository.removePage(page.notebookId, pageId)
        }

        // remove from quick nav
        if (settings != null && settings.quickNavPages.contains(pageId)) {
            proxy.setKv(
                "APPS_SETTINGS",
                settings.copy(quickNavPages = settings.quickNavPages - pageId),
                AppSettings.serializer()
            )
        }

        launch {
            appRepository.pageRepository.delete(pageId)
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

fun <T : Any> Flow<T>.withPrevious(): Flow<Pair<T?, T>> = flow {
    var prev: T? = null
    this@withPrevious.collect {
        emit(prev to it)
        prev = it
    }
}

fun pointsToPath(points: List<SimplePointF>): Path {
    val path = Path()
    val prePoint = PointF(points[0].x, points[0].y)
    path.moveTo(prePoint.x, prePoint.y)

    for (point in points) {
        // skip strange jump point.
        //if (abs(prePoint.y - point.y) >= 30) continue
        path.quadTo(prePoint.x, prePoint.y, point.x, point.y)
        prePoint.x = point.x
        prePoint.y = point.y
    }
    return path
}

// points is in page coordinates
fun handleErase(
    page: PageView,
    history: History,
    points: List<SimplePointF>,
    eraser: Eraser
) {
    val paint = Paint().apply {
        this.strokeWidth = 30f
        this.style = Paint.Style.STROKE
        this.strokeCap = Paint.Cap.ROUND
        this.strokeJoin = Paint.Join.ROUND
        this.isAntiAlias = true
    }
    val path = pointsToPath(points)
    var outPath = Path()

    if (eraser == Eraser.SELECT) {
        path.close()
        outPath = path
    }


    if (eraser == Eraser.PEN) {
        paint.getFillPath(path, outPath)
    }

    val deletedStrokes = selectStrokesFromPath(page.strokes, outPath)

    val deletedStrokeIds = deletedStrokes.map { it.id }
    page.removeStrokes(deletedStrokeIds)

    history.addOperationsToHistory(listOf(Operation.AddStroke(deletedStrokes)))

    page.drawArea(
        area = pageAreaToCanvasArea(strokeBounds(deletedStrokes), page.scroll)
    )
}

enum class SelectPointPosition {
    LEFT,
    RIGHT,
    CENTER
}

// touchpoints is in view coordinates
fun handleDraw(
    page: PageView,
    historyBucket: MutableList<String>,
    strokeSize: Float,
    color: Int,
    pen: Pen,
    touchPoints: List<TouchPoint>
) {
    try {
        val initialPoint = touchPoints[0]
        val boundingBox = RectF(
            initialPoint.x,
            initialPoint.y + page.scroll,
            initialPoint.x,
            initialPoint.y + page.scroll
        )

        val points = touchPoints.map {
            boundingBox.union(it.x, it.y + page.scroll)
            StrokePoint(
                x = it.x,
                y = it.y + page.scroll,
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
            pageId = page.id,
            top = boundingBox.top,
            bottom = boundingBox.bottom,
            left = boundingBox.left,
            right = boundingBox.right,
            points = points,
            color = color
        )
        page.addStrokes(listOf(stroke))
        // this is causing lagging and crushing, neo pens are not good
        page.drawArea(pageAreaToCanvasArea(strokeBounds(stroke).toRect(), page.scroll))
        historyBucket.add(stroke.id)
    } catch (e: Exception) {
        Log.e(TAG, "Handle Draw: An error occurred while handling the drawing: ${e.message}")
    }
}

/*
* Gets list of points, and return line from first point to last.
* The line consist of 100 points, I do not know how it works (for 20 it want draw correctly)
* Then it cals handle draw to make mark on canvas.
 */
fun handleLine(
    page: PageView,
    historyBucket: MutableList<String>,
    strokeSize: Float,
    color: Int,
    pen: Pen,
    touchPoints: List<TouchPoint>
) {
    val startPoint = touchPoints.first()
    val endPoint = touchPoints.last()

    // Setting intermediate values for tilt and pressure
    startPoint.tiltX = touchPoints[touchPoints.size / 10].tiltX
    startPoint.tiltY = touchPoints[touchPoints.size / 10].tiltY
    startPoint.pressure = touchPoints[touchPoints.size / 10].pressure
    endPoint.tiltX = touchPoints[9 * touchPoints.size / 10].tiltX
    endPoint.tiltY = touchPoints[9 * touchPoints.size / 10].tiltY
    endPoint.pressure = touchPoints[9 * touchPoints.size / 10].pressure

    // Helper function to interpolate between two values
    fun lerp(start: Float, end: Float, fraction: Float) = start + (end - start) * fraction

    val numberOfPoints = 100 // Define how many points should line have
    val points2 = List(numberOfPoints) { i ->
        val fraction = i.toFloat() / (numberOfPoints - 1)
        val x = lerp(startPoint.x, endPoint.x, fraction)
        val y = lerp(startPoint.y, endPoint.y, fraction)
        val pressure = lerp(startPoint.pressure, endPoint.pressure, fraction)
        val size = lerp(startPoint.size, endPoint.size, fraction)
        val tiltX = (lerp(startPoint.tiltX.toFloat(), endPoint.tiltX.toFloat(), fraction)).toInt()
        val tiltY = (lerp(startPoint.tiltY.toFloat(), endPoint.tiltY.toFloat(), fraction)).toInt()
        val timestamp = System.currentTimeMillis()

        TouchPoint(x, y, pressure, size, tiltX, tiltY, timestamp)
    }

    handleDraw(page, historyBucket, strokeSize, color, pen, points2)
}


inline fun Modifier.ifTrue(predicate: Boolean, builder: () -> Modifier) =
    then(if (predicate) builder() else Modifier)

fun strokeToTouchPoints(stroke: Stroke): List<TouchPoint> {
    return stroke.points.map {
        TouchPoint(
            it.x,
            it.y,
            it.pressure,
            stroke.size,
            it.tiltX,
            it.tiltY,
            it.timestamp
        )
    }
}

fun pageAreaToCanvasArea(pageArea: Rect, scroll: Int): Rect {
    return Rect(
        pageArea.left, pageArea.top - scroll, pageArea.right, pageArea.bottom - scroll
    )
}

fun strokeBounds(stroke: Stroke): RectF {
    return RectF(
        stroke.left, stroke.top, stroke.right, stroke.bottom
    )
}

fun imageBounds(image: Image): RectF {
    return RectF(
        image.x.toFloat(),
        image.y.toFloat(),
        image.x + image.width.toFloat(),
        image.y + image.height.toFloat()
    )
}


fun strokeBounds(strokes: List<Stroke>): Rect {
    if (strokes.isEmpty()) return Rect()
    val stroke = strokes[0]
    val rect = Rect(
        stroke.left.toInt(), stroke.top.toInt(), stroke.right.toInt(), stroke.bottom.toInt()
    )
    strokes.forEach {
        rect.union(
            Rect(
                it.left.toInt(), it.top.toInt(), it.right.toInt(), it.bottom.toInt()
            )
        )
    }
    return rect
}

fun imageBoundsInt(image: Image, padding: Int = 0): Rect {
    return Rect(
        image.x + padding,
        image.y + padding,
        image.x + image.width + padding,
        image.y + image.height + padding
    )
}

fun imageBoundsInt(images: List<Image>): Rect {
    if (images.isEmpty()) return Rect()
    val rect = imageBoundsInt(images[0])
    images.forEach {
        rect.union(
            imageBoundsInt(it)
        )
    }
    return rect
}

data class SimplePoint(val x: Int, val y: Int)
data class SimplePointF(val x: Float, val y: Float)

fun pathToRegion(path: Path): Region {
    val bounds = RectF()
    path.computeBounds(bounds, true)
    val region = Region()
    region.setPath(
        path,
        bounds.toRegion()
    )
    return region
}

fun divideStrokesFromCut(
    strokes: List<Stroke>,
    cutLine: List<SimplePointF>
): Pair<List<Stroke>, List<Stroke>> {
    val maxY = cutLine.maxOfOrNull { it.y }
    val cutArea = listOf(SimplePointF(0f, maxY!!)) + cutLine + listOf(
        SimplePointF(
            cutLine.last().x,
            maxY
        )
    )
    val cutPath = pointsToPath(cutArea)
    cutPath.close()

    val bounds = RectF().apply {
        cutPath.computeBounds(this, true)
    }
    val cutRegion = pathToRegion(cutPath)

    val strokesOver: MutableList<Stroke> = mutableListOf()
    val strokesUnder: MutableList<Stroke> = mutableListOf()

    strokes.forEach { stroke ->
        if (stroke.top > bounds.bottom) strokesUnder.add(stroke)
        else if (stroke.bottom < bounds.top) strokesOver.add(stroke)
        else {
            if (stroke.points.any { point ->
                    cutRegion.contains(
                        point.x.toInt(),
                        point.y.toInt()
                    )
                }) strokesUnder.add(stroke)
            else strokesOver.add(stroke)
        }
    }

    return strokesOver to strokesUnder
}

fun selectStrokesFromPath(strokes: List<Stroke>, path: Path): List<Stroke> {
    val bounds = RectF()
    path.computeBounds(bounds, true)

    //region is only 16 bit, so we need to move our region
    val translatedPath = Path(path)
    translatedPath.offset(0f, - bounds.top)
    val region = pathToRegion(translatedPath)

    return strokes.filter {
        strokeBounds(it).intersect(bounds)
    }.filter { it.points.any { region.contains(it.x.toInt(), (it.y-bounds.top).toInt()) } }
}

fun offsetStroke(stroke: Stroke, offset: Offset): Stroke {
    return stroke.copy(
        points = stroke.points.map { p -> p.copy(x = p.x + offset.x, y = p.y + offset.y) },
        top = stroke.top + offset.y,
        bottom = stroke.bottom + offset.y,
        left = stroke.left + offset.x,
        right = stroke.right + offset.x,
    )
}

fun offsetImage(image: Image, offset: Offset): Image {
    return image.copy(
        x = image.x + offset.x.toInt(),
        y = image.y + offset.y.toInt(),
        height = image.height,
        width = image.width,
        uri = image.uri,
        pageId = image.pageId
    )
}

// Why it is needed? I try to removed it, and sharing bimap seems to work.
class Provider : FileProvider(R.xml.file_paths)

fun shareBitmap(context: Context, bitmap: Bitmap) {
    val bmpWithBackground =
        Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmpWithBackground)
    canvas.drawColor(Color.WHITE)
    canvas.drawBitmap(bitmap, 0f, 0f, null)

    val cachePath = File(context.cacheDir, "images")
    Log.i(TAG, cachePath.toString())
    cachePath.mkdirs()
    try {
        val stream =
            FileOutputStream("$cachePath/share.png")
        bmpWithBackground.compress(
            Bitmap.CompressFormat.PNG,
            100,
            stream
        )
        stream.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }

    val bitmapFile = File(cachePath, "share.png")
    val contentUri = FileProvider.getUriForFile(
        context,
        "com.olup.notable.provider", //(use your app signature + ".provider" )
        bitmapFile
    )

    val sendIntent = Intent().apply {
        if (contentUri != null) {
            action = Intent.ACTION_SEND
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
            putExtra(Intent.EXTRA_STREAM, contentUri)
            type = "image/png"
        }

        context.grantUriPermission("android", contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    ContextCompat.startActivity(context, Intent.createChooser(sendIntent, "Choose an app"), null)
}


