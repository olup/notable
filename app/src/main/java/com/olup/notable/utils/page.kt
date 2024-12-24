package com.olup.notable
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.pdf.PdfDocument
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Environment
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.asAndroidBitmap
import com.olup.notable.db.BookRepository
import com.olup.notable.db.PageRepository
import com.olup.notable.db.Stroke
import io.shipbook.shipbooksdk.Log
import java.io.FileOutputStream
import java.io.File
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.io.path.div

fun exportBook(context: Context, bookId: String) {
    val book = BookRepository(context).getById(bookId) ?: return
    val pages = PageRepository(context)
    exportPdf("Notebooks", book.title) {
        book.pageIds.forEachIndexed { i, pageId -> writePage(i + 1, pages, pageId) }
    }
    copyBookPdfLinkForObsidian(context, bookId, book.title)
}

fun copyBookPdfLinkForObsidian(context:Context, bookId: String, bookName: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val textToCopy = """
        [[../attachments/Notable/Notebooks/${bookName}.pdf]]
        [[Notable Book Link][notable://book-${bookId}]]
    """.trimIndent()
    val clip = ClipData.newPlainText("Notable Book PDF Link", textToCopy)
    clipboard.setPrimaryClip(clip)
}

fun exportPage(context: Context, pageId: String) {
    val pages = PageRepository(context)
    exportPdf("pages", "notable-page-${pageId}") {
        writePage(1, pages, pageId)
    }
}

fun exportPageToPng(context: Context, pageId: String) {
    val pages = PageRepository(context)
    val (page, strokes) = pages.getWithStrokeById(pageId)

    val strokeHeight = if (strokes.isEmpty()) 0 else strokes.maxOf(Stroke::bottom).toInt() + 50
    val strokeWidth = if (strokes.isEmpty()) 0 else strokes.maxOf(Stroke::right).toInt() + 50

    val height = strokeHeight.coerceAtLeast(SCREEN_HEIGHT)
    val width = strokeWidth.coerceAtLeast(SCREEN_WIDTH)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Draw background
    drawBg(canvas, page.nativeTemplate, 0)

    // Draw strokes
    for (stroke in strokes) {
        drawStroke(canvas, stroke, IntOffset(0, 0))
    }

    // Save the bitmap as PNG
    val filePath = Environment.getExternalStorageDirectory().toPath() /
            "org" / "attachments" / "Notable" / "Pages" / "notable-page-${pageId}.png"
    File(filePath.parent.toString()).mkdirs()
    FileOutputStream(filePath.toString()).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    bitmap.recycle()
    copyPagePngLinkForObsidian(context, pageId)
}

fun copyPagePngLinkForObsidian(context: Context, pageId: String) {
   val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
   val textToCopy = """
       [[../attachments/Notable/Pages/notable-page-${pageId}.png]]
       [[Notable Link][notable://page-${pageId}]]
   """.trimIndent()
   val clip = ClipData.newPlainText("Notable Page Link", textToCopy)
   clipboard.setPrimaryClip(clip)
}


fun exportPageToJpeg(context: Context, pageId: String) {
    val pages = PageRepository(context)
    val (page, strokes) = pages.getWithStrokeById(pageId)

    val strokeHeight = if (strokes.isEmpty()) 0 else strokes.maxOf(Stroke::bottom).toInt() + 50
    val strokeWidth = if (strokes.isEmpty()) 0 else strokes.maxOf(Stroke::right).toInt() + 50

    val height = strokeHeight.coerceAtLeast(SCREEN_HEIGHT)
    val width = strokeWidth.coerceAtLeast(SCREEN_WIDTH)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Draw background
    drawBg(canvas, page.nativeTemplate, 0)

    // Draw strokes
    for (stroke in strokes) {
        drawStroke(canvas, stroke, IntOffset(0, 0))
    }

    // Save the bitmap as JPEG
    val filePath = Environment.getExternalStorageDirectory().toPath() /
            "org" / "attachments" / "Notable" / "Pages" / "notable-page-${pageId}.jpg"
    File(filePath.parent.toString()).mkdirs()
    FileOutputStream(filePath.toString()).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    }
    bitmap.recycle()
}


fun exportBookToPng(context: Context, bookId: String) {
    val book = BookRepository(context).getById(bookId) ?: return
    val pages = PageRepository(context)
    
    val dirPath = Environment.getExternalStorageDirectory().toPath() /
            "org" / "attachments" / "Notable" / "Notebooks" / book.title
    File(dirPath.toString()).mkdirs()

    book.pageIds.forEachIndexed { index, pageId ->
        val (page, strokes) = pages.getWithStrokeById(pageId)

        val strokeHeight = if (strokes.isEmpty()) 0 else strokes.maxOf(Stroke::bottom).toInt() + 50
        val strokeWidth = if (strokes.isEmpty()) 0 else strokes.maxOf(Stroke::right).toInt() + 50

        val height = strokeHeight.coerceAtLeast(SCREEN_HEIGHT)
        val width = strokeWidth.coerceAtLeast(SCREEN_WIDTH)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBg(canvas, page.nativeTemplate, 0)

        for (stroke in strokes) {
            drawStroke(canvas, stroke, IntOffset(0, 0))
        }

        val filePath = dirPath / "notable-page-${pageId}.png"
        FileOutputStream(filePath.toString()).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
    }
}

private inline fun exportPdf(dir: String, name: String, write: PdfDocument.() -> Unit) {
    val document = PdfDocument()
    document.write()
    val filePath = Environment.getExternalStorageDirectory().toPath() /
            "org" / "attachments" / "Notable" / dir / "$name.pdf"
    Files.createDirectories(filePath.parent)
    FileOutputStream(filePath.absolutePathString()).use(document::writeTo)
    document.close()
}

private fun PdfDocument.writePage(number: Int, repo: PageRepository, id: String) {
    val (page, strokes) = repo.getWithStrokeById(id)

    val strokeHeight = if (strokes.isEmpty()) 0 else strokes.maxOf(Stroke::bottom).toInt() + 50
    val strokeWidth = if (strokes.isEmpty()) 0 else strokes.maxOf(Stroke::right).toInt() + 50

    val height = strokeHeight.coerceAtLeast(SCREEN_HEIGHT) // todo do not rely on this anymore
    val width = strokeWidth.coerceAtLeast(SCREEN_WIDTH) // todo do not rely on this anymore

    val documentPage =
        startPage(PdfDocument.PageInfo.Builder(width, height, number).create())

    drawBg(documentPage.canvas, page.nativeTemplate, 0)

    for (stroke in strokes) {
        drawStroke(documentPage.canvas, stroke, IntOffset(0, 0))
    }

    finishPage(documentPage)
}
