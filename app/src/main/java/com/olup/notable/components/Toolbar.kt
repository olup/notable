package com.olup.notable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import com.olup.notable.PageModel

fun PresentlyUsedToolIcon(mode: Mode, pen: Pen) : Int{
   return  when(mode){
       Mode.Draw -> {
           when(pen){
               Pen.BALLPEN  ->  R.drawable.ballpen
               Pen.FOUNTAIN ->  R.drawable.fountain
               Pen.BRUSH ->  R.drawable.brush
               Pen.MARKER ->  R.drawable.marker
               Pen.PENCIL ->  R.drawable.pencil
           }
       }
       Mode.Erase -> R.drawable.eraser
       Mode.Select -> R.drawable.lasso
   }
}

@Composable
@ExperimentalComposeUiApi
fun Toolbar(
    navController: NavController, state: EditorState
) {
    var isStrokeSelectionOpen by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var isPageSettingsModalOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(isStrokeSelectionOpen, isMenuOpen) {
        state.isDrawing = !isStrokeSelectionOpen && !isMenuOpen
    }

    fun handleChangePen(pen: Pen) {
        if (state.mode == Mode.Draw && state.pen == pen) {
            isStrokeSelectionOpen = true
        } else {
            state.mode = Mode.Draw
            state.pen = pen
        }

    }

    fun handleClosePenSettings() {
        isStrokeSelectionOpen = false
    }

    fun handleEraser() {
        state.mode = Mode.Erase

    }

    fun handleSelection() {
        state.mode = Mode.Select
    }

    fun onChangeStrokeSize(size : Float){
        val settings = state.penSettings.toMutableMap()
        settings.set(state.pen.penName, PenSetting(size, settings[state.pen.penName]!!.color))
        state.penSettings = settings
    }


    if(isPageSettingsModalOpen){
        PageSettingsModal(pageModel = state.pageModel) {
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
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth()

            ) {
                toolbarButton(
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

                toolbarButton(
                    isSelected = state.mode == Mode.Draw && state.pen == Pen.BALLPEN,
                    onSelect = {
                        handleChangePen(Pen.BALLPEN)
                    },
                    iconId = R.drawable.ballpen,
                    contentDescription = "ballpen"
                )
                toolbarButton(
                    isSelected = state.mode == Mode.Draw && state.pen == Pen.PENCIL,
                    onSelect = {
                        handleChangePen(Pen.PENCIL)
                    },
                    iconId = R.drawable.pencil,
                    contentDescription = "pencil"
                )
                toolbarButton(
                    isSelected = state.mode == Mode.Draw && state.pen == Pen.BRUSH,
                    onSelect = {
                        handleChangePen(Pen.BRUSH)
                    },
                    iconId = R.drawable.brush,
                    contentDescription = "brush"
                )
                toolbarButton(
                    isSelected = state.mode == Mode.Draw && state.pen == Pen.FOUNTAIN,
                    onSelect = {
                        handleChangePen(Pen.FOUNTAIN)
                    },
                    iconId = R.drawable.fountain,
                    contentDescription = "fountain"
                )
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(0.5.dp)
                        .background(Color.Black)
                )
                toolbarButton(
                    isSelected = state.mode == Mode.Draw && state.pen == Pen.MARKER,
                    onSelect = {
                        handleChangePen(Pen.MARKER)
                    },
                    iconId = R.drawable.marker,
                    contentDescription = "marker"
                )
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(0.5.dp)
                        .background(Color.Black)
                )
                toolbarButton(
                    isSelected = state.mode == Mode.Erase, onSelect = {
                        handleEraser()
                    }, iconId = R.drawable.eraser, contentDescription = "eraser"
                )
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(0.5.dp)
                        .background(Color.Black)
                )
                toolbarButton(
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

                Spacer(Modifier.weight(1f))

                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(0.5.dp)
                        .background(Color.Black)
                )

                if (state.bookId != null) {
                    val book = AppRepository(context).bookRepository.getById(state.bookId)

                    // TODO maybe have generic utils for this ?
                    val pageNumber = book!!.pageIds.indexOf(state.pageId) +1
                    val totalPageNumber = book!!.pageIds.size

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.height(35.dp).padding(10.dp, 0.dp)
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
                Column {
                    toolbarButton(
                        onSelect = {
                            isMenuOpen = !isMenuOpen
                        }, iconId = R.drawable.menu, contentDescription = "menu"
                    )
                    if (isMenuOpen) ToolbarMenu(
                        navController = navController,
                        state = state,
                        onClose = { isMenuOpen = false },
                        onPageSettingsOpen = {isPageSettingsModalOpen = true}
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.Black)
            )
            if (isStrokeSelectionOpen) {
                val thisPenSettings = state.penSettings[state.pen.penName]!!
                Popup(
                    offset = IntOffset(0, convertDpToPixel(37.dp, context).toInt()),
                    onDismissRequest = {
                        handleClosePenSettings()
                    },
                    properties = PopupProperties(focusable = true)


                ) {
                    Column(
                        Modifier
                            .background(Color.White)
                            .height(37.dp)
                    ) {
                        Row {
                            toolbarButton(text = "Size 3",
                                isSelected = thisPenSettings.strokeSize == 3f,onSelect = { onChangeStrokeSize(3f) })
                            toolbarButton(text = "Size 5",
                                isSelected = thisPenSettings.strokeSize == 5f,onSelect = { onChangeStrokeSize(5f) })
                            toolbarButton(text = "Size 10",
                                isSelected = thisPenSettings.strokeSize == 10f,onSelect = {onChangeStrokeSize(10f)})
                            toolbarButton(text = "Size 20",
                                isSelected = thisPenSettings.strokeSize == 20f,onSelect = { onChangeStrokeSize(20f) })

                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .background(Color.Black)
                            )

                            toolbarButton(text = "Black")
                            toolbarButton(text = "Gray")
                            toolbarButton(text = "Light Gray")
                            toolbarButton(text = "White")
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.Black)
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
        Row(
            Modifier
                .fillMaxWidth()
               // .noRippleClickable { state.isToolbarOpen = true }
        ) {
            toolbarButton(
                onSelect = { state.isToolbarOpen = true },
                iconId = PresentlyUsedToolIcon(state.mode, state.pen),
                contentDescription = "open toolbar"
            )
        }

    }
}
