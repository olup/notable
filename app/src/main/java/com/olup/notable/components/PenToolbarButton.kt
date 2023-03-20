package com.olup.notable

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*

@Composable
fun PenToolbarButton(
    pen: Pen,
    icon: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    sizes: List<Pair<String, Float>>,
    penSetting: PenSetting,
    onChangeSetting: (PenSetting) -> Unit
) {
    var isStrokeMenuOpen by remember { mutableStateOf(false) }

    Box {

        toolbarButton(
            isSelected = isSelected,
            onSelect = {
                if (isSelected) isStrokeMenuOpen = !isStrokeMenuOpen
                else onSelect()
            },
            iconId = icon,
            contentDescription = pen.penName
        )

        if (isStrokeMenuOpen) {
            StrokeMenu(value = penSetting, onChange = onChangeSetting, onClose = {isStrokeMenuOpen = false}, options = sizes)
        }
    }
}