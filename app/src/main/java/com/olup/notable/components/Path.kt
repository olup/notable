package com.olup.notable.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material.icons.sharp.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.olup.notable.noRippleClickable

// It seems wrong to re-implement a Path selector dialog,
// but until now I only saw self drawed UI elements, not built-in android ones
// It should be simple, if I only render a text,
// and if it is clicked upon I open a Directory Selector Dialog
@Composable
fun PathMenu(value: String, onChange: (String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }

    Box() {
        Row() {
            Text(text = value,
                fontWeight = FontWeight.Light,
                modifier = Modifier.noRippleClickable { isExpanded = true }
                // Register Directory selector dialog on Click event
            )

            Icon(
                Icons.Rounded.ArrowDropDown, contentDescription = "open select",
                modifier = Modifier.height(20.dp)
            )
        }
        if (isExpanded) Popup(onDismissRequest = { isExpanded = false }) {
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .border(0.5.dp, Color.Black, RectangleShape)
                    .background(Color.White)
            ) {
                Text(text = "dialogOpens", // as I am new to Android, first test if the changes until now work
                    fontWeight = FontWeight.Light,
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(20.dp, 10.dp)
                        .noRippleClickable {
                            //onChange(it.first)
                            isExpanded = false
                        })
            }
        }
    }

}