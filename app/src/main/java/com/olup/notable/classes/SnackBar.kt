package com.olup.notable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

val LocalSnackContext = staticCompositionLocalOf { SnackState() }

data class SnackConf(
    val id: String = UUID.randomUUID().toString(),
    val text: String? = null,
    val duration: Int? = null,
    val content: (@Composable () -> Unit)? = null,
    val actions: List<Pair<String, () -> Unit>>? = null
)

class SnackState {
    val snackFlow = MutableSharedFlow<SnackConf?>()
    val cancelSnackFlow = MutableSharedFlow<String?>()
    suspend fun displaySnack(conf: SnackConf): suspend () -> Unit {
        snackFlow.emit(conf)
        return suspend {
            removeSnack(conf.id)
        }
    }

    // TODO: check if this is a good approach,
    // this does work, but I have doubts if it is a proper way for doing it
    // Register Observers for Global Actions
    companion object {
        val globalSnackFlow = MutableSharedFlow<SnackConf>()
    }

    fun registerGlobalSnackObserver() {
        CoroutineScope(Dispatchers.Main).launch {
            globalSnackFlow.collect {
                displaySnack(it)
            }
        }
    }

    private suspend fun removeSnack(id: String) {
        cancelSnackFlow.emit(id)
    }
}

@Composable
fun SnackBar(state: SnackState) {
    val snacks = remember {
        mutableStateListOf<SnackConf>()
    }

    fun getSnacks() = snacks

    LaunchedEffect(Unit) {
        launch {
            state.cancelSnackFlow.collect { snackId ->
                getSnacks().removeIf { it.id == snackId }
            }
        }
        launch {
            state.snackFlow.collect { snack ->
                if (snack != null) {
                    getSnacks().add(snack)
                    if (snack.duration != null) {
                        launch {
                            delay(snack.duration.toLong())
                            getSnacks().removeIf { it.id == snack.id }
                        }
                    }
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxHeight()
            .padding(3.dp), verticalArrangement = Arrangement.Bottom
    ) {
        snacks.map {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Black)
                        .padding(15.dp, 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (it.text != null) {
                        Row {
                            Text(text = it.text, color = Color.White)
                            if (it.actions != null && it.actions.isEmpty().not()) {
                                it.actions.map {
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = it.first,
                                        color = Color.White,
                                        modifier = Modifier.noRippleClickable { it.second() })
                                }
                            }
                        }

                    } else it.content?.let { content ->
                        content()
                    }
                }
            }
        }
    }
}