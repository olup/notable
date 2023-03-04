package com.example.inka

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Path
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData
import com.example.inka.db.Stroke
import com.example.inka.ui.theme.InkaTheme
import com.onyx.android.sdk.api.device.EpdDeviceManager
import com.onyx.android.sdk.utils.ResManager




@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
class MainActivity : ComponentActivity() {
    private val restartCountState = MutableLiveData(0)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        EpdDeviceManager.enterAnimationUpdate(true);
        setContent {
            val restartCount = restartCountState.observeAsState()

            InkaTheme {
                    Box(
                        Modifier
                            .background(Color.White)
                    ){
                        Router(restartCount.value!!)
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
        restartCountState.value = restartCountState.value!! + 1
    }
}