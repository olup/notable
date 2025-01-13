package com.olup.notable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
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
                    onSelect = {
                        onChange(
                            PenSetting(
                                strokeSize = it.second,
                                color = value.color
                            )
                        )
                    },
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
    onClose: () -> Unit,
    options: List<Color>
) {
    val context = LocalContext.current

    Popup(
        offset = IntOffset(0, convertDpToPixel(43.dp, context).toInt()),
        onDismissRequest = { onClose() },
        properties = PopupProperties(focusable = true),
        alignment = Alignment.TopCenter
    ) {
        Row(
            Modifier
                .background(Color.White)
                .border(1.dp, Color.Black)
                .height(IntrinsicSize.Max)
        ) {
            options.map { color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color)
                        .border(1.dp, if (color == currentColor) Color.Black else Color.Transparent)
                        .clickable { onSelect(color) }
                        .padding(8.dp)
                )
            }
        }
    }
}