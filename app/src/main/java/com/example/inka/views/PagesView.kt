package com.example.inka

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import java.io.File

@ExperimentalFoundationApi
@Composable
fun PagesView(navController: NavController, bookId: String) {
    val appRepository = AppRepository(LocalContext.current)
    val book by appRepository.bookRepository.getByIdLive(bookId).observeAsState()
    if(book == null) return

    val pageIds = book!!.pageIds
    val openPageId = book?.openPageId

    Column(
        Modifier
            .fillMaxSize()
    ) {
        Topbar {
            Text(text = "Library", modifier = Modifier
                .noRippleClickable {
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
                    val isOpen = pageId == openPageId
                    PagePreview(modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .border(if (isOpen) 2.dp else 1.dp, Color.Black, RectangleShape)
                        .combinedClickable(
                            onClick = {
                                navController.navigate("books/$bookId/pages/$pageId")
                            },
                            onLongClick = {
                                navController.navigate("pages/$pageId/modal-settings")
                            },
                        ),
                        pageId
                    )

                }
            }
        }
    }
}
