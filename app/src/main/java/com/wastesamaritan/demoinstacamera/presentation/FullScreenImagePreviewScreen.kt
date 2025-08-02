package com.wastesamaritan.demoinstacamera.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.wastesamaritan.demoinstacamera.data.ImageRepository
import kotlinx.coroutines.launch
import android.os.Environment
import androidx.compose.material.icons.filled.Send
import com.wastesamaritan.demoinstacamera.data.ImageEntity
import java.io.File
import java.io.FileWriter
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenImagePreviewScreen(
    imageId: Int,
    navController: NavController,
    imageRepository: ImageRepository
) {
    val image by imageRepository.getImageById(imageId).collectAsState(initial = null)
    var showInfo by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var exportResult by remember { mutableStateOf<String?>(null) }
    if (image == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showInfo = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "Info", tint = Color.White)
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            imageRepository.deleteImage(image!!)
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            val result = exportImageAndTextSeparately(image!!)
                            exportResult = result
                        }
                    }) {
                        Icon(Icons.Filled.Send, contentDescription = "Export Files", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.5f))
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Image(
                painter = rememberAsyncImagePainter(image!!.filePath),
                contentDescription = "Snap",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            if (showInfo) {
                AlertDialog(
                    onDismissRequest = { showInfo = false },
                    title = { 
                        Text(
                            "Image Details",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "📅 Captured: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(image!!.timestamp))}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            
                           Spacer(modifier = Modifier.height(12.dp))
                            
                            if (!image!!.comment.isNullOrBlank()) {
                                Text(
                                    text = "Comment:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Text(
                                    text = image!!.comment ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            
                            // Orientation data
                            Text(
                                text = "Orientation Data:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Text("   Yaw: ${image!!.yaw.format(2)}°")
                            Text("   Pitch: ${image!!.pitch.format(2)}°")
                            Text("   Roll: ${image!!.roll.format(2)}°")
                            
                            if (image!!.horizontalFov != null) {
                                Text("   Horizontal FOV: ${image!!.horizontalFov.format(2)}°")
                            }
                            
                            if (!image!!.rotationMatrix.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "🔄 Rotation Matrix:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Text(
                                    text = image!!.rotationMatrix ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showInfo = false }) { 
                            Text("Close")
                        }
                    }
                )
            }
            exportResult?.let { msg ->
                Snackbar(
                    action = {
                        TextButton(onClick = { exportResult = null }) { Text("OK", color = Color.White) }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) { Text(msg, color = Color.White) }
            }
        }
    }
}

private fun Float?.format(digits: Int) = this?.let { String.format("%.${digits}f", it) } ?: "--"

fun exportImageAndTextSeparately(image: ImageEntity): String {
    return try {
        val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val exportFolder = File(downloadsFolder, "ImageExports")
        if (!exportFolder.exists()) {
            exportFolder.mkdirs()
        }

        val imageFolder = File(exportFolder, "image_${image.timestamp}")
        if (!imageFolder.exists()) {
            imageFolder.mkdirs()
        }

        // Copy the image to the folder
        val sourceFile = File(image.filePath)
        val destFile = File(imageFolder, "image.jpg")
        sourceFile.copyTo(destFile, overwrite = true)

        // Create metadata.txt in the same folder
        val metadataFile = File(imageFolder, "metadata.txt")
        FileWriter(metadataFile).use { writer ->
            writer.appendLine("╔══════════════════════════════════════════════════════════════╗")
            writer.appendLine("║                    IMAGE EXPORT METADATA                     ║")
            writer.appendLine("╚══════════════════════════════════════════════════════════════╝")
            writer.appendLine()
            writer.appendLine("📅 Export Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}")
            writer.appendLine("⏰ Export Time: ${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())}")
            writer.appendLine()
            writer.appendLine("┌──────────────────────────────────────────────────────────────┐")
            writer.appendLine("│  📅 Captured: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(image.timestamp))}")
            writer.appendLine("│  📄 File: image.jpg")
            writer.appendLine("│  🔭 Horizontal FOV: ${image.horizontalFov.format(2)}")
            writer.appendLine()
            writer.appendLine("│  🧮 Rotation Matrix:")
            writer.appendLine("│  ${image.rotationMatrix ?: "Not available"}")
            writer.appendLine()

            if (image.yaw != null || image.pitch != null || image.roll != null) {
                writer.appendLine("│  📊 Orientation:")
                writer.appendLine("│     Yaw: ${image.yaw?.format(2) ?: "N/A"}°")
                writer.appendLine("│     Pitch: ${image.pitch?.format(2) ?: "N/A"}°")
                writer.appendLine("│     Roll: ${image.roll?.format(2) ?: "N/A"}°")
            }

            writer.appendLine("│  💬 Comment: ${image.comment ?: "No comment added"}")
            writer.appendLine("└──────────────────────────────────────────────────────────────┘")
        }

        android.util.Log.d("DatabaseGallery", "Exported image and metadata to ${imageFolder.absolutePath}")
        "Export successful: ${imageFolder.absolutePath}"
    } catch (e: Exception) {
        android.util.Log.e("DatabaseGallery", "Failed to export image", e)
        "Export failed: ${e.localizedMessage}"
    }
}

