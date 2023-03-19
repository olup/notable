package com.olup.notable

import android.content.Context
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.unit.IntOffset
import com.olup.notable.db.BookRepository
import com.olup.notable.db.PageRepository
import com.olup.notable.db.Stroke
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.math.max

fun computePageHeight(strokes : List<Stroke>, minHeight : Int):Int{
    if(strokes.isEmpty()) return minHeight
    else return max(strokes.maxOf{ it.bottom }.toInt() + 50, minHeight)
}

fun exportBook(context : Context, bookId: String){
    val book = BookRepository(context).getById(bookId) ?: return
    val pageRepository = PageRepository(context)

    var document = PdfDocument()

    for (pageId in book.pageIds){
        val (page, strokes) = pageRepository.getWithStrokeById(pageId)
        val height = computePageHeight(strokes, SCREEN_HEIGHT)
        var documentPage = document.startPage(PdfDocument.PageInfo.Builder(SCREEN_WIDTH, height, 1).create())

        drawBg(documentPage.canvas, page.nativeTemplate, 0)
        for(stroke in strokes){
            drawStroke(documentPage.canvas, stroke, IntOffset(0,0))
        }

        document.finishPage(documentPage)
    }

    val filePath = Paths.get(android.os.Environment.getExternalStorageDirectory().toString(), android.os.Environment.DIRECTORY_DOCUMENTS.toString(), "notable", "notebooks",  "${book.title}}.pdf")
    Files.createDirectories(filePath.parent)
    document.writeTo( FileOutputStream(filePath.absolutePathString()))
    document.close()
}

fun exportPage(context:Context, pageId : String){
    val (page, strokes) = PageRepository(context).getWithStrokeById(pageId)

    var document = PdfDocument()
    val height = computePageHeight(strokes, SCREEN_HEIGHT)
    var documentPage = document.startPage(PdfDocument.PageInfo.Builder(SCREEN_WIDTH, height, 1).create())
    drawBg(documentPage.canvas, page.nativeTemplate ,0)
    for(stroke in strokes){
        drawStroke(documentPage.canvas, stroke, IntOffset(0,0))
    }
    document.finishPage(documentPage)
    val filePath = Paths.get(android.os.Environment.getExternalStorageDirectory().toString(), android.os.Environment.DIRECTORY_DOCUMENTS.toString(), "notable", "pages",  "notable-page-${pageId.takeLast(6)}.pdf")
    Files.createDirectories(filePath.parent)
    document.writeTo( FileOutputStream(filePath.absolutePathString()))
    document.close()
}
