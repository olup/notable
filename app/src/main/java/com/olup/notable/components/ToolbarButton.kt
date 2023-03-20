package com.olup.notable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun toolbarButton(
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    iconId: Int? = null,
    text: String? = null,
    contentDescription: String = "",
    modifier: Modifier = Modifier
) {
    Box(
        Modifier.then(modifier)
            .noRippleClickable {
                onSelect()
            }
            .background(if (isSelected) Color.Black else Color.Transparent)
            .padding(7.dp)

    ) {
        if (iconId != null) {
            Icon(
                painter = painterResource(id = iconId),
                contentDescription,
                Modifier,
                if (isSelected) Color.White else Color.Black
            )
        }

        if (text != null) {
            Text(
                text = text,
                color = if (isSelected) Color.White else Color.Black
            )
        }
    }
}