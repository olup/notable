package com.olup.notable

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import io.shipbook.shipbooksdk.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.olup.notable.ui.theme.InkaTheme
import com.onyx.android.sdk.api.device.epd.EpdController
import io.shipbook.shipbooksdk.ShipBook
import kotlinx.coroutines.launch


var SCREEN_WIDTH = EpdController.getEpdHeight().toInt()
var SCREEN_HEIGHT = EpdController.getEpdWidth().toInt()

var TAG = "MainActivity"

@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ShipBook.start(
            this.application, "648adf9364c9825976c1d57e",
            "7c53dffa949e3b55e37ab04672138feb"
        );

        Log.i(TAG, "Notable started")


        if (SCREEN_WIDTH == 0) {
            SCREEN_WIDTH = applicationContext.resources.displayMetrics.widthPixels
            SCREEN_HEIGHT = applicationContext.resources.displayMetrics.heightPixels
        }

        val snackState = SnackState()
        snackState.registerGlobalSnackObserver()
        // Refactor - we prob don't need this
        EditorSettingCacheManager.init(applicationContext)


        //EpdDeviceManager.enterAnimationUpdate(true);

        val intentData = intent.data?.lastPathSegment
        setContent {
            InkaTheme {
                CompositionLocalProvider(SnackContext provides snackState) {
                    Box(
                        Modifier
                            .background(Color.White)
                    ) {
                        Router()
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.Black)
                    )
                    SnackBar(state = snackState)
                }
            }
        }
    }


    override fun onRestart() {
        super.onRestart()
        // redraw after device sleep
        this.lifecycleScope.launch {
            DrawCanvas.restartAfterConfChange.emit(Unit)
        }
    }

    override fun onPause() {
        super.onPause()
        this.lifecycleScope.launch {
            DrawCanvas.refreshUi.emit(Unit)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        this.lifecycleScope.launch {
            DrawCanvas.refreshUi.emit(Unit)
        }
    }

    // when the screen orientation is changed, set new screen width  restart is not necessary,
    // as we need first to update page dimensions which is done in EditorView
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.i(TAG, "Switched to Landscape")
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.i(TAG, "Switched to Portrait")
        }
        SCREEN_WIDTH = applicationContext.resources.displayMetrics.widthPixels
        SCREEN_HEIGHT = applicationContext.resources.displayMetrics.heightPixels
//        this.lifecycleScope.launch {
//            DrawCanvas.restartAfterConfChange.emit(Unit)
//        }
    }
}