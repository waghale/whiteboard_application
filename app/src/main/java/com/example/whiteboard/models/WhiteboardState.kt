package com.example.whiteboard.models

import kotlinx.serialization.Serializable


//@Serializable
////data class WhiteboardState(
////    val strokes: List<Stroke> = emptyList(),
////    val width: Int = 1920,
////    val height: Int = 1080,
////    val createdAt: Long = System.currentTimeMillis()
////)

@Serializable
data class WhiteboardState(
    val strokes: List<Stroke> = emptyList(),
    val shapes: List<Shape> = emptyList(),
    val textBoxes: List<TextBox> = emptyList()
)
