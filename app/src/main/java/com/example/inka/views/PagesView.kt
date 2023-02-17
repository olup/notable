package com.example.inka

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import com.example.inka.db.BookRepository
import com.example.inka.db.Notebook
import java.io.File

@ExperimentalFoundationApi
@Composable
fun PagesView(navController: NavController, bookId: Int) {
    val appRepository = AppRepository(LocalContext.current)
    val pageIds = appRepository.bookRepository.getById(bookId).pageIds

    Column(
        Modifier
            .fillMaxSize()
    ) {
        Topbar {
            Text(text = "Library", modifier = Modifier
                .clickable {
                    navController.navigate("library")
                }
                .padding(10.dp))
        }
        Column(
            Modifier
                .padding(10.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(pageIds) { pageId ->

                    val imgFile = File(LocalContext.current.filesDir, "pages/previews/thumbs/$pageId")

                    // on below line we are checking if the image file exist or not.
                    var imgBitmap: Bitmap? = null
                    if (imgFile.exists()) {
                        // on below line we are creating an image bitmap variable
                        // and adding a bitmap to it from image file.
                        println("exists")
                        imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    }

                    Image(
                        // on the below line we are specifying the drawable image for our image.
                        // painter = painterResource(id = courseList[it].languageImg),
                        painter = rememberAsyncImagePainter(model = imgBitmap),

                        // on the below line we are specifying
                        // content description for our image
                        contentDescription = "Image",

                        // on the below line we are setting the height
                        // and width for our image.
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .border(2.dp, Color.Black, RectangleShape)
                            .clickable { navController.navigate("book/$bookId/editor/$pageId") }
                    )

                }
            }
        }
    }
}
