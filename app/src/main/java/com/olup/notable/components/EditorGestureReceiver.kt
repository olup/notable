package com.olup.notable

import io.shipbook.shipbooksdk.Log
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import com.olup.notable.EditorControlTower
import kotlinx.coroutines.launch


@Composable
@ExperimentalComposeUiApi
fun EditorGestureReceiver(
    goToNextPage: () -> Unit,
    goToPreviousPage: () -> Unit,
    controlTower: EditorControlTower,
    state: EditorState
) {

    val coroutineScope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                awaitEachGesture {

                    val down = awaitFirstDown()
                    val inputId = down.id

                    val initialPosition = down.position
                    val initialTimestamp = System.currentTimeMillis();

                    var lastPosition = initialPosition
                    var lastTimestamp = initialTimestamp

                    var inputsCount = 0

                    // ignore non-touch
                    if (down.type != PointerType.Touch) return@awaitEachGesture


                    do {
                        val event = awaitPointerEvent()
                        val fingerChange = event.changes.filter { it.type == PointerType.Touch }
                        // is already consumed return
                        if (fingerChange.find { it.isConsumed } != null) {
                            return@awaitEachGesture
                            Log.i(TAG, "Canceling gesture - already consumemd")
                        }
                        fingerChange.forEach { it.consume() }

                        val eventReference =
                            fingerChange.find { it.id.value == inputId.value } ?: break

                        lastPosition = eventReference.position
                        lastTimestamp = System.currentTimeMillis();
                        inputsCount = fingerChange.size

                        if (fingerChange.any { !it.pressed }) break
                    } while (true)

                    Log.i(TAG, "leaving gesture")

                    val totalDelta = (initialPosition - lastPosition).getDistance()
                    val gestureDuration = lastTimestamp - initialTimestamp

                    if (totalDelta == 0f && gestureDuration < 150) {
                        // in case of double tap
                        if (withTimeoutOrNull(100) {
                                awaitFirstDown()
                                if (inputsCount == 1) {
                                    state.isToolbarOpen = !state.isToolbarOpen
                                }
                            } != null) return@awaitEachGesture

                        // in case of single tap
                        if (inputsCount == 2) {
                            state.mode = if (state.mode == Mode.Draw) Mode.Erase else Mode.Draw
                        }
                        return@awaitEachGesture

                    }

                    val verticalDrag = lastPosition.y - initialPosition.y
                    val horinzontalDrag = lastPosition.x - initialPosition.x


                    if (verticalDrag < -200) {
                        if (inputsCount == 1) {
                            coroutineScope.launch {
                                controlTower.onSingleFingerVerticalSwipe(
                                    SimplePointF(
                                        initialPosition.x, initialPosition.y
                                    ), verticalDrag.toInt()
                                )
                            }
                        }
                    }
                    if (verticalDrag > 200) {
                        if (inputsCount == 1) {
                            coroutineScope.launch {
                                controlTower.onSingleFingerVerticalSwipe(
                                    SimplePointF(
                                        initialPosition.x, initialPosition.y
                                    ), verticalDrag.toInt()
                                )
                            }
                        }
                    }
                    if (horinzontalDrag < -200) {
                        if (inputsCount == 1) {
                            goToNextPage()
                        } else if (inputsCount == 2) {
                            Log.i(TAG, "Redo")
                            coroutineScope.launch {
                                History.moveHistory(UndoRedoType.Redo)
                                DrawCanvas.refreshUi.emit(Unit)
                            }
                        }
                    }
                    if (horinzontalDrag > 200) {
                        if (inputsCount == 1) {
                            goToPreviousPage()
                        } else if (inputsCount == 2) {
                            Log.i(TAG, "Undo")
                            coroutineScope.launch {
                                History.moveHistory(UndoRedoType.Undo)
                                DrawCanvas.refreshUi.emit(Unit)
                            }
                        }

                    }

                }
            }
            .fillMaxWidth()
            .fillMaxHeight()
    )
}