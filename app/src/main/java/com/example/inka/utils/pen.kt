package com.example.inka

import android.graphics.Color
import com.onyx.android.sdk.pen.style.StrokeStyle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

enum class Pen (val penName : String) {
    BALLPEN("BALLPEN"),
    PENCIL("PENCIL"),
    BRUSH("BRUSH"),
    MARKER("MARKER"),
    FOUNTAIN("FOUNTAIN")
}

fun penToStroke(pen: Pen): Int {
    return when (pen) {
        Pen.BALLPEN -> StrokeStyle.PENCIL
        Pen.PENCIL -> StrokeStyle.CHARCOAL
        Pen.BRUSH -> StrokeStyle.NEO_BRUSH
        Pen.MARKER -> StrokeStyle.MARKER
        Pen.FOUNTAIN -> StrokeStyle.BRUSH
    }
}



data class PenSetting(
    var strokeSize: Float,
    var color: Int
)
