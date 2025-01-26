package com.olup.notable

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

@Composable
fun PenToolbarButton(
    pen: Pen,
    icon: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    sizes: List<Pair<String, Float>>,
    penSetting: PenSetting,
    onChangeSetting: (PenSetting) -> Unit,
    onStrokeMenuOpenChange: ((Boolean) -> Unit)? = null
) {
    var isStrokeMenuOpen by remember { mutableStateOf(false) }

    if (onStrokeMenuOpenChange != null) {
        LaunchedEffect(isStrokeMenuOpen) {
            onStrokeMenuOpenChange(isStrokeMenuOpen)
        }
    }


    Box {

        ToolbarButton(
            isSelected = isSelected,
            onSelect = {
                if (isSelected) isStrokeMenuOpen = !isStrokeMenuOpen
                else onSelect()
            },
            penColor = Color(penSetting.color),
            iconId = icon,
            contentDescription = pen.penName
        )

        if (isStrokeMenuOpen) {
            StrokeMenu(
                value = penSetting,
                onChange = { onChangeSetting(it) },
                onClose = { isStrokeMenuOpen = false },
                options = sizes
            )
        }
    }
}