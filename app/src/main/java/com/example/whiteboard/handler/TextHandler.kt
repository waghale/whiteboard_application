package com.example.whiteboard.handler



import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog

import com.example.whiteboard.models.TextBox
import com.example.whiteboard.viewmodels.WhiteboardViewModel
import com.example.whiteboard.views.DrawingView
import kotlin.math.max
import kotlin.math.min






import android.graphics.Typeface


class TextHandler(
    private val view: DrawingView,
    private val viewModel: WhiteboardViewModel?
) {
    private var textStart: PointF? = null
    private var textEnd: PointF? = null
    private var selectedTextBox: TextBox? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var activeEditText: EditText? = null

    fun startTextInteraction(x: Float, y: Float) {
        val vm = viewModel ?: return
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

    fun updateTextInteraction(x: Float, y: Float, vm: WhiteboardViewModel) {
        selectedTextBox?.let { moveTextBox(it, x, y, vm) }
    }

    fun commitText(vm: WhiteboardViewModel, x: Float, y: Float) {
        selectedTextBox?.let { vm.updateTextBox(it) }
        selectedTextBox = null
        if (textStart != null && textEnd != null) {
            showInsertTextDialog(x, y)
            textStart = null
            textEnd = null
        }
    }

    internal fun moveTextBox(tb: TextBox, x: Float, y: Float, vm: WhiteboardViewModel) {
        val rectF = tb.bounds.toRectF()
        val newLeft = x - dragOffsetX
        val newTop = y - dragOffsetY
        val newRight = newLeft + rectF.width()
        val newBottom = newTop + rectF.height()
        vm.updateTextBox(tb.copy(bounds = com.example.whiteboard.models.RectFSerializable.from(RectF(newLeft, newTop, newRight, newBottom))))
    }

    private fun showInsertTextDialog(x: Float, y: Float) {
        val vm = viewModel ?: return
        val builder = AlertDialog.Builder(view.context)
        builder.setTitle("Insert Text")
        val layout = LinearLayout(view.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }
        val editText = EditText(view.context).apply {
            hint = "Enter text"
            maxLines = 3
            setLines(2)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        layout.addView(editText)
        val sizeSeek = SeekBar(view.context).apply {
            max = 100
            progress = vm.getTextSize().toInt()
        }
        layout.addView(sizeSeek)
        val styleGroup = RadioGroup(view.context).apply {
            orientation = RadioGroup.HORIZONTAL
        }
        listOf("Normal", "Bold", "Italic").forEachIndexed { idx, label ->
            val rb = RadioButton(view.context).apply { text = label; id = idx }
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
        val alignGroup = RadioGroup(view.context).apply {
            orientation = RadioGroup.HORIZONTAL
        }
        listOf("Left", "Center", "Right").forEachIndexed { idx, label ->
            val rb = RadioButton(view.context).apply { text = label; id = idx }
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
        val fontGroup = Spinner(view.context).apply {
            adapter = ArrayAdapter(
                view.context, android.R.layout.simple_spinner_dropdown_item,
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
        val colors = listOf(
            "Black" to "#FF000000",
            "Red" to "#FFFF0000",
            "Blue" to "#FF0000FF",
            "Green" to "#FF00FF00",
            "Yellow" to "#FFFFFF00",
            "Purple" to "#FF800080"
        )
        val colorGroup = Spinner(view.context).apply {
            adapter = ArrayAdapter(
                view.context, android.R.layout.simple_spinner_dropdown_item, colors.map { it.first }
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
            vm.setTextSize(size)
            vm.setTextStyle(typeface)
            vm.setFontFamily(font)
            vm.setTextColor(color)
            vm.setTextAlignment(alignment)
            val rect = RectF(x, y, x + 300, y + 100)
            vm.addTextBox(
                bounds = rect,
                text = text,
                color = color,
                textSize = size,
                typeface = typeface,
                fontFamily = font,
                alignment = alignment
            )
            view.invalidate()
        }
        builder.setNegativeButton("Cancel", null)
        val dialog = builder.show()
        editText.requestFocus()
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    fun handleTextArea(rect: RectF, vm: WhiteboardViewModel) {
        activeEditText?.let { editText ->
            safeRemoveView(editText)
        }
        val existingTextBox = findTextBoxAt(rect.left, rect.top)
        val editText = EditText(view.context).apply {
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
            isSingleLine = false
        }
        (view.parent as? ViewGroup)?.addView(editText)
        activeEditText = editText
        editText.requestFocus()
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        editText.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                saveTextBox(editText, rect, existingTextBox, vm)
            }
        }
        editText.setOnEditorActionListener { v, _, _ ->
            saveTextBox(editText, rect, existingTextBox, vm)
            true
        }
    }

    private fun saveTextBox(
        editText: EditText,
        rect: RectF,
        existingTextBox: TextBox?,
        vm: WhiteboardViewModel
    ) {
        val text = editText.text.toString().trim()
        safeRemoveView(editText)
        activeEditText = null
        if (text.isEmpty()) {
            view.invalidate()
            return
        }
        val left = min(rect.left, rect.right)
        val top = min(rect.top, rect.bottom)
        val right = max(rect.left, rect.right)
        val bottom = max(rect.top, rect.bottom)
        val safeRect = RectF(left, top, right, bottom)
        if (existingTextBox != null) {
            val updated = existingTextBox.copy(
                text = text,
                bounds = com.example.whiteboard.models.RectFSerializable.from(safeRect)
            )
            vm.updateTextBox(updated)
        } else {
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
        view.invalidate()
    }

    internal fun findTextBoxAt(x: Float, y: Float): TextBox? {
        return viewModel?.textBoxes?.value?.firstOrNull { tb ->
            val rect = tb.bounds.toRectF()
            rect.contains(x, y)
        }
    }

    internal fun getSelectedTextBox(): TextBox? = selectedTextBox

    internal fun setSelectedTextBox(textBox: TextBox?) {
        selectedTextBox = textBox
        view.invalidate()
    }

    private fun safeRemoveView(view: View?) {
        if (view == null) return
        val parent = view.parent
        if (parent is ViewGroup) {
            try {
                parent.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}