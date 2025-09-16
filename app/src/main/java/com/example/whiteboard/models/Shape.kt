package com.example.whiteboard.models

import android.graphics.RectF
import com.example.whiteboard.viewmodels.WhiteboardViewModel

import kotlinx.serialization.Serializable



@Serializable
data class Shape(
    var id: String = java.util.UUID.randomUUID().toString(), // 🔹 unique id
    var type: WhiteboardViewModel.ShapeType,
    var bounds: RectFSerializable? = null, // nullable for polygons
    var points: List<Point> = emptyList(),
    var color: String = "#FF000000",
    var strokeWidth: Float = 8f,
    var rotationAngle: Float = 0f,
    var startAngle: Float? = null, // Added for arcs
    var endAngle: Float? = null    // Added for arcs
) {
    fun getBoundsRectF(): RectF? {
        return bounds?.toRectF()
    }

    // Custom deep copy function to avoid conflict with auto-generated copy
    fun deepCopy(
        id: String = this.id,
        type: WhiteboardViewModel.ShapeType = this.type,
        bounds: RectFSerializable? = this.bounds?.let { RectFSerializable(it.left, it.top, it.right, it.bottom) },
        points: List<Point> = this.points.map { it.copy() }, // Deep copy points
        color: String = this.color,
        strokeWidth: Float = this.strokeWidth,
        rotationAngle: Float = this.rotationAngle,
        startAngle: Float? = this.startAngle,
        endAngle: Float? = this.endAngle
    ) = Shape(id, type, bounds, points, color, strokeWidth, rotationAngle, startAngle, endAngle)
}














