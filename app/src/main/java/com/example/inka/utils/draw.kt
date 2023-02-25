package com.example.inka

import android.graphics.*
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
        this.alpha = 100
    }

    val path = pointsToPath(points.map { Pair(it.x, it.y) })

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
