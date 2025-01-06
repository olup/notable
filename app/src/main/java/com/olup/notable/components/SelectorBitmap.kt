package com.olup.notable

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.olup.notable.EditorControlTower
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
                        // finish ongoing movement
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
                        //TODO: implement coping for images
                    }
                )
        )

        // If we can calculate offset of buttons show selection handling tools
        // TODO: improve this code
        selectionState.selectionStartOffset?.let { startOffset ->
            selectionState.selectionDisplaceOffset?.let { displaceOffset ->
                val xPos = selectionState.selectionRect?.let { rect ->
                    ((rect.left ?: 0) - (rect.right ?: 0)) / 2 + 35 * 3
                } ?: 0
                val offset = startOffset + displaceOffset + IntOffset(x = -xPos, y = -100)
                // Overlay buttons near the selection box
                Row(
                    modifier = Modifier
                        .offset { offset }
                        .background(Color.White.copy(alpha = 0.8f))
                        .padding(4.dp)
                        .height(35.dp)
                ) {
                    ToolbarButton(
                        iconId = R.drawable.delete,
                        isSelected = false,
                        onSelect = {
                           controlTower.deleteSelection()
                        },
                        modifier = Modifier.height(37.dp)
                    )
                    ToolbarButton(
                        iconId = R.drawable.plus,
                        isSelected = false,
                        onSelect = { },
                        modifier = Modifier.height(37.dp)
                    )
                    ToolbarButton(
                        iconId = R.drawable.minus,
                        isSelected = false,
                        onSelect = { },
                        modifier = Modifier.height(37.dp)
                    )
                }
            }
        }

    }
}