package com.olup.notable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.olup.notable.db.Page


@Composable
fun PageMenu(
    notebookId: String? = null,
    pageId: String,
    index: Int? = null,
    canDelete: Boolean,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val appRepository = AppRepository(context)
    Popup(
        alignment = Alignment.TopStart,
        onDismissRequest = { onClose() },
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            Modifier
                .border(1.dp, Color.Black, RectangleShape)
                .background(Color.White)
                .width(IntrinsicSize.Max)
        ) {
            if (notebookId != null && index != null) {
                Box(
                    Modifier
                        .padding(10.dp)
                        .noRippleClickable {
                            appRepository.bookRepository.changeePageIndex(
                                notebookId,
                                pageId,
                                index - 1
                            )
                        }
                ) {
                    Text("Move Left")
                }

                Box(
                    Modifier
                        .padding(10.dp)
                        .noRippleClickable {
                            appRepository.bookRepository.changeePageIndex(
                                notebookId,
                                pageId,
                                index + 1
                            )
                        }) {
                    Text("Move right")
                }
                Box(
                    Modifier
                        .padding(10.dp)
                        .noRippleClickable {
                            val page = Page(notebookId = notebookId)
                            appRepository.pageRepository.create(page)
                            appRepository.bookRepository.addPage(notebookId, page.id, index + 1)
                        }) {
                    Text("Insert after")
                }
            }

            Box(
                Modifier
                    .padding(10.dp)
                    .noRippleClickable {
                        appRepository.duplicatePage(pageId)
                    }) {
                Text("Duplicate")
            }
            if (canDelete) {
                Box(
                    Modifier
                        .padding(10.dp)
                        .noRippleClickable {
                            deletePage(context, pageId)
                        }) {
                    Text("Delete")
                }
            }
        }
    }
}

