package com.example.whiteboard.ui



/**
 * ToolbarFragment
 * -----------------
 * Fragment that provides a toolbar with tools:
 * - Pen (color & width picker)
 * - Eraser (size & shape picker)
 * - Undo/Redo
 * - Save/Load
 * - Shapes (rectangle, circle, line, polygon)
 * - Insert Text
 * - Select/Move tool
 *
 * Communicates with WhiteboardViewModel to update tool state and trigger actions.
 */



import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.whiteboard.R
import com.example.whiteboard.activity.LoadActivity
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
        val shapesBtn  = v.findViewById<Button>(R.id.btn_shapes)
        val insertTextBtn = v.findViewById<Button>(R.id.btn_insert_text)
        val selectMoveBtn = v.findViewById<Button>(R.id.btn_select_move)
        val btn_load = v.findViewById<Button>(R.id.btn_load)

        penBtn.setOnClickListener {
           // vm.setTool(WhiteboardViewModel.Tool.PEN)

            showColorPickerDialog1()


        }
        eraserBtn.setOnClickListener { //vm.setTool(WhiteboardViewModel.Tool.ERASER)
            //showEraserSizeDialog()
            //showEraserShapeDialog()
            showEraserDialog()}
        undoBtn.setOnClickListener { vm.undo() }
        redoBtn.setOnClickListener { vm.redo() }
        saveBtn.setOnClickListener { vm.save(requireContext()) }
        loadBtn.setOnClickListener { vm.load(requireContext()) }
        shapesBtn.setOnClickListener { showShapePickerDialog1() }

        insertTextBtn.setOnClickListener {
            vm.setTool(WhiteboardViewModel.Tool.TEXT) // set the tool to TEXT

//            val drawingView = requireActivity().findViewById<DrawingView>(R.id.drawingView)
//            // Use center of screen as default position, user can drag later
//            val x = drawingView.width / 2f
//            val y = drawingView.height / 2f
//            drawingView.showInsertTextDialog(x, y) // new dialog to set text, size, style & color
        }

        selectMoveBtn.setOnClickListener {
            vm.setTool(WhiteboardViewModel.Tool.SELECT_MOVE)
        }
        btn_load.setOnClickListener {
            val intent = Intent(requireContext(), LoadActivity::class.java)
            startActivity(intent)
        }


        return v
    }

    private fun showColorPickerDialog() {
        val colors = arrayOf(
            "#FF000000", // Black
            "#FFFF0000", // Red
            "#FF0000FF", // Blue
            "#FF00FF00", // Green
            "#FFFFFF00", // Yellow
            "#FF800080"  // Purple
        )

        val dialogView = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        val gridLayout = dialogView.findViewById<GridLayout>(R.id.colorGrid)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Choose Pen Color")
            .setView(dialogView)
            .create()

        colors.forEach { colorHex ->
            val circleView = layoutInflater.inflate(R.layout.item_color_circle, gridLayout, false)
            val colorCircle = circleView.findViewById<View>(R.id.colorCircle)
            colorCircle.background.setTint(android.graphics.Color.parseColor(colorHex))
            colorCircle.setOnClickListener {
                vm.setTool(WhiteboardViewModel.Tool.PEN)
                vm.setColor(colorHex)
                dialog.dismiss()
            }
            gridLayout.addView(circleView)
        }

        dialog.show()
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
            btn.text = when(width) {
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

    private fun showShapePickerDialog() {
        val shapes = listOf(
            Pair("Rectangle", ShapeType.RECTANGLE),
            Pair("Circle", ShapeType.CIRCLE),
            Pair("Line", ShapeType.LINE),
            Pair("Polygon", ShapeType.POLYGON)
        )

        val dialogView = layoutInflater.inflate(R.layout.dialog_shape_picker, null)
        val gridLayout = dialogView.findViewById<GridLayout>(R.id.shapeGrid)
        val drawingView = requireActivity().findViewById<DrawingView>(R.id.drawingView)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Choose Shape")
            .setView(dialogView)
            .create()

        shapes.forEach { (name, type) ->
            val btn = Button(requireContext())
            btn.text = name
            btn.setOnClickListener {
                drawingView.startShapeDrawing(type) // call your DrawingView to start shape drawing
                dialog.dismiss()
            }
            gridLayout.addView(btn)
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
        val strokeGrid = dialogView.findViewById<GridLayout>(R.id.strokeWidthGrid) // Add this in your XML
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
                vm.setShapeStrokeWidth(width)  // update ViewModel shape stroke width
            }
            strokeGrid.addView(btn)
        }


        // Shape buttons
        shapes.forEach { (name, type) ->
            val btn = Button(requireContext())
            btn.text = name
            btn.setOnClickListener {
                drawingView.startShapeDrawing(type, selectedColor)
                drawingView.viewModel?.setStrokeWidth(selectedStroke)
                dialog.dismiss()
            }
            shapeGrid.addView(btn)
        }

        dialog.show()
    }


    private fun showEraserSizeDialog() {
        val eraserSizes = listOf(
            Pair("Small", 20f),
            Pair("Medium", 45f),
            Pair("Large", 60f)
        )

        val dialogView = layoutInflater.inflate(R.layout.dialog_stroke_width_picker, null)
        val gridLayout = dialogView.findViewById<GridLayout>(R.id.strokeWidthGrid)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Choose Eraser Size")
            .setView(dialogView)
            .create()

        eraserSizes.forEach { (label, size) ->
            val btn = Button(requireContext())
            btn.text = label
            btn.setOnClickListener {
                vm.setTool(WhiteboardViewModel.Tool.ERASER)
                vm.setStrokeWidth(size)
                dialog.dismiss()
            }
            gridLayout.addView(btn)
        }

        dialog.show()
    }



    private fun showEraserShapeDialog() {
        val shapes = listOf(
            Pair("Round", WhiteboardViewModel.EraserShape.ROUND),
            Pair("Square", WhiteboardViewModel.EraserShape.SQUARE)
        )

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Choose Eraser Shape")
            .create()

        val gridLayout = GridLayout(requireContext()).apply {
            columnCount = shapes.size
            setPadding(16, 16, 16, 16)
        }

        shapes.forEach { (label, shape) ->
            val btn = Button(requireContext()).apply {
                text = label
                setOnClickListener {
                    vm.setTool(WhiteboardViewModel.Tool.ERASER)
                    vm.setEraserShape(shape)
                    dialog.dismiss()
                }
            }
            gridLayout.addView(btn)
        }

        dialog.setView(gridLayout)
        dialog.show()
    }


    private fun showEraserDialog() {
        val shapes = listOf(
            Pair("Round", WhiteboardViewModel.EraserShape.ROUND),
            Pair("Square", WhiteboardViewModel.EraserShape.SQUARE),
            Pair("Select Area", WhiteboardViewModel.EraserShape.SELECT_AREA) // Added Select Area
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

        // Add size buttons (ignore if Select Area is chosen)
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
                // Only apply size if not Select Area
                if (selectedShape != WhiteboardViewModel.EraserShape.SELECT_AREA) {
                    vm.setStrokeWidth(selectedSize)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }






}
