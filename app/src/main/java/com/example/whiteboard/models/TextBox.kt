package com.example.whiteboard.models

import android.graphics.Paint
import android.graphics.Typeface
import kotlinx.serialization.Serializable




@Serializable
data class TextBox(
    val id: String,
    var text: String,
    var bounds: RectFSerializable,
    var color: String,
    var textSize: Float,
    var typeface: Int,
    var fontFamily: String,
    var alignment: Paint.Align = Paint.Align.LEFT,
    var rotationAngle: Float = 0f
)




