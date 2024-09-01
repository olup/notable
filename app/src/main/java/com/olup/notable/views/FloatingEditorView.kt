package com.olup.notable.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.olup.notable.EditorView
import com.olup.notable.R
import com.olup.notable.ui.theme.InkaTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingEditorView(
    navController: NavController,
    pageId: String,
    onDismissRequest: () -> Unit
) {
    var isFullScreen by remember { mutableStateOf(false) } // State for full-screen mode

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        InkaTheme {
            Box(
                modifier = Modifier
                    .fillMaxHeight(if (isFullScreen) 1f else 0.98f) // Adjust height based on full-screen state
                    .fillMaxWidth() // Ensure it fills the width
                    .background(Color.White)
            ) {
                Column {
                    Box(modifier = Modifier.weight(1f)) {
                        EditorView(
                            navController = navController,
                            _bookId = null,
                            _pageId = pageId
                        )
                    }
                }
            }
        }
    }
}
