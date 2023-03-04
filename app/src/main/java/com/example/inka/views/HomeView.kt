package com.example.inka

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.inka.db.BookRepository
import com.example.inka.db.Notebook
import com.example.inka.db.Page
import java.io.File

@ExperimentalFoundationApi
@Composable
fun Library(navController: NavController) {
    val appRepository = AppRepository(LocalContext.current)
    val books by appRepository.bookRepository.getAll().observeAsState()
    val singlePages by appRepository.pageRepository.getSinglePages().observeAsState()

    Column(
        Modifier.fillMaxSize()
    ) {
        Topbar(
        ) {
            Text(text = "Add notebook",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .noRippleClickable {
                        appRepository.bookRepository.create(
                            Notebook()
                        )
                    }
                    .padding(10.dp))
            Text(text = "Add quick page",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .noRippleClickable {
                        val page = Page(notebookId = null)
                        appRepository.pageRepository.create(page)
                        navController.navigate("pages/${page.id}")
                    }
                    .padding(10.dp))
        }
        Column(
            Modifier.padding(10.dp)
        ) {
            if (singlePages != null && singlePages?.size != 0) {
                Text(text = "Quick pages")
                Spacer(Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    items(singlePages!!.reversed()) { page ->
                        val pageId = page.id
                        PagePreview(
                            modifier = Modifier
                                .width(100.dp)
                                .aspectRatio(3f / 4f)
                                .border(1.dp, Color.Black, RectangleShape)
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("pages/$pageId")
                                    },
                                    onLongClick = {
                                        navController.navigate("pages/$pageId/modal-settings")
                                    },
                                ),
                            pageId = pageId
                        )
                    }

                }
                Spacer(Modifier.height(10.dp))
            }

            Text(text = "Notebooks")
            Spacer(Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(books ?: listOf<Notebook>()) { item ->

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .border(1.dp, Color.Black, RectangleShape)
                            .background(Color.White)
                            .clip(RoundedCornerShape(2))
                            .combinedClickable(
                                onClick = {
                                    val bookId = item.id
                                    val pageId = item.openPageId
                                    navController.navigate("books/$bookId/pages/$pageId")
                                },
                                onLongClick = {
                                    val bookId = item.id
                                    navController.navigate("books/$bookId/modal-settings")
                                },
                            )
                    ) {
                        Text(
                            text = item.pageIds.size.toString(),
                            modifier = Modifier
                                .background(Color.Black)
                                .padding(5.dp),
                            color = Color.White
                        )
                        Row(Modifier.fillMaxSize()) {
                            Text(
                                text = item.title,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .align(CenterVertically)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}



