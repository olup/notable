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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import com.olup.notable.PageModel


@Composable
fun ToolbarMenu(navController: NavController, state: EditorState, page: PageModel, onClose: () -> Unit) {
    val context = LocalContext.current
    Popup(
        alignment = Alignment.TopEnd,
        onDismissRequest = { onClose() },
        offset = IntOffset(
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
                        navController.popBackStack(
                            route = "library", inclusive = false
                        )
                    }) {
                Text("Library")
            }
            Box(Modifier.padding(10.dp).noRippleClickable {
                exportPage(page)
                onClose()
            }) {
                Text("Export page")
            }
            /*Box(Modifier.padding(10.dp)) {
                Text("Export book")
            }*/
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

