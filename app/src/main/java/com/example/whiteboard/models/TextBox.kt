package com.example.whiteboard.models

import android.graphics.Paint
import android.graphics.Typeface
import kotlinx.serialization.Serializable

//
//data class TextBox(
//    val id: String,
//    var text: String,
//    var x: Float,
//    var y: Float,
//    var color: String = "#FF000000",
//    var textSize: Float = 32f,
//    var typeface: Int = Typeface.NORMAL, // Typeface.NORMAL, BOLD, ITALIC
//    var fontFamily: String = "sans-serif"
//)

//@Serializable
//data class TextBox(
//    val id: String,
//    var text: String,
//    var bounds: RectFSerializable,   // area selection
//    var color: String,
//    var textSize: Float,
//    var typeface: Int,
//    var fontFamily: String
//)

@Serializable
data class TextBox(
    val id: String,
    var text: String,
    var bounds: RectFSerializable,
    var color: String,
    var textSize: Float,
    var typeface: Int,
    var fontFamily: String,
    var alignment: Paint.Align = Paint.Align.LEFT
)




