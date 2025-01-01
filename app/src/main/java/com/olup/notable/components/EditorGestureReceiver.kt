package com.olup.notable

import io.shipbook.shipbooksdk.Log
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
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
    val appSettings by AppRepository(LocalContext.current)
        .kvProxy
        .observeKv("APP_SETTINGS", AppSettings.serializer(), AppSettings(version = 1))
        .observeAsState()
    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                awaitEachGesture {

                    val down = awaitFirstDown()
                    val inputId = down.id

                    val initialPositions = mutableMapOf(inputId to down.position)
                    val initialTimestamp = System.currentTimeMillis()

                    val lastPositions = mutableMapOf(inputId to down.position)
                    var lastTimestamp = initialTimestamp

                    var inputsCount = 0

                    // ignore non-touch
                    if (down.type != PointerType.Touch) {
                        Log.i(TAG, "Ignoring non-touch input")
                        return@awaitEachGesture
                    }

                    do {
                        val event = awaitPointerEvent()
                        val fingerChange = event.changes.filter { it.type == PointerType.Touch }
                        // is already consumed return
                        if (fingerChange.find { it.isConsumed } != null) {
                            Log.i(TAG, "Canceling gesture - already consumed")
                            return@awaitEachGesture
                        }
                        fingerChange.forEach { change ->
                            // Consume changes and update positions
                            change.consume()
                            lastPositions[change.id] = change.position // Update latest position
                        }

                        val eventReference =
                            fingerChange.find { it.id.value == inputId.value } ?: break

                        lastTimestamp = System.currentTimeMillis()
                        inputsCount = fingerChange.size

                        if (fingerChange.any { !it.pressed }) break
                    } while (true)

                    // Calculate the total delta (movement distance) for all pointers
                    // how much it was moved
                    val totalDelta = initialPositions.keys
                        .sumOf { id ->
                            val initial = initialPositions[id] ?: Offset.Zero
                            val last = lastPositions[id] ?: Offset.Zero
                            (initial - last)
                                .getDistance()
                                .toDouble()
                        }
                        .toFloat() //TODO: fix unnecessary conversions
                    val gestureDuration = lastTimestamp - initialTimestamp
                    Log.i(TAG, "Leaving gesture. totalDelta: ${totalDelta}, gestureDuration: ${gestureDuration} ")
                    //Tolerance of 15 f for movement of fingers
                    if (totalDelta < 15f && gestureDuration < 150) {
                        // check how many fingers touched
                        when (inputsCount) {
                            1 -> {
                                //Time for double click 170ms, as it seams better:
                                if (withTimeoutOrNull(170) {
                                        val secondDown = awaitFirstDown()
                                        Log.i(
                                            TAG,
                                            "Second down detected: ${secondDown.type}, position: ${secondDown.position}"
                                        )
                                        if (secondDown.type != PointerType.Touch) {
                                            Log.i(
                                                TAG,
                                                "Ignoring non-touch input during double-tap detection"
                                            )
                                            return@withTimeoutOrNull null
                                        }
                                        if (inputsCount == 1) {
                                            resolveGesture(
                                                settings = appSettings,
                                                default = AppSettings.defaultDoubleTapAction,
                                                override = AppSettings::doubleTapAction,
                                                state = state,
                                                scope = coroutineScope,
                                                previousPage = goToPreviousPage,
                                                nextPage = goToNextPage,
                                            )
                                        }
                                        true
                                    } != null) return@awaitEachGesture

                            }

                            2 -> {
                                resolveGesture(
                                    settings = appSettings,
                                    default = AppSettings.defaultTwoFingerTapAction,
                                    override = AppSettings::twoFingerTapAction,
                                    state = state,
                                    scope = coroutineScope,
                                    previousPage = goToPreviousPage,
                                    nextPage = goToNextPage,
                                )
                            }

                        }

                    }
                    if (totalDelta < 15f && gestureDuration > 400) {
                        if (inputsCount == 1)
                            resolveGesture(
                                settings = appSettings,
                                default = AppSettings.defaultHoldAction,
                                override = AppSettings::holdAction,
                                state = state,
                                scope = coroutineScope,
                                previousPage = goToPreviousPage,
                                nextPage = goToNextPage,
                                x= lastPositions[inputId]?.x ?: 0f,
                                y= lastPositions[inputId]?.y ?: 0f
                            )
                    }
                    val lastPosition = lastPositions[inputId]
                    val initialPosition = initialPositions[inputId]
                    if (lastPosition != null && initialPosition != null) {
                        val verticalDrag = lastPosition.y - initialPosition.y
                        val horizontalDrag = lastPosition.x - initialPosition.x


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
                        if (horizontalDrag < -200) {
                            if (inputsCount == 1) {
                                resolveGesture(
                                    settings = appSettings,
                                    default = AppSettings.defaultSwipeLeftAction,
                                    override = AppSettings::swipeLeftAction,
                                    state = state,
                                    scope = coroutineScope,
                                    previousPage = goToPreviousPage,
                                    nextPage = goToNextPage,
                                )
                            } else if (inputsCount == 2) {
                                resolveGesture(
                                    settings = appSettings,
                                    default = AppSettings.defaultTwoFingerSwipeLeftAction,
                                    override = AppSettings::twoFingerSwipeLeftAction,
                                    state = state,
                                    scope = coroutineScope,
                                    previousPage = goToPreviousPage,
                                    nextPage = goToNextPage,
                                )
                            }
                        }
                        if (horizontalDrag > 200) {
                            if (inputsCount == 1) {
                                resolveGesture(
                                    settings = appSettings,
                                    default = AppSettings.defaultSwipeRightAction,
                                    override = AppSettings::swipeRightAction,
                                    state = state,
                                    scope = coroutineScope,
                                    previousPage = goToPreviousPage,
                                    nextPage = goToNextPage,
                                )
                            } else if (inputsCount == 2) {
                                resolveGesture(
                                    settings = appSettings,
                                    default = AppSettings.defaultTwoFingerSwipeRightAction,
                                    override = AppSettings::twoFingerSwipeRightAction,
                                    state = state,
                                    scope = coroutineScope,
                                    previousPage = goToPreviousPage,
                                    nextPage = goToNextPage,
                                )
                            }
                        }
                    }
                }
            }
            .fillMaxWidth()
            .fillMaxHeight()
    )
}

private fun resolveGesture(
    settings: AppSettings?,
    default: AppSettings.GestureAction,
    override: AppSettings.() -> AppSettings.GestureAction?,
    state: EditorState,
    scope: CoroutineScope,
    previousPage: () -> Unit,
    nextPage: () -> Unit,
    x: Float = 0f,
    y: Float = 0f
) {
    when (if (settings != null) override(settings) else default) {
        null -> Log.i(TAG, "No Action")
        AppSettings.GestureAction.PreviousPage -> previousPage()
        AppSettings.GestureAction.NextPage -> nextPage()

        AppSettings.GestureAction.ChangeTool ->
            state.mode = if (state.mode == Mode.Draw) Mode.Erase else Mode.Draw

        AppSettings.GestureAction.ToggleZen ->
            state.isToolbarOpen = !state.isToolbarOpen

        AppSettings.GestureAction.Undo -> {
            Log.i(TAG, "Undo")
            scope.launch {
                History.moveHistory(UndoRedoType.Undo)
//                moved to history operation - avoids unnecessary refresh, and ensures that it will be done after drawing.
//                DrawCanvas.refreshUi.emit(Unit)
            }
        }

        AppSettings.GestureAction.Redo -> {
            Log.i(TAG, "Redo")
            scope.launch {
                History.moveHistory(UndoRedoType.Redo)
//                DrawCanvas.refreshUi.emit(Unit)
            }
        }

        AppSettings.GestureAction.Select -> {
            Log.i(TAG, "select")
            scope.launch {
                DrawCanvas.imageCoordinateToSelect.emit(Pair<Int,Int> (x.toInt(),y.toInt()))
            }
        }
    }
}
