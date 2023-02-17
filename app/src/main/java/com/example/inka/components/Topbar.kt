package com.example.inka

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Topbar(content: @Composable() () -> Unit){
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.height(40.dp)) {
            content()
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Black)
        )
    }
}