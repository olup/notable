package com.olup.notable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.olup.notable.db.BookRepository


@Composable
fun NotebookConfigDialog(navController: NavController, bookId: String) {
    val bookRepository = BookRepository(LocalContext.current)
    val book = bookRepository.getById(bookId)?:return
    val context = LocalContext.current

    var bookTitle by remember {
        mutableStateOf(book.title)
    }

    val focusManager = LocalFocusManager.current

    Box(
        Modifier
            .fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(0.dp)
                .noRippleClickable { navController.navigateUp() },
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
                        modifier = Modifier.noRippleClickable {
                            deleteBook(context, bookId)
                            navController.navigateUp()
                        })
                    Spacer(Modifier.weight(1.0f))
                    Text(
                        text = "Ok",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.noRippleClickable {
                            val updatedBook = book.copy(title = bookTitle)
                            bookRepository.update(updatedBook)
                            navController.navigateUp()
                        })

                }
            }
        }
    }
}