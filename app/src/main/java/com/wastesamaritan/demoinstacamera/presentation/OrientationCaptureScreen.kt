package com.wastesamaritan.demoinstacamera.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaActionSound
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraInfo
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import com.wastesamaritan.demoinstacamera.component.FixedGradientSocialButton
import com.wastesamaritan.demoinstacamera.data.ImageRepository
import com.wastesamaritan.demoinstacamera.data.ImageEntity

data class CapturedOrientationData(
    val timestamp: Long,
    val yaw: Float?,
    val pitch: Float?,
    val roll: Float?,
    val rotationMatrix: String?
)

@Composable
fun rememberOrientationCapture(): State<CapturedOrientationData?> {
    val context = LocalContext.current
    val capturedData = remember { mutableStateOf<CapturedOrientationData?>(null) }
    
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val yaw = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                    val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                    
                    capturedData.value = CapturedOrientationData(
                        timestamp = System.currentTimeMillis(),
                        yaw = yaw,
                        pitch = pitch,
                        roll = roll,
                        rotationMatrix = rotationMatrix.joinToString()
                    )
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }
    
    return capturedData
}

private fun Float?.format(digits: Int) = this?.let { String.format("%.${digits}f", it) } ?: "--"

@Composable
fun OrientationCaptureScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val imageRepository = remember { ImageRepository(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var lastSnapPath by remember { mutableStateOf<String?>(null) }
    val shutterSound = remember { MediaActionSound() }
    val orientationData = rememberOrientationCapture()
    var cameraError by remember { mutableStateOf<String?>(null) }

    // Camera permission handling
    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasStoragePermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    val storagePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasStoragePermission = granted
    }
    
    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasCameraPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
        
        // Check storage permission for Android 10 and below
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
            hasStoragePermission = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasStoragePermission) {
                storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            hasStoragePermission = true // Android 11+ doesn't need this permission
        }
    }

    // Intercept back press
    BackHandler {
        navController.popBackStack()
    }

    Box(Modifier.fillMaxSize()) {
        if (!hasCameraPermission) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera permission is required to use the camera.", color = Color.White)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) }) {
                    Text("Grant Camera Permission")
                }
            }
        } else {
            // Fullscreen device camera preview
            if (cameraError != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(cameraError!!, color = Color.White)
                }
            } else {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val hasBackCamera = cameraProvider.hasCamera(androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA)
                            if (!hasBackCamera) {
                                cameraError = "No back camera available on this device."
                                return@addListener
                            }
                            cameraProvider.unbindAll()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val capture = ImageCapture.Builder().build()
                            imageCapture = capture
                            val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    capture
                                )
                            } catch (exc: Exception) {
                                Log.e("OrientationCapture", "Camera binding failed", exc)
                                cameraError = "Failed to bind camera: ${exc.message}"
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Last snap thumbnail (bottom left)
            lastSnapPath?.let { path ->
                Image(
                    painter = rememberAsyncImagePainter(path),
                    contentDescription = "Last Snap",
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                )
            }

            // Orientation display (top center)
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Yaw: ${orientationData.value?.yaw.format(2)}  Pitch: ${orientationData.value?.pitch.format(2)}  Roll: ${orientationData.value?.roll.format(2)}",
                    color = Color.White,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(8.dp))
            }

            // Back button (top left)
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 50.dp, start = 16.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            // Single Export button (bottom center)
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 50.dp)
            ) {
                FixedGradientSocialButton(
                    text = "Export Orientation",
                    icon = Icons.Filled.Download,
                    onClick = {
                        orientationData.value?.let { data ->
                            coroutineScope.launch {
                                val filePath = exportSingleOrientationToFile(context, data)
                                Toast.makeText(context, "Saved to: $filePath", Toast.LENGTH_LONG).show()
                            }
                        } ?: run {
                            Toast.makeText(context, "No orientation data available", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

private suspend fun exportSingleOrientationToFile(context: Context, orientation: CapturedOrientationData): String {
    try {
        // Try to save to public Downloads folder first
        val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val orientationFolder = File(downloadsFolder, "OrientationData")
        
        // Check if we can write to Downloads folder
        if (downloadsFolder.canWrite() && orientationFolder.exists() || orientationFolder.mkdirs()) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "orientation_$timestamp.txt"
            val file = File(orientationFolder, fileName)
            
            FileWriter(file).use { writer ->
                writer.appendLine("╔══════════════════════════════════════════════════════════════╗")
                writer.appendLine("║                    ORIENTATION DATA EXPORT                  ║")
                writer.appendLine("╚══════════════════════════════════════════════════════════════╝")
                writer.appendLine("")
                writer.appendLine("📅 Export Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}")
                writer.appendLine("⏰ Export Time: ${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())}")
                writer.appendLine("")
                writer.appendLine("┌──────────────────────────────────────────────────────────────┐")
                writer.appendLine("│                    ORIENTATION VALUES                       │")
                writer.appendLine("├──────────────────────────────────────────────────────────────┤")
                writer.appendLine("│  Yaw   │ ${orientation.yaw.format(2)}°")
                writer.appendLine("│  Pitch │ ${orientation.pitch.format(2)}°")
                writer.appendLine("│  Roll  │ ${orientation.roll.format(2)}°")
                writer.appendLine("└──────────────────────────────────────────────────────────────┘")
                writer.appendLine("")
                writer.appendLine("📊 DETAILED DATA:")
                writer.appendLine("┌──────────────────────────────────────────────────────────────┐")
                writer.appendLine("│  Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(orientation.timestamp))}")
                writer.appendLine("│  Yaw:      ${orientation.yaw.format(2)}°")
                writer.appendLine("│  Pitch:    ${orientation.pitch.format(2)}°")
                writer.appendLine("│  Roll:     ${orientation.roll.format(2)}°")
                writer.appendLine("└──────────────────────────────────────────────────────────────┘")
                writer.appendLine("")
                writer.appendLine("🔄 ROTATION MATRIX:")
                writer.appendLine("┌──────────────────────────────────────────────────────────────┐")
                writer.appendLine("│  ${orientation.rotationMatrix}")
                writer.appendLine("└──────────────────────────────────────────────────────────────┘")
                writer.appendLine("")
                writer.appendLine("📝 NOTES:")
                writer.appendLine("• Yaw:   Rotation around Z-axis (left/right)")
                writer.appendLine("• Pitch: Rotation around X-axis (up/down)")
                writer.appendLine("• Roll:  Rotation around Y-axis (tilt)")
                writer.appendLine("")
                writer.appendLine("╔══════════════════════════════════════════════════════════════╗")
                writer.appendLine("║                        END OF EXPORT                        ║")
                writer.appendLine("╚══════════════════════════════════════════════════════════════╝")
            }
            
            Log.d("OrientationCapture", "Exported orientation to ${file.absolutePath}")
            return "Downloads/OrientationData/$fileName"
        } else {
            // Fallback to app's private directory
            val privateFolder = File(context.getExternalFilesDir(null), "OrientationData")
            if (!privateFolder.exists()) {
                privateFolder.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "orientation_$timestamp.txt"
            val file = File(privateFolder, fileName)
            
            FileWriter(file).use { writer ->
                writer.appendLine("╔══════════════════════════════════════════════════════════════╗")
                writer.appendLine("║                    ORIENTATION DATA EXPORT                  ║")
                writer.appendLine("╚══════════════════════════════════════════════════════════════╝")
                writer.appendLine("")
                writer.appendLine("📅 Export Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}")
                writer.appendLine("⏰ Export Time: ${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())}")
                writer.appendLine("")
                writer.appendLine("┌──────────────────────────────────────────────────────────────┐")
                writer.appendLine("│                    ORIENTATION VALUES                       │")
                writer.appendLine("├──────────────────────────────────────────────────────────────┤")
                writer.appendLine("│  Yaw   │ ${orientation.yaw.format(2)}°")
                writer.appendLine("│  Pitch │ ${orientation.pitch.format(2)}°")
                writer.appendLine("│  Roll  │ ${orientation.roll.format(2)}°")
                writer.appendLine("└──────────────────────────────────────────────────────────────┘")
                writer.appendLine("")
                writer.appendLine("📊 DETAILED DATA:")
                writer.appendLine("┌──────────────────────────────────────────────────────────────┐")
                writer.appendLine("│  Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(orientation.timestamp))}")
                writer.appendLine("│  Yaw:      ${orientation.yaw.format(2)}°")
                writer.appendLine("│  Pitch:    ${orientation.pitch.format(2)}°")
                writer.appendLine("│  Roll:     ${orientation.roll.format(2)}°")
                writer.appendLine("└──────────────────────────────────────────────────────────────┘")
                writer.appendLine("")
                writer.appendLine("🔄 ROTATION MATRIX:")
                writer.appendLine("┌──────────────────────────────────────────────────────────────┐")
                writer.appendLine("│  ${orientation.rotationMatrix}")
                writer.appendLine("└──────────────────────────────────────────────────────────────┘")
                writer.appendLine("")
                writer.appendLine("📝 NOTES:")
                writer.appendLine("• Yaw:   Rotation around Z-axis (left/right)")
                writer.appendLine("• Pitch: Rotation around X-axis (up/down)")
                writer.appendLine("• Roll:  Rotation around Y-axis (tilt)")
                writer.appendLine("")
                writer.appendLine("╔══════════════════════════════════════════════════════════════╗")
                writer.appendLine("║                        END OF EXPORT                        ║")
                writer.appendLine("╚══════════════════════════════════════════════════════════════╝")
            }
            
            Log.d("OrientationCapture", "Exported orientation to private directory: ${file.absolutePath}")
            return "App Files/OrientationData/$fileName"
        }
    } catch (e: Exception) {
        Log.e("OrientationCapture", "Failed to export orientation", e)
        return "Export failed: ${e.message}"
    }
} 