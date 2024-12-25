package com.olup.notable

import android.graphics.*
import io.shipbook.shipbooksdk.Log
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toOffset
import com.olup.notable.db.Stroke
import com.onyx.android.sdk.data.note.ShapeCreateArgs
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.NeoBrushPen
import com.onyx.android.sdk.pen.NeoCharcoalPen
import com.onyx.android.sdk.pen.NeoFountainPen
import kotlin.math.abs


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
        this.alpha = 30

    }

    val path = pointsToPath(points.map { SimplePointF(it.x, it.y) })

    canvas.drawPath(path, copyPaint)
}

fun drawStroke(canvas: Canvas, stroke: Stroke, offset: IntOffset) {
    //canvas.save()
    //canvas.translate(offset.x.toFloat(), offset.y.toFloat())

    val paint = Paint().apply {
        color = stroke.color
        this.strokeWidth = stroke.size
    }

    val points = strokeToTouchPoints(offsetStroke(stroke, offset.toOffset()))

    when (stroke.pen) {
        Pen.BALLPEN -> drawBallPenStroke(canvas, paint, stroke.size, points)
        Pen.REDBALLPEN -> drawBallPenStroke(canvas, paint, stroke.size, points)
        Pen.GREENBALLPEN -> drawBallPenStroke(canvas, paint, stroke.size, points)
        Pen.BLUEBALLPEN -> drawBallPenStroke(canvas, paint, stroke.size, points)
        Pen.PENCIL -> NeoCharcoalPen.drawNormalStroke(
            null, canvas, paint, points, stroke.color, stroke.size, ShapeCreateArgs(), Matrix(),false
        )
        Pen.BRUSH -> NeoBrushPen.drawStroke(canvas, paint, points, stroke.size, pressure, false)
        Pen.MARKER -> drawMarkerStroke(canvas, paint, stroke.size, points)
        Pen.FOUNTAIN -> NeoFountainPen.drawStroke(
            canvas, paint, points, 1f, stroke.size, pressure, false
        )
    }
    //canvas.restore()
}


const val padding = 0
const val lineHeight = 50
const val dotSize = 4f

fun drawLinedBg(canvas: Canvas, scroll: Int) {
    val height = canvas.height
    val width = canvas.width

    // white bg
    canvas.drawColor(Color.WHITE)

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
    canvas.drawColor(Color.WHITE)

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
    Log.i(TAG, "Drawing BG")
    val height = canvas.height
    val width = canvas.width

    // white bg
    canvas.drawColor(Color.WHITE)

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

fun drawBg(canvas: Canvas, nativeTemplate: String, scroll: Int){
    when(nativeTemplate){
        "blank" -> canvas.drawColor(Color.WHITE)
        "dotted" -> drawDottedBg(canvas, scroll)
        "lined" -> drawLinedBg(canvas, scroll)
        "squared" -> drawSquaredBg(canvas, scroll)
     }
}

val selectPaint = Paint().apply {
    strokeWidth = 5f
    style = Paint.Style.STROKE
    pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
    isAntiAlias = true
    color = Color.GRAY
}