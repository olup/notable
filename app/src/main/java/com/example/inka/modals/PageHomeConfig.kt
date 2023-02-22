package com.example.inka

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.example.inka.db.BookRepository
import com.onyx.android.sdk.utils.BroadcastHelper.App
import java.io.File


@Composable
fun PageHomeConfigDialog(navController: NavController, pageId: Int) {
    val appRepository = AppRepository(LocalContext.current)
    val page = appRepository.pageRepository.getById(pageId)
    val context = LocalContext.current

    Box(
        Modifier
            .fillMaxSize()
    )
    {
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
                Text(text = "Page setting")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.End) {
                    Text(text = "Delete",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.noRippleClickable {
                            deletePage(context, pageId)

                            // remove refernce to page from book
                            // TODO move this to repository
                            if (page?.notebookId != null) {
                                val book = appRepository.bookRepository.getById(page.notebookId)!!
                                val updatedBook =
                                    book.copy(pageIds = book.pageIds.filter { it != page.id }, openPageId = if(book.openPageId==page.id) null else book.openPageId )
                                appRepository.bookRepository.update(updatedBook)
                            }

                            navController.navigateUp()
                        })

                }
            }
        }
    }
}