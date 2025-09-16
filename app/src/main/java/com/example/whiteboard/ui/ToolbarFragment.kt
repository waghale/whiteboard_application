package com.example.whiteboard.ui


import android.content.Intent
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.whiteboard.R
import com.example.whiteboard.activity.LoadActivity
import com.example.whiteboard.activity.WhiteboardActivity
import com.example.whiteboard.models.Point
import com.example.whiteboard.viewmodels.WhiteboardViewModel
import com.example.whiteboard.viewmodels.WhiteboardViewModel.ShapeType
import com.example.whiteboard.views.DrawingView



class ToolbarFragment : Fragment() {
    private val vm: WhiteboardViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_toolbar, container, false)

        val penBtn = v.findViewById<Button>(R.id.btn_pen)
        val eraserBtn = v.findViewById<Button>(R.id.btn_eraser)
        val undoBtn = v.findViewById<Button>(R.id.btn_undo)
        val redoBtn = v.findViewById<Button>(R.id.btn_redo)
        val saveBtn = v.findViewById<Button>(R.id.btn_save)
        val loadBtn = v.findViewById<Button>(R.id.btn_load)
        val shapesBtn = v.findViewById<Button>(R.id.btn_shapes)
        val insertTextBtn = v.findViewById<Button>(R.id.btn_insert_text)
        val selectMoveBtn = v.findViewById<Button>(R.id.btn_select_move)
        val rotateLeftBtn = v.findViewById<Button>(R.id.btn_rotate_left)
        val rotateRightBtn = v.findViewById<Button>(R.id.btn_rotate_right)

        penBtn.setOnClickListener {
            showColorPickerDialog1()
        }

        rotateLeftBtn.setOnClickListener {
            showRotateLeftPopup(it, vm)
        }

        rotateRightBtn.setOnClickListener {
            showRotateRightPopup(it, vm)
        }

        eraserBtn.setOnClickListener {
            showEraserDialog()
        }

        undoBtn.setOnClickListener {
            vm.undo()
        }

        redoBtn.setOnClickListener {
            vm.redo()
        }

        saveBtn.setOnClickListener {
            Log.d("ToolbarFragment", "Save button clicked")
            val activity = activity as? WhiteboardActivity
            if (activity != null) {
                val state = vm.getCurrentState()
                if (state.shapes.isEmpty() && state.textBoxes.isEmpty() && state.strokes.isEmpty()) {
                    Toast.makeText(requireContext(), "Nothing to save", Toast.LENGTH_SHORT).show()
                    Log.d("ToolbarFragment", "Empty state, nothing to save")
                    return@setOnClickListener
                }
                activity.startSaving() // Call public method to set isSaving and show progress bar
                Log.d("ToolbarFragment", "Saving state: ${state.shapes.size} shapes, ${state.textBoxes.size} textBoxes")
                vm.save(requireContext())
            } else {
                Log.e("ToolbarFragment", "Activity is not WhiteboardActivity")
                Toast.makeText(requireContext(), "Failed to save: Invalid activity", Toast.LENGTH_LONG).show()
            }
        }

        loadBtn.setOnClickListener {
            val intent = Intent(requireContext(), LoadActivity::class.java)
            startActivity(intent)
        }

        shapesBtn.setOnClickListener {
            showShapePickerDialog1()
        }

        insertTextBtn.setOnClickListener {
            vm.setTool(WhiteboardViewModel.Tool.TEXT)
            showInsertTextDialog()
        }

        selectMoveBtn.setOnClickListener {
            showSelectMovePopup(it)
        }

        return v
    }

    private fun showColorPickerDialog1() {
        val colors = arrayOf(
            "#FF000000", "#FFFF0000", "#FF0000FF",
            "#FF00FF00", "#FFFFFF00", "#FF800080"
        )

        val strokeWidths = listOf(4f, 8f, 16f) // Thin, Medium, Thick

        val dialogView = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        val colorGrid = dialogView.findViewById<GridLayout>(R.id.colorGrid)
        val strokeGrid = dialogView.findViewById<GridLayout>(R.id.strokeWidthGrid)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Choose Pen Color & Width")
            .setView(dialogView)
            .create()

        var selectedStrokeWidth = 8f // default

        // Add stroke width items
        strokeWidths.forEach { width ->
            val btn = Button(requireContext())
            btn.text = when (width) {
                4f -> "Thin"
                8f -> "Medium"
                16f -> "Thick"
                else -> width.toInt().toString()
            }
            btn.setOnClickListener {
                selectedStrokeWidth = width
            }
            strokeGrid.addView(btn)
        }

        // Add color circles
        colors.forEach { colorHex ->
            val circleView = layoutInflater.inflate(R.layout.item_color_circle, colorGrid, false)
            val colorCircle = circleView.findViewById<View>(R.id.colorCircle)
            colorCircle.background.setTint(android.graphics.Color.parseColor(colorHex))
            colorCircle.setOnClickListener {
                vm.setTool(WhiteboardViewModel.Tool.PEN)
                vm.setColor(colorHex)
                vm.setStrokeWidth(selectedStrokeWidth)
                dialog.dismiss()
            }
            colorGrid.addView(circleView)
        }

        dialog.show()
    }

    private fun showShapePickerDialog1() {
        val shapes = listOf(
            "Rectangle" to ShapeType.RECTANGLE,
            "Circle" to ShapeType.CIRCLE,
            "Line" to ShapeType.LINE,
            "Polygon" to ShapeType.POLYGON
        )

        val colors = listOf(
            "#FF000000", "#FFFF0000", "#FF0000FF",
            "#FF00FF00", "#FFFFFF00", "#FF800080"
        )

        val strokeWidths = listOf(
            4f to "Thin",
            8f to "Medium",
            16f to "Thick"
        )

        val dialogView = layoutInflater.inflate(R.layout.dialog_shape_picker, null)
        val shapeGrid = dialogView.findViewById<GridLayout>(R.id.shapeGrid)
        val colorGrid = dialogView.findViewById<GridLayout>(R.id.colorGrid)
        val strokeGrid = dialogView.findViewById<GridLayout>(R.id.strokeWidthGrid)
        val drawingView = requireActivity().findViewById<DrawingView>(R.id.drawingView)

        var selectedColor = "#FF000000" // default
        var selectedStroke = 8f // default Medium

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Choose Shape, Color & Width")
            .setView(dialogView)
            .create()

        // Color picker
        colors.forEach { colorHex ->
            val circleView = layoutInflater.inflate(R.layout.item_color_circle, colorGrid, false)
            val colorCircle = circleView.findViewById<View>(R.id.colorCircle)
            colorCircle.background.setTint(android.graphics.Color.parseColor(colorHex))
            colorCircle.setOnClickListener {
                selectedColor = colorHex
            }
            colorGrid.addView(circleView)
        }

        // Stroke width buttons
        strokeWidths.forEach { (width, label) ->
            val btn = Button(requireContext())
            btn.text = label
            btn.setOnClickListener {
                selectedStroke = width
                vm.setShapeStrokeWidth(width)
            }
            strokeGrid.addView(btn)
        }

        // Shape buttons
        shapes.forEach { (name, type) ->
            val btn = Button(requireContext())
            btn.text = name
            btn.setOnClickListener {
                vm.setCurrentShape(type)
                vm.setColor(selectedColor)
                vm.setShapeStrokeWidth(selectedStroke)
                drawingView.startShapeDrawing(type, selectedColor)
                dialog.dismiss()
            }
            shapeGrid.addView(btn)
        }

        dialog.show()
    }

    private fun showEraserDialog() {
        val shapes = listOf(
            Pair("Round", WhiteboardViewModel.EraserShape.ROUND),
            Pair("Square", WhiteboardViewModel.EraserShape.SQUARE),
            Pair("Select Area", WhiteboardViewModel.EraserShape.SELECT_AREA),
            Pair("Freeform", WhiteboardViewModel.EraserShape.FREEFORM),
                    Pair("Pixel", WhiteboardViewModel.EraserShape.PIXEL)
        )

        val sizes = listOf(
            Pair("Small", 10f),
            Pair("Medium", 25f),
            Pair("Large", 40f)
        )

        val dialogView = layoutInflater.inflate(R.layout.dialog_stroke_width_picker, null)
        val shapeGrid = dialogView.findViewById<GridLayout>(R.id.shapeGrid)
        val sizeGrid = dialogView.findViewById<GridLayout>(R.id.strokeWidthGrid)

        var selectedShape = WhiteboardViewModel.EraserShape.ROUND
        var selectedSize = 25f // default medium

        // Add shape buttons
        shapes.forEach { (label, shape) ->
            val btn = Button(requireContext()).apply {
                text = label
                setOnClickListener { selectedShape = shape }
            }
            shapeGrid.addView(btn)
        }

        // Add size buttons
        sizes.forEach { (label, size) ->
            val btn = Button(requireContext()).apply {
                text = label
                setOnClickListener { selectedSize = size }
            }
            sizeGrid.addView(btn)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Choose Eraser Size & Shape")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                vm.setTool(WhiteboardViewModel.Tool.ERASER)
                vm.setEraserShape(selectedShape)
                if (selectedShape != WhiteboardViewModel.EraserShape.SELECT_AREA) {
                    vm.setStrokeWidth(selectedSize)
                }
                if (selectedShape == WhiteboardViewModel.EraserShape.FREEFORM) {
                    Toast.makeText(requireContext(), "Draw to erase content", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun showSelectMovePopup(anchor: View) {
        val popupView = layoutInflater.inflate(R.layout.dialog_select_area, null)
        val gridLayout = popupView.findViewById<GridLayout>(R.id.shapeGrid)
        val dragHandle = popupView.findViewById<TextView>(R.id.dragHandle)

        val popup = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            elevation = 10f
        }

        var lastX = 0
        var lastY = 0
        var initialX = 0
        var initialY = 0

        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX.toInt()
                    lastY = event.rawY.toInt()
                }
                MotionEvent.ACTION_MOVE -> {
                    val offsetX = event.rawX.toInt() - lastX
                    val offsetY = event.rawY.toInt() - lastY

                    initialX += offsetX
                    initialY += offsetY

                    popup.update(initialX, initialY, -1, -1)

                    lastX = event.rawX.toInt()
                    lastY = event.rawY.toInt()
                }
            }
            true
        }

        val options = listOf(
            "Move" to R.drawable.select_area,
            "Resize" to R.drawable.select_area,
            "Rotate" to R.drawable.select_area,
            "Delete" to R.drawable.select_area,
            "Close" to R.drawable.select_area
        )

        val buttonSizeDp = 40
        val scale = resources.displayMetrics.density
        val buttonSizePx = (buttonSizeDp * scale).toInt()

        options.forEach { (option, iconRes) ->
            val layout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width = buttonSizePx
                    height = buttonSizePx
                    setMargins(8, 8, 8, 8)
                }
                setBackgroundResource(android.R.drawable.btn_default)
                isClickable = true
                isFocusable = true
            }

            val imageView = ImageView(requireContext()).apply {
                setImageResource(iconRes)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    0,
                    2f
                ).apply { gravity = Gravity.CENTER }
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            layout.addView(imageView)

            val textView = TextView(requireContext()).apply {
                text = option
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                textSize = 12f
            }
            layout.addView(textView)

            layout.setOnClickListener {
                when (option) {
                    "Close" -> popup.dismiss()
                    "Move" -> {
                        val drawingView = requireActivity().findViewById<DrawingView>(R.id.drawingView)
                        vm.setTool(WhiteboardViewModel.Tool.SELECT_AREA)
                        Toast.makeText(requireContext(), "Draw a rectangle to select shapes", Toast.LENGTH_SHORT).show()
                        drawingView.onSelectionCompleted = { selectedItems ->
                            vm.setTool(WhiteboardViewModel.Tool.SELECT_MOVE)
                            val firstShape = selectedItems.shapes.firstOrNull()
                            drawingView.setSelectedShape(firstShape)
                        }
                        popup.dismiss()
                    }
                    else -> vm.setTool(WhiteboardViewModel.Tool.SELECT_MOVE)
                }
            }

            gridLayout.addView(layout)
        }

        val loc = IntArray(2)
        anchor.getLocationOnScreen(loc)
        initialX = loc[0]
        initialY = loc[1] + anchor.height
        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, initialX, initialY)
    }

    private fun showInsertTextDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_insert_text, null)
        val editText = dialogView.findViewById<EditText>(R.id.editText)
        val colorGrid = dialogView.findViewById<GridLayout>(R.id.colorGrid)
        val sizeGrid = dialogView.findViewById<GridLayout>(R.id.sizeGrid)

        val colors = listOf("#FF000000", "#FFFF0000", "#FF0000FF", "#FF00FF00", "#FFFFFF00", "#FF800080")
        val sizes = listOf(16, 24, 32)

        var selectedColor = colors[0]
        var selectedSize = sizes[0]

        colors.forEach { colorHex ->
            val btn = Button(requireContext()).apply {
                setBackgroundColor(android.graphics.Color.parseColor(colorHex))
                setOnClickListener { selectedColor = colorHex }
            }
            colorGrid.addView(btn)
        }

        sizes.forEach { size ->
            val btn = Button(requireContext()).apply {
                text = "$size"
                setOnClickListener { selectedSize = size }
            }
            sizeGrid.addView(btn)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Insert Text")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val drawingView = requireActivity().findViewById<DrawingView>(R.id.drawingView)
                val viewModel = drawingView.viewModel
                val text = editText.text.toString()
                if (text.isNotEmpty() && viewModel != null) {
                    val rect = RectF(
                        drawingView.width / 2f,
                        drawingView.height / 2f,
                        drawingView.width / 2f + 300,
                        drawingView.height / 2f + 100
                    )
                    viewModel.addTextBox(
                        bounds = rect,
                        text = text,
                        color = selectedColor,
                        textSize = selectedSize.toFloat()
                    )
                    drawingView.invalidate()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun showRotateLeftPopup(anchor: View, vm: WhiteboardViewModel) {
        val popupView = layoutInflater.inflate(R.layout.dialog_rotate_options, null)
        val popup = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popup.elevation = 10f
        popup.isOutsideTouchable = true

        val btnSelect = popupView.findViewById<Button>(R.id.btnSelect)
        val btnRotate90 = popupView.findViewById<Button>(R.id.btnRotate90)
        val btnRotate180 = popupView.findViewById<Button>(R.id.btnRotate180)

        btnSelect.setOnClickListener {
            Toast.makeText(requireContext(), "Select tool active", Toast.LENGTH_SHORT).show()
            vm.setTool(WhiteboardViewModel.Tool.SELECT)
            popup.dismiss()
        }

        btnRotate90.setOnClickListener {
            val currentSelection = vm.selectedItems.value ?: return@setOnClickListener
            currentSelection.shapes.forEach { shape ->
                shape.rotationAngle = (shape.rotationAngle - 90f) % 360
                if (shape.type == WhiteboardViewModel.ShapeType.POLYGON) {
                    val centerX = shape.points.map { it.x }.average().toFloat()
                    val centerY = shape.points.map { it.y }.average().toFloat()
                    val matrix = Matrix()
                    matrix.postRotate(-90f, centerX, centerY)
                    shape.points = shape.points.map { point ->
                        val pointArray = floatArrayOf(point.x, point.y)
                        matrix.mapPoints(pointArray)
                        Point(pointArray[0], pointArray[1])
                    }
                }
                vm.updateShape(shape)
            }
            currentSelection.textBoxes.forEach { tb ->
                tb.rotationAngle = (tb.rotationAngle - 90f) % 360
                vm.updateTextBox(tb)
            }
            currentSelection.strokes.forEach { stroke ->
                stroke.rotationAngle = (stroke.rotationAngle - 90f) % 360
                vm.updateStroke(stroke)
            }
            requireActivity().findViewById<DrawingView>(R.id.drawingView).invalidate()
            popup.dismiss()
        }

        btnRotate180.setOnClickListener {
            val currentSelection = vm.selectedItems.value ?: return@setOnClickListener
            currentSelection.shapes.forEach { shape ->
                shape.rotationAngle = (shape.rotationAngle - 180f) % 360
                if (shape.type == WhiteboardViewModel.ShapeType.POLYGON) {
                    val centerX = shape.points.map { it.x }.average().toFloat()
                    val centerY = shape.points.map { it.y }.average().toFloat()
                    val matrix = Matrix()
                    matrix.postRotate(-180f, centerX, centerY)
                    shape.points = shape.points.map { point ->
                        val pointArray = floatArrayOf(point.x, point.y)
                        matrix.mapPoints(pointArray)
                        Point(pointArray[0], pointArray[1])
                    }
                }
                vm.updateShape(shape)
            }
            currentSelection.textBoxes.forEach { tb ->
                tb.rotationAngle = (tb.rotationAngle - 180f) % 360
                vm.updateTextBox(tb)
            }
            currentSelection.strokes.forEach { stroke ->
                stroke.rotationAngle = (stroke.rotationAngle - 180f) % 360
                vm.updateStroke(stroke)
            }
            requireActivity().findViewById<DrawingView>(R.id.drawingView).invalidate()
            popup.dismiss()
        }

        popup.showAsDropDown(anchor, 0, 10)
    }

    private fun showRotateRightPopup(anchor: View, vm: WhiteboardViewModel) {
        val popupView = layoutInflater.inflate(R.layout.dialog_rotate_right_options, null)
        val popup = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popup.elevation = 10f
        popup.isOutsideTouchable = true

        val btnSelect = popupView.findViewById<Button>(R.id.btnSelect)
        val btnRotate90 = popupView.findViewById<Button>(R.id.btnRotate90)
        val btnRotate180 = popupView.findViewById<Button>(R.id.btnRotate180)

        btnSelect.setOnClickListener {
            Toast.makeText(requireContext(), "Select tool active", Toast.LENGTH_SHORT).show()
            vm.setTool(WhiteboardViewModel.Tool.SELECT)
            popup.dismiss()
        }

        btnRotate90.setOnClickListener {
            val currentSelection = vm.selectedItems.value ?: return@setOnClickListener
            currentSelection.shapes.forEach { shape ->
                if (shape.type == WhiteboardViewModel.ShapeType.POLYGON) {
                    val centerX = shape.points.map { it.x }.average().toFloat()
                    val centerY = shape.points.map { it.y }.average().toFloat()
                    val matrix = Matrix()
                    matrix.postRotate(90f, centerX, centerY)
                    shape.points = shape.points.map { point ->
                        val pointArray = floatArrayOf(point.x, point.y)
                        matrix.mapPoints(pointArray)
                        Point(pointArray[0], pointArray[1])
                    }
                    shape.rotationAngle = 0f
                } else {
                    shape.rotationAngle = (shape.rotationAngle + 90f) % 360
                }
                vm.updateShape(shape)
            }
            currentSelection.textBoxes.forEach { tb ->
                tb.rotationAngle = (tb.rotationAngle + 90f) % 360
                vm.updateTextBox(tb)
            }
            currentSelection.strokes.forEach { stroke ->
                stroke.rotationAngle = (stroke.rotationAngle + 90f) % 360
                vm.updateStroke(stroke)
            }
            requireActivity().findViewById<DrawingView>(R.id.drawingView).invalidate()
            popup.dismiss()
        }

        btnRotate180.setOnClickListener {
            val currentSelection = vm.selectedItems.value ?: return@setOnClickListener
            currentSelection.shapes.forEach { shape ->
                if (shape.type == WhiteboardViewModel.ShapeType.POLYGON) {
                    val centerX = shape.points.map { it.x }.average().toFloat()
                    val centerY = shape.points.map { it.y }.average().toFloat()
                    val matrix = Matrix()
                    matrix.postRotate(180f, centerX, centerY)
                    shape.points = shape.points.map { point ->
                        val pointArray = floatArrayOf(point.x, point.y)
                        matrix.mapPoints(pointArray)
                        Point(pointArray[0], pointArray[1])
                    }
                    shape.rotationAngle = 0f
                } else {
                    shape.rotationAngle = (shape.rotationAngle + 180f) % 360
                }
                vm.updateShape(shape)
            }
            currentSelection.textBoxes.forEach { tb ->
                tb.rotationAngle = (tb.rotationAngle + 180f) % 360
                vm.updateTextBox(tb)
            }
            currentSelection.strokes.forEach { stroke ->
                stroke.rotationAngle = (stroke.rotationAngle + 180f) % 360
                vm.updateStroke(stroke)
            }
            requireActivity().findViewById<DrawingView>(R.id.drawingView).invalidate()
            popup.dismiss()
        }

        popup.showAsDropDown(anchor, 0, 10)
    }
}