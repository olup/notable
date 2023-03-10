package com.olup.notable

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.util.DisplayMetrics
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.toRect
import androidx.core.graphics.toRegion
import com.olup.notable.db.Notebook
import com.olup.notable.db.Stroke
import com.olup.notable.db.StrokePoint
import com.olup.notable.PageModel
import com.onyx.android.sdk.data.note.TouchPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.absolutePathString


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
        if(page.notebookId != null){
            appRepository.bookRepository.removePage(page.notebookId, pageId)
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
    page: PageModel,
    history: History,
    points: List<SimplePointF>,
) {
    /* val paint = Paint().apply {
         this.strokeWidth = 10f
         this.style = Paint.Style.STROKE
         this.strokeCap = Paint.Cap.ROUND
         this.strokeJoin = Paint.Join.ROUND
         this.isAntiAlias = true
     }*/
    val path = pointsToPath(points)

    // lasso eraser
    path.close()
    val outPath = path


// stroke erasaer
//    val outPath = Path()
//    paint.getFillPath(path, outPath)

    val deletedStrokes = selectStrokesFromPath(page.strokes, path)

    val deletedStrokeIds = deletedStrokes.map { it.id }
    page.removeStrokes(deletedStrokeIds)

    history.addOperationsToHistory(listOf(Operation.AddStroke(deletedStrokes)))

    page.drawArea(
        canvasArea = pageAreaToCanvasArea(strokeBounds(deletedStrokes), page.scroll)
    )
}

enum class SelectPointPosition {
    LEFT,
    RIGHT,
    CENTER
}

// points is in page coodinates
fun handleSelect(
    scope: CoroutineScope,
    page: PageModel,
    editorState: EditorState,
    points: List<SimplePointF>
) {
    val state = editorState.selectionState

    val firstPointPosition =
        if (points.first().x < 50) SelectPointPosition.LEFT else if (points.first().x > page.pageWidth - 50) SelectPointPosition.RIGHT else SelectPointPosition.CENTER
    val lastPointPosition =
        if (points.last().x < 50) SelectPointPosition.LEFT else if (points.last().x > page.pageWidth - 50) SelectPointPosition.RIGHT else SelectPointPosition.CENTER

    if (firstPointPosition != SelectPointPosition.CENTER && lastPointPosition != SelectPointPosition.CENTER && firstPointPosition != lastPointPosition) {
        // Page cut situation
        val correctedPoints =
            if (firstPointPosition === SelectPointPosition.LEFT) points else points.reversed()
        // lets make this end to end
        val completePoints =
            listOf(SimplePointF(0f, correctedPoints.first().y)) + correctedPoints + listOf(
                SimplePointF(page.pageWidth.toFloat(), correctedPoints.last().y)
            )
        if (state.firstPageCut == null) {
            // this is the first page cut
            state.firstPageCut = completePoints
            println("Registered first curt")
        } else {
            // this is the second page cut, we can also select the strokes
            // first lets have the cuts in the right order
            if (completePoints[0].y > state.firstPageCut!![0].y) state.secondPageCut =
                completePoints
            else {
                state.secondPageCut = state.firstPageCut
                state.firstPageCut = completePoints
            }
            // let's get stroke selection from that
            val (_, after) = divideStrokesFromCut(page.strokes, state.firstPageCut!!)
            val (middle, _) = divideStrokesFromCut(after, state.secondPageCut!!)
            state.selectedStrokes = middle
        }
    } else {
        // lasso selection
        // padding inside the dashed selection square
        val padding = 30

        // rcreate the lasso selection
        val selectionPath = pointsToPath(points)
        selectionPath.close()

        // get the selected strokes
        val selectedStrokes = selectStrokesFromPath(page.strokes, selectionPath)
        if (selectedStrokes.isEmpty()) return

        // TODO collocate with control tower ?

        state.selectedStrokes = selectedStrokes

        // area of implication - in page and view reference
        val pageBounds = strokeBounds(selectedStrokes)
        pageBounds.inset(-padding, -padding)

        val bounds = pageAreaToCanvasArea(pageBounds, page.scroll)

        // create bitmap and draw strokes
        val selectedBitmap =
            Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
        val selectedCanvas = Canvas(selectedBitmap)
        selectedStrokes.forEach {
            drawStroke(
                selectedCanvas,
                it,
                IntOffset(-pageBounds.left, -pageBounds.top)
            )
        }

        // set state
        state.selectedBitmap = selectedBitmap
        state.selectionStartOffset = IntOffset(bounds.left, bounds.top)
        state.selectionRect = bounds
        state.selectionDisplaceOffset = IntOffset(0, 0)
        state.placementMode = PlacementMode.Move

//        page.removeStrokes(selectedStrokes.map{it.id})
        page.drawArea(bounds, selectedStrokes.map { it.id })

        scope.launch {
            DrawCanvas.refreshUi.emit(Unit)
            editorState.isDrawing = false
        }
    }
}


// touchpoints is in wiew coordinates
fun handleDraw(
    page: PageModel,
    historyBucket: MutableList<String>,
    strokeSize: Float,
    pen: Pen,
    touchPoints: List<TouchPoint>
) {
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
        pageId = page.pageId,
        top = boundingBox.top,
        bottom = boundingBox.bottom,
        left = boundingBox.left,
        right = boundingBox.right,
        points = points
    )
    page.addStrokes(listOf(stroke))
    page.drawArea(pageAreaToCanvasArea(strokeBounds(stroke).toRect(), page.scroll))
    historyBucket.add(stroke.id)
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

fun strokeBounds(strokes: List<Stroke>): Rect {
    if (strokes.size == 0) return Rect()
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
    val region = pathToRegion(path)

    return strokes.filter {
        strokeBounds(it).intersect(bounds)
    }.filter() { it.points.any { region.contains(it.x.toInt(), it.y.toInt()) } }
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

public class Provider : FileProvider(R.xml.paths) {
}

fun shareBitmap(context: Context, bitmap: Bitmap) {
    val bmpWithBackground =
        Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmpWithBackground)
    canvas.drawColor(Color.WHITE)
    canvas.drawBitmap(bitmap, 0f, 0f, null)

    val cachePath = File(context.cacheDir, "images")
    println(cachePath)
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
    );

    val sendIntent = Intent().apply {
        if (contentUri != null) {
            action = Intent.ACTION_SEND
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
            putExtra(Intent.EXTRA_STREAM, contentUri);
            type = "image/png";
        }

        context.grantUriPermission("android", contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    ContextCompat.startActivity(context, Intent.createChooser(sendIntent, "Choose an app"), null)
}

fun exportPage(page: PageModel){
    page.computeHeight()

    println(android.os.Environment.DIRECTORY_DOCUMENTS)

    var document = PdfDocument()
    var documentPage = document.startPage(PdfDocument.PageInfo.Builder(page.pageWidth, page.pageHeight, 1).create())

    page.drawArea(canvasArea = Rect(0,0,page.pageWidth, page.pageHeight), canvas = documentPage.canvas)
    document.finishPage(documentPage)
    val filePath = Paths.get(android.os.Environment.getExternalStorageDirectory().toString(), android.os.Environment.DIRECTORY_DOCUMENTS.toString(), "notable", "pages",  "notable-page-${page.pageId.takeLast(6)}.pdf")
    Files.createDirectories(filePath.parent)
    document.writeTo( FileOutputStream(filePath.absolutePathString()))
    document.close()
}

fun exportBook(book: Notebook){
}