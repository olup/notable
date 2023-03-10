package com.olup.notable

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import java.io.File

class PageView(val context:Context, val page :PageModel, val width : Int, val height : Int, val scroll : Int) {
    val windowedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val windowedCanvas = Canvas(windowedBitmap)

    private fun loadBitmap(): Boolean {
        val imgFile = File(context.filesDir, "pages/previews/full/${page.pageId}")
        var imgBitmap: Bitmap? = null
        if (imgFile.exists()) {
            imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            if (imgBitmap != null) {
                windowedCanvas.drawBitmap(imgBitmap, 0f, 0f, Paint());
                println("Page rendered from cache")
                return true
            } else {
                println("Cannot read cache image")
            }
        } else {
            println("Cannot find cache image")
        }
        return false
    }
}