package com.example.inka

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlin.math.max


@Composable
fun EditorGestureReceiver(
    pageId: Int,
    goToNextPage: () -> Unit,
    goToPreviousPage: () -> Unit,
    scroll: Int,

    updateScroll: (Int) -> Unit,
    forceUpdate: () -> Unit

    ) {

    val context = LocalContext.current
    Box(
        modifier = Modifier
            .pointerInput(pageId) {
                forEachGesture {
                    awaitPointerEventScope {
                        val down = awaitFirstDown(false)
                        if (down.type == PointerType.Stylus) return@awaitPointerEventScope

                        withTimeoutOrNull(100) {
                            waitForUpOrCancellation()
                            true
                        } ?: return@awaitPointerEventScope

                        val secondTap = withTimeoutOrNull(200) {
                            awaitFirstDown(false)
                            withTimeoutOrNull(100) {
                                waitForUpOrCancellation()
                                true
                            } ?: return@withTimeoutOrNull null

                            // Double tap
                            // mode = "breakBar"
                            // breakBarPosition = down.position.y

                            true
                        }

                        if (secondTap != null) return@awaitPointerEventScope
                        // single tap
                        /*if (mode == "breakBar") {
                mode = "normal"
                breakBarPosition = -1f
            }*/

                    }
                }
            }
            .pointerInput(pageId, scroll) {
                forEachGesture {
                    awaitPointerEventScope {
                        val down = awaitFirstDown(requireUnconsumed = false)

                        val initialPosition = down.position
                        var lastPosition = initialPosition
                        var isCanceled = false
                        var inputNumber = 0
                        // ignore stylus
                        if (down.type == PointerType.Stylus) return@awaitPointerEventScope

                        do {
                            val event: PointerEvent = awaitPointerEvent()
                            event.changes.forEach { pointerInputChange: PointerInputChange ->
                            }

                            val fingerChange = event.changes.filter { it.type == PointerType.Touch }

                            if (fingerChange.size == 1) {
                                if (inputNumber == 2) isCanceled = true
                                else if (inputNumber == 0) inputNumber = 1
                            } else if (fingerChange.size == 2) {
                                if (inputNumber == 1 || inputNumber == 0) inputNumber = 2
                            }

                            if (fingerChange.size > 0) {
                                lastPosition = fingerChange[0].position
                            }
                        } while (event.changes.any { it.pressed } && !isCanceled)

                        val verticalDrag = lastPosition.y - initialPosition.y
                        val horinzontalDrag = lastPosition.x - initialPosition.x


                        if (verticalDrag < -200) {
                            if (inputNumber == 1) {
                                updateScroll(scroll - verticalDrag.toInt())
                            }
                        }
                        if (verticalDrag > 200) {
                            if (inputNumber == 1) {
                                updateScroll(max(scroll - verticalDrag.toInt(), 0))
                            }

                        }
                        if (horinzontalDrag < -200) {
                            if (inputNumber == 1) {
                                goToNextPage()
                            } else if (inputNumber == 2) {
                                println("Redo")
                                undoRedo(context, UndoRedoType.Redo)
                                forceUpdate()
                            }


                        }
                        if (horinzontalDrag > 200) {
                            if (inputNumber == 1) {
                                goToPreviousPage()
                            } else if (inputNumber == 2) {
                                println("Undo")
                                undoRedo(context, UndoRedoType.Undo)
                                forceUpdate()
                            }
                        }
                    }
                }

            }

            .fillMaxWidth()
            .fillMaxHeight()
    )
}