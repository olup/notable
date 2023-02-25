package com.example.inka

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun toolbarButton(
    isSelected: Boolean = false, onSelect: () -> Unit, iconId: Int, contentDescription: String
) {
    Box(
        Modifier
            .noRippleClickable {
                onSelect()
            }
            .size(40.dp)
            .background(if (isSelected) Color.Black else Color.Transparent)
            .padding(8.dp)) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription,
            Modifier,
            if (isSelected) Color.White else Color.Black
        )
    }
}