package com.example.whiteboard.models

import kotlinx.serialization.Serializable




@Serializable
data class WhiteboardState(
    var strokes: List<Stroke> = emptyList(),
    var shapes: List<Shape> = emptyList(),
    var textBoxes: List<TextBox> = emptyList()
)
