package com.example.whiteboard.viewmodels

import android.content.Context
import android.graphics.Paint

import android.graphics.RectF
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.whiteboard.models.Point
import com.example.whiteboard.models.RectFSerializable
import com.example.whiteboard.models.Shape
import com.example.whiteboard.models.Stroke
import com.example.whiteboard.models.TextBox
import com.example.whiteboard.models.WhiteboardState
import com.example.whiteboard.services.FileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

import android.text.Layout



class WhiteboardViewModel : ViewModel() {

    enum class Tool { PEN, ERASER, SHAPE, TEXT, SELECT_MOVE }
    enum class ShapeType { RECTANGLE, CIRCLE, LINE, POLYGON }
    enum class EraserShape { ROUND, SQUARE, SELECT_AREA }

    private val _state = MutableLiveData(WhiteboardState())
    val state: LiveData<WhiteboardState> = _state

    private val _savedFile = MutableLiveData<String?>(null)
    val savedFile: LiveData<String?> = _savedFile

    private val undoStack = ArrayList<WhiteboardState>()
    private val redoStack = ArrayList<WhiteboardState>()

    private var currentTool: Tool = Tool.PEN
    private var currentColor: String = "#FF000000"
    private var currentStrokeWidth: Float = 8f
    private var currentShapeType: ShapeType? = null
    private var currentEraserShape: EraserShape = EraserShape.ROUND
    private var isAreaEraser: Boolean = false  // Flag for area erase

    var currentTextAlignment: Paint.Align = Paint.Align.LEFT

    private var currentShapeStrokeWidth: Float = 8f


    var currentTextColor: String = "#FF000000"
    var currentTextSize: Float = 32f
    var currentTextStyle: Int = android.graphics.Typeface.NORMAL
    var currentFontFamily: String = "sans-serif"

    private val _textBoxes = MutableLiveData<List<TextBox>>(emptyList())
    val textBoxes: LiveData<List<TextBox>> = _textBoxes

    private val _shapes = MutableLiveData<List<Shape>>(emptyList())
    val shapes: LiveData<List<Shape>> = _shapes

    /** TOOL SETTINGS **/
    fun setTool(t: Tool) { currentTool = t }
    fun getTool(): Tool = currentTool

    fun setShapeStrokeWidth(width: Float) { currentShapeStrokeWidth = width }
    fun getShapeStrokeWidth(): Float = currentShapeStrokeWidth



    fun setTextColor(colorHex: String) { currentTextColor = colorHex }
    fun getTextColor(): String = currentTextColor

    fun setTextSize(size: Float) { currentTextSize = size }
    fun getTextSize(): Float = currentTextSize

    fun setTextStyle(style: Int) { currentTextStyle = style }
    fun getTextStyle(): Int = currentTextStyle

    fun setFontFamily(font: String) { currentFontFamily = font }
    fun getFontFamily(): String = currentFontFamily


    fun setTextAlignment(align: Paint.Align) { currentTextAlignment = align }
    fun getTextAlignment(): Paint.Align = currentTextAlignment

    fun setCurrentShape(type: ShapeType) {
        currentShapeType = type
        currentTool = Tool.SHAPE
    }
    fun getCurrentShapeType(): ShapeType? = currentShapeType

    fun setColor(colorHex: String) { currentColor = colorHex }
    fun getColor(): String = currentColor

    fun setStrokeWidth(width: Float) { currentStrokeWidth = width }
    fun getStrokeWidth(): Float = currentStrokeWidth

    fun setEraserShape(shape: EraserShape) {
        currentEraserShape = shape
        isAreaEraser = (shape == EraserShape.SELECT_AREA)
    }
    fun getEraserShape(): EraserShape = currentEraserShape

//    fun eraseArea(rect: RectF) {
//        // Remove shapes inside the area
//        _shapes.value = _shapes.value?.filter { shape ->
//            val bounds = shape.bounds?.toRectF()
//            bounds == null || !rect.contains(bounds)
//        }
//
//        // Remove strokes inside the area
//        val currentStrokes = _state.value?.strokes?.toMutableList() ?: mutableListOf()
//        val filtered = currentStrokes.filter { stroke ->
//            !stroke.points.all { rect.contains(it.x, it.y) }
//        }
//        _state.value = _state.value?.copy(strokes = filtered)
//    }



    fun eraseArea(rect: RectF) {
        // 🔹 Remove shapes partially/fully inside selection rectangle
        _shapes.value = _shapes.value?.filterNot { shape ->
            val bounds = shape.bounds?.toRectF()
            bounds != null && RectF.intersects(rect, bounds)
        }

        // 🔹 Remove strokes partially/fully inside
        val currentStrokes = _state.value?.strokes?.toMutableList() ?: mutableListOf()
        val filteredStrokes = currentStrokes.filterNot { stroke ->
            stroke.points.any { rect.contains(it.x, it.y) }
        }
        _state.value = _state.value?.copy(strokes = filteredStrokes)

        // 🔹 Remove TextBoxes overlapping selection
        _textBoxes.value = _textBoxes.value?.filterNot { tb ->
            val bounds = tb.bounds.toRectF()

            // 🔹 Intersection check with small margin for safety
            val overlap = rect.left < bounds.right &&
                    rect.right > bounds.left &&
                    rect.top < bounds.bottom &&
                    rect.bottom > bounds.top
            overlap
        }
    }




    fun setAreaEraser(enabled: Boolean) { isAreaEraser = enabled }
    fun getIsAreaEraser(): Boolean = isAreaEraser

    /** SHAPE MANAGEMENT **/
    fun addShape(shape: Shape) {
        // Copy the shape and assign the current stroke width
        val shapeWithWidth = shape.copy(strokeWidth = currentShapeStrokeWidth)
        val list = _shapes.value?.toMutableList() ?: mutableListOf()
        list.add(shapeWithWidth)
        _shapes.value = list
    }

    fun removeShape(shape: Shape) {
        val list = _shapes.value?.toMutableList() ?: return
        list.remove(shape)
        _shapes.value = list
    }

    fun removeShapeAt(index: Int) {
        val list = _shapes.value?.toMutableList() ?: return
        if (index in list.indices) {
            list.removeAt(index)
            _shapes.value = list
        }
    }

    fun clearShapes() { _shapes.value = emptyList() }




    /** STROKE MANAGEMENT **/
    fun addStroke(points: List<Point>, isEraser: Boolean = false) {
        val stroke = Stroke(
            id = UUID.randomUUID().toString(),
            points = points,
            color = if (isEraser) "#FFFFFFFF" else currentColor,
            width = currentStrokeWidth,
            isEraser = isEraser
        )
        addStroke(stroke)
    }

    fun addStroke(stroke: Stroke) {
        val prev = _state.value ?: WhiteboardState()
        val list = prev.strokes.toMutableList()
        list.add(stroke)
        updateState(prev.copy(strokes = list))
    }

    /** UNDO / REDO **/
    private fun updateState(newState: WhiteboardState) {
        undoStack.add(_state.value ?: WhiteboardState())
        redoStack.clear()
        _state.value = newState
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(_state.value ?: WhiteboardState())
            _state.value = prev
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(_state.value ?: WhiteboardState())
            _state.value = next
        }
    }

    /** FILE MANAGEMENT **/
//    fun save(context: Context) {
//        val svc = FileService(context)
//        val current = _state.value ?: WhiteboardState()
//        CoroutineScope(Dispatchers.IO).launch {
//            val file = svc.save(current)
//            _savedFile.postValue(file.absolutePath)
//        }
//    }


    fun save(context: Context) {
        val svc = FileService(context)
        val current = WhiteboardState(
            strokes = _state.value?.strokes ?: emptyList(),
            shapes = _shapes.value ?: emptyList(),
            textBoxes = _textBoxes.value ?: emptyList()
        )
        CoroutineScope(Dispatchers.IO).launch {
            val file = svc.save(current)
            _savedFile.postValue(file.absolutePath)
        }
    }


    fun load(context: Context) {
        val svc = FileService(context)
        CoroutineScope(Dispatchers.IO).launch {
            val first = svc.getLatestFile()
            if (first != null) {
                val loaded = svc.load(first)
                undoStack.clear()
                redoStack.clear()
                _state.postValue(loaded)
                _savedFile.postValue(first.absolutePath)
            } else {
                _savedFile.postValue(null)
            }
        }
    }




    fun addTextBox(
        bounds: RectF,
        text: String = "Text",
        color: String = currentTextColor,
        textSize: Float = currentTextSize,
        typeface: Int = currentTextStyle,
        fontFamily: String = currentFontFamily,
        alignment: Paint.Align = currentTextAlignment
    ) {
        val list = _textBoxes.value?.toMutableList() ?: mutableListOf()
        val tb = TextBox(
            id = UUID.randomUUID().toString(),
            text = text,
            bounds = RectFSerializable.from(bounds),
            color = color,
            textSize = textSize,
            typeface = typeface,
            fontFamily = fontFamily,
            alignment = alignment
        )
        list.add(tb)
        _textBoxes.value = list
    }

    fun updateTextBox(textBox: TextBox) {
        val list = _textBoxes.value?.toMutableList() ?: mutableListOf()
        val index = list.indexOfFirst { it.id == textBox.id }
        if (index != -1) {
            list[index] = textBox
            _textBoxes.value = list
        }
    }

    fun removeTextBox(textBox: TextBox) {
        val list = _textBoxes.value?.toMutableList() ?: mutableListOf()
        list.removeAll { it.id == textBox.id }
        _textBoxes.value = list
    }

}
