package com.olup.notable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import com.olup.notable.PageModel
import kotlinx.coroutines.coroutineScope
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

    Popup(
        alignment = Alignment.TopEnd, onDismissRequest = { onClose() }, offset = IntOffset(
            convertDpToPixel(-10.dp, context).toInt(), convertDpToPixel(50.dp, context).toInt()
        ), properties = PopupProperties(focusable = true)
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
                        navController.popBackStack(
                            route = "library?folderId={folderId}", inclusive = false
                        )
                    }) {
                Text("Library")
            }
            Box(
                Modifier
                    .padding(10.dp)
                    .noRippleClickable {
                        scope.launch {
                            val removeSnack = snackManager.displaySnack(
                                SnackConf(
                                    text = "Exporting the page to PDF..."
                                )
                            )
                            delay(10L) // Why do I need this ?

                            exportPage(context, state.pageId)

                            removeSnack()
                            snackManager.displaySnack(
                                SnackConf(
                                    text = "Book exported successfully", duration = 2000
                                )
                            )
                            onClose()
                        }

                    }) {
                Text("Export page")
            }
            if (state.bookId != null) Box(
                Modifier
                    .padding(10.dp)
                    .noRippleClickable {
                        scope.launch {
                            val removeSnack = snackManager.displaySnack(
                                SnackConf(
                                    text = "Exporting the book to PDF...", id = "exportSnack"
                                )
                            )
                            delay(10L) // Why do I need this ?

                            exportBook(context, state.bookId ?: return@launch)

                            removeSnack()
                            snackManager.displaySnack(
                                SnackConf(
                                    text = "Book exported successfully", duration = 3000
                                )
                            )
                            onClose()
                        }
                    }) {
                Text("Export book")
            }
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
                        }) {
                    Text("Share selection")
                }
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
                    }) {
                Text("Page Settings")
            }

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

