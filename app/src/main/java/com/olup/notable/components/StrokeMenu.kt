package com.olup.notable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable // Add this import
import androidx.compose.foundation.layout.*
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
fun StrokeMenu(
    value: PenSetting,
    onChange: (setting: PenSetting) -> Unit,
    onClose: () -> Unit,
    options: List<Pair<String, Float>>
) {
    val context = LocalContext.current

    Popup(
        offset = IntOffset(0, convertDpToPixel(43.dp, context).toInt()), onDismissRequest = {
            onClose()
        }, properties = PopupProperties(focusable = true), alignment = Alignment.TopCenter
    ) {
        Row(
            Modifier
                .background(Color.White)
                .border(1.dp, Color.Black)
                .height(IntrinsicSize.Max)
        ) {
            options.map {
                ToolbarButton(
                    text = it.first,
                    isSelected = value.strokeSize == it.second,
                    onSelect = { onChange(PenSetting(strokeSize = it.second, color = value.color)) },
                    modifier = Modifier
                )
            }
        }

    }
}


@Composable
fun ColorSelectionDialog(
    currentColor: Color,
    onSelect: (Color) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    Popup(
        offset = IntOffset(0, convertDpToPixel(43.dp, context).toInt()), onDismissRequest = {
            onClose()
        }, properties = PopupProperties(focusable = true), alignment = Alignment.TopCenter
    ) {
        Row(
            Modifier
                .background(Color.White)
                .border(1.dp, Color.Black)
                .padding(8.dp)
        ) {
            listOf(Color.Red, Color.Blue, Color.Green, Color.Black, Color.LightGray).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color)
                        .clickable { // Ensure clickable is recognized
                            onSelect(color)
                            onClose()
                        }
                        .border(1.dp, if (currentColor == color) Color.Black else Color.Transparent)
                )
            }
        }
    }
}