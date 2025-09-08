package com.example.whiteboard.ui


/**
 * WhiteboardFragment
 * ------------------
 * Main UI fragment which hosts the DrawingView (canvas area)
 * and a ToolbarFragment (tools like pen, eraser, etc.).
 * Uses WhiteboardViewModel to manage and observe state.
 */

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.whiteboard.R
import com.example.whiteboard.viewmodels.WhiteboardViewModel
import com.example.whiteboard.views.DrawingView

class WhiteboardFragment : Fragment() {

    private val vm: WhiteboardViewModel by viewModels({ requireActivity() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_whiteboard, container, false)

        val drawingView = root.findViewById<DrawingView>(R.id.drawingView)
        drawingView.viewModel = vm

        // Setup toolbar fragment in container
        childFragmentManager.beginTransaction()
            .replace(R.id.toolbar_container, ToolbarFragment())
            .commit()

        // ✅ observe state changes → refresh DrawingView on undo/redo/load
        vm.state.observe(viewLifecycleOwner) {
            drawingView.invalidate()
        }

        // observe save/load results
        vm.savedFile.observe(viewLifecycleOwner) { file ->
            if (file != null) {
                Toast.makeText(requireContext(), "Saved/Loaded: $file", Toast.LENGTH_SHORT).show()
            } else {
                //Toast.makeText(requireContext(), "No file found!", Toast.LENGTH_SHORT).show()
            }
        }

        return root
    }
}
