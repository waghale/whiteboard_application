
package com.example.whiteboard.viewmodels

import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.whiteboard.models.Point
import com.example.whiteboard.models.RectFSerializable
import com.example.whiteboard.models.SelectedItems
import com.example.whiteboard.models.Shape
import com.example.whiteboard.models.Stroke
import com.example.whiteboard.models.TextBox
import com.example.whiteboard.models.WhiteboardState
import com.example.whiteboard.services.FileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import androidx.lifecycle.viewModelScope





class WhiteboardViewModel : ViewModel() {

    enum class Tool { PEN, ERASER, SHAPE, TEXT, SELECT_MOVE, SELECT_AREA, SELECT }
    enum class ShapeType { RECTANGLE, CIRCLE, LINE, POLYGON }
    enum class EraserShape { ROUND, SQUARE, SELECT_AREA, FREEFORM,PIXEL }

    private var _state = MutableLiveData(WhiteboardState())
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
    private var isAreaEraser: Boolean = false

    var currentTextAlignment: Paint.Align = Paint.Align.LEFT
    var currentTextColor: String = "#FF000000"
    var currentTextSize: Float = 32f
    var currentTextStyle: Int = android.graphics.Typeface.NORMAL
    var currentFontFamily: String = "sans-serif"

    private var currentShapeStrokeWidth: Float = 8f

    private var _shapes = MutableLiveData<List<Shape>>(emptyList())
    var shapes: LiveData<List<Shape>> = _shapes

    private var _textBoxes = MutableLiveData<List<TextBox>>(emptyList())
    var textBoxes: LiveData<List<TextBox>> = _textBoxes

    private val _selectedItems = MutableLiveData<SelectedItems>(SelectedItems())
    val selectedItems: LiveData<SelectedItems> = _selectedItems

    /** TOOL SETTINGS **/
    fun setTool(t: Tool) {
        currentTool = t
        Log.d("WhiteboardViewModel", "Tool set: $t")
    }

    fun getTool(): Tool = currentTool

    fun getCurrentState(): WhiteboardState {
        return _state.value ?: WhiteboardState()
    }

    fun setShapeStrokeWidth(width: Float) {
        currentShapeStrokeWidth = width
        Log.d("WhiteboardViewModel", "Shape stroke width set: $width")
    }

    fun getShapeStrokeWidth(): Float = currentShapeStrokeWidth

    fun setTextColor(colorHex: String) {
        currentTextColor = colorHex
        Log.d("WhiteboardViewModel", "Text color set: $colorHex")
    }

    fun getTextColor(): String = currentTextColor

    fun setTextSize(size: Float) {
        currentTextSize = size
        Log.d("WhiteboardViewModel", "Text size set: $size")
    }

    fun getTextSize(): Float = currentTextSize

    fun setTextStyle(style: Int) {
        currentTextStyle = style
        Log.d("WhiteboardViewModel", "Text style set: $style")
    }

    fun getTextStyle(): Int = currentTextStyle

    fun setFontFamily(font: String) {
        currentFontFamily = font
        Log.d("WhiteboardViewModel", "Font family set: $font")
    }

    fun getFontFamily(): String = currentFontFamily

    fun setTextAlignment(align: Paint.Align) {
        currentTextAlignment = align
        Log.d("WhiteboardViewModel", "Text alignment set: $align")
    }

    fun getTextAlignment(): Paint.Align = currentTextAlignment

    fun setCurrentShape(type: ShapeType) {
        currentShapeType = type
        currentTool = Tool.SHAPE
        Log.d("WhiteboardViewModel", "Shape type set: $type")
    }

    fun getCurrentShapeType(): ShapeType? = currentShapeType

    fun setColor(colorHex: String) {
        currentColor = colorHex
        Log.d("WhiteboardViewModel", "Color set: $colorHex")
    }

    fun getColor(): String = currentColor

    fun setStrokeWidth(width: Float) {
        currentStrokeWidth = width
        Log.d("WhiteboardViewModel", "Stroke width set: $width")
    }

    fun getStrokeWidth(): Float = currentStrokeWidth

    fun setEraserShape(shape: EraserShape) {
        currentEraserShape = shape
        isAreaEraser = (shape == EraserShape.SELECT_AREA)
        Log.d("WhiteboardViewModel", "Eraser shape set: $shape, isAreaEraser: $isAreaEraser")
    }

    fun getEraserShape(): EraserShape = currentEraserShape

    fun setAreaEraser(enabled: Boolean) {
        isAreaEraser = enabled
        Log.d("WhiteboardViewModel", "Area eraser set: $enabled")
    }

    fun getIsAreaEraser(): Boolean = isAreaEraser

    /** STATE MANAGEMENT **/
    private fun updateState() {
        val currentState = WhiteboardState(
            strokes = _state.value?.strokes?.map { it.copy(points = it.points.map { p -> p.copy() }) } ?: emptyList(),
            shapes = _shapes.value?.map { it.copy(points = it.points.map { p -> p.copy() }) } ?: emptyList(),
            textBoxes = _textBoxes.value?.map { it.copy() } ?: emptyList()
        )
        if (undoStack.size > 50) undoStack.removeAt(0) // Limit stack size
        undoStack.add(currentState)
        redoStack.clear()
        _state.value = currentState
        _shapes.value = currentState.shapes
        _textBoxes.value = currentState.textBoxes
        _selectedItems.value = SelectedItems() // Clear selections after state change
        Log.d("WhiteboardViewModel", "State updated: ${currentState.shapes.size} shapes, ${currentState.shapes.count { it.type == ShapeType.POLYGON }} polygons")
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(
                WhiteboardState(
                    strokes = _state.value?.strokes?.map { it.copy(points = it.points.map { p -> p.copy() }) } ?: emptyList(),
                    shapes = _shapes.value?.map { it.copy(points = it.points.map { p -> p.copy() }) } ?: emptyList(),
                    textBoxes = _textBoxes.value?.map { it.copy() } ?: emptyList()
                )
            )
            _state.value = prev
            _shapes.value = prev.shapes
            _textBoxes.value = prev.textBoxes
            _selectedItems.value = SelectedItems() // Clear selections on undo
            Log.d("WhiteboardViewModel", "Undo: Restored ${prev.shapes.size} shapes, ${prev.shapes.count { it.type == ShapeType.POLYGON }} polygons")
        } else {
            Log.d("WhiteboardViewModel", "Undo: Stack empty")
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(
                WhiteboardState(
                    strokes = _state.value?.strokes?.map { it.copy(points = it.points.map { p -> p.copy() }) } ?: emptyList(),
                    shapes = _shapes.value?.map { it.copy(points = it.points.map { p -> p.copy() }) } ?: emptyList(),
                    textBoxes = _textBoxes.value?.map { it.copy() } ?: emptyList()
                )
            )
            _state.value = next
            _shapes.value = next.shapes
            _textBoxes.value = next.textBoxes
            _selectedItems.value = SelectedItems() // Clear selections on redo
            Log.d("WhiteboardViewModel", "Redo: Restored ${next.shapes.size} shapes, ${next.shapes.count { it.type == ShapeType.POLYGON }} polygons")
        } else {
            Log.d("WhiteboardViewModel", "Redo: Stack empty")
        }
    }

    /** ID GENERATION **/
    private fun generateStrokeId(): String {
        return UUID.randomUUID().toString()
    }

    private fun generateShapeId(): String {
        return UUID.randomUUID().toString()
    }

    private fun generateTextBoxId(): String {
        return UUID.randomUUID().toString()
    }

    /** SHAPE MANAGEMENT **/
    fun addShape(shape: Shape) {
        val shapeWithIdAndWidth = shape.copy(id = generateShapeId(), strokeWidth = currentShapeStrokeWidth)
        val list = _shapes.value?.toMutableList() ?: mutableListOf()
        list.add(shapeWithIdAndWidth)
        _shapes.value = list
        updateState()
        Log.d("WhiteboardViewModel", "Added shape: ${shapeWithIdAndWidth.type}, id: ${shapeWithIdAndWidth.id}, points: ${shapeWithIdAndWidth.points.size}")
    }

    fun removeShape(shape: Shape) {
        val currentShapes = _shapes.value?.toMutableList() ?: mutableListOf()
        currentShapes.removeAll { it.id == shape.id }
        _shapes.value = currentShapes
        Log.d("WhiteboardViewModel", "Removed shape: ${shape.type}, id: ${shape.id}")
        updateState()
    }

    fun removeShapeAt(index: Int) {
        val list = _shapes.value?.toMutableList() ?: return
        if (index in list.indices) {
            val shape = list[index]
            list.removeAt(index)
            _shapes.value = list
            updateState()
            Log.d("WhiteboardViewModel", "Removed shape at index $index: ${shape.type}, id: ${shape.id}")
        }
    }

    fun clearShapes() {
        _shapes.value = emptyList()
        updateState()
        Log.d("WhiteboardViewModel", "Cleared all shapes")
    }

    fun updateShape(updated: Shape) {
        val list = _shapes.value?.toMutableList() ?: mutableListOf()
        val index = list.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            list[index] = updated.copy(points = updated.points.map { it.copy() }) // Ensure deep copy
            _shapes.value = list
            updateState()
            Log.d("WhiteboardViewModel", "Updated shape: ${updated.type}, id: ${updated.id}, points: ${updated.points.size}")
        }
    }

    /** STROKE MANAGEMENT **/
    fun addStroke(points: List<Point>, isEraser: Boolean = false) {
        val stroke = Stroke(
            id = generateStrokeId(),
            points = points.map { it.copy() },
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
        _state.value = prev.copy(strokes = list)
        updateState()
        Log.d("WhiteboardViewModel", "Added stroke: ${stroke.id}, points: ${stroke.points.size}")
    }

    fun addStroke(points: List<Point>, isEraser: Boolean, color: String = getColor(), width: Float = getStrokeWidth(), id: String = generateStrokeId()) {
        val stroke = Stroke(id = id, points = points.map { it.copy() }, color = color, width = width, isEraser = isEraser)
        val prev = _state.value ?: WhiteboardState()
        val list = prev.strokes.toMutableList()
        list.add(stroke)
        _state.value = prev.copy(strokes = list)
        updateState()
        Log.d("WhiteboardViewModel", "Added stroke: ${stroke.id}, points: ${points.size}")
    }

    fun updateStroke(updated: Stroke) {
        val prev = _state.value ?: WhiteboardState()
        val strokes = prev.strokes.toMutableList()
        val index = strokes.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            strokes[index] = updated.copy(points = updated.points.map { it.copy() }) // Ensure deep copy
            _state.value = prev.copy(strokes = strokes)
            updateState()
            Log.d("WhiteboardViewModel", "Updated stroke: ${updated.id}, points: ${updated.points.size}")
        }
    }

    fun removeStrokes(strokesToRemove: List<Stroke>) {
        val currentState = _state.value ?: WhiteboardState()
        val updatedStrokes = currentState.strokes.filterNot { stroke ->
            strokesToRemove.any { it.id == stroke.id }
        }
        _state.value = currentState.copy(strokes = updatedStrokes)
        Log.d("WhiteboardViewModel", "Removed ${strokesToRemove.size} strokes, ${updatedStrokes.size} strokes remaining")
        updateState()
    }

    /** TEXTBOX MANAGEMENT **/
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
            id = generateTextBoxId(),
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
        updateState()
        Log.d("WhiteboardViewModel", "Added text box: ${tb.text}, id: ${tb.id}")
    }

    fun updateTextBox(textBox: TextBox) {
        val list = _textBoxes.value?.toMutableList() ?: mutableListOf()
        val index = list.indexOfFirst { it.id == textBox.id }
        if (index != -1) {
            list[index] = textBox.copy()
            _textBoxes.value = list
            updateState()
            Log.d("WhiteboardViewModel", "Updated text box: ${textBox.text}, id: ${textBox.id}")
        }
    }

    fun removeTextBox(textBox: TextBox) {
        val list = _textBoxes.value?.toMutableList() ?: mutableListOf()
        list.removeAll { it.id == textBox.id }
        _textBoxes.value = list
        updateState()
        Log.d("WhiteboardViewModel", "Removed text box: ${textBox.text}, id: ${textBox.id}")
    }

    /** ERASE MANAGEMENT **/
    fun eraseArea(rect: RectF) {
        val currentShapes = _shapes.value?.toMutableList() ?: mutableListOf()
        val currentTextBoxes = _textBoxes.value?.toMutableList() ?: mutableListOf()
        val currentStrokes = _state.value?.strokes?.toMutableList() ?: mutableListOf()

        // Remove shapes partially/fully inside selection rectangle
        _shapes.value = currentShapes.filterNot { shape ->
            val bounds = shape.bounds?.toRectF() ?: run {
                if (shape.points.isEmpty()) return@filterNot false
                val left = shape.points.minOf { it.x }
                val top = shape.points.minOf { it.y }
                val right = shape.points.maxOf { it.x }
                val bottom = shape.points.maxOf { it.y }
                RectF(left, top, right, bottom)
            }
            RectF.intersects(rect, bounds)
        }

        // Remove text boxes partially/fully inside selection
        _textBoxes.value = currentTextBoxes.filterNot { tb ->
            val bounds = tb.bounds.toRectF()
            RectF.intersects(rect, bounds)
        }

        // Remove strokes partially/fully inside
        _state.value = _state.value?.copy(strokes = currentStrokes.filterNot { stroke ->
            stroke.points.any { rect.contains(it.x, it.y) }
        }) ?: WhiteboardState(strokes = emptyList())

        updateState()
        Log.d("WhiteboardViewModel", "Erased: ${_shapes.value?.size} shapes remaining, ${_shapes.value?.count { it.type == ShapeType.POLYGON }} polygons")
    }

    /** SELECTION MANAGEMENT **/
    fun selectShape(shape: Shape) {
        val current = _selectedItems.value ?: SelectedItems()
        if (!current.shapes.contains(shape)) {
            current.shapes.add(shape)
            _selectedItems.value = current
            Log.d("WhiteboardViewModel", "Selected shape: ${shape.type}, id: ${shape.id}")
        }
    }

    fun selectStroke(stroke: Stroke) {
        val current = _selectedItems.value ?: SelectedItems()
        if (!current.strokes.contains(stroke)) {
            current.strokes.add(stroke)
            _selectedItems.value = current
            Log.d("WhiteboardViewModel", "Selected stroke: ${stroke.id}")
        }
    }

    fun selectTextBox(textBox: TextBox) {
        val current = _selectedItems.value ?: SelectedItems()
        if (!current.textBoxes.contains(textBox)) {
            current.textBoxes.add(textBox)
            _selectedItems.value = current
            Log.d("WhiteboardViewModel", "Selected text box: ${textBox.text}, id: ${textBox.id}")
        }
    }

    fun clearSelection() {
        _selectedItems.value = SelectedItems()
        Log.d("WhiteboardViewModel", "Cleared selection")
    }

    fun setSelectedItems(items: SelectedItems) {
        _selectedItems.value = items
        Log.d("WhiteboardViewModel", "Set selected items: ${items.shapes.size} shapes, ${items.textBoxes.size} text boxes, ${items.strokes.size} strokes")
    }

    /** MOVE MANAGEMENT **/
    fun moveSelectedItems(dx: Float, dy: Float) {
        val current = _selectedItems.value ?: return
        val updatedShapes = current.shapes.map { shape ->
            if (shape.type == ShapeType.POLYGON) {
                shape.copy(points = shape.points.map { point ->
                    point.copy(x = point.x + dx, y = point.y + dy)
                })
            } else {
                shape.bounds?.let { bounds ->
                    shape.copy(
                        bounds = RectFSerializable.from(
                            RectF(
                                bounds.left + dx,
                                bounds.top + dy,
                                bounds.right + dx,
                                bounds.bottom + dy
                            )
                        )
                    )
                } ?: shape
            }
        }
        current.shapes.clear()
        current.shapes.addAll(updatedShapes)
        updatedShapes.forEach { updateShape(it) }

        current.textBoxes.forEach { tb ->
            tb.bounds = RectFSerializable.from(
                RectF(
                    tb.bounds.left + dx,
                    tb.bounds.top + dy,
                    tb.bounds.right + dx,
                    tb.bounds.bottom + dy
                )
            )
            updateTextBox(tb)
        }
        current.strokes.forEach { stroke ->
            stroke.points = stroke.points.map { point ->
                point.copy(x = point.x + dx, y = point.y + dy)
            }
            updateStroke(stroke)
        }
        updateState()
        Log.d("WhiteboardViewModel", "Moved selected items: ${current.shapes.size} shapes, ${current.shapes.count { it.type == ShapeType.POLYGON }} polygons")
    }

    /** ROTATION MANAGEMENT **/
    fun rotateSelectedLeft(degrees: Float = 90f) {
        val current = _selectedItems.value ?: return
        val updatedShapes = current.shapes.map { shape ->
            if (shape.type == ShapeType.POLYGON) {
                val centerX = shape.points.map { it.x }.average().toFloat()
                val centerY = shape.points.map { it.y }.average().toFloat()
                val matrix = android.graphics.Matrix()
                matrix.postRotate(-degrees, centerX, centerY)
                val newPoints = shape.points.map { point ->
                    val pointArray = floatArrayOf(point.x, point.y)
                    matrix.mapPoints(pointArray)
                    Point(pointArray[0], pointArray[1])
                }
                shape.copy(points = newPoints, rotationAngle = 0f)
            } else {
                shape.copy(rotationAngle = (shape.rotationAngle - degrees) % 360)
            }
        }
        current.shapes.clear()
        current.shapes.addAll(updatedShapes)
        updatedShapes.forEach { updateShape(it) }

        current.textBoxes.forEach { tb ->
            tb.rotationAngle = (tb.rotationAngle - degrees) % 360
            updateTextBox(tb)
        }
        current.strokes.forEach { stroke ->
            if (stroke.points.isNotEmpty()) {
                val centerX = stroke.points.map { it.x }.average().toFloat()
                val centerY = stroke.points.map { it.y }.average().toFloat()
                val matrix = android.graphics.Matrix()
                matrix.postRotate(-degrees, centerX, centerY)
                stroke.points = stroke.points.map { point ->
                    val pointArray = floatArrayOf(point.x, point.y)
                    matrix.mapPoints(pointArray)
                    Point(pointArray[0], pointArray[1])
                }
                stroke.rotationAngle = 0f
            }
            updateStroke(stroke)
        }
        updateState()
        Log.d("WhiteboardViewModel", "Rotated left: ${current.shapes.size} shapes, ${current.shapes.count { it.type == ShapeType.POLYGON }} polygons")
    }

    fun rotateSelectedShapes(degrees: Float) {
        val current = _selectedItems.value ?: return
        val updatedShapes = current.shapes.map { shape ->
            if (shape.type == ShapeType.POLYGON) {
                val centerX = shape.points.map { it.x }.average().toFloat()
                val centerY = shape.points.map { it.y }.average().toFloat()
                val matrix = android.graphics.Matrix()
                matrix.postRotate(degrees, centerX, centerY)
                val newPoints = shape.points.map { point ->
                    val pointArray = floatArrayOf(point.x, point.y)
                    matrix.mapPoints(pointArray)
                    Point(pointArray[0], pointArray[1])
                }
                shape.copy(points = newPoints, rotationAngle = 0f)
            } else {
                shape.copy(rotationAngle = (shape.rotationAngle + degrees) % 360)
            }
        }
        current.shapes.clear()
        current.shapes.addAll(updatedShapes)
        updatedShapes.forEach { updateShape(it) }

        current.strokes.forEach { stroke ->
            if (stroke.points.isNotEmpty()) {
                val centerX = stroke.points.map { it.x }.average().toFloat()
                val centerY = stroke.points.map { it.y }.average().toFloat()
                val matrix = android.graphics.Matrix()
                matrix.postRotate(degrees, centerX, centerY)
                stroke.points = stroke.points.map { point ->
                    val pointArray = floatArrayOf(point.x, point.y)
                    matrix.mapPoints(pointArray)
                    Point(pointArray[0], pointArray[1])
                }
                stroke.rotationAngle = 0f
                updateStroke(stroke)
            }
        }
        current.textBoxes.forEach { tb ->
            tb.rotationAngle = (tb.rotationAngle + degrees) % 360
            updateTextBox(tb)
        }
        updateState()
        Log.d("WhiteboardViewModel", "Rotated shapes: ${current.shapes.size} shapes, ${current.shapes.count { it.type == ShapeType.POLYGON }} polygons")
    }

    fun rotateItem(item: Shape, degrees: Float) {
        val updatedShape = if (item.type == ShapeType.POLYGON) {
            val centerX = item.points.map { it.x }.average().toFloat()
            val centerY = item.points.map { it.y }.average().toFloat()
            val matrix = android.graphics.Matrix()
            matrix.postRotate(degrees, centerX, centerY)
            val newPoints = item.points.map { point ->
                val pointArray = floatArrayOf(point.x, point.y)
                matrix.mapPoints(pointArray)
                Point(pointArray[0], pointArray[1])
            }
            item.copy(points = newPoints, rotationAngle = 0f)
        } else {
            item.copy(rotationAngle = (item.rotationAngle + degrees) % 360)
        }
        updateShape(updatedShape)
        Log.d("WhiteboardViewModel", "Rotated item: ${updatedShape.type}, id: ${updatedShape.id}, points: ${updatedShape.points.size}")
    }

    /** FILE MANAGEMENT **/
    fun save(context: Context) {
        val svc = FileService(context)
        val current = WhiteboardState(
            strokes = _state.value?.strokes?.map { it.copy(points = it.points.map { p -> p.copy() }) } ?: emptyList(),
            shapes = _shapes.value?.map { it.copy(points = it.points.map { p -> p.copy() }) } ?: emptyList(),
            textBoxes = _textBoxes.value?.map { it.copy() } ?: emptyList()
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = svc.save(current)
                _savedFile.postValue(file.absolutePath)
                Log.d("WhiteboardViewModel", "Saved file: ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e("WhiteboardViewModel", "Save failed: ${e.message}", e)
                _savedFile.postValue(null)
            }
        }
    }

    fun load(context: Context, filePath: String? = null) {
        val svc = FileService(context)
        viewModelScope.launch(Dispatchers.IO) {
            val file = filePath?.let { File(it) } ?: svc.getLatestFile()
            if (file != null && file.exists()) {
                try {
                    val loaded = svc.load(file)
                    undoStack.clear()
                    redoStack.clear()
                    _state.postValue(loaded)
                    _shapes.postValue(loaded.shapes.map { it.copy(points = it.points.map { p -> p.copy() }) })
                    _textBoxes.postValue(loaded.textBoxes.map { it.copy() })
                    _selectedItems.postValue(SelectedItems())
                    _savedFile.postValue(file.absolutePath)
                    Log.d("WhiteboardViewModel", "Loaded file: ${file.absolutePath}, ${loaded.shapes.size} shapes, ${loaded.shapes.count { it.type == ShapeType.POLYGON }} polygons")
                } catch (e: Exception) {
                    Log.e("WhiteboardViewModel", "Failed to load file: ${file.absolutePath}", e)
                    _savedFile.postValue(null)
                }
            } else {
                _savedFile.postValue(null)
                Log.d("WhiteboardViewModel", "Load: No file found or invalid file path: $filePath")
            }
        }
    }
}



