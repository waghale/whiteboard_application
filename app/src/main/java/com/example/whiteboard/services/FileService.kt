package com.example.whiteboard.services



import android.content.Context
import com.example.whiteboard.models.WhiteboardState
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.util.Log

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException






class FileService(private val context: Context) {
    private val folder = File(context.filesDir, "whiteboard_saves").apply { mkdirs() }
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun save(state: WhiteboardState): File {
        val jsonStr = serializeToJson(state)
        val file = File(folder, "whiteboard_${System.currentTimeMillis()}.json")
        try {
            file.writeText(jsonStr)
            Log.d("FileService", "Saved file: ${file.absolutePath}, Size: ${file.length()} bytes")
            return file
        } catch (e: IOException) {
            Log.e("FileService", "Failed to write file: ${file.absolutePath}, Error: ${e.message}", e)
            throw e
        }
    }

    fun load(file: File): WhiteboardState {
        if (!file.exists()) {
            Log.e("FileService", "File does not exist: ${file.absolutePath}")
            throw IOException("File does not exist: ${file.absolutePath}")
        }
        if (!file.canRead()) {
            Log.e("FileService", "File is not readable: ${file.absolutePath}")
            throw IOException("File is not readable: ${file.absolutePath}")
        }
        val json = try {
            file.readText()
        } catch (e: IOException) {
            Log.e("FileService", "Failed to read file: ${file.absolutePath}, Error: ${e.message}", e)
            throw IOException("Failed to read file: ${file.absolutePath}", e)
        }
        if (json.isBlank()) {
            Log.e("FileService", "File is empty: ${file.absolutePath}")
            throw IllegalStateException("File is empty: ${file.absolutePath}")
        }
        try {
            Log.d("FileService", "Loading JSON from ${file.absolutePath}: $json")
            return deserializeFromJson(json)
        } catch (e: JsonParseException) {
            Log.e("FileService", "Failed to deserialize JSON from file: ${file.absolutePath}, Error: ${e.message}", e)
            throw IllegalStateException("Failed to deserialize JSON: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("FileService", "Unexpected error loading file: ${file.absolutePath}, Error: ${e.message}", e)
            throw e
        }
    }

    private fun serializeToJson(state: WhiteboardState): String {
        return gson.toJson(state)
    }

    private fun deserializeFromJson(json: String): WhiteboardState {
        return gson.fromJson(json, WhiteboardState::class.java)
    }

    fun getLatestFile(): File? {
        return getAllFilesSortedByLatest().firstOrNull()
    }

    fun getAllFilesSortedByLatest(): List<File> {
        val files = folder.listFiles()
            ?.filter { it.extension == "json" && it.exists() && it.canRead() }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
        Log.d("FileService", "Found ${files.size} files in ${folder.absolutePath}")
        return files
    }
}



