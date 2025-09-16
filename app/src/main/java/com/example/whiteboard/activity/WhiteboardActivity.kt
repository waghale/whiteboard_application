package com.example.whiteboard.activity


import android.os.Bundle

import android.view.View
import androidx.appcompat.app.AppCompatActivity



import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels

import com.example.whiteboard.R
import com.example.whiteboard.ui.ToolbarFragment
import com.example.whiteboard.viewmodels.WhiteboardViewModel
import com.example.whiteboard.views.DrawingView


import android.content.pm.PackageManager


class WhiteboardActivity : AppCompatActivity() {
    private val viewModel: WhiteboardViewModel by viewModels()
    private var isLoading = false
    private var isSaving = false

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isSaving", isSaving)
        outState.putBoolean("isLoading", isLoading)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whiteboard)

        // Restore state
        isSaving = savedInstanceState?.getBoolean("isSaving") ?: false
        isLoading = savedInstanceState?.getBoolean("isLoading") ?: false

        // Initialize DrawingView and set ViewModel
        val drawingView = findViewById<DrawingView>(R.id.drawingView)
        drawingView.viewModel = viewModel

        // Initialize ProgressBar
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Initialize ToolbarFragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.toolbarFragmentContainer, ToolbarFragment())
                .commit()
        }

        // Handle file loading from intent
        if (savedInstanceState == null) {
            intent.getStringExtra("file_path")?.let { filePath ->
                if (!isLoading && !isSaving) {
                    isLoading = true
                    Log.d("WhiteboardActivity", "Attempting to load file: $filePath")
                    progressBar?.visibility = View.VISIBLE
                    viewModel.load(this, filePath)
                }
            }
        }

        // Observe ViewModel state changes
        viewModel.state.observe(this) { state ->
            Log.d("WhiteboardActivity", "State updated: ${state.shapes.size} shapes, ${state.shapes.count { it.type == WhiteboardViewModel.ShapeType.POLYGON }} polygons")
            drawingView.invalidate()
            if (isLoading || isSaving) {
                progressBar?.visibility = View.GONE
            }
        }
        viewModel.shapes.observe(this) { shapes ->
            Log.d("WhiteboardActivity", "Shapes updated: ${shapes.size}")
            drawingView.invalidate()
        }
        viewModel.textBoxes.observe(this) { textBoxes ->
            Log.d("WhiteboardActivity", "TextBoxes updated: ${textBoxes.size}")
            drawingView.invalidate()
        }
        viewModel.savedFile.observe(this) { filePath ->
            Log.d("WhiteboardActivity", "savedFile triggered, isSaving=$isSaving, isLoading=$isLoading, filePath=$filePath")
            if (isLoading) {
                isLoading = false
                progressBar?.visibility = View.GONE
                filePath?.let {
                    Log.d("WhiteboardActivity", "Loaded file path: $it")
                    Toast.makeText(this, "Whiteboard loaded successfully", Toast.LENGTH_SHORT).show()
                } ?: run {
                    if (intent.getStringExtra("file_path") != null) {
                        Log.e("WhiteboardActivity", "Failed to load file: ${intent.getStringExtra("file_path")}")
                        Toast.makeText(this, "Failed to load whiteboard file. Check file or permissions.", Toast.LENGTH_LONG).show()
                    }
                }
            } else if (isSaving) {
                isSaving = false
                progressBar?.visibility = View.GONE
                filePath?.let {
                    Log.d("WhiteboardActivity", "Saved file path: $it")
                    Toast.makeText(this, "Whiteboard saved successfully", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Log.e("WhiteboardActivity", "Failed to save file")
                    Toast.makeText(this, "Failed to save whiteboard file", Toast.LENGTH_LONG).show()
                }
            } else {
                Log.w("WhiteboardActivity", "savedFile triggered but neither loading nor saving")
            }
        }
    }

    // Public method to start saving
    fun startSaving() {
        isSaving = true
        findViewById<ProgressBar>(R.id.progressBar)?.visibility = View.VISIBLE
        Log.d("WhiteboardActivity", "Started saving operation")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("WhiteboardActivity", "Permission granted, initiating save")
            startSaving()
            viewModel.save(this)
        } else {
            Log.e("WhiteboardActivity", "Storage permission denied")
            Toast.makeText(this, "Storage permission denied", Toast.LENGTH_LONG).show()
        }
    }
}




