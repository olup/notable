package com.olup.notable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.olup.notable.components.SelectMenu
import kotlinx.coroutines.launch

@Composable
fun PageSettingsModal(pageModel: PageModel, onClose: () -> Unit) {
    var pageTemplate by remember { mutableStateOf(pageModel.pageFromDb!!.nativeTemplate) }
    val scope = rememberCoroutineScope()
    Dialog(
        onDismissRequest = { onClose() }
    ) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .fillMaxWidth()
                .border(2.dp, Color.Black, RectangleShape)
        ) {
            Column(
                Modifier.padding(20.dp, 10.dp)
            ) {
                Text(text = "Page setting")
            }
            Box(
                Modifier
                    .height(0.5.dp)
                    .fillMaxWidth()
                    .background(Color.Black)
            )


            Column(
                Modifier.padding(20.dp, 10.dp)
            ) {

                Row() {
                    Text(text = "Background Template")
                    Spacer(Modifier.width(10.dp))
                    SelectMenu(
                        options = listOf(
                            "blank" to "Blank page",
                            "dotted" to "Dot grid",
                            "lined" to "Lines",
                            "squared" to "Small squares grid"
                        ),
                        onChange = {
                            val updatedPage = pageModel.pageFromDb!!.copy(nativeTemplate = it)
                            pageModel.updatePageSettings(updatedPage)
                            scope.launch {  DrawCanvas.refreshUi.emit(Unit) }
                            pageTemplate = pageModel.pageFromDb!!.nativeTemplate
                        },
                        value = pageTemplate
                    )

                }
                Spacer(Modifier.height(10.dp))
            }

        }

    }
}