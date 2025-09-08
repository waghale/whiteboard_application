package com.example.whiteboard.views

import android.content.Context
import android.graphics.*
import android.text.InputType
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Spinner

import com.example.whiteboard.models.Point
import com.example.whiteboard.models.Shape
import com.example.whiteboard.viewmodels.WhiteboardViewModel
import com.example.whiteboard.viewmodels.WhiteboardViewModel.ShapeType
import kotlin.math.abs

import com.example.whiteboard.models.RectFSerializable
import kotlin.math.min
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import com.example.whiteboard.models.TextBox
import kotlin.math.max

class DrawingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var viewModel: WhiteboardViewModel? = null
    private var textStart: PointF? = null
    private var textEnd: PointF? = null
    private var activeEditText: EditText? = null

    private var selectedTextBox: TextBox? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    val drawTextPaint = Paint().apply { isAntiAlias = true }

    private val drawPaint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 8f
    }

    private val path = Path()
    private val points = ArrayList<PointF>()

    private var shapeStart: PointF? = null
    private var shapeEnd: PointF? = null

    private var eraseStart: PointF? = null
    private var eraseEnd: PointF? = null

    private val MIN_SIZE = 50f
    private val MAX_SIZE = 500f

    fun startShapeDrawing(type: WhiteboardViewModel.ShapeType) {
        viewModel?.setTool(WhiteboardViewModel.Tool.SHAPE)
        viewModel?.setCurrentShape(type)
    }

    fun startShapeDrawing(type: WhiteboardViewModel.ShapeType, colorHex: String) {
        viewModel?.setTool(WhiteboardViewModel.Tool.SHAPE)
        viewModel?.setCurrentShape(type)
        viewModel?.setColor(colorHex)
    }

//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        val vm = viewModel ?: return
//
//        // Draw existing shapes
//        vm.shapes.value?.forEach { shape ->
//            val paint = Paint(drawPaint).apply {
//                strokeWidth = shape.strokeWidth
//                style = Paint.Style.STROKE
//                color = try { Color.parseColor(shape.color) } catch (_: Exception) { Color.BLACK }
//                isAntiAlias = true
//            }
//
//            val bounds: RectF? = shape.bounds?.toRectF()
//            when (shape.type) {
//                WhiteboardViewModel.ShapeType.RECTANGLE -> bounds?.let { canvas.drawRect(it, paint) }
//                WhiteboardViewModel.ShapeType.CIRCLE -> bounds?.let {
//                    val cx = (it.left + it.right) / 2
//                    val cy = (it.top + it.bottom) / 2
//                    val radius = min(it.width(), it.height()) / 2
//                    canvas.drawCircle(cx, cy, radius, paint)
//                }
//                WhiteboardViewModel.ShapeType.LINE -> bounds?.let { canvas.drawLine(it.left, it.top, it.right, it.bottom, paint) }
//                WhiteboardViewModel.ShapeType.POLYGON -> if (shape.points.size >= 3) {
//                    val path = Path()
//                    path.moveTo(shape.points[0].x, shape.points[0].y)
//                    for (i in 1 until shape.points.size) path.lineTo(shape.points[i].x, shape.points[i].y)
//                    path.close()
//                    canvas.drawPath(path, paint)
//                }
//            }
//        }
//
//        // Draw existing strokes
//        vm.state.value?.strokes?.forEach { s ->
//            val p = Paint(drawPaint).apply {
//                strokeWidth = s.width
//                style = Paint.Style.STROKE
//                isAntiAlias = true
//                color = if (s.isEraser) Color.WHITE else try { Color.parseColor(s.color) } catch (_: Exception) { Color.BLACK }
//            }
//
//            if (s.points.isNotEmpty()) {
//                if (s.isEraser && vm.getEraserShape() == WhiteboardViewModel.EraserShape.SQUARE) {
//                    s.points.forEach { point ->
//                        val half = s.width / 2
//                        canvas.drawRect(point.x - half, point.y - half, point.x + half, point.y + half, p.apply { style = Paint.Style.FILL })
//                    }
//                } else {
//                    val strokePath = Path()
//                    strokePath.moveTo(s.points[0].x, s.points[0].y)
//                    for (i in 1 until s.points.size) strokePath.lineTo(s.points[i].x, s.points[i].y)
//                    canvas.drawPath(strokePath, p)
//                }
//            }
//        }
//
//        // Draw current stroke preview
//        canvas.drawPath(path, drawPaint)
//
//        // Draw TextBoxes
//        vm.textBoxes.value?.forEach { tb ->
//            drawTextPaint.color = Color.parseColor(tb.color)
//            drawTextPaint.textSize = tb.textSize
//            drawTextPaint.typeface = Typeface.create(tb.fontFamily, tb.typeface)
//            drawTextPaint.textAlign = tb.alignment
//
//            val rect = tb.bounds.toRectF()
//            val fm = drawTextPaint.fontMetrics
//            val textHeight = fm.bottom - fm.top
//            val textY = rect.top + (rect.height() + textHeight) / 2 - fm.bottom
//
//            // Draw the text
//            canvas.drawText(tb.text, getAlignedX(rect, drawTextPaint.textAlign), textY, drawTextPaint)
//
//            // Highlight the selected TextBox
//            if (tb == selectedTextBox) {
//                val borderPaint = Paint().apply {
//                    color = Color.BLUE
//                    style = Paint.Style.STROKE
//                    strokeWidth = 3f
//                    pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
//                }
//                canvas.drawRect(rect, borderPaint)
//            }
//        }
//
//        // 🔹 Draw select-area eraser rectangle (dashed border + semi-transparent fill)
//        if (vm.getTool() == WhiteboardViewModel.Tool.ERASER &&
//            vm.getEraserShape() == WhiteboardViewModel.EraserShape.SELECT_AREA &&
//            eraseStart != null && eraseEnd != null
//        ) {
//            val rect = RectF(
//                min(eraseStart!!.x, eraseEnd!!.x),
//                min(eraseStart!!.y, eraseEnd!!.y),
//                max(eraseStart!!.x, eraseEnd!!.x),
//                max(eraseStart!!.y, eraseEnd!!.y)
//            )
//
//            // Semi-transparent fill
//            val fillPaint = Paint().apply {
//                color = Color.RED
//                alpha = 50
//                style = Paint.Style.FILL
//            }
//            canvas.drawRect(rect, fillPaint)
//
//            // Dashed border
//            val borderPaint = Paint().apply {
//                color = Color.RED
//                style = Paint.Style.STROKE
//                strokeWidth = 3f
//                pathEffect = DashPathEffect(floatArrayOf(15f, 15f), 0f)
//                isAntiAlias = true
//            }
//            canvas.drawRect(rect, borderPaint)
//        }
//    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val vm = viewModel ?: return

        // Draw existing shapes
        vm.shapes.value?.forEach { shape ->
            val paint = Paint(drawPaint).apply {
                strokeWidth = shape.strokeWidth
                style = Paint.Style.STROKE
                color = try { Color.parseColor(shape.color) } catch (_: Exception) { Color.BLACK }
                isAntiAlias = true
                // Solid lines for shapes already placed
                pathEffect = null
            }

            val bounds: RectF? = shape.bounds?.toRectF()
            when (shape.type) {
                WhiteboardViewModel.ShapeType.RECTANGLE -> bounds?.let { canvas.drawRect(it, paint) }
                WhiteboardViewModel.ShapeType.CIRCLE -> bounds?.let {
                    val cx = (it.left + it.right) / 2
                    val cy = (it.top + it.bottom) / 2
                    val radius = min(it.width(), it.height()) / 2
                    canvas.drawCircle(cx, cy, radius, paint)
                }
                WhiteboardViewModel.ShapeType.LINE -> bounds?.let { canvas.drawLine(it.left, it.top, it.right, it.bottom, paint) }
                WhiteboardViewModel.ShapeType.POLYGON -> if (shape.points.size >= 3) {
                    val path = Path()
                    path.moveTo(shape.points[0].x, shape.points[0].y)
                    for (i in 1 until shape.points.size) path.lineTo(shape.points[i].x, shape.points[i].y)
                    path.close()
                    canvas.drawPath(path, paint)
                }
            }
        }

        // Draw the shape currently being drawn (dashed preview)
        shapeStart?.let { start ->
            shapeEnd?.let { end ->
                val previewPaint = Paint(drawPaint).apply {
                    strokeWidth = viewModel?.getShapeStrokeWidth() ?: 8f
                    style = Paint.Style.STROKE
                    color = try { Color.parseColor(vm.getColor()) } catch (_: Exception) { Color.BLACK }
                    isAntiAlias = true
                    pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f) // 🔹 Dashed preview
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
                            val x = cx + radius * Math.cos(angle)
                            val y = cy + radius * Math.sin(angle)
                            if (i == 0) path.moveTo(x.toFloat(), y.toFloat())
                            else path.lineTo(x.toFloat(), y.toFloat())
                        }
                        path.close()
                        canvas.drawPath(path, previewPaint)
                    }
                    else -> {}
                }
            }
        }

        // Draw existing strokes
        vm.state.value?.strokes?.forEach { s ->
            val p = Paint(drawPaint).apply {
                strokeWidth = s.width
                style = Paint.Style.STROKE
                isAntiAlias = true
                color = if (s.isEraser) Color.WHITE else try { Color.parseColor(s.color) } catch (_: Exception) { Color.BLACK }
            }

            if (s.points.isNotEmpty()) {
                if (s.isEraser && vm.getEraserShape() == WhiteboardViewModel.EraserShape.SQUARE) {
                    s.points.forEach { point ->
                        val half = s.width / 2
                        canvas.drawRect(point.x - half, point.y - half, point.x + half, point.y + half, p.apply { style = Paint.Style.FILL })
                    }
                } else {
                    val strokePath = Path()
                    strokePath.moveTo(s.points[0].x, s.points[0].y)
                    for (i in 1 until s.points.size) strokePath.lineTo(s.points[i].x, s.points[i].y)
                    canvas.drawPath(strokePath, p)
                }
            }
        }

        // Draw current stroke preview
        canvas.drawPath(path, drawPaint)

        // Draw TextBoxes
        vm.textBoxes.value?.forEach { tb ->
            drawTextPaint.color = Color.parseColor(tb.color)
            drawTextPaint.textSize = tb.textSize
            drawTextPaint.typeface = Typeface.create(tb.fontFamily, tb.typeface)
            drawTextPaint.textAlign = tb.alignment

            val rect = tb.bounds.toRectF()
            val fm = drawTextPaint.fontMetrics
            val textHeight = fm.bottom - fm.top
            val textY = rect.top + (rect.height() + textHeight) / 2 - fm.bottom

            canvas.drawText(tb.text, getAlignedX(rect, drawTextPaint.textAlign), textY, drawTextPaint)

            if (tb == selectedTextBox) {
                val borderPaint = Paint().apply {
                    color = Color.BLUE
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                    pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
                }
                canvas.drawRect(rect, borderPaint)
            }
        }

        // Draw select-area eraser (dashed + semi-transparent)
        if (vm.getTool() == WhiteboardViewModel.Tool.ERASER &&
            vm.getEraserShape() == WhiteboardViewModel.EraserShape.SELECT_AREA &&
            eraseStart != null && eraseEnd != null
        ) {
            val rect = RectF(
                min(eraseStart!!.x, eraseEnd!!.x),
                min(eraseStart!!.y, eraseEnd!!.y),
                max(eraseStart!!.x, eraseEnd!!.x),
                max(eraseStart!!.y, eraseEnd!!.y)
            )

            val fillPaint = Paint().apply {
                color = Color.RED
                alpha = 50
                style = Paint.Style.FILL
            }
            canvas.drawRect(rect, fillPaint)

            val borderPaint = Paint().apply {
                color = Color.RED
                style = Paint.Style.STROKE
                strokeWidth = 3f
                pathEffect = DashPathEffect(floatArrayOf(15f, 15f), 0f)
                isAntiAlias = true
            }
            canvas.drawRect(rect, borderPaint)
        }
    }



    override fun onTouchEvent(event: MotionEvent): Boolean {
        val vm = viewModel ?: return false
        val tool = vm.getTool()
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                when (tool) {
                    WhiteboardViewModel.Tool.SHAPE -> {
                        shapeStart = PointF(x, y)
                        shapeEnd = PointF(x, y)
                    }

                    WhiteboardViewModel.Tool.ERASER -> {
                        if (vm.getEraserShape() == WhiteboardViewModel.EraserShape.SELECT_AREA) {
                            eraseStart = PointF(x, y)
                            eraseEnd = PointF(x, y)
                        } else {
                            path.moveTo(x, y)
                            points.clear()
                            points.add(PointF(x, y))
                            drawPaint.strokeWidth = vm.getStrokeWidth()
                            drawPaint.color = Color.WHITE
                        }
                    }

                    WhiteboardViewModel.Tool.TEXT -> {
                        val tb = findTextBoxAt(x, y)
                        if (tb != null) {
                            selectedTextBox = tb
                            dragOffsetX = x - tb.bounds.left
                            dragOffsetY = y - tb.bounds.top
                        } else {
                            textStart = PointF(x, y)
                            textEnd = PointF(x, y)
                            selectedTextBox = null
                        }
                    }

                    else -> {
                        path.moveTo(x, y)
                        points.clear()
                        points.add(PointF(x, y))
                        drawPaint.strokeWidth = vm.getStrokeWidth()
                        drawPaint.color = try { Color.parseColor(vm.getColor()) } catch (_: Exception) { Color.BLACK }
                    }
                }
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                when (tool) {
                    WhiteboardViewModel.Tool.SHAPE -> shapeEnd = PointF(x, y)

                    WhiteboardViewModel.Tool.ERASER -> {
                        if (vm.getEraserShape() == WhiteboardViewModel.EraserShape.SELECT_AREA) {
                            eraseEnd = PointF(x, y)
                        } else {
                            points.lastOrNull()?.let { last ->
                                if (abs(x - last.x) >= 5 || abs(y - last.y) >= 5) {
                                    path.lineTo(x, y)
                                    points.add(PointF(x, y))
                                }
                            }
                        }
                    }

                    WhiteboardViewModel.Tool.TEXT -> {
                        selectedTextBox?.let { tb ->
                            val rectF = tb.bounds.toRectF()
                            val newLeft = x - dragOffsetX
                            val newTop = y - dragOffsetY
                            val newRight = newLeft + rectF.width()
                            val newBottom = newTop + rectF.height()
                            val updated = tb.copy(
                                bounds = RectFSerializable.from(RectF(newLeft, newTop, newRight, newBottom))
                            )
                            vm.updateTextBox(updated)
                        }

                        textStart?.let { textEnd = PointF(x, y) }
                    }

                    else -> {
                        points.lastOrNull()?.let { last ->
                            if (abs(x - last.x) >= 5 || abs(y - last.y) >= 5) {
                                path.lineTo(x, y)
                                points.add(PointF(x, y))
                            }
                        }
                    }
                }
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                when (tool) {
                    WhiteboardViewModel.Tool.SHAPE -> handleShapeDraw(vm)

                    WhiteboardViewModel.Tool.ERASER -> {
                        if (vm.getEraserShape() == WhiteboardViewModel.EraserShape.SELECT_AREA) {
                            eraseStart?.let { start ->
                                eraseEnd?.let { end ->
                                    val left = min(start.x, end.x)
                                    val top = min(start.y, end.y)
                                    val right = max(start.x, end.x)
                                    val bottom = max(start.y, end.y)
                                    val eraseRect = RectF(left, top, right, bottom)

                                    // Remove intersecting shapes (including polygons)
                                    vm.shapes.value?.toList()?.forEach { shape ->
                                        val bounds = shape.bounds?.toRectF() ?: when (shape.type) {
                                            WhiteboardViewModel.ShapeType.POLYGON -> computePolygonBounds(shape.points)
                                            else -> null
                                        }

                                        bounds?.let {
                                            if (RectF.intersects(it, eraseRect)) {
                                                vm.removeShape(shape)
                                            }
                                        }
                                    }

                                    // Remove strokes inside selected area
                                    vm.eraseArea(eraseRect)
                                }
                            }
                            eraseStart = null
                            eraseEnd = null
                        } else {
                            val converted = points.map { Point(it.x, it.y) }
                            vm.addStroke(points = converted, isEraser = true)
                            path.reset()
                            points.clear()
                        }
                    }

                    WhiteboardViewModel.Tool.TEXT -> {
                        selectedTextBox?.let { tb ->
                            val dx = abs((tb.bounds.left + dragOffsetX) - x)
                            val dy = abs((tb.bounds.top + dragOffsetY) - y)
                            if (dx < 5 && dy < 5) {
                                handleTextArea(vm, tb.bounds.toRectF())
                            } else {
                                vm.updateTextBox(tb)
                            }
                            selectedTextBox = null
                        }

                        if (textStart != null && textEnd != null) {
                            val left = min(textStart!!.x, textEnd!!.x)
                            val top = min(textStart!!.y, textEnd!!.y)
                            showInsertTextDialog(left, top)
                            textStart = null
                            textEnd = null
                        }
                    }

                    else -> {
                        val converted = points.map { Point(it.x, it.y) }
                        vm.addStroke(points = converted, isEraser = (tool == WhiteboardViewModel.Tool.ERASER))
                        path.reset()
                        points.clear()
                    }
                }
                invalidate()
            }
        }

        return true
    }

    private fun handleShapeDraw(vm: WhiteboardViewModel) {
        val start = shapeStart ?: return
        val end = shapeEnd ?: return
        val width = abs(end.x - start.x).coerceIn(MIN_SIZE, MAX_SIZE)
        val height = abs(end.y - start.y).coerceIn(MIN_SIZE, MAX_SIZE)
        val left = if (end.x >= start.x) start.x else start.x - width
        val top = if (end.y >= start.y) start.y else start.y - height
        val right = left + width
        val bottom = top + height

        val shape = vm.getCurrentShapeType()?.let { type ->
            if (type == WhiteboardViewModel.ShapeType.POLYGON) {
                val sides = 5
                val cx = (left + right) / 2
                val cy = (top + bottom) / 2
                val radius = min(width, height) / 2
                val points = mutableListOf<Point>()
                for (i in 0 until sides) {
                    val angle = 2 * Math.PI * i / sides - Math.PI / 2
                    val x = cx + radius * Math.cos(angle)
                    val y = cy + radius * Math.sin(angle)
                    points.add(Point(x.toFloat(), y.toFloat()))
                }
                Shape(type = type, bounds = null, points = points, color = vm.getColor(), strokeWidth = vm.getStrokeWidth() ?: 8f)
            } else {
                Shape(type = type, bounds = RectFSerializable.from(RectF(left, top, right, bottom)), color = vm.getColor(), strokeWidth = vm.getStrokeWidth() ?: 8f)
            }
        }
        if (shape != null) vm.addShape(shape)
        shapeStart = null
        shapeEnd = null
    }

//    private fun handleSelectAreaErase(vm: WhiteboardViewModel) {
//        val start = eraseStart ?: return
//        val end = eraseEnd ?: return
//        val rect = RectF(
//            min(start.x, end.x),
//            min(start.y, end.y),
//            max(start.x, end.x),
//            max(start.y, end.y)
//        )
//        vm.eraseArea(rect)
//        eraseStart = null
//        eraseEnd = null
//    }


    private fun handleSelectAreaErase(vm: WhiteboardViewModel) {
        val start = eraseStart ?: return
        val end = eraseEnd ?: return
        val rect = RectF(
            min(start.x, end.x),
            min(start.y, end.y),
            max(start.x, end.x),
            max(start.y, end.y)
        )
        vm.eraseArea(rect) // ✅ Updated function call
        eraseStart = null
        eraseEnd = null
    }






    private fun showInsertTextDialog(x: Float, y: Float) {
        val vm = viewModel ?: return
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Insert Text")

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        // EditText for text input
        val editText = EditText(context).apply {
            hint = "Enter text"
            maxLines = 3
            setLines(2)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        layout.addView(editText)

        // SeekBar for text size
        val sizeSeek = SeekBar(context).apply {
            max = 100
            progress = vm.getTextSize().toInt()
        }
        layout.addView(sizeSeek)

        // RadioGroup for style selection
        val styleGroup = RadioGroup(context).apply {
            orientation = RadioGroup.HORIZONTAL
        }
        listOf("Normal", "Bold", "Italic").forEachIndexed { idx, label ->
            val rb = RadioButton(context).apply { text = label; id = idx }
            styleGroup.addView(rb)
        }
        styleGroup.check(
            when (vm.getTextStyle()) {
                Typeface.BOLD -> 1
                Typeface.ITALIC -> 2
                else -> 0
            }
        )
        layout.addView(styleGroup)

        // RadioGroup for text alignment
        val alignGroup = RadioGroup(context).apply {
            orientation = RadioGroup.HORIZONTAL
        }
        listOf("Left", "Center", "Right").forEachIndexed { idx, label ->
            val rb = RadioButton(context).apply { text = label; id = idx }
            alignGroup.addView(rb)
        }
        alignGroup.check(
            when (vm.getTextAlignment()) {
                Paint.Align.CENTER -> 1
                Paint.Align.RIGHT -> 2
                else -> 0
            }
        )
        layout.addView(alignGroup)

        // Spinner for font family
        val fontGroup = Spinner(context).apply {
            adapter = ArrayAdapter(
                context, android.R.layout.simple_spinner_dropdown_item,
                listOf("sans-serif", "serif", "monospace")
            )
            setSelection(
                when (vm.getFontFamily()) {
                    "serif" -> 1
                    "monospace" -> 2
                    else -> 0
                }
            )
        }
        layout.addView(fontGroup)

        // Spinner for text color
        val colors = listOf(
            "Black" to "#FF000000",
            "Red" to "#FFFF0000",
            "Blue" to "#FF0000FF",
            "Green" to "#FF00FF00",
            "Yellow" to "#FFFFFF00",
            "Purple" to "#FF800080"
        )
        val colorGroup = Spinner(context).apply {
            adapter = ArrayAdapter(
                context, android.R.layout.simple_spinner_dropdown_item, colors.map { it.first }
            )
            val currentColorIndex = colors.indexOfFirst { it.second == vm.getColor() }
                .coerceAtLeast(0)
            setSelection(currentColorIndex)
        }
        layout.addView(colorGroup)

        builder.setView(layout)

        builder.setPositiveButton("OK") { _, _ ->
            val text = editText.text.toString().takeIf { it.isNotBlank() } ?: return@setPositiveButton
            val size = sizeSeek.progress.toFloat()
            val typeface = when (styleGroup.checkedRadioButtonId) {
                1 -> Typeface.BOLD
                2 -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            val font = fontGroup.selectedItem.toString()
            val color = colors[colorGroup.selectedItemPosition].second
            val alignment = when (alignGroup.checkedRadioButtonId) {
                1 -> Paint.Align.CENTER
                2 -> Paint.Align.RIGHT
                else -> Paint.Align.LEFT
            }

            // Update ViewModel current text settings
            vm.setTextSize(size)
            vm.setTextStyle(typeface)
            vm.setFontFamily(font)
            vm.setTextColor(color)
            vm.setTextAlignment(alignment)

            // Add the text box at position
            val rect = RectF(x, y, x + 300, y + 100) // default width/height
            vm.addTextBox(
                bounds = rect,
                text = text,
                color = color,
                textSize = size,
                typeface = typeface,
                fontFamily = font,
                alignment = alignment
            )

            // Force redraw after adding text
            invalidate()
        }

        builder.setNegativeButton("Cancel", null)

        val dialog = builder.show()

        // Auto focus + keyboard open
        editText.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }








    private fun handleTextArea(vm: WhiteboardViewModel, rect: RectF) {
        // Remove any previous EditText
        activeEditText?.let { editText ->
            safeRemoveView(editText)
        }

        val existingTextBox = findTextBoxAt(rect.left, rect.top)

        val editText = EditText(context).apply {
            x = rect.left
            y = rect.top
            width = rect.width().toInt()
            height = rect.height().toInt()
            setBackgroundColor(Color.TRANSPARENT)
            setText(existingTextBox?.text ?: "")
            setTextColor(Color.parseColor(existingTextBox?.color ?: vm.getTextColor()))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, existingTextBox?.textSize ?: vm.getTextSize())
            typeface = Typeface.create(
                existingTextBox?.fontFamily ?: vm.currentFontFamily,
                existingTextBox?.typeface ?: vm.currentTextStyle
            )
            gravity = when (existingTextBox?.alignment ?: vm.currentTextAlignment) {
                Paint.Align.CENTER -> Gravity.CENTER
                Paint.Align.RIGHT -> Gravity.END
                else -> Gravity.START
            }
            isSingleLine = false       // allow multiline text
        }

        (parent as? ViewGroup)?.addView(editText)
        activeEditText = editText

        // Show keyboard
        editText.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)

        // Update text when focus is lost
        editText.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                saveTextBox(editText, rect, existingTextBox, vm)
            }
        }

        // Optional: Update text on "Done" button
        editText.setOnEditorActionListener { v, _, _ ->
            saveTextBox(editText, rect, existingTextBox, vm)
            true
        }
    }

    // Helper function to save text box
//    private fun saveTextBox(
//        editText: EditText,
//        rect: RectF,
//        existingTextBox: TextBox?,
//        vm: WhiteboardViewModel
//    )
//    {
//        val text = editText.text.toString()
//        (editText.parent as? ViewGroup)?.removeView(editText)
//
//        activeEditText = null
//
//        if (existingTextBox != null) {
//            val updated = existingTextBox.copy(
//                text = text,
//                bounds = RectFSerializable.from(rect)
//            )
//            vm.updateTextBox(updated)
//        } else {
//            vm.addTextBox(
//                bounds = rect,
//                text = text,
//                color = vm.getTextColor(),
//                textSize = vm.getTextSize(),
//                typeface = vm.currentTextStyle,
//                fontFamily = vm.currentFontFamily,
//                alignment = vm.currentTextAlignment
//            )
//        }
//
//        invalidate()
//    }


    private fun saveTextBox(
        editText: EditText,
        rect: RectF,
        existingTextBox: TextBox?,
        vm: WhiteboardViewModel
    ) {
        val text = editText.text.toString().trim()

        // Remove EditText from parent
        safeRemoveView(editText)

        activeEditText = null

        // रिकामं text असेल तर काहीही save करू नको
        if (text.isEmpty()) {
            invalidate()
            return
        }

        // Bounds सुरक्षित (normalized)
        val left = min(rect.left, rect.right)
        val top = min(rect.top, rect.bottom)
        val right = max(rect.left, rect.right)
        val bottom = max(rect.top, rect.bottom)
        val safeRect = RectF(left, top, right, bottom)

        if (existingTextBox != null) {
            // Update existing textbox → keep style
            val updated = existingTextBox.copy(
                text = text,
                bounds = RectFSerializable.from(safeRect)
            )
            vm.updateTextBox(updated)
        } else {
            // New textbox → use current style from VM
            vm.addTextBox(
                bounds = safeRect,
                text = text,
                color = vm.getTextColor(),
                textSize = vm.getTextSize(),
                typeface = vm.currentTextStyle,
                fontFamily = vm.currentFontFamily,
                alignment = vm.currentTextAlignment
            )
        }

        invalidate()
    }













    private fun getAlignedX(rect: RectF, align: Paint.Align): Float {
        return when (align) {
            Paint.Align.CENTER -> rect.centerX()
            Paint.Align.RIGHT -> rect.right
            else -> rect.left
        }
    }

    private fun findTextBoxAt(x: Float, y: Float): TextBox? {
        val vm = viewModel ?: return null
        return vm.textBoxes.value?.firstOrNull { tb ->
            val rect = tb.bounds.toRectF()
            rect.contains(x, y)
        }
    }

    private fun safeRemoveView(view: View?) {
        if (view == null) return
        val parent = view.parent
        if (parent is ViewGroup) {
            try {
                parent.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace() // Debug साठी, पण crash होणार नाही
            }
        }
    }


    private fun computePolygonBounds(points: List<Point>): RectF {
        val left = points.minOf { it.x }
        val top = points.minOf { it.y }
        val right = points.maxOf { it.x }
        val bottom = points.maxOf { it.y }
        return RectF(left, top, right, bottom)
    }







}







