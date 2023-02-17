package com.example.inka

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.inka.db.PageRepository
import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.data.note.TouchPoint
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.math.max

// export page
fun exportPageToPdf(context: Context, bookId: Int, pageId: Int) {
    val appRepository = AppRepository(context)
    val page = appRepository.pageRepository.getWithStrokeById(pageId)
    val book = appRepository.bookRepository.getById(bookId)

    val pageWidth = EpdController.getEpdHeight() // TODO get page width
    var maxPointY = 0f

    page.strokes.forEach { s ->
        s.points.forEach { p ->
            if (p.y > maxPointY) {
                maxPointY = p.y
            }
        }
    }
    val pageHeight = max(maxPointY + 50, EpdController.getEpdWidth())

    var pdfDocument = PdfDocument()
    val pdfPage =
        pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), 1).create())
    var canvas = pdfPage.canvas

    drawDottedBg(canvas,0)

    page.strokes.forEach {
        val points = it.points.map {
            TouchPoint(
                it.x,
                it.y,
                it.pressure,
                it.size,
                it.tiltX,
                it.tiltY,
                it.timestamp
            )
        }

        drawStroke(
            canvas, it.stroke.pen, it.stroke.size, points
        )
    }
    pdfDocument.finishPage(pdfPage)


    val bookTitle = book.title
    val pageNaturalIndex = book.pageIds.indexOf(pageId) +1
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "inka/$bookTitle-$bookId/pages/$pageNaturalIndex.pdf")
    Files.createDirectories(Path(file.absolutePath).parent)


    try {
        // after creating a file name we will
        // write our PDF file to that location.
        pdfDocument.writeTo(FileOutputStream(file))

    } catch (e: Exception) {
        // below line is used
        // to handle error
        e.printStackTrace()
    }
    // after storing our pdf to that
    // location we are closing our PDF file.
    pdfDocument.close()


}

fun exportBook(context: Context, bookId: Int){

}