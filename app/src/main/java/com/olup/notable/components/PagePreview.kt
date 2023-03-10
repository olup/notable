package com.olup.notable

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import java.io.File

@Composable
fun PagePreview(modifier: Modifier, pageId: String){
    val imgFile = File(LocalContext.current.filesDir, "pages/previews/thumbs/$pageId")

    var imgBitmap: Bitmap? = null
    if (imgFile.exists()) {
        imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
    }

    if(imgBitmap == null) {
        Text("No preview available yet", modifier.then(
                Modifier.padding(10.dp)
            ))
    }else {
        Image(
            painter = rememberAsyncImagePainter(model = imgBitmap),
            contentDescription = "Image",
            modifier = modifier
        )
    }
}