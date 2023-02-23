package com.example.inka

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.inka.noRippleClickable
import java.io.File

@Composable
fun PagePreview(modifier: Modifier, pageId: String){
    val imgFile = File(LocalContext.current.filesDir, "pages/previews/thumbs/$pageId")

    var imgBitmap: Bitmap? = null
    if (imgFile.exists()) {
        imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
    }

    Image(
        painter = rememberAsyncImagePainter(model = imgBitmap),
        contentDescription = "Image",
        modifier = modifier
    )
}