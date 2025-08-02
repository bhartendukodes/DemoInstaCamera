package com.wastesamaritan.demoinstacamera.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun CameraConnectionStatus(cameraConnected: Boolean) {
    val icon = if (cameraConnected) Icons.Default.CheckCircle else Icons.Default.Warning
    val iconColor = if (cameraConnected) Color(0xFF388E3C) else Color(0xFFD32F2F)
    val title = if (cameraConnected) "Camera Connected" else "Camera Not Connected"
    val message = if (cameraConnected) {
        "Your device is successfully connected to the camera via Wi-Fi."
    } else {
        "Please connect your device to the camera's Wi-Fi network to continue."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .size(48.dp)
                .padding(bottom = 8.dp)
        )

        Text(
            text = title,
            color = iconColor,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Text(
            text = message,
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
