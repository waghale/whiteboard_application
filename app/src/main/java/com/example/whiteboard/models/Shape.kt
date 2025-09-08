package com.example.whiteboard.models

import android.graphics.RectF
import com.example.whiteboard.viewmodels.WhiteboardViewModel

import kotlinx.serialization.Serializable

//@Serializable
//data class Shape(
//    val type: ShapeType,
//    @Contextual val bounds: RectF = RectF(), // <-- tell serializer to handle this contextually
//    val points: List<Point> = emptyList(), // For polygon
//    val color: String = "#FF000000",
//    val strokeWidth: Float = 8f
//)


@kotlinx.serialization.Serializable
data class Shape(
    val type: WhiteboardViewModel.ShapeType,
    val bounds: RectFSerializable? = null, // nullable for polygons
    val points: List<Point> = emptyList(),
    val color: String = "#FF000000",
    val strokeWidth: Float = 8f
) {
    fun getBoundsRectF(): RectF? {
        return bounds?.toRectF()
    }
}





