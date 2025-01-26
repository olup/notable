package com.olup.notable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
    val snackManager = LocalSnackContext.current
    val page = AppRepository(context).pageRepository.getById(state.pageId)!!
    val parentFolder =
        if (page.notebookId != null)
            AppRepository(context).bookRepository.getById(page.notebookId)!!
                .parentFolderId
        else page.parentFolderId

    Popup(
        alignment = Alignment.TopEnd,
        onDismissRequest = { onClose() },
        offset =
        IntOffset(
            convertDpToPixel((-10).dp, context).toInt(),
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
                            val message = exportPage(context, state.pageId)
                            removeSnack()
                            snackManager.displaySnack(
                                SnackConf(text = message, duration = 2000)
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
                            val message = exportPageToPng(context, state.pageId)
                            removeSnack()
                            snackManager.displaySnack(
                                SnackConf(text = message, duration = 2000)
                            )
                            onClose()
                        }
                    }
            ) { Text("Export page to PNG") }

            Box(
                Modifier
                    .padding(10.dp)
                    .noRippleClickable {
                        scope.launch {
                            delay(10L) // Why do I need this ?

                            copyPagePngLinkForObsidian(context, state.pageId)

                            snackManager.displaySnack(
                                SnackConf(text = "Copied page link for obsidian", duration = 2000)
                            )
                            onClose()
                        }
                    }
            ) { Text("Copy page png link for obsidian") }

            Box(
                Modifier
                    .padding(10.dp)
                    .noRippleClickable {
                        scope.launch {
                            val removeSnack =
                                snackManager.displaySnack(
                                    SnackConf(text = "Exporting the page to JPEG...")
                                )
                            delay(10L) // Why do I need this ?

                            val message = exportPageToJpeg(context, state.pageId)
                            removeSnack()
                            snackManager.displaySnack(
                                SnackConf(text = message, duration = 2000)
                            )
                            onClose()
                        }
                    }
            ) { Text("Export page to JPEG") }

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

                                val message = exportBook(context, state.bookId)
                                removeSnack()
                                snackManager.displaySnack(
                                    SnackConf(text = message, duration = 2000)
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


                                val message =
                                    exportBookToPng(context, state.bookId)

                                removeSnack()
                                snackManager.displaySnack(
                                    SnackConf(text = message, duration = 2000)
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
                        .background(Color.Black)
                )
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
                    .background(Color.Black)
            )
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
