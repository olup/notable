package com.example.inka

import android.graphics.RectF
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AppState() {
    var pen by mutableStateOf(Pen.BALLPEN) // should save
    var strokeSize by mutableStateOf( 10f) // should save
    var isToolbarOpen by mutableStateOf(false) // should save
    var mode by mutableStateOf(Mode.DRAW) // should save
}