package com.wastesamaritan.demoinstacamera.presentation

import android.os.Environment
import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.wastesamaritan.demoinstacamera.component.FixedGradientSocialButton
import com.wastesamaritan.demoinstacamera.component.ObjectDetectionOverlay
import com.wastesamaritan.demoinstacamera.component.CommentDialog
import android.media.MediaActionSound
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material3.Button
import androidx.core.content.ContextCompat
import androidx.camera.camera2.interop.Camera2CameraInfo
import android.hardware.camera2.CameraCharacteristics
import androidx.annotation.OptIn
import kotlin.math.atan
import androidx.camera.core.CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop

data class OrientationData(
    val yaw: Float? = null,
    val pitch: Float? = null,
    val roll: Float? = null,
    val rotationMatrix: String? = null
)

@Composable
fun rememberOrientation(): OrientationData {
    val context = LocalContext.current
    val orientationData = remember { mutableStateOf(OrientationData()) }
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
                    orientationData.value = OrientationData(
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
    return orientationData.value
}

private fun Float?.format(digits: Int) = this?.let { String.format("%.${digits}f", it) } ?: "--"

@OptIn(ExperimentalCamera2Interop::class)
fun calculateFov(cameraInfo: CameraInfo): Float? {
    val camera2CameraInfo = Camera2CameraInfo.from(cameraInfo)
    val sensorSize = camera2CameraInfo.getCameraCharacteristic(
        CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
    )
    val focalLength = camera2CameraInfo.getCameraCharacteristic(
        CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
    )?.firstOrNull()
    return if (sensorSize != null && focalLength != null) {
        (2 * Math.toDegrees(
            atan((sensorSize.width / (2 * focalLength)).toDouble())
        )).toFloat()
    } else null
}

@Composable
fun Insta360FullCameraScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val imageRepository = remember { com.wastesamaritan.demoinstacamera.data.ImageRepository(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var lastSnapPath by remember { mutableStateOf<String?>(null) }
    var showGallery by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(true) } // Set to true when recording starts, false when stopped
    var showStopDialog by remember { mutableStateOf(false) }
    val shutterSound = remember { MediaActionSound() }
    val orientation = rememberOrientation()
    var cameraError by remember { mutableStateOf<String?>(null) }
    var horizontalFov by remember { mutableStateOf<Float?>(null) }
    
    // Object detection states
    var isObjectDetected by remember { mutableStateOf(false) }
    var showCommentDialog by remember { mutableStateOf(false) }
    var capturedImagePath by remember { mutableStateOf<String?>(null) }
    var capturedOrientation by remember { mutableStateOf<OrientationData?>(null) }

    // Camera permission handling
    var hasCameraPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasCameraPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // Intercept back press while recording
    BackHandler(enabled = isRecording) {
        showStopDialog = true
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Stop Recording") },
            text = { Text("Please stop recording before leaving the camera screen.") },
            confirmButton = {
                Button(onClick = { showStopDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Comment dialog
    if (showCommentDialog && capturedImagePath != null) {
        CommentDialog(
            onDismiss = { 
                showCommentDialog = false
                capturedImagePath = null
                capturedOrientation = null
            },
            onConfirm = { comment ->
                coroutineScope.launch {
                    capturedImagePath?.let { path ->
                        capturedOrientation?.let { orient ->
                            imageRepository.insertImage(
                                com.wastesamaritan.demoinstacamera.data.ImageEntity(
                                    filePath = path,
                                    yaw = orient.yaw,
                                    pitch = orient.pitch,
                                    roll = orient.roll,
                                    rotationMatrix = orient.rotationMatrix,
                                    horizontalFov = horizontalFov,
                                    comment = comment,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                            lastSnapPath = path
                        }
                    }
                    showCommentDialog = false
                    capturedImagePath = null
                    capturedOrientation = null
                }
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
        if (!hasCameraPermission) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera permission is required to use the camera.", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) }) {
                    Text("Grant Camera Permission")
                }
            }
        } else {
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
                            cameraProvider.unbindAll() // <-- Added cleanup here
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val capture = ImageCapture.Builder().build()
                            imageCapture = capture
                            val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    capture
                                )
                                horizontalFov = calculateFov(camera.cameraInfo)
                            } catch (exc: Exception) {
                                Log.e("Insta360FullCamera", "Camera binding failed", exc)
                                cameraError = "Failed to bind camera: ${exc.message}"
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Object detection overlay
            ObjectDetectionOverlay(
                isObjectDetected = isObjectDetected
            )

            // 2. Last snap thumbnail (bottom left)
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

            // 2. Show orientation above the snap/gallery/stop buttons
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Yaw: ${orientation.yaw.format(2)}  Pitch: ${orientation.pitch.format(2)}  Roll: ${orientation.roll.format(2)}",
                    color = Color.White,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(8.dp))
            }

            // 3. Bottom overlay bar
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(bottom = 32.dp, top = 16.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery button
                    FixedGradientSocialButton(
                        text = "Gallery",
                        icon = Icons.Filled.PhotoLibrary,
                        onClick = { showGallery = true },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    // Snap button
                    FixedGradientSocialButton(
                        text = "Capture",
                        icon = Icons.Filled.CameraAlt,
                        onClick = {
                            shutterSound.play(MediaActionSound.SHUTTER_CLICK)
                            val file = File(
                                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                                "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
                            )
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                            val currentOrientation = orientation
                            imageCapture?.takePicture(
                                outputOptions,
                                androidx.core.content.ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        capturedImagePath = file.absolutePath
                                        capturedOrientation = currentOrientation
                                        showCommentDialog = true
                                    }
                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e("Insta360FullCamera", "Image capture failed", exception)
                                    }
                                }
                            )
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    // Stop button
                    FixedGradientSocialButton(
                        text = "Stop",
                        icon = Icons.Filled.Stop,
                        onClick = {
                            try {
                                com.arashivision.sdkcamera.camera.InstaCameraManager.getInstance().stopNormalRecord()
                            } catch (_: Exception) {}
                            isRecording = false
                            navController.popBackStack()
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                }
            }

            // 4. Gallery modal/grid
            if (showGallery) {
                DatabaseGalleryScreen(
                    navController = navController,
                    onClose = { showGallery = false }
                )
            }
        }
    }
} 