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
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult


import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember


import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider

suspend fun exportBook(context: Context, bookId: String): String {
    val book = BookRepository(context).getById(bookId) ?: return "Book ID not found"
    val pages = PageRepository(context)
    val message = exportPdf(context, "Notebooks", book.title) {
        book.pageIds.forEachIndexed { i, pageId -> writePage(context, i + 1, pages, pageId) }
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
        writePage(context, 1, pages, pageId)
    }
}

fun exportPageToPng(context: Context, pageId: String): String {
    val pages = PageRepository(context)
    val (page, strokes) = pages.getWithStrokeById(pageId)
    val (page2, images) = pages.getWithImageById(pageId)

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
    for (image in images) {
        drawImage(context, canvas, image, IntOffset(0, 0))
    }

    //TODO Draw images
    if (true) {
        val cachePath = File(context.cacheDir, "images")
        Log.i(TAG, cachePath.toString())
        cachePath.mkdirs()
        try {
            val stream =
                FileOutputStream("$cachePath/share.png")
            bitmap.compress(
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

            context.grantUriPermission(
                "android",
                contentUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        }

        ContextCompat.startActivity(
            context,
            Intent.createChooser(sendIntent, "Choose an app"),
            null
        )
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
    //TODO Draw images

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
            //TODO Draw images

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


private fun PdfDocument.writePage(context: Context, number: Int, repo: PageRepository, id: String) {
    val (page, strokes) = repo.getWithStrokeById(id)
    //TODO: improve that function
    val (page2, images) = repo.getWithImageById(id)

    // Define the target page size (A4 in points: 595 x 842)
    val A4_WIDTH = 595
    val A4_HEIGHT = 842

    val strokeHeight = if (strokes.isEmpty()) 0 else strokes.maxOf(Stroke::bottom).toInt() + 50
    val strokeWidth = if (strokes.isEmpty()) 0 else strokes.maxOf(Stroke::right).toInt() + 50
    val scaleFactor = A4_WIDTH.toFloat() / SCREEN_WIDTH

    // todo do not rely on this anymore
    // I slightly modified it, should be better
    val contentHeight = strokeHeight.coerceAtLeast(SCREEN_HEIGHT)
    val pageHeight = (contentHeight * scaleFactor).toInt()
    val contentWidth = strokeWidth.coerceAtLeast(SCREEN_WIDTH)


    val documentPage =
        startPage(PdfDocument.PageInfo.Builder(A4_WIDTH, pageHeight, number).create())

    // Center content on the A4 page
    val offsetX = (A4_WIDTH - (contentWidth * scaleFactor)) / 2
    val offsetY = (A4_HEIGHT - (contentHeight * scaleFactor)) / 2

    documentPage.canvas.scale(scaleFactor, scaleFactor)
    drawBg(documentPage.canvas, page.nativeTemplate, 0, scaleFactor)

    for (stroke in strokes) {
        drawStroke(documentPage.canvas, stroke, IntOffset(0, 0))
    }

    for (image in images) {
        drawImage(context, documentPage.canvas, image, IntOffset(0, 0))
    }

    finishPage(documentPage)
}


/**
 * Converts a URI to a Bitmap using the provided [context] and [uri].
 *
 * @param context The context used to access the content resolver.
 * @param uri The URI of the image to be converted to a Bitmap.
 * @return The Bitmap representation of the image, or null if conversion fails.
 * https://medium.com/@munbonecci/how-to-display-an-image-loaded-from-the-gallery-using-pick-visual-media-in-jetpack-compose-df83c78a66bf
 */
fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    // Obtain the content resolver from the context
    val contentResolver: ContentResolver = context.contentResolver

    // Check the API level to use the appropriate method for decoding the Bitmap
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // For Android P (API level 28) and higher, use ImageDecoder to decode the Bitmap
        val source = ImageDecoder.createSource(contentResolver, uri)
        ImageDecoder.decodeBitmap(source)
    } else {
        // For versions prior to Android P, use BitmapFactory to decode the Bitmap
        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            Bitmap.createBitmap(BitmapFactory.decodeStream(stream))
        }
        bitmap
    }
}
