package com.example.inka

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
import androidx.navigation.NavController
import com.example.inka.convertDpToPixel
import com.example.inka.noRippleClickable

@Composable
fun ToolbarMenu(navController: NavController, onClose: () -> Unit) {
    val context = LocalContext.current
    Popup(
        alignment = Alignment.TopEnd,
        onDismissRequest = { onClose() },
        offset = IntOffset(0, convertDpToPixel(40.dp, context).toInt())
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
                            route = "library",
                            inclusive = false
                        )
                    }) {
                Text("Library")
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.Black)
            )
            Box(Modifier.padding(10.dp)) {
                Text("Refresh page")
            }
            Box(Modifier.padding(10.dp)) {
                Text("This is an experiment")
            }
            Box(Modifier.padding(10.dp)) {
                Text("This is an experiment")
            }
        }
    }
}
