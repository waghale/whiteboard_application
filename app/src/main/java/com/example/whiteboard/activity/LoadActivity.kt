package com.example.whiteboard.activity


import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.whiteboard.R
import com.example.whiteboard.services.FileService
import java.io.File

class LoadActivity : AppCompatActivity() {

    private lateinit var fileService: FileService
    private lateinit var listView: ListView
    private var files: List<File> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_load)

        fileService = FileService(this)
        listView = findViewById(R.id.listViewFiles)

        // Get files sorted by latest first
        files = fileService.getAllFilesSortedByLatest()

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            files.map { it.name }
        )
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedFile = files[position]

            // Open the WhiteboardActivity with selected JSON file
            val intent = Intent(this, WhiteboardActivity::class.java)
            intent.putExtra("file_path", selectedFile.absolutePath)
            startActivity(intent)
        }
    }
}
