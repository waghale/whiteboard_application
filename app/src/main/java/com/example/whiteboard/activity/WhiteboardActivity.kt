package com.example.whiteboard.activity

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.whiteboard.models.*
import com.example.whiteboard.services.FileService
import java.io.File

class WhiteboardActivity : AppCompatActivity() {

    private lateinit var fileService: FileService
    private lateinit var whiteboardView: WhiteboardView
    private var whiteboardState: WhiteboardState = WhiteboardState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fileService = FileService(this)

        // Load file if passed
        intent.getStringExtra("file_path")?.let { path ->
            val file = File(path)
            whiteboardState = fileService.load(file)
        }

        // Initialize custom canvas view
        whiteboardView = WhiteboardView()
        setContentView(whiteboardView)
    }

    inner class WhiteboardView : View(this@WhiteboardActivity) {

        private val paint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        private val currentStroke = mutableListOf<PointF>()

        init {
            // Force background always white
            setBackgroundColor(Color.WHITE)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            // Fill canvas with white (double safety, works even if background cleared)
            canvas.drawColor(Color.WHITE)

            // Draw saved strokes
            whiteboardState.strokes.forEach { stroke ->
                val p = Paint().apply {
                    color = Color.parseColor(stroke.color)
                    strokeWidth = stroke.width.toFloat()
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                if (stroke.points.size > 1) {
                    for (i in 0 until stroke.points.size - 1) {
                        val pt1 = stroke.points[i]
                        val pt2 = stroke.points[i + 1]
                        canvas.drawLine(pt1.x, pt1.y, pt2.x, pt2.y, p)
                    }
                }
            }

            // Draw shapes
            whiteboardState.shapes.forEach { shape ->
                val p = Paint().apply {
                    color = Color.parseColor(shape.color)
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                    isAntiAlias = true
                }
                shape.bounds?.let { b ->
                    val rect = RectF(b.left, b.top, b.right, b.bottom)
                    canvas.drawRect(rect, p)
                }
            }

            // Draw text boxes
            whiteboardState.textBoxes.forEach { tb ->
                val p = Paint().apply {
                    color = Color.parseColor(tb.color)
                    textSize = tb.textSize.toFloat()
                    isAntiAlias = true
                }
                canvas.drawText(tb.text, tb.bounds.left, tb.bounds.top + tb.textSize, p)
            }

            // Draw current stroke
            if (currentStroke.size > 1) {
                for (i in 0 until currentStroke.size - 1) {
                    val pt1 = currentStroke[i]
                    val pt2 = currentStroke[i + 1]
                    canvas.drawLine(pt1.x, pt1.y, pt2.x, pt2.y, paint)
                }
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val point = PointF(event.x, event.y)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> currentStroke.clear()
                MotionEvent.ACTION_MOVE -> currentStroke.add(point)
                MotionEvent.ACTION_UP -> {
                    currentStroke.add(point)

                    // Convert currentStroke to model Point list
                    val strokePoints = currentStroke.map { pt ->
                        com.example.whiteboard.models.Point(pt.x, pt.y)
                    }

                    whiteboardState = whiteboardState.copy(
                        strokes = whiteboardState.strokes + Stroke(
                            points = strokePoints,
                            color = String.format("#%06X", 0xFFFFFF and paint.color), // hex string
                            width = paint.strokeWidth
                        )
                    )

                    currentStroke.clear()
                }
            }
            invalidate()
            return true
        }
    }


    // Call this to save whiteboard state
    private fun saveWhiteboard() {
        fileService.save(whiteboardState)
    }

    // Optional: open file picker activity to load JSON
    private fun openFile(filePath: String) {
        val intent = Intent(this, WhiteboardActivity::class.java)
        intent.putExtra("file_path", filePath)
        startActivity(intent)
        finish()
    }
}
