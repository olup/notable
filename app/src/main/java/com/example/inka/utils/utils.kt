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
import androidx.core.graphics.toRect
import com.example.inka.db.Stroke
import com.example.inka.db.StrokePoint
import com.onyx.android.sdk.data.note.TouchPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File


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
    history:History,
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
    page: PageModel,
    state: SelectionState,
    points: List<SimplePointF>
) {

    val firstPointPosition =
        if (points.first().x < 50) SelectPointPosition.LEFT else if (points.first().x > page.pageWidth - 50) SelectPointPosition.RIGHT else SelectPointPosition.CENTER
    val lastPointPosition =
        if (points.last().x < 50) SelectPointPosition.LEFT else if (points.last().x > page.pageWidth - 50) SelectPointPosition.RIGHT else SelectPointPosition.CENTER

    if (firstPointPosition != SelectPointPosition.CENTER && lastPointPosition != SelectPointPosition.CENTER && firstPointPosition != lastPointPosition){
        // Page cut situation
        val correctedPoints = if(firstPointPosition === SelectPointPosition.LEFT) points else points.reversed()
        // lets make this end to end
        val completePoints = listOf(SimplePointF(0f,correctedPoints.first().y)) +  correctedPoints + listOf(SimplePointF(page.pageWidth.toFloat(),correctedPoints.last().y))
        if(state.firstPageCut == null) {
            // this is the first page cut
            state.firstPageCut = completePoints
            println("Registered first curt")
        }
        else {
            // this is the second page cut, we can also select the strokes
            // first lets have the cuts in the right order
            if(completePoints[0].y > state.firstPageCut!![0].y) state.secondPageCut = completePoints
            else {
                state.secondPageCut = state.firstPageCut
                state.firstPageCut = completePoints
            }
            // let's get stroke selection from that
            val (_, after) = divideStrokesFromCut(page.strokes, state.firstPageCut!!)
            val (middle, _) = divideStrokesFromCut(after, state.secondPageCut!!)
            state.selectedStrokes = middle
        }
    }else {
        // random selection
        val selectionPath = pointsToPath(points)
        selectionPath.close()
        state.selectedStrokes = selectStrokesFromPath(page.strokes, selectionPath)
    }
}

// touchpoints is in wiew coordinates
fun handleDraw(
    page: PageModel,
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

    runBlocking {
        History.registerHistoryOperationBlock(listOf(Operation.DeleteStroke(listOf(stroke.id))))
    }
}


inline fun Modifier.ifTrue(predicate: Boolean, builder: () -> Modifier) =
    then(if (predicate) builder() else Modifier)

fun strokeToTouchPoints(stroke: Stroke, scroll: Int): List<TouchPoint> {
    return stroke.points.map {
        TouchPoint(
            it.x,
            it.y - scroll,
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
    if(strokes.size == 0) return Rect()
    val stroke = strokes[0]
    val rect = Rect(
        stroke.left.toInt(), stroke.top.toInt(), stroke.right.toInt(), stroke.bottom.toInt()
    )
    strokes.forEach{
        rect.union(Rect(
            it.left.toInt(), it.top.toInt(), it.right.toInt(), it.bottom.toInt()
        ))
    }
    return rect
}

data class SimplePoint(val x : Int, val y : Int)
data class SimplePointF(val x : Float, val y : Float)

fun pathToRegion(path: Path) : Region {
    val bounds = RectF()
    path.computeBounds(bounds, true)
   val region =  Region()
    region.setPath(
        path,
        Region(
            bounds.left.toInt(),
            bounds.top.toInt(),
            bounds.right.toInt(),
            bounds.bottom.toInt()
        )
    )
    return region
}

fun divideStrokesFromCut(strokes : List<Stroke>, cutLine : List<SimplePointF>): Pair<List<Stroke>, List<Stroke>>{
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

    val strokesOver : MutableList<Stroke> = mutableListOf()
    val strokesUnder : MutableList<Stroke> = mutableListOf()

    strokes.forEach{ stroke ->
        if (stroke.top > bounds.bottom) strokesUnder.add(stroke)
        else if (stroke.bottom < bounds.top) strokesOver.add(stroke)
        else {
            if(stroke.points.any { point ->
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

fun selectStrokesFromPath(strokes: List<Stroke>, path : Path) : List<Stroke>{
    val bounds = RectF()
    path.computeBounds(bounds, true)
    val region = pathToRegion(path)

    return strokes.filter {
        strokeBounds(it).intersect(bounds)
    }.filter() {it.points.any{region.contains(it.x.toInt(), it.y.toInt())}}
}