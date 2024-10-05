package com.olup.notable.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.olup.notable.AppRepository
import com.olup.notable.AppSettings
import com.olup.notable.EditorView
import com.olup.notable.R
import com.olup.notable.db.Page
import com.olup.notable.ui.theme.InkaTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingEditorView(
    navController: NavController,
    bookId: String? = null,
    pageId: String? = null,
    onDismissRequest: () -> Unit
) {
    var isFullScreen by remember { mutableStateOf(false) } // State for full-screen mode

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        InkaTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize() // Ensure it fills the entire screen
                    .background(Color.White)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(Color.White)
                    ) {
                        if (pageId != null) {
                            EditorView(
                                navController = navController,
                                _bookId = null,
                                _pageId = pageId
                            )
                        } else if (bookId != null) {
                            // get first page of notebook and use it as pageId
                            val appRepository = AppRepository(LocalContext.current)
                            val firstPageId = appRepository.bookRepository.getById(bookId)?.pageIds?.firstOrNull()
                            if (firstPageId == null) {
                                // new page uuid
                                val page = Page(
                                    notebookId = null,
                                    parentFolderId = null,
                                    nativeTemplate = appRepository.kvProxy.get(
                                        "APP_SETTINGS", AppSettings.serializer()
                                    )?.defaultNativeTemplate ?: "blank"
                                )
                                EditorView(
                                    navController = navController,
                                    _bookId = bookId,
                                    _pageId = page.id
                                )
                            } else {
                                EditorView(
                                    navController = navController,
                                    _bookId = bookId,
                                    _pageId = firstPageId
                                )
                            }

                            
                        }
                    }
                }
            }
        }
    }
}
