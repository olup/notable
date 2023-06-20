package com.olup.notable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun EraserToolbarButton(
    value: Eraser,
    onChange: (Eraser) -> Unit,
    onMenuOpenChange: ((Boolean) -> Unit)?,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val context = LocalContext.current
    var isMenuOpen by remember { mutableStateOf(false) }

    if (onMenuOpenChange != null) {
        LaunchedEffect(isMenuOpen) {
            onMenuOpenChange(isMenuOpen)
        }
    }


    Box {

        ToolbarButton(
            isSelected = isSelected,
            onSelect = {
                if (isSelected) isMenuOpen = !isMenuOpen
                else onSelect()
            },
            iconId = if (value == Eraser.PEN) R.drawable.eraser else R.drawable.eraser_select,
            contentDescription = "Eraser"
        )

        if (isMenuOpen) {
            Popup(
                offset = IntOffset(0, convertDpToPixel(43.dp, context).toInt()),
                onDismissRequest = {
                    isMenuOpen = false
                },
                properties = PopupProperties(focusable = true),
                alignment = Alignment.TopCenter
            ) {
                Row(
                    Modifier
                        .background(Color.White)
                        .border(1.dp, Color.Black)
                        .height(IntrinsicSize.Max)
                ) {
                    ToolbarButton(
                        iconId = R.drawable.eraser,
                        isSelected = value == Eraser.PEN,
                        onSelect = { onChange(Eraser.PEN) },
                        modifier = Modifier.height(37.dp)
                    )
                    ToolbarButton(
                        iconId = R.drawable.eraser_select,
                        isSelected = value == Eraser.SELECT,
                        onSelect = { onChange(Eraser.SELECT) },
                        modifier = Modifier.height(37.dp)
                    )
                }

            }
        }
    }
}