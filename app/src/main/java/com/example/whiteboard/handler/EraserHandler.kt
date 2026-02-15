package com.example.whiteboard.handler



import android.graphics.*
import android.util.Log
import com.example.whiteboard.models.Point
import com.example.whiteboard.models.Stroke
import com.example.whiteboard.viewmodels.WhiteboardViewModel
import com.example.whiteboard.views.DrawingView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt



class EraserHandler(
    private val view: DrawingView,
    private val viewModel: WhiteboardViewModel?,
    private val drawingHandler: DrawingHandler
) {
    private var eraseStart: PointF? = null
    private var eraseEnd: PointF? = null
    private var eraserPath = Path()
    private var eraserPoints = mutableListOf<Point>()

    fun startErasing(x: Float, y: Float, vm: WhiteboardViewModel) {
        when (vm.getEraserShape()) {
            WhiteboardViewModel.EraserShape.SELECT_AREA -> {
                eraseStart = PointF(x, y)
                eraseEnd = PointF(x, y)
            }
            WhiteboardViewModel.EraserShape.FREEFORM -> {
                eraserPath.reset()
                eraserPoints.clear()
                eraserPath.moveTo(x, y)
                eraserPoints.add(Point(x, y))
                Log.d("EraserHandler", "Freeform eraser started at ($x, $y)")
            }
            else -> drawingHandler.startFreehand(x, y, vm, isEraser = true)
        }
    }

    fun startFreeformErasing(x: Float, y: Float, vm: WhiteboardViewModel) {
        eraserPath.reset()
        eraserPoints.clear()
        eraserPath.moveTo(x, y)
        eraserPoints.add(Point(x, y))
        Log.d("EraserHandler", "Freeform eraser started at ($x, $y)")
    }

    fun updateErasing(x: Float, y: Float, vm: WhiteboardViewModel) {
        when (vm.getEraserShape()) {
            WhiteboardViewModel.EraserShape.SELECT_AREA -> eraseEnd = PointF(x, y)
            WhiteboardViewModel.EraserShape.FREEFORM -> {
                eraserPath.lineTo(x, y)
                eraserPoints.add(Point(x, y))
                Log.d("EraserHandler", "Eraser path updated: ${eraserPoints.size} points, current point ($x, $y)")
                checkEraserIntersections(vm)
            }
            else -> drawingHandler.continueFreehand(x, y)
        }
    }

    fun updateFreeformErasing(x: Float, y: Float, vm: WhiteboardViewModel) {
        eraserPath.lineTo(x, y)
        eraserPoints.add(Point(x, y))
        Log.d("EraserHandler", "Eraser path updated: ${eraserPoints.size} points, current point ($x, $y)")
        checkEraserIntersections(vm)
    }

    fun commitEraser(vm: WhiteboardViewModel) {
        when (vm.getEraserShape()) {
            WhiteboardViewModel.EraserShape.SELECT_AREA -> {
                eraseStart?.let { start ->
                    eraseEnd?.let { end ->
                        val eraseRect = RectF(min(start.x, end.x), min(start.y, end.y), max(start.x, end.x), max(start.y, end.y))
                        vm.shapes.value?.toList()?.forEach { shape ->
                            val bounds = shape.bounds?.toRectF() ?: if (shape.type == WhiteboardViewModel.ShapeType.POLYGON)
                                computePolygonBounds(shape.points) else null
                            bounds?.let { if (RectF.intersects(it, eraseRect)) vm.removeShape(shape) }
                        }
                        vm.eraseArea(eraseRect)
                    }
                }
                eraseStart = null
                eraseEnd = null
            }
            WhiteboardViewModel.EraserShape.FREEFORM -> {
                eraserPath.reset()
                eraserPoints.clear()
                Log.d("EraserHandler", "Freeform eraser committed")
            }
            else -> drawingHandler.commitFreehand(vm, isEraser = true)
        }
    }

    fun commitFreeformErasing(vm: WhiteboardViewModel) {
        eraserPath.reset()
        eraserPoints.clear()
        Log.d("EraserHandler", "Freeform eraser committed")
    }

    fun drawEraserRectangle(canvas: Canvas) {
        eraseStart?.let { start ->
            eraseEnd?.let { end ->
                val erasePaint = Paint().apply {
                    style = Paint.Style.STROKE
                    color = Color.RED
                    strokeWidth = 3f
                    pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
                }
                canvas.drawRect(min(start.x, end.x), min(start.y, end.y), max(start.x, end.x), max(start.y, end.y), erasePaint)
            }
        }
    }

    fun drawFreeformEraser(canvas: Canvas, vm: WhiteboardViewModel) {
        if (vm.getTool() == WhiteboardViewModel.Tool.ERASER && vm.getEraserShape() == WhiteboardViewModel.EraserShape.FREEFORM) {
            val eraserPaint = Paint().apply {
                style = Paint.Style.STROKE
                color = Color.RED
                strokeWidth = vm.getStrokeWidth() * 1.2f
                isAntiAlias = true
                alpha = 200
            }
            canvas.drawPath(eraserPath, eraserPaint)
        }
    }

    private fun checkEraserIntersections(vm: WhiteboardViewModel) {
        val eraserStrokeWidth = vm.getStrokeWidth()
        Log.d("EraserHandler", "Checking intersections with eraser stroke width: $eraserStrokeWidth")

        val eraserBounds = RectF()
        eraserPath.computeBounds(eraserBounds, true)
        eraserBounds.inset(-eraserStrokeWidth / 8, -eraserStrokeWidth / 8)
        Log.d("EraserHandler", "Eraser bounds: $eraserBounds")

        val eraserSegments = mutableListOf<Pair<PointF, PointF>>()
        var lastPoint: PointF? = null
        val pathMeasure = PathMeasure(eraserPath, false)
        val pathLength = pathMeasure.length
        val points = FloatArray(2)
        var distance = 0f
        while (distance < pathLength) {
            pathMeasure.getPosTan(distance, points, null)
            val currentPoint = PointF(points[0], points[1])
            if (lastPoint != null) {
                eraserSegments.add(Pair(lastPoint!!, currentPoint))
            }
            lastPoint = currentPoint
            distance += 1f
        }
        Log.d("EraserHandler", "Eraser path segments: ${eraserSegments.size}")

        val strokes = vm.state.value?.strokes?.toMutableList() ?: mutableListOf()
        val strokesToRemove = mutableListOf<Stroke>()
        strokes.forEach { stroke ->
            val strokeBounds = RectF()
            stroke.points.forEach { p -> strokeBounds.union(p.x, p.y) }
            strokeBounds.inset(-stroke.width / 4, -stroke.width / 4)
            if (RectF.intersects(strokeBounds, eraserBounds)) {
                Log.d("EraserHandler", "Stroke ${stroke.id} bounds intersect: $strokeBounds")
                var intersects = false
                for (i in 0 until stroke.points.size - 1) {
                    val p1 = PointF(stroke.points[i].x, stroke.points[i].y)
                    val p2 = PointF(stroke.points[i + 1].x, stroke.points[i + 1].y)
                    for (eraserSegment in eraserSegments) {
                        if (doLineSegmentsIntersect(p1, p2, eraserSegment.first, eraserSegment.second, eraserStrokeWidth, stroke.width)) {
                            intersects = true
                            break
                        }
                    }
                    if (intersects) break
                }
                if (intersects) {
                    strokesToRemove.add(stroke)
                    Log.d("EraserHandler", "Intersection confirmed for stroke ${stroke.id}")
                }
            }
        }
        if (strokesToRemove.isNotEmpty()) {
            vm.removeStrokes(strokesToRemove)
            Log.d("EraserHandler", "Removed ${strokesToRemove.size} strokes")
        }

        val shapes = vm.shapes.value?.toMutableList() ?: mutableListOf()
        val shapesToRemove = mutableListOf<com.example.whiteboard.models.Shape>()
        shapes.forEach { shape ->
            val shapeBounds = shape.bounds?.toRectF() ?: computePolygonBounds(shape.points)
            if (RectF.intersects(shapeBounds, eraserBounds)) {
                Log.d("EraserHandler", "Shape ${shape.type} bounds intersect: $shapeBounds")
                var intersects = false
                when (shape.type) {
                    WhiteboardViewModel.ShapeType.RECTANGLE -> {
                        val rectPoints = listOf(
                            PointF(shapeBounds.left, shapeBounds.top),
                            PointF(shapeBounds.right, shapeBounds.top),
                            PointF(shapeBounds.right, shapeBounds.bottom),
                            PointF(shapeBounds.left, shapeBounds.bottom)
                        )
                        for (i in rectPoints.indices) {
                            val p1 = rectPoints[i]
                            val p2 = rectPoints[(i + 1) % rectPoints.size]
                            for (eraserSegment in eraserSegments) {
                                if (doLineSegmentsIntersect(p1, p2, eraserSegment.first, eraserSegment.second, eraserStrokeWidth, shape.strokeWidth)) {
                                    intersects = true
                                    break
                                }
                            }
                            if (intersects) break
                        }
                    }
                    WhiteboardViewModel.ShapeType.CIRCLE -> {
                        val centerX = shapeBounds.centerX()
                        val centerY = shapeBounds.centerY()
                        val radius = min(shapeBounds.width(), shapeBounds.height()) / 2
                        for (eraserSegment in eraserSegments) {
                            if (doesLineSegmentIntersectCircle(eraserSegment.first, eraserSegment.second, PointF(centerX, centerY), radius, eraserStrokeWidth)) {
                                intersects = true
                                break
                            }
                        }
                    }
                    WhiteboardViewModel.ShapeType.LINE -> {
                        if (shape.points.size >= 2) {
                            val p1 = PointF(shape.points[0].x, shape.points[0].y)
                            val p2 = PointF(shape.points[1].x, shape.points[1].y)
                            for (eraserSegment in eraserSegments) {
                                if (doLineSegmentsIntersect(p1, p2, eraserSegment.first, eraserSegment.second, eraserStrokeWidth, shape.strokeWidth)) {
                                    intersects = true
                                    break
                                }
                            }
                        }
                    }
                    WhiteboardViewModel.ShapeType.POLYGON -> {
                        for (i in shape.points.indices) {
                            val p1 = PointF(shape.points[i].x, shape.points[i].y)
                            val p2 = PointF(shape.points[(i + 1) % shape.points.size].x, shape.points[(i + 1) % shape.points.size].y)
                            for (eraserSegment in eraserSegments) {
                                if (doLineSegmentsIntersect(p1, p2, eraserSegment.first, eraserSegment.second, eraserStrokeWidth, shape.strokeWidth)) {
                                    intersects = true
                                    break
                                }
                            }
                            if (intersects) break
                        }
                    }
                }
                if (intersects) {
                    shapesToRemove.add(shape)
                    Log.d("EraserHandler", "Intersection confirmed for shape ${shape.type}")
                }
            }
        }
        shapesToRemove.forEach { shape ->
            vm.removeShape(shape)
            Log.d("EraserHandler", "Removed shape: ${shape.type}")
        }

        val textBoxes = vm.textBoxes.value?.toMutableList() ?: mutableListOf()
        val textBoxesToRemove = mutableListOf<com.example.whiteboard.models.TextBox>()
        textBoxes.forEach { textBox ->
            val textBounds = textBox.bounds.toRectF()
            if (RectF.intersects(textBounds, eraserBounds)) {
                Log.d("EraserHandler", "TextBox ${textBox.text} bounds intersect: $textBounds")
                val rectPoints = listOf(
                    PointF(textBounds.left, textBounds.top),
                    PointF(textBounds.right, textBounds.top),
                    PointF(textBounds.right, textBounds.bottom),
                    PointF(textBounds.left, textBounds.bottom)
                )
                var intersects = false
                for (i in rectPoints.indices) {
                    val p1 = rectPoints[i]
                    val p2 = rectPoints[(i + 1) % rectPoints.size]
                    for (eraserSegment in eraserSegments) {
                        if (doLineSegmentsIntersect(p1, p2, eraserSegment.first, eraserSegment.second, eraserStrokeWidth, 1f)) {
                            intersects = true
                            break
                        }
                    }
                    if (intersects) break
                }
                if (intersects) {
                    textBoxesToRemove.add(textBox)
                    Log.d("EraserHandler", "Intersection confirmed for textbox ${textBox.text}")
                }
            }
        }
        textBoxesToRemove.forEach { textBox ->
            vm.removeTextBox(textBox)
            Log.d("EraserHandler", "Removed textbox: ${textBox.text}")
        }
    }

    private fun doLineSegmentsIntersect(p1: PointF, p2: PointF, p3: PointF, p4: PointF, width1: Float, width2: Float): Boolean {
        val box1 = RectF(
            minOf(p1.x, p2.x) - width1 / 2,
            minOf(p1.y, p2.y) - width1 / 2,
            maxOf(p1.x, p2.x) + width1 / 2,
            maxOf(p1.y, p2.y) + width1 / 2
        )
        val box2 = RectF(
            minOf(p3.x, p4.x) - width2 / 2,
            minOf(p3.y, p4.y) - width2 / 2,
            maxOf(p3.x, p4.x) + width2 / 2,
            maxOf(p3.y, p4.y) + width2 / 2
        )
        if (!box1.intersect(box2)) return false

        val o1 = orientation(p1, p2, p3)
        val o2 = orientation(p1, p2, p4)
        val o3 = orientation(p3, p4, p1)
        val o4 = orientation(p3, p4, p2)

        if (o1 != o2 && o3 != o4) return true

        if (o1 == 0 && onSegment(p1, p3, p2)) return true
        if (o2 == 0 && onSegment(p1, p4, p2)) return true
        if (o3 == 0 && onSegment(p3, p1, p4)) return true
        if (o4 == 0 && onSegment(p3, p2, p4)) return true

        return false
    }

    private fun orientation(p: PointF, q: PointF, r: PointF): Int {
        val value = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y)
        return when {
            abs(value) < 0.0001f -> 0
            value > 0 -> 1
            else -> -1
        }
    }

    private fun onSegment(p: PointF, q: PointF, r: PointF): Boolean {
        return q.x <= maxOf(p.x, r.x) && q.x >= minOf(p.x, r.x) &&
                q.y <= maxOf(p.y, r.y) && q.y >= minOf(p.y, r.y)
    }

    private fun doesLineSegmentIntersectCircle(p1: PointF, p2: PointF, center: PointF, radius: Float, strokeWidth: Float): Boolean {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val fx = p1.x - center.x
        val fy = p1.y - center.y

        val a = dx * dx + dy * dy
        val b = 2 * (fx * dx + fy * dy)
        val c = fx * fx + fy * fy - (radius + strokeWidth / 2) * (radius + strokeWidth / 2)
        val discriminant = b * b - 4 * a * c

        if (discriminant < 0) return false

        val t1 = (-b + sqrt(discriminant)) / (2 * a)
        val t2 = (-b - sqrt(discriminant)) / (2 * a)

        return (t1 in 0.0..1.0 || t2 in 0.0..1.0)
    }

    private fun computePolygonBounds(points: List<Point>): RectF {
        val left = points.minOf { it.x }
        val top = points.minOf { it.y }
        val right = points.maxOf { it.x }
        val bottom = points.maxOf { it.y }
        return RectF(left, top, right, bottom)
    }
}