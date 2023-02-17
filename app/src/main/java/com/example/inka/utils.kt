package com.example.inka

import android.content.Context
import android.graphics.*
import com.example.inka.db.StrokeWithPoints
import com.onyx.android.sdk.data.note.TouchPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.io.path.Path
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

fun drawPage(context: Context, canvas: Canvas, canvasOffset: Int, pageId: Int, pageOffset: Int, sectionHeight:Int, strokes: List<StrokeWithPoints>?) {

    val appRepository = AppRepository(context)

    canvas.save();
    canvas.clipRect(0,canvasOffset, canvas.width, canvasOffset+sectionHeight  );

   val timeToBg = measureTimeMillis {
        drawDottedBg(canvas, pageOffset - canvasOffset)
    }
    println("Took $timeToBg to draw the BG")

    var str: List<StrokeWithPoints> = strokes ?: listOf()

    if(strokes == null) {
        val timeToQuery = measureTimeMillis {
            str = appRepository.pageRepository.getWithStrokeById(pageId).strokes
        }
        println("Took $timeToQuery to fetch all page's points")
    }



    val timeToDraw = measureTimeMillis {
        str.forEach {
            var shouldDraw = false
            val points = it.points.map {
                if (it.y > pageOffset && it.y < pageOffset + sectionHeight) {
                    shouldDraw = true
                }
                TouchPoint(
                    it.x,
                    it.y - pageOffset + canvasOffset,
                    it.pressure,
                    it.size,
                    it.tiltX,
                    it.tiltY,
                    it.timestamp
                )
            }


            if (shouldDraw) {
                drawStroke(
                    canvas, it.stroke.pen, it.stroke.size, points
                )
            }
        }
    }

    println("Took $timeToDraw to draw all page's points")

    canvas.restore();
}

fun pageBitmapToFile(context: Context, bitmap: Bitmap, pageId: Int) {
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

fun renderCachedPageToCanvas(context: Context, canvas:Canvas, pageId: Int): Boolean {
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
    }else{
        println("Cannot find cache image")
    }

    return false
}
