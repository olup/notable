package com.example.inka

data class Point(
    var x: Float = 0f,
    var y: Float = 0f,
    var pressure: Float = 0f,
    var size: Float = 0f,
    var tiltX: Int = 0,
    var tiltY: Int = 0,
    var timestamp: Long = 0,
)