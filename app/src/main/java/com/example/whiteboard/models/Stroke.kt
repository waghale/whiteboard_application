package com.example.whiteboard.models

import kotlinx.serialization.Serializable
import java.util.UUID



@Serializable
data class Stroke(
    var id: String = UUID.randomUUID().toString(),
    var points: List<Point> = emptyList(),
    var color: String,
    var width: Float,
    var isEraser: Boolean = false,
    var rotationAngle: Float = 0f
)