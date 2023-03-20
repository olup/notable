package com.olup.notable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import java.util.Date
import java.util.UUID

val strokeStyle = androidx.compose.ui.graphics.drawscope.Stroke(
    width = 2f,
    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
)

@Composable
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
fun SelectedBitmap(
    editorState: EditorState,
    controlTower: EditorControlTower
) {
    val selectionState = editorState.selectionState
    if (selectionState.selectedBitmap == null) return

    Box(
        Modifier
            .fillMaxSize()
            .noRippleClickable {
                controlTower.applySelectionDisplace()
                selectionState.reset()
                editorState.isDrawing = true
            }) {
        Image(
            bitmap = selectionState.selectedBitmap!!.asImageBitmap(),
            contentDescription = "Selection bitmap",
            modifier = Modifier
                .offset {
                    if (selectionState.selectionStartOffset == null) return@offset IntOffset(
                        0,
                        0
                    ) // guard
                    selectionState.selectionStartOffset!! + selectionState.selectionDisplaceOffset!!
                }
                .drawBehind {
                    drawRect(
                        color = Color.Gray,
                        topLeft = Offset(0f, 0f),
                        size = size,
                        style = strokeStyle
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        selectionState.selectionDisplaceOffset =
                            selectionState.selectionDisplaceOffset!! + dragAmount.round()
                    }
                }
                .combinedClickable(
                    indication = null, interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                    onDoubleClick = {
                        // finish ongoind movement
                        controlTower.applySelectionDisplace()
                        // set operation to paste only
                        selectionState.placementMode = PlacementMode.Paste
                        // change the selected stokes' ids - it's a copy
                        selectionState.selectedStrokes = selectionState.selectedStrokes!!.map {
                            it.copy(
                                id = UUID
                                    .randomUUID()
                                    .toString(),
                                createdAt = Date()
                            )
                        }
                        // move the selection a bit, to show the copy
                        selectionState.selectionDisplaceOffset = IntOffset(
                            x = selectionState.selectionDisplaceOffset!!.x + 50,
                            y = selectionState.selectionDisplaceOffset!!.y + 50,
                        )
                    }
                )
        )
    }
}