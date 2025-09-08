package com.example.whiteboard.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.whiteboard.databinding.ActivityMainBinding

/**
 * MainActivity
 * ----------------
 * Entry point of the Whiteboard application.
 * - Hosts the WhiteboardFragment (drawing canvas + toolbar).
 * - Uses ViewBinding (ActivityMainBinding) for UI inflation.
 */
class MainActivity : AppCompatActivity() {

    // ViewBinding reference for accessing views
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout using ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)

        // Set the activity content to the binding's root view
        setContentView(binding.root)

        // Currently simple → just loads fragment from XML layout
        // Future: could add navigation, toolbar, etc.
    }
}