package com.example.whiteboard.models

data class SelectedItems(
    val shapes: MutableList<Shape> = mutableListOf(),
    val strokes: MutableList<Stroke> = mutableListOf(),
    val textBoxes: MutableList<TextBox> = mutableListOf()
) {
    fun isNotEmpty(): Boolean {
        return shapes.isNotEmpty() || strokes.isNotEmpty() || textBoxes.isNotEmpty()
    }
}


