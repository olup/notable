package com.example.inka

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.navigation.NavController

@Composable
@ExperimentalComposeUiApi
fun Toolbar(
    navController: NavController, state: PageEditorState
) {
    var isStrokeSelectionOpen by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var strokeSize by remember(state.strokeSize) { mutableStateOf(state.strokeSize) } // local state - resetted when global chnages
    val context = LocalContext.current

    LaunchedEffect(isStrokeSelectionOpen) {
        if (isStrokeSelectionOpen) {
            state.isDrawing = false
        } else {
            state.isDrawing = true
            state.strokeSize = strokeSize
        }
    }

    fun handleChangeStrokeSize(size: Float) {
        strokeSize = size
        penSettings[state.pen]!!.strokeSize = size
    }

    fun handleChangePen(pen: Pen) {
        if (state.mode == Mode.DRAW && state.pen == pen) {
            isStrokeSelectionOpen = !isStrokeSelectionOpen
        } else {
            state.mode = Mode.DRAW
            state.pen = pen
            state.strokeSize = penSettings[state.pen]!!.strokeSize!! // global state
        }

    }

    fun handleEraser() {
        state.mode = Mode.ERASE
        state.pen = Pen.BALLPEN
        state.strokeSize = 10f
    }

    if (state.isToolbarOpen) {
        Column(
            modifier = Modifier.width(IntrinsicSize.Max)
        ) {
            Row(
                Modifier
                    .background(Color.White)
                    .height(40.dp)
                    .ifTrue(state.isToolbarOpen) {
                        Modifier.width(LocalConfiguration.current.screenWidthDp.dp)
                    }

            ) {
                toolbarButton(
                    onSelect = {
                        state.isToolbarOpen = !state.isToolbarOpen
                    }, iconId = R.drawable.topbar_open, contentDescription = "close toolbar"
                )
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color.Black)
                )

                toolbarButton(
                    isSelected = state.mode == Mode.DRAW && state.pen == Pen.BALLPEN, onSelect = {
                        handleChangePen(Pen.BALLPEN)
                    }, iconId = R.drawable.ballpen, contentDescription = "ballpen"
                )
                toolbarButton(
                    isSelected = state.mode == Mode.DRAW && state.pen == Pen.PENCIL, onSelect = {
                        handleChangePen(Pen.PENCIL)
                    }, iconId = R.drawable.pencil, contentDescription = "pencil"
                )
                toolbarButton(
                    isSelected = state.mode == Mode.DRAW && state.pen == Pen.BRUSH, onSelect = {
                        handleChangePen(Pen.BRUSH)
                    }, iconId = R.drawable.brush, contentDescription = "brush"
                )
                toolbarButton(
                    isSelected = state.mode == Mode.DRAW && state.pen == Pen.MARKER, onSelect = {
                        handleChangePen(Pen.MARKER)
                    }, iconId = R.drawable.marker, contentDescription = "marker"
                )
                toolbarButton(
                    isSelected = state.mode == Mode.DRAW && state.pen == Pen.FOUNTAIN, onSelect = {
                        handleChangePen(Pen.FOUNTAIN)
                    }, iconId = R.drawable.fountain, contentDescription = "fountain"
                )
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color.Black)
                )
                toolbarButton(
                    isSelected = state.mode == Mode.ERASE, onSelect = {
                        handleEraser()
                    }, iconId = R.drawable.eraser, contentDescription = "eraser"
                )
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color.Black)
                )
                toolbarButton(
                    isSelected = state.mode == Mode.SELECT,
                    onSelect = { state.mode = Mode.SELECT },
                    iconId = R.drawable.lasso,
                    contentDescription = "lasso"
                )
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color.Black)
                )
                Spacer(Modifier.weight(1f))


                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color.Black)
                )

                if (state.bookId != null) {
                    toolbarButton(
                        onSelect = {
                            navController.navigate("books/${state.bookId}/pages")
                        }, iconId = R.drawable.pages, contentDescription = "pages"
                    )
                }
                Column {
                    toolbarButton(
                        onSelect = {
                            isMenuOpen = !isMenuOpen
                        }, iconId = R.drawable.topbar_open, contentDescription = "menu"
                    )
                    if (isMenuOpen) ToolbarMenu(
                        navController = navController,
                        { isMenuOpen = false })
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.Black)
            )
            if (isStrokeSelectionOpen && penSettings[state.pen] != null) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(10.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth()
                    ) {
                        val colors = SliderDefaults.colors(
                            activeTickColor = Color.Black,
                            thumbColor = Color.Black,
                            activeTrackColor = Color.Black,
                            inactiveTrackColor = Color.Gray
                        )
                        Text(text = "Stroke size", Modifier.padding(end = 10.dp))
                        Slider(
                            value = strokeSize,
                            valueRange = 1f..20f,
                            onValueChange = { strokeSize = it },
                            colors = colors,
                            modifier = Modifier
                                .width(600.dp)
                                .height(25.dp)
                        )
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.Black)
                )
                Box(modifier = Modifier
                    .fillMaxSize()
                    .noRippleClickable { isStrokeSelectionOpen = false })
            }
        }
    } else {
        toolbarButton(
            onSelect = { state.isToolbarOpen = !state.isToolbarOpen },
            iconId = R.drawable.topbar_close,
            contentDescription = "open toolbar"
        )
    }
}
