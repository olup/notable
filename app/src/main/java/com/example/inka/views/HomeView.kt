package com.example.inka

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
import com.example.inka.db.BookRepository
import com.example.inka.db.Notebook

@ExperimentalFoundationApi
@Composable
fun Library(navController: NavController) {
    val bookRepository = BookRepository(LocalContext.current)
    val books by bookRepository.getAll().observeAsState()

    Column(
        Modifier.fillMaxSize()
    ) {
        Topbar(
        ) {
            Text(text = "Add notebook",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clickable {
                        bookRepository.create(
                            Notebook()
                        )
                    }
                    .padding(10.dp))
        }
        Column(
            Modifier.padding(10.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
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
                                    navController.navigate("book/$bookId/editor/$pageId")
                                },
                                onLongClick = {
                                    val bookId = item.id
                                    navController.navigate("book/edit/$bookId")
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

@Composable
fun BookEditDialog(navController: NavController, bookId: Int) {
    val bookRepository = BookRepository(LocalContext.current)
    val book = bookRepository.getById(bookId)

    var bookTitle by remember {
        mutableStateOf(book.title)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val focusManager = LocalFocusManager.current

    Box(
        Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource, indication = null
            ) { navController.navigateUp() }) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(0.dp)
                .clickable(interactionSource = interactionSource, indication = null) { },
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .fillMaxWidth()
                    .border(2.dp, Color.Black, RectangleShape)
                    .padding(20.dp)
            ) {
                Text(text = "Notebook setting")
                Spacer(Modifier.height(10.dp))
                BasicTextField(
                    value = bookTitle,
                    onValueChange = { bookTitle = it },
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                )

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.End) {

                    Text(text = "Delete",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable {
                                bookRepository.delete(bookId)
                                navController.navigateUp()
                            })
                    Spacer(Modifier.weight(1.0f))
                    Text(text = "Ok", textAlign = TextAlign.Center, modifier = Modifier.clickable {
                            val updatedBook = book.copy(title = bookTitle)
                            bookRepository.update(updatedBook)
                            navController.navigateUp()
                        })

                }
            }
        }
    }
}



