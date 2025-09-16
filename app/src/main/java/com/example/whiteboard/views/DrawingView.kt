package com.example.whiteboard.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.whiteboard.handler.DrawingHandler
import com.example.whiteboard.handler.EraserHandler
import com.example.whiteboard.handler.RotationHandler
import com.example.whiteboard.handler.SelectionHandler
import com.example.whiteboard.handler.TextHandler
import com.example.whiteboard.viewmodels.WhiteboardViewModel
import com.example.whiteboard.models.Point
import com.example.whiteboard.models.SelectedItems
import com.example.whiteboard.models.TextBox


class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    var viewModel: WhiteboardViewModel? = null
) : View(context, attrs) {

    private val drawingHandler = DrawingHandler(this, viewModel)
    private val eraserHandler = EraserHandler(this, viewModel, drawingHandler)
    private val textHandler = TextHandler(this, viewModel)
    private val selectionHandler = SelectionHandler(this, viewModel)
    private val rotationHandler = RotationHandler(viewModel)
    private var isSelecting = false
    var onSelectionCompleted: ((SelectedItems) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val vm = viewModel ?: return
        drawingHandler.drawShapes(canvas, vm)
        drawingHandler.drawTextBoxes(canvas, vm)
        drawingHandler.drawShapePreview(canvas, vm)
        drawingHandler.drawSelectedItems(canvas, vm)
        drawingHandler.drawStrokes(canvas, vm)
        selectionHandler.drawSelectionRectangle(canvas)
        eraserHandler.drawEraserRectangle(canvas)
        eraserHandler.drawFreeformEraser(canvas, vm)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val vm = viewModel ?: return false
        val tool = vm.getTool()
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                when (tool) {
                    WhiteboardViewModel.Tool.SHAPE -> drawingHandler.startShapeDrawing(x, y)
                    WhiteboardViewModel.Tool.ERASER -> {
                        when (vm.getEraserShape()) {
                            WhiteboardViewModel.EraserShape.SELECT_AREA -> eraserHandler.startErasing(x, y, vm)
                            WhiteboardViewModel.EraserShape.FREEFORM -> eraserHandler.startFreeformErasing(x, y, vm)
                            else -> drawingHandler.startFreehand(x, y, vm, isEraser = true)
                        }
                    }
                    WhiteboardViewModel.Tool.TEXT -> textHandler.startTextInteraction(x, y)
                    WhiteboardViewModel.Tool.SELECT_AREA, WhiteboardViewModel.Tool.SELECT -> selectionHandler.startSelection(x, y)
                    WhiteboardViewModel.Tool.SELECT_MOVE -> selectionHandler.startMove(x, y, vm)
                    else -> drawingHandler.startFreehand(x, y, vm)
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                when (tool) {
                    WhiteboardViewModel.Tool.SHAPE -> drawingHandler.updateShapeDrawing(x, y)
                    WhiteboardViewModel.Tool.ERASER -> {
                        when (vm.getEraserShape()) {
                            WhiteboardViewModel.EraserShape.SELECT_AREA -> eraserHandler.updateErasing(x, y, vm)
                            WhiteboardViewModel.EraserShape.FREEFORM -> eraserHandler.updateFreeformErasing(x, y, vm)
                            else -> drawingHandler.continueFreehand(x, y)
                        }
                    }
                    WhiteboardViewModel.Tool.TEXT -> textHandler.updateTextInteraction(x, y, vm)
                    WhiteboardViewModel.Tool.SELECT_AREA, WhiteboardViewModel.Tool.SELECT -> selectionHandler.updateSelection(x, y)
                    WhiteboardViewModel.Tool.SELECT_MOVE -> {
                        if (event.pointerCount == 2) {
                            rotationHandler.handleRotation(event, vm.selectedItems.value ?: SelectedItems())
                        } else {
                            selectionHandler.updateMove(x, y, vm)
                        }
                    }
                    else -> drawingHandler.continueFreehand(x, y)
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                when (tool) {
                    WhiteboardViewModel.Tool.SHAPE -> drawingHandler.commitShape(vm)
                    WhiteboardViewModel.Tool.ERASER -> {
                        when (vm.getEraserShape()) {
                            WhiteboardViewModel.EraserShape.SELECT_AREA -> eraserHandler.commitEraser(vm)
                            WhiteboardViewModel.EraserShape.FREEFORM -> eraserHandler.commitFreeformErasing(vm)
                            else -> drawingHandler.commitFreehand(vm, isEraser = true)
                        }
                    }
                    WhiteboardViewModel.Tool.TEXT -> textHandler.commitText(vm, x, y)
                    WhiteboardViewModel.Tool.SELECT_AREA -> selectionHandler.commitSelection(vm, onSelectionCompleted)
                    WhiteboardViewModel.Tool.SELECT -> selectionHandler.performSelection(vm, onSelectionCompleted)
                    WhiteboardViewModel.Tool.SELECT_MOVE -> selectionHandler.finishMove()
                    else -> drawingHandler.commitFreehand(vm)
                }
                invalidate()
            }
        }
        return true
    }

    fun startShapeDrawing(type: WhiteboardViewModel.ShapeType, colorHex: String? = null) {
        drawingHandler.startShapeDrawing(type, colorHex)
    }

    fun selectLastShape() {
        selectionHandler.selectLastShape()
    }

    fun selectLastTextBox() {
        selectionHandler.selectLastTextBox()
    }

    fun selectLastDrawable() {
        selectionHandler.selectLastDrawable()
    }

    fun finishSelection() {
        selectionHandler.finishSelection(onSelectionCompleted)
    }

    fun setSelectedShape(shape: com.example.whiteboard.models.Shape?) {
        selectionHandler.setSelectedShape(shape)
    }

    fun setSelectedItems(items: SelectedItems) {
        selectionHandler.setSelectedItems(items)
    }

    fun enableSelection() {
        isSelecting = true
        selectionHandler.clearSelection()
        invalidate()
    }

    fun disableSelection() {
        isSelecting = false
        selectionHandler.clearSelection()
        invalidate()
    }

    // Public methods to delegate to TextHandler
    fun findTextBoxAt(x: Float, y: Float): TextBox? {
        return textHandler.findTextBoxAt(x, y)
    }

    fun moveTextBox(tb: TextBox, x: Float, y: Float, vm: WhiteboardViewModel) {
        textHandler.moveTextBox(tb, x, y, vm)
    }

    fun getSelectedTextBox(): TextBox? {
        return textHandler.getSelectedTextBox()
    }

    fun setSelectedTextBox(textBox: TextBox?) {
        textHandler.setSelectedTextBox(textBox)
    }
}