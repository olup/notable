package com.example.inka

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

val ICON_PADDING = 7.dp
inline fun Modifier.ifTrue(predicate: Boolean, builder: () -> Modifier) =
    then(if (predicate) builder() else Modifier)

val penSizes = hashMapOf<Pen, Float>(
    Pen.BRUSH to 5f,
    Pen.BALLPEN to 5f,
    Pen.PENCIL to 5f,
    Pen.MARKER to 5f,
    Pen.FOUNTAIN to 5f
)

@Composable
@ExperimentalComposeUiApi
fun Toolbar(
    navController: NavController,
    state : EditorState
) {
    var isStrokeSelectionOpen by remember { mutableStateOf(false) }
    var strokeSize by remember { mutableStateOf(state.strokeSize) }
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
        penSizes[state.pen] = size
    }

    fun handleChangePen(pen: Pen) {
        state.pen = pen
        state.strokeSize = penSizes[pen]!!
        strokeSize = penSizes[pen]!!
    }

    Column(
        modifier = Modifier.width(IntrinsicSize.Max)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Black)
        )
        Row(
            Modifier
                .background(Color.White)
                .height(40.dp)
                .ifTrue(state.isToolbarOpen) {
                    Modifier.width(LocalConfiguration.current.screenWidthDp.dp)
                }

        ) {
            Box(
                Modifier
                    .noRippleClickable {
                        state.isToolbarOpen = ! state.isToolbarOpen
                    }
                    .size(40.dp)
                    .padding(ICON_PADDING)) {
                Icon(
                    painter = if (!state.isToolbarOpen) painterResource(id = R.drawable.topbar_close) else painterResource(
                        id = R.drawable.topbar_open
                    ), "toolbar switch", Modifier, Color.Gray
                )
            }
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color.Black)
            )
            if (state.isToolbarOpen) {
                Box(
                    Modifier
                        .noRippleClickable {
                            if (state.pen == Pen.BALLPEN) {
                                isStrokeSelectionOpen = !isStrokeSelectionOpen
                            }
                            handleChangePen(
                                Pen.BALLPEN
                            )
                            state.strokeSize = penSizes[Pen.BALLPEN]!!
                        }
                        .size(40.dp)
                        .padding(ICON_PADDING)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ballpen),
                        "ballpen",
                        Modifier,
                        if (state.pen == Pen.BALLPEN) Color.Black else Color.Gray
                    )
                }
                Box(
                    Modifier
                        .noRippleClickable {
                            if (state.pen == Pen.PENCIL) {
                                isStrokeSelectionOpen = !isStrokeSelectionOpen
                            }
                            handleChangePen(
                                Pen.PENCIL
                            )
                            state.strokeSize = penSizes[Pen.PENCIL]!!
                        }
                        .size(40.dp)
                        .padding(ICON_PADDING)) {
                    Icon(
                        painter = painterResource(id = R.drawable.pencil),
                        "pencil",
                        Modifier,
                        if (state.pen == Pen.PENCIL) Color.Black else Color.Gray
                    )
                }
                Box(
                    Modifier
                        .noRippleClickable {
                            if (state.pen == Pen.BRUSH) {
                                isStrokeSelectionOpen = !isStrokeSelectionOpen
                            }
                            handleChangePen(
                                Pen.BRUSH
                            )
                            state.strokeSize = penSizes[Pen.BRUSH]!!
                        }
                        .size(40.dp)
                        .padding(ICON_PADDING)) {
                    Icon(
                        painter = painterResource(id = R.drawable.brush),
                        "brush",
                        Modifier,
                        if (state.pen == Pen.BRUSH) Color.Black else Color.Gray
                    )
                }
                Box(
                    Modifier
                        .noRippleClickable {
                            if (state.pen == Pen.MARKER) {
                                isStrokeSelectionOpen = !isStrokeSelectionOpen
                            }
                            handleChangePen(
                                Pen.MARKER
                            )
                            state.strokeSize = penSizes[Pen.MARKER]!!
                        }
                        .size(40.dp)
                        .padding(ICON_PADDING)) {
                    Icon(
                        painter = painterResource(id = R.drawable.marker),
                        "marker",
                        Modifier,
                        if (state.pen == Pen.MARKER) Color.Black else Color.Gray
                    )
                }
                Box(
                    Modifier
                        .noRippleClickable {
                            if (state.pen == Pen.FOUNTAIN) {
                                isStrokeSelectionOpen = !isStrokeSelectionOpen
                            }
                            handleChangePen(
                                Pen.FOUNTAIN
                            )
                            state.strokeSize = penSizes[Pen.FOUNTAIN]!!
                        }
                        .size(40.dp)
                        .padding(ICON_PADDING)) {
                    Icon(
                        painter = painterResource(id = R.drawable.fountain),
                        "fountain",
                        Modifier,
                        if (state.pen == Pen.FOUNTAIN) Color.Black else Color.Gray
                    )
                }
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color.Black)
                )
                Box(
                    Modifier
                        .size(40.dp)
                        .padding(ICON_PADDING)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.eraser),
                        "eraser",
                        Modifier,
                        Color.Gray
                    )
                }
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color.Black)
                )

                Box(
                    Modifier
                        .size(40.dp)
                        .padding(ICON_PADDING)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.lasso),
                        "lasoo",
                        Modifier,
                        Color.Gray
                    )
                }

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
                    Box(
                        Modifier
                            .noRippleClickable {
                                navController.navigate("book/${state.bookId}/pages")
                            }
                            .size(40.dp)
                            .padding(ICON_PADDING)) {
                        Icon(
                            painter = painterResource(id = R.drawable.pages),
                            "library",
                            Modifier,
                            Color.Gray
                        )
                    }
                }
                Box(
                    Modifier
                        .noRippleClickable {
                            navController.popBackStack(route = "library", inclusive = false)
                        }
                        .size(40.dp)
                        .padding(ICON_PADDING)) {
                    Icon(
                        painter = painterResource(id = R.drawable.library),
                        "library",
                        Modifier,
                        Color.Gray
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Black)
        )
        if (isStrokeSelectionOpen) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(ICON_PADDING)
            ) {
                Row(
                    Modifier

                        .fillMaxWidth()
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
                        onValueChange = { handleChangeStrokeSize(it) },
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
}
