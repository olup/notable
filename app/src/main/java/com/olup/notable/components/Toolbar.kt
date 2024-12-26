package com.olup.notable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

fun PresentlyUsedToolIcon(mode: Mode, pen: Pen): Int {
    return when (mode) {
        Mode.Draw -> {
            when (pen) {
                Pen.BALLPEN -> R.drawable.ballpen
                Pen.REDBALLPEN -> R.drawable.ballpenred
                Pen.BLUEBALLPEN -> R.drawable.ballpenblue
                Pen.GREENBALLPEN -> R.drawable.ballpengreen
                Pen.FOUNTAIN -> R.drawable.fountain
                Pen.BRUSH -> R.drawable.brush
                Pen.MARKER -> R.drawable.marker
                Pen.PENCIL -> R.drawable.pencil
            }
        }
        Mode.Erase -> R.drawable.eraser
        Mode.Select -> R.drawable.lasso
        Mode.Line -> R.drawable.line
    }
}
fun isSelected(state: EditorState, penType: Pen): Boolean {
    return if (state.mode == Mode.Draw && state.pen == penType) {
        true
    } else if (state.mode == Mode.Line && state.pen == penType) {
        true
    } else {
        false
    }
}


@Composable
@ExperimentalComposeUiApi
fun Toolbar(
    navController: NavController, state: EditorState
) {
    val scope = rememberCoroutineScope()
    var isStrokeSelectionOpen by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var isPageSettingsModalOpen by remember { mutableStateOf(false) }
    var isPaletteOpen by remember { mutableStateOf(false) }
    val penColorMap = mapOf(
        Pen.REDBALLPEN to Color.Red,
        Pen.BLUEBALLPEN to Color.Blue,
        Pen.GREENBALLPEN to Color.Green,
    )
    var selectedColor by remember {
        mutableStateOf(
            penColorMap[state.pen] ?: Color.Black
        )
    } // Add a state for selected color
    var isColorSelectionDialogOpen by remember { mutableStateOf(false) } // State for color selection dialog

    val context = LocalContext.current

    LaunchedEffect(isMenuOpen) {
        state.isDrawing = !isMenuOpen
    }

    fun handleChangePen(pen: Pen) {
        if (state.mode == Mode.Draw && state.pen == pen) {
            isStrokeSelectionOpen = true
        } else {
            state.mode = Mode.Draw
            state.pen = pen
        }
    }

    fun handleEraser() {
        state.mode = Mode.Erase
    }

    fun handleSelection() {
        state.mode = Mode.Select
    }
    fun handleLine() {
        state.mode = Mode.Line
    }

    fun onChangeStrokeSetting(penName: String, setting: PenSetting) {
        val settings = state.penSettings.toMutableMap()
        settings[penName] = setting.copy()
        state.penSettings = settings
    }

    fun changePenColor(color: Color) {
        val settings = state.penSettings.toMutableMap()
        val selectedPenName = state.pen.penName
        settings[selectedPenName] = settings[selectedPenName]?.copy(
            color = android.graphics.Color.argb(
                (color.alpha * 255).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
        ) ?: return // Convert Color to Int
        state.penSettings = settings
    }

    // Show Color Selection Dialog
    if (isColorSelectionDialogOpen) {
        ColorSelectionDialog(
            currentColor = selectedColor,
            onSelect = { color ->
                selectedColor = color
                changePenColor(color) // Change pen color for all pens
                isColorSelectionDialogOpen = false
            },
            onClose = { isColorSelectionDialogOpen = false },
            options = listOf(
                Color.Red,
                Color.Green,
                Color.Blue,
                Color.Cyan,
                Color.Magenta,
                Color.Yellow,
                Color.Gray,
                Color.DarkGray,
                Color.Black,
            ) // List of color options
        )
    }

    if (isPageSettingsModalOpen) {
        PageSettingsModal(pageView = state.pageView) {
            isPageSettingsModalOpen = false
        }
    }
    if (state.isToolbarOpen) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier
                    .background(Color.White)
                    .height(37.dp)
                    .fillMaxWidth()
            ) {
                ToolbarButton(
                    onSelect = {
                        state.isToolbarOpen = !state.isToolbarOpen
                    }, iconId = R.drawable.topbar_open, contentDescription = "close toolbar"
                )
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(0.5.dp)
                        .background(Color.Black)
                )

                PenToolbarButton(
                    onStrokeMenuOpenChange = { state.isDrawing = !it },
                    pen = Pen.BALLPEN,
                    icon = R.drawable.ballpen,
                    isSelected = isSelected(state, Pen.BALLPEN),
                    onSelect = { handleChangePen(Pen.BALLPEN) },
                    sizes = listOf("S" to 3f, "M" to 5f, "L" to 10f, "XL" to 20f),
                    penSetting = state.penSettings[Pen.BALLPEN.penName] ?: return,
                    onChangeSetting = { onChangeStrokeSetting(Pen.BALLPEN.penName, it) })

                PenToolbarButton(
                    onStrokeMenuOpenChange = { state.isDrawing = !it },
                    pen = Pen.REDBALLPEN,
                    icon = R.drawable.ballpenred,
                    isSelected = isSelected(state, Pen.REDBALLPEN),
                    onSelect = { handleChangePen(Pen.REDBALLPEN) },
                    sizes = listOf("S" to 3f, "M" to 5f, "L" to 10f, "XL" to 20f),
                    penSetting = state.penSettings[Pen.REDBALLPEN.penName] ?: return,
                    onChangeSetting = { onChangeStrokeSetting(Pen.REDBALLPEN.penName, it) },
                )

                PenToolbarButton(
                    onStrokeMenuOpenChange = { state.isDrawing = !it },
                    pen = Pen.BLUEBALLPEN,
                    icon = R.drawable.ballpenblue,
                    isSelected = isSelected(state, Pen.BLUEBALLPEN),
                    onSelect = { handleChangePen(Pen.BLUEBALLPEN) },
                    sizes = listOf("S" to 3f, "M" to 5f, "L" to 10f, "XL" to 20f),
                    penSetting = state.penSettings[Pen.BLUEBALLPEN.penName] ?: return,
                    onChangeSetting = { onChangeStrokeSetting(Pen.BLUEBALLPEN.penName, it) },
                )

                PenToolbarButton(
                    onStrokeMenuOpenChange = { state.isDrawing = !it },
                    pen = Pen.GREENBALLPEN,
                    icon = R.drawable.ballpengreen,
                    isSelected = isSelected(state, Pen.GREENBALLPEN),
                    onSelect = { handleChangePen(Pen.GREENBALLPEN) },
                    sizes = listOf("S" to 3f, "M" to 5f, "L" to 10f, "XL" to 20f),
                    penSetting = state.penSettings[Pen.GREENBALLPEN.penName] ?: return,
                    onChangeSetting = { onChangeStrokeSetting(Pen.GREENBALLPEN.penName, it) },
                )

                PenToolbarButton(
                    onStrokeMenuOpenChange = { state.isDrawing = !it },
                    pen = Pen.PENCIL,
                    icon = R.drawable.pencil,
                    isSelected = isSelected(state, Pen.PENCIL),
                    onSelect = { handleChangePen(Pen.PENCIL) },
                    sizes = listOf("S" to 3f, "M" to 5f, "L" to 10f, "XL" to 20f),
                    penSetting = state.penSettings[Pen.PENCIL.penName] ?: return,
                    onChangeSetting = { onChangeStrokeSetting(Pen.PENCIL.penName, it) },
                )

                PenToolbarButton(
                    onStrokeMenuOpenChange = { state.isDrawing = !it },
                    pen = Pen.BRUSH,
                    icon = R.drawable.brush,
                    isSelected =isSelected(state, Pen.BRUSH),
                    onSelect = { handleChangePen(Pen.BRUSH) },
                    sizes = listOf("S" to 3f, "M" to 5f, "L" to 10f, "XL" to 20f),
                    penSetting = state.penSettings[Pen.BRUSH.penName] ?: return,
                    onChangeSetting = { onChangeStrokeSetting(Pen.BRUSH.penName, it) },
                )

                PenToolbarButton(
                    onStrokeMenuOpenChange = { state.isDrawing = !it },
                    pen = Pen.FOUNTAIN,
                    icon = R.drawable.fountain,
                    isSelected =isSelected(state, Pen.FOUNTAIN),
                    onSelect = { handleChangePen(Pen.FOUNTAIN) },
                    sizes = listOf("S" to 3f, "M" to 5f, "L" to 10f, "XL" to 20f),
                    penSetting = state.penSettings[Pen.FOUNTAIN.penName] ?: return,
                    onChangeSetting = { onChangeStrokeSetting(Pen.FOUNTAIN.penName, it) },
                )

                LineToolbarButton(
                    onStrokeMenuOpenChange = { state.isDrawing = !it },
                    icon = R.drawable.line,
                    isSelected = state.mode == Mode.Line,
                    onSelect = {handleLine() },
                )

                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(0.5.dp)
                        .background(Color.Black)
                )

                PenToolbarButton(
                    onStrokeMenuOpenChange = { state.isDrawing = !it },
                    pen = Pen.MARKER,
                    icon = R.drawable.marker,
                    isSelected = isSelected(state, Pen.MARKER),
                    onSelect = { handleChangePen(Pen.MARKER) },
                    sizes = listOf("L" to 40f, "XL" to 60f),
                    penSetting = state.penSettings[Pen.MARKER.penName] ?: return,
                    onChangeSetting = {
                        onChangeStrokeSetting(
                            Pen.MARKER.penName,
                            it.copy(it.strokeSize, android.graphics.Color.LTGRAY)
                        )
                    })

                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(0.5.dp)
                        .background(Color.Black)
                )
                EraserToolbarButton(
                    isSelected = state.mode == Mode.Erase,
                    onSelect = {
                        handleEraser()
                    },
                    onMenuOpenChange = { isStrokeSelectionOpen = it },
                    value = state.eraser,
                    onChange = { state.eraser = it })
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(0.5.dp)
                        .background(Color.Black)
                )
                ToolbarButton(
                    isSelected = state.mode == Mode.Select,
                    onSelect = { handleSelection() },
                    iconId = R.drawable.lasso,
                    contentDescription = "lasso"
                )
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(0.5.dp)
                        .background(Color.Black)
                )

                ToolbarButton(
                    iconId = R.drawable.palette,
                    contentDescription = "palette",
                    onSelect = {
                        isColorSelectionDialogOpen = true // Open the color selection dialog
                    }
                )
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(0.5.dp)
                        .background(Color.Black)
                )

                Spacer(Modifier.weight(1f))

                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(0.5.dp)
                        .background(Color.Black)
                )

                ToolbarButton(
                    onSelect = {
                        scope.launch {
                            History.moveHistory(UndoRedoType.Undo)
                            DrawCanvas.refreshUi.emit(Unit)
                        }
                    },
                    iconId = R.drawable.undo,
                    contentDescription = "undo"
                )

                ToolbarButton(
                    onSelect = {
                        scope.launch {
                            History.moveHistory(UndoRedoType.Redo)
                            DrawCanvas.refreshUi.emit(Unit)
                        }
                    },
                    iconId = R.drawable.redo,
                    contentDescription = "redo"
                )

                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(0.5.dp)
                        .background(Color.Black)
                )

                if (state.bookId != null) {
                    val book = AppRepository(context).bookRepository.getById(state.bookId)

                    // TODO maybe have generic utils for this ?
                    val pageNumber = book!!.pageIds.indexOf(state.pageId) + 1
                    val totalPageNumber = book!!.pageIds.size

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(35.dp)
                            .padding(10.dp, 0.dp)
                    ) {
                        Text(
                            text = "${pageNumber}/${totalPageNumber}",
                            fontWeight = FontWeight.Light,
                            modifier = Modifier.noRippleClickable {
                                navController.navigate("books/${state.bookId}/pages")
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .width(0.5.dp)
                            .background(Color.Black)
                    )
                }
                // Add Library Button
                ToolbarButton(
                    iconId = R.drawable.home, // Replace with your library icon resource
                    contentDescription = "library",
                    onSelect = {
                        navController.navigate("library") // Navigate to main library
                    }
                )
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(0.5.dp)
                        .background(Color.Black)
                )
                Column {
                    ToolbarButton(
                        onSelect = {
                            isMenuOpen = !isMenuOpen
                        }, iconId = R.drawable.menu, contentDescription = "menu"
                    )
                    if (isMenuOpen) ToolbarMenu(navController = navController,
                        state = state,
                        onClose = { isMenuOpen = false },
                        onPageSettingsOpen = { isPageSettingsModalOpen = true })
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.Black)
            )
        }
    } else {
        ToolbarButton(
            onSelect = { state.isToolbarOpen = true },
            iconId = PresentlyUsedToolIcon(state.mode, state.pen),
            penColor = if(state.mode != Mode.Erase) state.penSettings[state.pen.penName]?.color?.let { Color(it) } else null,
            contentDescription = "open toolbar",
            modifier = Modifier.height(37.dp)
        )
    }
}