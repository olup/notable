package com.olup.notable

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
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import io.shipbook.shipbooksdk.Log
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

                    var inputsCount = 1
                    var holdConsumed = false

                    // ignore non-touch
                    if (down.type != PointerType.Touch) {
                        Log.i(TAG, "Ignoring non-touch input")
                        return@awaitEachGesture
                    }

                    do {
                        // Wait for a pointer event or a timeout (e.g., 100ms)
                        val event = withTimeoutOrNull(1000L) { awaitPointerEvent() }

                        if (event != null) {
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
                            inputsCount = fingerChange.size
                            if (fingerChange.any { !it.pressed }) {
                                lastTimestamp = System.currentTimeMillis()
                                break
                            }
                        }
                        // events are only send on change, so we need to check for holding in place separately
                        lastTimestamp = System.currentTimeMillis()

                        val elapsedTime = lastTimestamp - initialTimestamp
                        if (elapsedTime >= 300 && inputsCount == 1 && !holdConsumed) {
                            Log.i(TAG, "Held for ${elapsedTime}ms")
                            if (calculateTotalDelta(initialPositions, lastPositions) < 15f)
                                resolveGesture(
                                    settings = appSettings,
                                    default = AppSettings.defaultHoldAction,
                                    override = AppSettings::holdAction,
                                    state = state,
                                    scope = coroutineScope,
                                    previousPage = goToPreviousPage,
                                    nextPage = goToNextPage,
                                    x = lastPositions[inputId]?.x ?: 0f,
                                    y = lastPositions[inputId]?.y ?: 0f
                                )
                            holdConsumed = true
                        }

                    } while (true)

                    // Calculate the total delta (movement distance) for all pointers
                    val totalDelta = calculateTotalDelta(initialPositions, lastPositions)
                    val gestureDuration = lastTimestamp - initialTimestamp
                    Log.i(
                        TAG,
                        "Leaving gesture. totalDelta: ${totalDelta}, gestureDuration: $gestureDuration "
                    )
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
                    val lastPosition = lastPositions[inputId]
                    val initialPosition = initialPositions[inputId]
                    if (lastPosition != null && initialPosition != null) {
                        val verticalDrag = lastPosition.y - initialPosition.y
                        val horizontalDrag = lastPosition.x - initialPosition.x

                        // Determine if the movement is primarily vertical or horizontal
                        val isVerticalMove =
                            kotlin.math.abs(verticalDrag) > kotlin.math.abs(horizontalDrag)
                        val isHorizontalMove =
                            kotlin.math.abs(horizontalDrag) > kotlin.math.abs(verticalDrag)

                        // Handle vertical movements
                        if (isVerticalMove && inputsCount == 1) {
                            coroutineScope.launch {
                                when {
                                    verticalDrag < -200 -> {
                                        controlTower.onSingleFingerVerticalSwipe(
                                            SimplePointF(initialPosition.x, initialPosition.y),
                                            verticalDrag.toInt()
                                        )
                                    }

                                    verticalDrag > 200 -> {
                                        controlTower.onSingleFingerVerticalSwipe(
                                            SimplePointF(initialPosition.x, initialPosition.y),
                                            verticalDrag.toInt()
                                        )
                                    }
                                }
                            }
                        }

                        // Handle horizontal movements
                        if (isHorizontalMove) {
                            when {
                                horizontalDrag < -200 -> {
                                    resolveGesture(
                                        settings = appSettings,
                                        default = if (inputsCount == 1) AppSettings.defaultSwipeLeftAction else AppSettings.defaultTwoFingerSwipeLeftAction,
                                        override = if (inputsCount == 1) AppSettings::swipeLeftAction else AppSettings::twoFingerSwipeLeftAction,
                                        state = state,
                                        scope = coroutineScope,
                                        previousPage = goToPreviousPage,
                                        nextPage = goToNextPage,
                                    )
                                }

                                horizontalDrag > 200 -> {
                                    resolveGesture(
                                        settings = appSettings,
                                        default = if (inputsCount == 1) AppSettings.defaultSwipeRightAction else AppSettings.defaultTwoFingerSwipeRightAction,
                                        override = if (inputsCount == 1) AppSettings::swipeRightAction else AppSettings::twoFingerSwipeRightAction,
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
                DrawCanvas.imageCoordinateToSelect.emit(Pair(x.toInt(), y.toInt()))
            }
        }
    }
}

// Calculate movement of fingers since touched
fun calculateTotalDelta(
    initialPositions: Map<PointerId, Offset>,
    lastPositions: Map<PointerId, Offset>
): Float {
    return initialPositions.keys.sumOf { id ->
        val initial = initialPositions[id] ?: Offset.Zero
        val last = lastPositions[id] ?: Offset.Zero
        (initial - last).getDistance().toDouble()
    }.toFloat()
}