package com.olup.notable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.olup.notable.ui.theme.InkaTheme
import com.onyx.android.sdk.api.device.EpdDeviceManager
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch


@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        EpdDeviceManager.enterAnimationUpdate(true);
        setContent {
            InkaTheme {
                    Box(
                        Modifier
                            .background(Color.White)
                    ){
                        Router()
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.Black)
                    )
            }
        }
    }


    override fun onRestart() {
        super.onRestart()
        this.lifecycleScope.launch {
                DrawCanvas.restartAfterConfChange.emit(Unit)
        }
    }
}