package com.example.inka

import android.content.res.Configuration
import android.widget.Space
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlin.concurrent.thread


inline fun Modifier.ifTrue(predicate: Boolean, builder: () -> Modifier) =
    then(if (predicate) builder() else Modifier)

val penSizes = hashMapOf<Pen, Float>(
    Pen.BRUSH to 10f, Pen.BALLPEN to 10f, Pen.PENCIL to 10f, Pen.MARKER to 10f, Pen.FOUNTAIN to 10f
)

@Composable
@ExperimentalComposeUiApi
fun Toolbar(
    navController: NavController,
    pen: Pen,
    onChangePen: (Pen) -> Unit,
    _strokeSize: Float,
    onChangeStrokeSize: (Float) -> Unit,
    onChangeIsDrawing:(Boolean)->Unit,
    pageId : Int,
    bookId : Int,
) {
    var isToolbarOpen by remember { mutableStateOf(true) }
    var isStrokeSelectionOpen by remember { mutableStateOf(false) }
    var strokeSize by remember { mutableStateOf(_strokeSize) }
    val context = LocalContext.current

    LaunchedEffect(isStrokeSelectionOpen) {
        if(isStrokeSelectionOpen) {
            onChangeIsDrawing(false)
        }
        else {
            onChangeIsDrawing(true)
            onChangeStrokeSize(strokeSize)
        }
    }

    fun handleChangeStrokeSize(size: Float) {
        strokeSize=size
        penSizes[pen] = size
    }

    fun handleChangePen(pen: Pen) {
        onChangePen(pen)
        onChangeStrokeSize(penSizes[pen]!!)
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
                .height(IntrinsicSize.Max)
                .ifTrue(isToolbarOpen) {
                    Modifier.width(LocalConfiguration.current.screenWidthDp.dp)
                }

        ) {
            Box(Modifier
                .clickable(indication = null,
                    interactionSource = remember { MutableInteractionSource() }) {
                    isToolbarOpen = !isToolbarOpen
                }
                .size(40.dp)
                .padding(10.dp)) {
                Icon(
                    painter= if (!isToolbarOpen) painterResource(id = R.drawable.topbar_close) else painterResource(id = R.drawable.topbar_open),
                    "toolbar switch",
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
            if (isToolbarOpen) {
                Box(Modifier
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        if (pen == Pen.BALLPEN) {
                            isStrokeSelectionOpen = !isStrokeSelectionOpen
                        }
                        handleChangePen(
                            Pen.BALLPEN
                        )
                        onChangeStrokeSize(penSizes[Pen.BALLPEN]!!)
                    }
                    .size(40.dp)
                    .padding(10.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ballpen),
                        "ballpen",
                        Modifier,
                        if (pen == Pen.BALLPEN) Color.Black else Color.Gray
                    )
                }
                Box(Modifier
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        if (pen == Pen.PENCIL) {
                            isStrokeSelectionOpen = !isStrokeSelectionOpen
                        }
                        handleChangePen(
                            Pen.PENCIL
                        )
                        onChangeStrokeSize(penSizes[Pen.PENCIL]!!)
                    }
                    .size(40.dp)
                    .padding(10.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.pencil),
                        "pencil",
                        Modifier,
                        if (pen == Pen.PENCIL) Color.Black else Color.Gray
                    )
                }
                Box(Modifier
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        if (pen == Pen.BRUSH) {
                            isStrokeSelectionOpen = !isStrokeSelectionOpen
                        }
                        handleChangePen(
                            Pen.BRUSH
                        )
                        onChangeStrokeSize(penSizes[Pen.BRUSH]!!)
                    }
                    .size(40.dp)
                    .padding(10.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.brush),
                        "brush",
                        Modifier,
                        if (pen == Pen.BRUSH) Color.Black else Color.Gray
                    )
                }
                Box(Modifier
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        if (pen == Pen.MARKER) {
                            isStrokeSelectionOpen = !isStrokeSelectionOpen
                        }
                        handleChangePen(
                            Pen.MARKER
                        )
                        onChangeStrokeSize(penSizes[Pen.MARKER]!!)
                    }
                    .size(40.dp)
                    .padding(10.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.marker),
                        "marker",
                        Modifier,
                        if (pen == Pen.MARKER) Color.Black else Color.Gray
                    )
                }
                Box(Modifier
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        if (pen == Pen.FOUNTAIN) {
                            isStrokeSelectionOpen = !isStrokeSelectionOpen
                        }
                        handleChangePen(
                            Pen.FOUNTAIN
                        )
                        onChangeStrokeSize(penSizes[Pen.FOUNTAIN]!!)
                    }
                    .size(40.dp)
                    .padding(10.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.fountain),
                        "fountain",
                        Modifier,
                        if (pen == Pen.FOUNTAIN) Color.Black else Color.Gray
                    )
                }
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color.Black)
                )
                Box(Modifier
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        thread(start = true) {
                            exportPageToPdf(context, bookId = bookId, pageId = pageId)
                        }
                    }
                    .size(40.dp)
                    .padding(10.dp)) {
                    Icon(
                        painter= painterResource(id = R.drawable.eraser), "eraser", Modifier, Color.Gray
                    )
                }
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color.Black)
                )
                Box(Modifier
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        thread(start = true) {
                            exportPageToPdf(context, bookId = bookId, pageId = pageId)
                        }
                    }
                    .size(40.dp)
                    .padding(10.dp)) {
                    Icon(
                        painter= painterResource(id = R.drawable.lasso), "lasoo", Modifier, Color.Gray
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
                Box(Modifier
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        navController.navigate("book/${bookId}/pages")
                    }
                    .size(40.dp)
                    .padding(10.dp)) {
                    Icon(
                        painter= painterResource(id = R.drawable.pages), "library", Modifier, Color.Gray
                    )
                }
                Box(Modifier
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        navController.popBackStack(route = "library", inclusive = false)
                    }
                    .size(40.dp)
                    .padding(10.dp)) {
                    Icon(
                        painter= painterResource(id = R.drawable.library), "library", Modifier, Color.Gray
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
                    .padding(10.dp)
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { isStrokeSelectionOpen = false })
        }
    }
}
