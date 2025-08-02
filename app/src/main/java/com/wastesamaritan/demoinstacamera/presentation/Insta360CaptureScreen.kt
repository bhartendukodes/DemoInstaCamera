package com.wastesamaritan.demoinstacamera.presentation

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arashivision.sdkcamera.camera.InstaCameraManager
import com.arashivision.sdkcamera.camera.model.CaptureMode
import com.arashivision.insta360.basecamera.camera.ICameraController
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavController
import com.wastesamaritan.demoinstacamera.component.FixedGradientSocialButton
import androidx.compose.material.icons.filled.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Insta360CaptureScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    if (showGallery) {
        GalleryScreen(onBack = { showGallery = false })
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("Insta360 Capture", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Spacer(Modifier.height(16.dp))
        // Main Buttons
        FixedGradientSocialButton(
            onClick = {
                val isSdCardEnabled = InstaCameraManager.getInstance().isSdCardEnabled
                val freeSpace = InstaCameraManager.getInstance().cameraStorageFreeSpace
                if (!isSdCardEnabled) {
                    Toast.makeText(context, "SD card not available!", Toast.LENGTH_SHORT).show()
                    return@FixedGradientSocialButton
                }
                if (freeSpace < 100 * 1024 * 1024) {
                    Toast.makeText(context, "Insufficient space (${freeSpace / 1024 / 1024}MB free)", Toast.LENGTH_SHORT).show()
                    return@FixedGradientSocialButton
                }
                try {
                    InstaCameraManager.getInstance().setCaptureMode(CaptureMode.CAPTURE_NORMAL) { resultCode ->
                        if (resultCode == 0) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                InstaCameraManager.getInstance().startNormalCapture()
                            }, 500)
                            Toast.makeText(context, "Taking photo...", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to set photo mode: $resultCode", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to take photo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            icon = Icons.Filled.CameraAlt,
            text = "Start Capturing through Insta360",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        FixedGradientSocialButton(
            onClick = {
                navController.navigate("capture_orientation")
            },
            icon = Icons.Filled.Settings,
            text = "Capture Orientation",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        FixedGradientSocialButton(
            onClick = {navController.navigate("db_gallery")},
            icon = Icons.Filled.PhotoLibrary,
            text = "Gallery",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        FixedGradientSocialButton(
            onClick = {
                if (!isRecording) {
                    val isSdCardEnabled = InstaCameraManager.getInstance().isSdCardEnabled
                    val freeSpace = InstaCameraManager.getInstance().cameraStorageFreeSpace
                    if (!isSdCardEnabled) {
                        Toast.makeText(context, "SD card not available!", Toast.LENGTH_SHORT).show()
                        return@FixedGradientSocialButton
                    }
                    if (freeSpace < 100 * 1024 * 1024) {
                        Toast.makeText(context, "Insufficient space (${freeSpace / 1024 / 1024}MB free)", Toast.LENGTH_SHORT).show()
                        return@FixedGradientSocialButton
                    }
                    try {
                        InstaCameraManager.getInstance().setCaptureMode(CaptureMode.RECORD_NORMAL, object : ICameraController.ISetOptionsCallback {
                            override fun onSetOptionsResult(resultCode: Int) {
                                if (resultCode == 0) {
                                    InstaCameraManager.getInstance().startNormalRecord()
                                    isRecording = true
                                    Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show()
                                    navController.navigate("full_camera")
                                } else {
                                    Toast.makeText(context, "Failed to set video mode: $resultCode", Toast.LENGTH_SHORT).show()
                                }
                            }
                        })
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to start recording: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    try {
                        InstaCameraManager.getInstance().stopNormalRecord()
                        isRecording = false
                        Toast.makeText(context, "Recording stopped", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to stop recording: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            icon = if (isRecording) Icons.Filled.Stop else Icons.Filled.Videocam,
            text = if (isRecording) "Stop Recording with Insta360" else "Start Recording with Insta360",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}