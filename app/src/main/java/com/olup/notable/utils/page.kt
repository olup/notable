package com.olup.notable

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.compose.ui.unit.IntOffset
import com.olup.notable.db.BookRepository
import com.olup.notable.db.PageRepository
import com.olup.notable.db.Stroke
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import kotlin.math.max

fun exportBook(context: Context, bookId: String) {
    val book = BookRepository(context).getById(bookId) ?: return
    val pageRepository = PageRepository(context)
    exportPdf("notebooks", book.title) {
        book.pageIds.forEachIndexed { i, pageId ->
            addPage(pageRepository, pageId, i + 1)
        }
    }
}

fun exportPage(context: Context, pageId: String) {
    val pageRepository = PageRepository(context)
    exportPdf("pages", "notable-page-${pageId.takeLast(6)}") {
        addPage(pageRepository, pageId, 1)
    }
}

private val ExportRoot
    get() = Environment.getExternalStorageDirectory().toPath() /
            Environment.DIRECTORY_DOCUMENTS / "notable"

private inline fun exportPdf(folder: String, title: String, write: PdfDocument.() -> Unit) {
    val document = PdfDocument()
    document.write()
    val filePath = ExportRoot / folder / "$title.pdf"
    Files.createDirectories(filePath.parent)
    FileOutputStream(filePath.absolutePathString()).use(document::writeTo)
    document.close()
}

private fun PdfDocument.addPage(pageRepository: PageRepository, pageId: String, pageNumber: Int) {
    val (page, strokes) = pageRepository.getWithStrokeById(pageId)
    val height = computePageHeight(strokes, SCREEN_HEIGHT)
    val documentPage =
        startPage(PdfDocument.PageInfo.Builder(SCREEN_WIDTH, height, pageNumber).create())

    drawBg(documentPage.canvas, page.nativeTemplate, 0)
    for (stroke in strokes) {
        drawStroke(documentPage.canvas, stroke, IntOffset(0, 0))
    }

    finishPage(documentPage)
}

fun computePageHeight(strokes: List<Stroke>, minHeight: Int): Int =
    if (strokes.isEmpty()) minHeight
    else max(strokes.maxOf { it.bottom }.toInt() + 50, minHeight)
