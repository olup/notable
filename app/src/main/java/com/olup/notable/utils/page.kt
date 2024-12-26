package com.olup.notable

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.pdf.PdfDocument
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Environment
import android.provider.MediaStore
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
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun exportBook(context: Context, bookId: String): String {
    val book = BookRepository(context).getById(bookId) ?: return "Book ID not found"
    val pages = PageRepository(context)
    val message = exportPdf(context, "Notebooks", book.title) {
        book.pageIds.forEachIndexed { i, pageId -> writePage(i + 1, pages, pageId) }
    }
    copyBookPdfLinkForObsidian(context, bookId, book.title)
    return message
}

fun copyBookPdfLinkForObsidian(context: Context, bookId: String, bookName: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val textToCopy = """
        [[../attachments/Notable/Notebooks/${bookName}.pdf]]
        [[Notable Book Link][notable://book-${bookId}]]
    """.trimIndent()
    val clip = ClipData.newPlainText("Notable Book PDF Link", textToCopy)
    clipboard.setPrimaryClip(clip)
}

suspend fun exportPage(context: Context, pageId: String): String {
    val pages = PageRepository(context)
    return exportPdf(context, "pages", "notable-page-${pageId}") {
        writePage(1, pages, pageId)
    }
}

fun exportPageToPng(context: Context, pageId: String): String {
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
    return try {
        // Save the bitmap as PNG
        val filePath = Environment.getExternalStorageDirectory().toPath() /
                "org" / "attachments" / "Notable" / "Pages" / "notable-page-${pageId}.png"
        File(filePath.parent.toString()).mkdirs()
        FileOutputStream(filePath.toString()).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        copyPagePngLinkForObsidian(context, pageId)
        "Page exported successfully to PNG"
    } catch (e: SecurityException) {
        Log.e("ExportPng", "Permission error: ${e.message}")
        "Permission denied. Please allow storage access and try again."
    } catch (e: IOException) {
        Log.e("ExportPng", "I/O error while exporting Png: ${e.message}")
        "An error occurred while exporting the PNG."
    } catch (e: Exception) {
        Log.e("ExportPng", "Unexpected error: ${e.message}")
        "Unexpected error occurred. Please try again."
    }
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


fun exportPageToJpeg(context: Context, pageId: String): String {
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

    return try {
        // Save the bitmap as JPEG
        val filePath = Environment.getExternalStorageDirectory().toPath() /
                "org" / "attachments" / "Notable" / "Pages" / "notable-page-${pageId}.jpg"
        File(filePath.parent.toString()).mkdirs()
        FileOutputStream(filePath.toString()).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        bitmap.recycle()
        "Page exported successfully to Jpeg"
    } catch (e: SecurityException) {
        Log.e("ExportJpeg", "Permission error: ${e.message}")
        "Permission denied. Please allow storage access and try again."
    } catch (e: IOException) {
        Log.e("ExportJpeg", "I/O error while exporting Jpeg: ${e.message}")
        "An error occurred while exporting the JPG."
    } catch (e: Exception) {
        Log.e("ExportJpeg", "Unexpected error: ${e.message}")
        "Unexpected error occurred. Please try again."
    }
}


fun exportBookToPng(context: Context, bookId: String): String {
    return try {
        val book = BookRepository(context).getById(bookId) ?: return "Book ID not found"

        val pages = PageRepository(context)

        val dirPath = Environment.getExternalStorageDirectory().toPath() /
                "org" / "attachments" / "Notable" / "Notebooks" / book.title
        File(dirPath.toString()).mkdirs()

        book.pageIds.forEachIndexed { index, pageId ->
            val (page, strokes) = pages.getWithStrokeById(pageId)

            val strokeHeight =
                if (strokes.isEmpty()) 0 else strokes.maxOf(Stroke::bottom).toInt() + 50
            val strokeWidth =
                if (strokes.isEmpty()) 0 else strokes.maxOf(Stroke::right).toInt() + 50

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
        "Page exported successfully to Jpeg"
    } catch (e: SecurityException) {
        Log.e("ExportJpeg", "Permission error: ${e.message}")
        "Permission denied. Please allow storage access and try again."
    } catch (e: IOException) {
        Log.e("ExportJpeg", "I/O error while exporting Jpeg: ${e.message}")
        "An error occurred while exporting the JPG.."
    } catch (e: Exception) {
        Log.e("ExportJpeg", "Unexpected error: ${e.message}")
        "Unexpected error occurred. Please try again."
    }
}

private suspend fun exportPdf(
    context: Context,
    dir: String,
    name: String,
    write: PdfDocument.() -> Unit
): String = withContext(Dispatchers.IO) {
    try {
        val document = PdfDocument()
        document.write()

        // Prepare content values for the Media Store
        val contentValues = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, "$name.pdf")
            put(MediaStore.Files.FileColumns.MIME_TYPE, "application/pdf")
            put(
                MediaStore.Files.FileColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOCUMENTS + "/Notable"
            )
        }

        // Insert the file into the Media Store
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            ?: throw IOException("Failed to create Media Store entry")

        // Write the PDF file to the specified URI
        resolver.openOutputStream(uri)?.use { outputStream ->
            Log.d("ExportPdf", "Saving PDF to: $outputStream")
            document.writeTo(outputStream)
        }
        document.close()
        "Page exported successfully to PDF"
    } catch (e: SecurityException) {
        Log.e("ExportPdf", "Permission error: ${e.message}")
        "Permission denied. Please allow storage access and try again."
    } catch (e: IOException) {
        Log.e("ExportPdf", "I/O error while exporting PDF: ${e.message}")
        "An error occurred while exporting the PDF."
    } catch (e: Exception) {
        Log.e("ExportPdf", "Unexpected error: ${e.message}")
        "Unexpected error occurred. Please try again."
    }
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
