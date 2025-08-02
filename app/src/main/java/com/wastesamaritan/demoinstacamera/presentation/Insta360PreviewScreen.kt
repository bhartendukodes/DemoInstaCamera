package com.wastesamaritan.demoinstacamera.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrowseGallery
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Warning
import com.wastesamaritan.demoinstacamera.component.CameraConnectionStatus
import com.wastesamaritan.demoinstacamera.component.FixedGradientSocialButton

@Composable
fun Insta360PreviewScreen(
    navController: androidx.navigation.NavHostController,
    viewModel: Insta360ViewModel = viewModel()
) {
    val cameraConnected by viewModel.cameraConnected.collectAsState()
    val cameraType by viewModel.cameraType.collectAsState()
    val cameraVersion by viewModel.cameraVersion.collectAsState()
    val cameraSerial by viewModel.cameraSerial.collectAsState()
    val networkBindError by viewModel.networkBindError.collectAsState()
    val context = LocalContext.current
    var showCaptureScreen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (showCaptureScreen) {
            Insta360CaptureScreen(onBack = { showCaptureScreen = false }, navController = navController)
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                CameraConnectionStatus(cameraConnected = cameraConnected)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Type: $cameraType",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Firmware: $cameraVersion",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Serial: $cameraSerial",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (!networkBindError.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = networkBindError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                FixedGradientSocialButton(
                    onClick = {
                        if (cameraConnected) {
                            viewModel.disconnectCamera()
                        } else {
                            viewModel.connectCamera(context)
                        }
                    },
                    icon = if (cameraConnected) Icons.Filled.LinkOff else Icons.Filled.CameraAlt,
                    text = if (cameraConnected) "Disconnect" else "Connect"
                )

                Spacer(modifier = Modifier.height(14.dp))

                FixedGradientSocialButton(
                    onClick = {
                        val intent = Intent(context, PreviewActivity::class.java)
                        context.startActivity(intent)
                    },
                    icon = Icons.Filled.Preview,
                    text = "Preview"
                )

                Spacer(modifier = Modifier.height(14.dp))

                FixedGradientSocialButton(
                    onClick = {showCaptureScreen = true},
                    icon = Icons.Filled.CameraAlt,
                    text = "Capture"
                )

                Spacer(modifier = Modifier.height(14.dp))

                FixedGradientSocialButton(
                    onClick = {navController.navigate("gallery")},
                    icon = Icons.Filled.BrowseGallery,
                    text = "Gallery"
                )

            }
        }
    }
} 