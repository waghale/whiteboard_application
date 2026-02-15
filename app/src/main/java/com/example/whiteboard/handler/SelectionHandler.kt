package com.example.whiteboard.handler



import android.graphics.*
import com.example.whiteboard.models.SelectedItems
import com.example.whiteboard.models.Stroke
import com.example.whiteboard.models.TextBox
import com.example.whiteboard.viewmodels.WhiteboardViewModel
import com.example.whiteboard.views.DrawingView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SelectionHandler(
    private val view: DrawingView,
    private val viewModel: WhiteboardViewModel?
) {
    private var selectionStart: PointF? = null
    private var selectionEnd: PointF? = null
    private var selectedShape: com.example.whiteboard.models.Shape? = null
    private var selectedItems: SelectedItems? = null
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    fun drawSelectionRectangle(canvas: Canvas) {
        selectionStart?.let { start ->
            selectionEnd?.let { end ->
                val selPaint = Paint().apply {
                    style = Paint.Style.STROKE
                    color = Color.BLUE
                    strokeWidth = 2f
                    pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
                }
                canvas.drawRect(min(start.x, end.x), min(start.y, end.y), max(start.x, end.x), max(start.y, end.y), selPaint)
            }
        }
    }

    fun startSelection(x: Float, y: Float) {
        selectionStart = PointF(x, y)
        selectionEnd = PointF(x, y)
    }

    fun updateSelection(x: Float, y: Float) {
        selectionEnd = PointF(x, y)
    }

    fun startMove(x: Float, y: Float, vm: WhiteboardViewModel) {
        vm.selectedItems.value?.takeIf { it.isNotEmpty() }?.let {
            lastTouchX = x
            lastTouchY = y
        } ?: run {
            val textBox = view.findTextBoxAt(x, y)
            view.setSelectedTextBox(textBox?.also {
                dragOffsetX = x - it.bounds.left
                dragOffsetY = y - it.bounds.top
            })
            if (view.getSelectedTextBox() == null) {
                selectedShape = vm.shapes.value?.firstOrNull { shape ->
                    if (shape.type == WhiteboardViewModel.ShapeType.POLYGON) {
                        computePolygonBounds(shape.points).contains(x, y)
                    } else {
                        shape.bounds?.toRectF()?.contains(x, y) == true
                    }
                }?.also {
                    val bounds = if (it.type == WhiteboardViewModel.ShapeType.POLYGON) {
                        computePolygonBounds(it.points)
                    } else {
                        it.bounds?.toRectF() ?: RectF()
                    }
                    dragOffsetX = x - bounds.left
                    dragOffsetY = y - bounds.top
                }
            }
            lastTouchX = x
            lastTouchY = y
        }
    }

    fun updateMove(x: Float, y: Float, vm: WhiteboardViewModel) {
        vm.selectedItems.value?.let { items ->
            moveSelectedItems(x, y, vm)
        } ?: run {
            view.getSelectedTextBox()?.let { moveTextBox(it, x, y, vm) }
            selectedShape?.let { moveShape(it, x, y, vm) }
            lastTouchX = x
            lastTouchY = y
        }
    }

    fun commitSelection(vm: WhiteboardViewModel, onSelectionCompleted: ((SelectedItems) -> Unit)?) {
        selectionStart?.let { start ->
            selectionEnd?.let { end ->
                val rect = RectF(min(start.x, end.x), min(start.y, end.y), max(start.x, end.x), max(start.y, end.y))
                val selectedShapes = vm.shapes.value?.filter { shape ->
                    val bounds = shape.bounds?.toRectF() ?: computePolygonBounds(shape.points)
                    RectF.intersects(bounds, rect)
                } ?: emptyList()
                val selectedStrokes = vm.state.value?.strokes?.filter { stroke ->
                    strokeIntersectsRect(stroke, rect)
                } ?: emptyList()
                val selectedTextBoxes = vm.textBoxes.value?.filter { tb ->
                    RectF.intersects(tb.bounds.toRectF(), rect)
                } ?: emptyList()
                selectedItems = SelectedItems(
                    shapes = selectedShapes.toMutableList(),
                    strokes = selectedStrokes.toMutableList(),
                    textBoxes = selectedTextBoxes.toMutableList()
                )
                onSelectionCompleted?.invoke(selectedItems!!)
            }
        }
        selectionStart = null
        selectionEnd = null
    }

    fun performSelection(vm: WhiteboardViewModel, onSelectionCompleted: ((SelectedItems) -> Unit)?) {
        if (selectionStart == null || selectionEnd == null) return
        val left = min(selectionStart!!.x, selectionEnd!!.x)
        val top = min(selectionStart!!.y, selectionEnd!!.y)
        val right = max(selectionStart!!.x, selectionEnd!!.x)
        val bottom = max(selectionStart!!.y, selectionEnd!!.y)
        val selectionRect = RectF(left, top, right, bottom)
        val selectedShapes = vm.shapes.value?.filter { shape ->
            val bounds = shape.bounds?.toRectF() ?: computePolygonBounds(shape.points)
            RectF.intersects(bounds, selectionRect)
        } ?: emptyList()
        val selectedTextBoxes = vm.textBoxes.value?.filter { tb ->
            RectF(tb.bounds.left, tb.bounds.top, tb.bounds.right, tb.bounds.bottom)
                .intersect(selectionRect)
        } ?: emptyList()
        val selectedStrokes = vm.state.value?.strokes?.filter { stroke ->
            val strokeBounds = RectF()
            stroke.points.forEach { p -> strokeBounds.union(p.x, p.y) }
            strokeBounds.intersect(selectionRect)
        } ?: emptyList()
        vm.setSelectedItems(
            SelectedItems(
                shapes = selectedShapes.toMutableList(),
                textBoxes = selectedTextBoxes.toMutableList(),
                strokes = selectedStrokes.toMutableList()
            )
        )
        selectionStart = null
        selectionEnd = null
    }

    fun finishMove() {
        selectedShape = null
        view.setSelectedTextBox(null)
    }

    fun selectLastShape() {
        selectedShape = viewModel?.shapes?.value?.lastOrNull()
        view.invalidate()
    }

    fun selectLastTextBox() {
        view.setSelectedTextBox(viewModel?.textBoxes?.value?.lastOrNull())
        view.invalidate()
    }

    fun selectLastDrawable() {
        val vm = viewModel ?: return
        view.setSelectedTextBox(vm.textBoxes.value?.lastOrNull())
        if (view.getSelectedTextBox() != null) {
            val rect = view.getSelectedTextBox()!!.bounds.toRectF()
            dragOffsetX = rect.width() / 2
            dragOffsetY = rect.height() / 2
            selectedShape = null
            view.invalidate()
            return
        }
        selectedShape = vm.shapes.value?.lastOrNull()
        selectedShape?.let {
            val bounds = it.bounds?.toRectF() ?: computePolygonBounds(it.points)
            dragOffsetX = bounds.width() / 2
            dragOffsetY = bounds.height() / 2
            view.setSelectedTextBox(null)
            view.invalidate()
        }
    }

    fun finishSelection(onSelectionCompleted: ((SelectedItems) -> Unit)?) {
        selectedShape?.let { shape ->
            val items = SelectedItems(
                shapes = mutableListOf(shape),
                strokes = mutableListOf(),
                textBoxes = mutableListOf()
            )
            onSelectionCompleted?.invoke(items)
        }
        view.getSelectedTextBox()?.let { tb ->
            val items = SelectedItems(
                shapes = mutableListOf(),
                strokes = mutableListOf(),
                textBoxes = mutableListOf(tb)
            )
            onSelectionCompleted?.invoke(items)
        }
    }

    fun setSelectedShape(shape: com.example.whiteboard.models.Shape?) {
        selectedShape = shape
        view.invalidate()
    }

    fun setSelectedItems(items: SelectedItems) {
        selectedItems = items
        view.invalidate()
    }

    fun clearSelection() {
        selectedItems = null
        selectionStart = null
        selectionEnd = null
    }

    private fun moveShape(shape: com.example.whiteboard.models.Shape, x: Float, y: Float, vm: WhiteboardViewModel) {
        val dx = x - lastTouchX
        val dy = y - lastTouchY
        if (shape.type == WhiteboardViewModel.ShapeType.POLYGON) {
            shape.points.forEach { p ->
                p.x += dx
                p.y += dy
            }
        } else {
            shape.bounds?.let {
                val rect = it.toRectF()
                rect.offset(dx, dy)
                shape.bounds = com.example.whiteboard.models.RectFSerializable.from(rect)
            }
        }
        vm.updateShape(shape)
        lastTouchX = x
        lastTouchY = y
    }

    private fun moveTextBox(tb: TextBox, x: Float, y: Float, vm: WhiteboardViewModel) {
        view.moveTextBox(tb, x, y, vm)
    }

    private fun moveSelectedItems(x: Float, y: Float, vm: WhiteboardViewModel) {
        selectedItems?.let { items ->
            val dx = x - lastTouchX
            val dy = y - lastTouchY
            items.shapes.forEach { shape ->
                if (shape.type == WhiteboardViewModel.ShapeType.POLYGON) {
                    shape.points.forEach { p ->
                        p.x += dx
                        p.y += dy
                    }
                } else {
                    shape.bounds?.let {
                        val rect = it.toRectF()
                        rect.offset(dx, dy)
                        shape.bounds = com.example.whiteboard.models.RectFSerializable.from(rect)
                    }
                }
                vm.updateShape(shape)
            }
            items.textBoxes.forEach { tb ->
                val rect = tb.bounds.toRectF()
                rect.offset(dx, dy)
                tb.bounds = com.example.whiteboard.models.RectFSerializable.from(rect)
                vm.updateTextBox(tb)
            }
            items.strokes.forEach { stroke ->
                stroke.points.forEach { point ->
                    point.x += dx
                    point.y += dy
                }
                vm.updateStroke(stroke)
            }
            lastTouchX = x
            lastTouchY = y
            view.invalidate()
        }
    }

    private fun computePolygonBounds(points: List<com.example.whiteboard.models.Point>): RectF {
        val left = points.minOf { it.x }
        val top = points.minOf { it.y }
        val right = points.maxOf { it.x }
        val bottom = points.maxOf { it.y }
        return RectF(left, top, right, bottom)
    }

    private fun strokeIntersectsRect(stroke: Stroke, rect: RectF): Boolean {
        val points = stroke.points
        if (points.isEmpty()) return false
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val lineRect = RectF(
                min(p1.x, p2.x),
                min(p1.y, p2.y),
                max(p1.x, p2.x),
                max(p1.y, p2.y)
            )
            if (RectF.intersects(lineRect, rect)) {
                return true
            }
        }
        return false
    }
}