package com.olup.notable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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