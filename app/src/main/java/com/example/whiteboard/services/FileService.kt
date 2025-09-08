package com.example.whiteboard.services



import android.content.Context
import com.example.whiteboard.models.WhiteboardState
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

//class FileService(private val context: Context) {
//
//    private val json = Json { prettyPrint = true }
//
//    fun save(state: WhiteboardState): File {
//        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
//        val fileName = "whiteboard_${ts}.json"
//        val file = File(context.filesDir, fileName)
//        file.writeText(json.encodeToString(state))
//        return file
//    }
//
//    fun load(file: File): WhiteboardState {
//        val txt = file.readText()
//        return json.decodeFromString(txt)
//    }
//
//    fun listSavedFiles(): List<File> {
//        val dir = context.filesDir
//        return dir.listFiles { f -> f.name.startsWith("whiteboard_") }?.sortedByDescending { it.lastModified() } ?: emptyList()
//    }
//
//    fun getLatestFile(): File? {
//        return listSavedFiles().firstOrNull()
//    }
//}







class FileService(private val context: Context) {

    private val folder = File(context.filesDir, "whiteboard_saves").apply { mkdirs() }

    // Save WhiteboardState to JSON file
    fun save(state: WhiteboardState): File {
        val jsonStr = serializeToJson(state)
        val file = File(folder, "whiteboard_${System.currentTimeMillis()}.json")
        file.writeText(jsonStr)
        return file
    }

    // Load WhiteboardState from JSON file
    fun load(file: File): WhiteboardState {
        val json = file.readText()
        return deserializeFromJson(json)
    }

    // Serialize using kotlinx.serialization directly
    private fun serializeToJson(state: WhiteboardState): String {
        return Json { prettyPrint = true }.encodeToString(state)
    }

    // Deserialize JSON into WhiteboardState
    private fun deserializeFromJson(json: String): WhiteboardState {
        return Json.decodeFromString(json)
    }

    // Get the latest saved file
    fun getLatestFile(): File? {
        return folder.listFiles()?.maxByOrNull { it.lastModified() }
    }

//    fun getAllFilesSortedByLatest(): List<File> {
//        return folder.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
//    }

    fun getAllFilesSortedByLatest(): List<File> {
        return folder.listFiles()
            ?.filter { it.extension == "json" }   // फक्त JSON फाईल्स
            ?.sortedByDescending { it.lastModified() } // नवीन आधी
            ?: emptyList()
    }


}


