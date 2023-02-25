package com.example.inka

import android.graphics.Color
import com.onyx.android.sdk.pen.style.StrokeStyle

enum class Pen {
    BALLPEN,
    PENCIL,
    BRUSH,
    MARKER,
    FOUNTAIN
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

val penSettings = mapOf(
    Pen.BALLPEN to PenSetting(7f, Color.BLACK),
    Pen.PENCIL to PenSetting(7f, Color.BLACK),
    Pen.BRUSH to PenSetting(7f, Color.BLACK),
    Pen.MARKER to PenSetting(7f, Color.BLACK),
    Pen.FOUNTAIN to PenSetting(7f, Color.BLACK)


)