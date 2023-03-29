package com.olup.notable

import com.onyx.android.sdk.pen.style.StrokeStyle

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
        Pen.FOUNTAIN -> StrokeStyle.FOUNTAIN
    }
}


@kotlinx.serialization.Serializable
data class PenSetting(
    var strokeSize: Float,
    var color: Int
)

typealias NamedSettings = Map<String, PenSetting>
