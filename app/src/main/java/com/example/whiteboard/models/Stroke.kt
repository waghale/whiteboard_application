package com.example.whiteboard.models

import kotlinx.serialization.Serializable
import java.util.UUID



@Serializable
data class Stroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<Point> = emptyList(),
    val color: String,
    val width: Float,
    val isEraser: Boolean = false
)