package com.example.whiteboard.handler



import android.graphics.Matrix
import android.view.MotionEvent
import com.example.whiteboard.models.Point
import com.example.whiteboard.models.SelectedItems
import com.example.whiteboard.viewmodels.WhiteboardViewModel
import kotlin.math.atan2

class RotationHandler(
    private val viewModel: WhiteboardViewModel?
) {
    private var lastRotationAngle: Float? = null

    fun handleRotation(event: MotionEvent, items: SelectedItems) {
        if (event.pointerCount < 2) return
        val x1 = event.getX(0)
        val y1 = event.getY(0)
        val x2 = event.getX(1)
        val y2 = event.getY(1)
        val currentAngle = Math.toDegrees(
            atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
        ).toFloat()
        if (lastRotationAngle == null) {
            lastRotationAngle = currentAngle
            return
        }
        val deltaAngle = currentAngle - lastRotationAngle!!
        items.shapes.forEach { shape ->
            shape.rotationAngle = (shape.rotationAngle + deltaAngle) % 360
            if (shape.type == WhiteboardViewModel.ShapeType.POLYGON) {
                val centerX = shape.points.map { it.x }.average().toFloat()
                val centerY = shape.points.map { it.y }.average().toFloat()
                val matrix = Matrix()
                matrix.postRotate(deltaAngle, centerX, centerY)
                shape.points = shape.points.map { point ->
                    val pointArray = floatArrayOf(point.x, point.y)
                    matrix.mapPoints(pointArray)
                    Point(pointArray[0], pointArray[1])
                }
            }
            viewModel?.updateShape(shape)
        }
        items.textBoxes.forEach { tb ->
            tb.rotationAngle = (tb.rotationAngle + deltaAngle) % 360
            viewModel?.updateTextBox(tb)
        }
        items.strokes.forEach { stroke ->
            stroke.rotationAngle = (stroke.rotationAngle + deltaAngle) % 360
            viewModel?.updateStroke(stroke)
        }
        lastRotationAngle = currentAngle
    }

    fun resetRotationAngle() {
        lastRotationAngle = null
    }
}