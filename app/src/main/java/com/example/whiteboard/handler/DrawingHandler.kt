package com.example.whiteboard.handler

import android.graphics.*
import com.example.whiteboard.models.Point
import com.example.whiteboard.models.RectFSerializable
import com.example.whiteboard.models.SelectedItems
import com.example.whiteboard.models.Stroke
import com.example.whiteboard.viewmodels.WhiteboardViewModel
import com.example.whiteboard.views.DrawingView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.cos
import kotlin.math.sin
import android.graphics.Typeface




class DrawingHandler(
    private val view: DrawingView,
    private val viewModel: WhiteboardViewModel?
) {
    private var shapeStart: PointF? = null
    private var shapeEnd: PointF? = null
    private val path = Path()
    private val points = ArrayList<PointF>()
    private var lastMidPoint: PointF? = null // For Bezier curve smoothing
    private val drawPaint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 8f
    }
    private val drawTextPaint = Paint().apply { isAntiAlias = true }

    fun drawShapes(canvas: Canvas, vm: WhiteboardViewModel) {
        vm.shapes.value?.forEach { shape ->
            val paint = Paint(drawPaint).apply {
                strokeWidth = shape.strokeWidth
                style = Paint.Style.STROKE
                color = try { Color.parseColor(shape.color) } catch (_: Exception) { Color.BLACK }
                isAntiAlias = true
                pathEffect = null
            }
            when (shape.type) {
                WhiteboardViewModel.ShapeType.RECTANGLE,
                WhiteboardViewModel.ShapeType.CIRCLE,
                WhiteboardViewModel.ShapeType.LINE -> {
                    shape.bounds?.toRectF()?.let { bounds ->
                        val cx = bounds.centerX()
                        val cy = bounds.centerY()
                        canvas.save()
                        canvas.rotate(shape.rotationAngle, cx, cy)
                        when (shape.type) {
                            WhiteboardViewModel.ShapeType.RECTANGLE -> canvas.drawRect(bounds, paint)
                            WhiteboardViewModel.ShapeType.CIRCLE -> {
                                val radius = min(bounds.width(), bounds.height()) / 2
                                canvas.drawCircle(cx, cy, radius, paint)
                            }
                            WhiteboardViewModel.ShapeType.LINE -> canvas.drawLine(bounds.left, bounds.top, bounds.right, bounds.bottom, paint)
                            else -> {}
                        }
                        canvas.restore()
                    }
                }
                WhiteboardViewModel.ShapeType.POLYGON -> {
                    if (shape.points.size >= 3) {
                        val path = Path()
                        shape.points.forEachIndexed { i, point ->
                            if (i == 0) path.moveTo(point.x, point.y)
                            else path.lineTo(point.x, point.y)
                        }
                        path.close()
                        canvas.drawPath(path, paint)
                    }
                }
            }
        }
    }

    fun drawTextBoxes(canvas: Canvas, vm: WhiteboardViewModel) {
        vm.textBoxes.value?.forEach { tb ->
            val rect = tb.bounds.toRectF()
            val cx = rect.centerX()
            val cy = rect.centerY()
            canvas.save()
            canvas.rotate(tb.rotationAngle, cx, cy)
            drawTextPaint.color = Color.parseColor(tb.color)
            drawTextPaint.textSize = tb.textSize
            drawTextPaint.typeface = Typeface.create(tb.fontFamily, tb.typeface)
            drawTextPaint.textAlign = tb.alignment
            val fm = drawTextPaint.fontMetrics
            val textHeight = fm.bottom - fm.top
            val textY = rect.top + (rect.height() + textHeight) / 2 - fm.bottom
            canvas.drawText(tb.text, getAlignedX(rect, drawTextPaint.textAlign), textY, drawTextPaint)
            canvas.restore()
        }
    }

    fun drawShapePreview(canvas: Canvas, vm: WhiteboardViewModel) {
        shapeStart?.let { start ->
            shapeEnd?.let { end ->
                val previewPaint = Paint(drawPaint).apply {
                    strokeWidth = vm.getShapeStrokeWidth()
                    style = Paint.Style.STROKE
                    color = try { Color.parseColor(vm.getColor()) } catch (_: Exception) { Color.BLACK }
                    isAntiAlias = true
                    pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
                }
                val left = min(start.x, end.x)
                val top = min(start.y, end.y)
                val right = max(start.x, end.x)
                val bottom = max(start.y, end.y)
                when (vm.getCurrentShapeType()) {
                    WhiteboardViewModel.ShapeType.RECTANGLE -> canvas.drawRect(left, top, right, bottom, previewPaint)
                    WhiteboardViewModel.ShapeType.CIRCLE -> {
                        val cx = (left + right) / 2
                        val cy = (top + bottom) / 2
                        val radius = min(right - left, bottom - top) / 2
                        canvas.drawCircle(cx, cy, radius, previewPaint)
                    }
                    WhiteboardViewModel.ShapeType.LINE -> canvas.drawLine(left, top, right, bottom, previewPaint)
                    WhiteboardViewModel.ShapeType.POLYGON -> {
                        val sides = 5
                        val cx = (left + right) / 2
                        val cy = (top + bottom) / 2
                        val radius = min(right - left, bottom - top) / 2
                        val path = Path()
                        for (i in 0 until sides) {
                            val angle = 2 * Math.PI * i / sides - Math.PI / 2
                            val x = cx + radius * cos(angle).toFloat()
                            val y = cy + radius * sin(angle).toFloat()
                            if (i == 0) path.moveTo(x, y)
                            else path.lineTo(x, y)
                        }
                        path.close()
                        canvas.drawPath(path, previewPaint)
                    }
                    else -> {}
                }
            }
        }
    }

    fun drawSelectedItems(canvas: Canvas, vm: WhiteboardViewModel) {
        vm.selectedItems.value?.let { selection ->
            val borderPaint = Paint().apply {
                style = Paint.Style.STROKE
                color = Color.BLUE
                strokeWidth = 3f
                pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
            }
            selection.shapes.forEach { shape ->
                if (shape.type == WhiteboardViewModel.ShapeType.POLYGON && shape.points.size >= 3) {
                    val path = Path()
                    shape.points.forEachIndexed { i, point ->
                        if (i == 0) path.moveTo(point.x, point.y)
                        else path.lineTo(point.x, point.y)
                    }
                    path.close()
                    canvas.drawPath(path, borderPaint)
                } else {
                    shape.bounds?.toRectF()?.let { bounds ->
                        val cx = bounds.centerX()
                        val cy = bounds.centerY()
                        canvas.save()
                        canvas.rotate(shape.rotationAngle, cx, cy)
                        canvas.drawRect(bounds, borderPaint)
                        canvas.restore()
                    }
                }
            }
            selection.textBoxes.forEach { tb ->
                val rect = tb.bounds.toRectF()
                val cx = rect.centerX()
                val cy = rect.centerY()
                canvas.save()
                canvas.rotate(tb.rotationAngle, cx, cy)
                canvas.drawRect(rect, borderPaint)
                canvas.restore()
            }
            selection.strokes.forEach { stroke ->
                val strokeBounds = RectF()
                stroke.points.forEach { p -> strokeBounds.union(p.x, p.y) }
                canvas.drawRect(strokeBounds, borderPaint)
            }
        }
    }

    fun drawStrokes(canvas: Canvas, vm: WhiteboardViewModel) {
        // Draw committed strokes
        vm.state.value?.strokes?.forEach { stroke ->
            val strokePaint = Paint(drawPaint).apply {
                color = if (stroke.isEraser) Color.WHITE else try { Color.parseColor(stroke.color) } catch (_: Exception) { Color.BLACK }
                strokeWidth = stroke.width
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            if (stroke.points.isNotEmpty()) {
                val path = Path()
                stroke.points.forEachIndexed { i, point ->
                    if (i == 0) {
                        path.moveTo(point.x, point.y)
                    } else {
                        val prev = stroke.points[i - 1]
                        val midX = (prev.x + point.x) / 2
                        val midY = (prev.y + point.y) / 2
                        if (i == 1) {
                            path.lineTo(midX, midY)
                        } else {
                            path.quadTo(prev.x, prev.y, midX, midY)
                        }
                    }
                }
                canvas.drawPath(path, strokePaint)
            }
        }
        // Draw in-progress stroke
        if (points.isNotEmpty()) {
            val strokePaint = Paint(drawPaint).apply {
                isAntiAlias = true
            }
            canvas.drawPath(path, strokePaint)
        }
    }

    fun startShapeDrawing(type: WhiteboardViewModel.ShapeType, colorHex: String? = null) {
        viewModel?.setTool(WhiteboardViewModel.Tool.SHAPE)
        viewModel?.setCurrentShape(type)
        colorHex?.let { viewModel?.setColor(it) }
    }

    fun startShapeDrawing(x: Float, y: Float) {
        shapeStart = PointF(x, y)
        shapeEnd = PointF(x, y)
    }

    fun updateShapeDrawing(x: Float, y: Float) {
        shapeEnd = PointF(x, y)
    }

    fun commitShape(vm: WhiteboardViewModel) {
        shapeStart?.let { start ->
            shapeEnd?.let { end ->
                if (abs(end.x - start.x) > 10 || abs(end.y - start.y) > 10) {
                    val left = min(start.x, end.x)
                    val top = min(start.y, end.y)
                    val right = max(start.x, end.x)
                    val bottom = max(start.y, end.y)
                    val type = vm.getCurrentShapeType() ?: return
                    val shape = when (type) {
                        WhiteboardViewModel.ShapeType.RECTANGLE,
                        WhiteboardViewModel.ShapeType.CIRCLE,
                        WhiteboardViewModel.ShapeType.LINE -> com.example.whiteboard.models.Shape(
                            type = type,
                            bounds = RectFSerializable.from(RectF(left, top, right, bottom)),
                            color = vm.getColor(),
                            strokeWidth = vm.getShapeStrokeWidth()
                        )
                        WhiteboardViewModel.ShapeType.POLYGON -> {
                            val sides = 5
                            val cx = (left + right) / 2
                            val cy = (top + bottom) / 2
                            val radius = min(right - left, bottom - top) / 2
                            val points = mutableListOf<Point>()
                            for (i in 0 until sides) {
                                val angle = 2 * Math.PI * i / sides - Math.PI / 2
                                val x = cx + radius * cos(angle)
                                val y = cy + radius * sin(angle)
                                points.add(Point(x.toFloat(), y.toFloat()))
                            }
                            com.example.whiteboard.models.Shape(
                                type = type,
                                bounds = null,
                                points = points,
                                color = vm.getColor(),
                                strokeWidth = vm.getShapeStrokeWidth()
                            )
                        }
                    }
                    vm.addShape(shape)
                }
            }
        }
        shapeStart = null
        shapeEnd = null
    }

    fun startFreehand(x: Float, y: Float, vm: WhiteboardViewModel, isEraser: Boolean = false) {
        path.reset()
        points.clear()
        lastMidPoint = null
        path.moveTo(x, y)
        points.add(PointF(x, y))
        drawPaint.strokeWidth = vm.getStrokeWidth()
        drawPaint.color = if (isEraser) Color.WHITE else try { Color.parseColor(vm.getColor()) } catch (_: Exception) { Color.BLACK }
        view.invalidate()
    }

    fun continueFreehand(x: Float, y: Float) {
        points.lastOrNull()?.let { last ->
            val dx = abs(x - last.x)
            val dy = abs(y - last.y)
            // Adjust threshold for smoother sampling (e.g., 3 pixels)
            if (dx >= 3 || dy >= 3) {
                val midX = (last.x + x) / 2
                val midY = (last.y + y) / 2
                if (lastMidPoint == null) {
                    path.lineTo(midX, midY)
                } else {
                    path.quadTo(lastMidPoint!!.x, lastMidPoint!!.y, midX, midY)
                }
                lastMidPoint = PointF(midX, midY)
                points.add(PointF(x, y))
                view.invalidate()
            }
        }
    }

    fun commitFreehand(vm: WhiteboardViewModel, isEraser: Boolean = false) {
        val converted = points.map { Point(it.x, it.y) }
        if (converted.size > 1) {
            vm.addStroke(points = converted, isEraser = isEraser)
        }
        path.reset()
        points.clear()
        lastMidPoint = null
        view.invalidate()
    }

    private fun getAlignedX(rect: RectF, align: Paint.Align): Float {
        return when (align) {
            Paint.Align.CENTER -> rect.centerX()
            Paint.Align.RIGHT -> rect.right
            else -> rect.left
        }
    }
}