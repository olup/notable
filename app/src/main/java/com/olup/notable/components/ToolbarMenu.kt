package com.olup.notable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ToolbarMenu(
    navController: NavController,
    state: EditorState,
    onClose: () -> Unit,
    onPageSettingsOpen: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackManager = SnackContext.current
    val page = AppRepository(context).pageRepository.getById(state.pageId)!!
    val parentFolder =
        if (page.notebookId != null)
            AppRepository(context).bookRepository.getById(page.notebookId!!)!!
                .parentFolderId
        else page.parentFolderId

    Popup(
        alignment = Alignment.TopEnd,
        onDismissRequest = { onClose() },
        offset =
        IntOffset(
            convertDpToPixel(-10.dp, context).toInt(),
            convertDpToPixel(50.dp, context).toInt()
        ),
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            Modifier
                .border(1.dp, Color.Black, RectangleShape)
                .background(Color.White)
                .width(IntrinsicSize.Max)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .noRippleClickable {
                        navController.navigate(
                            route =
                            if (parentFolder != null) "library?folderId=${parentFolder}"
                            else "library"
                        )
                    }
            ) { Text("Library") }
            Box(
                Modifier
                    .padding(10.dp)
                    .noRippleClickable {
                        scope.launch {
                            val removeSnack =
                                snackManager.displaySnack(
                                    SnackConf(text = "Exporting the page to PDF...")
                                )
                            delay(10L) // Why do I need this ?

                            exportPage(context, state.pageId)

                            removeSnack()
                            snackManager.displaySnack(
                                SnackConf(text = "Page exported successfully to PDF", duration = 2000)
                            )
                            onClose()
                        }
                    }
            ) { Text("Export page to PDF") }

            Box(
                Modifier
                    .padding(10.dp)
                    .noRippleClickable {
                        scope.launch {
                            val removeSnack =
                                snackManager.displaySnack(
                                    SnackConf(text = "Exporting the page to PNG...")
                                )
                            delay(10L) // Why do I need this ?

                            exportPageToPng(context, state.pageId)

                            removeSnack()
                            snackManager.displaySnack(
                                SnackConf(text = "Page exported successfully to PNG", duration = 2000)
                            )
                            onClose()
                        }
                    }
            ) { Text("Export page to PNG") }

            if (state.bookId != null)
                Box(
                    Modifier
                        .padding(10.dp)
                        .noRippleClickable {
                            scope.launch {
                                val removeSnack =
                                    snackManager.displaySnack(
                                        SnackConf(
                                            text = "Exporting the book to PDF...",
                                            id = "exportSnack"
                                        )
                                    )
                                delay(10L) // Why do I need this ?

                                exportBook(context, state.bookId ?: return@launch)

                                removeSnack()
                                snackManager.displaySnack(
                                    SnackConf(
                                        text = "Book exported successfully to PDF",
                                        duration = 3000
                                    )
                                )
                                onClose()
                            }
                        }
                ) { Text("Export book to PDF") }

            if (state.bookId != null)
                Box(
                    Modifier
                        .padding(10.dp)
                        .noRippleClickable {
                                scope.launch {
                                    val removeSnack =
                                        snackManager.displaySnack(
                                            SnackConf(
                                                text = "Exporting the book to PNG...",
                                                id = "exportSnack"
                                            )
                                        )
                                    delay(10L) // Why do I need this ?

                                    exportBookToPng(context, state.bookId ?: return@launch)

                                    removeSnack()
                                    snackManager.displaySnack(
                                        SnackConf(
                                            text = "Book exported successfully to PNG",
                                            duration = 3000
                                        )
                                    )
                                    onClose()
                                }
                            }
                    ) { Text("Export book to PNG") }

            if (state.selectionState.selectedBitmap != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color.Black))
                Box(
                    Modifier
                        .padding(10.dp)
                        .noRippleClickable {
                            shareBitmap(context, state.selectionState.selectedBitmap!!)
                        }
                ) { Text("Share selection") }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color.Black))
            Box(
                Modifier
                    .padding(10.dp)
                    .noRippleClickable {
                        onPageSettingsOpen()
                        onClose()
                    }
            ) { Text("Page Settings") }

            /*Box(
                Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color.Black)
            )
            Box(Modifier.padding(10.dp)) {
                Text("Refresh page")
            }*/
        }
    }
}
