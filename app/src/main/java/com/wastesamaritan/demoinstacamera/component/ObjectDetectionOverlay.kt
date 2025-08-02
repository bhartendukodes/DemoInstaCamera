package com.wastesamaritan.demoinstacamera.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ObjectDetectionOverlay(
    modifier: Modifier = Modifier,
    isObjectDetected: Boolean = false
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Detection box
        Canvas(
            modifier = Modifier.size(300.dp)
        ) {
            val strokeWidth = 4f
            val boxColor = if (isObjectDetected) Color.Green else Color.White
            
            // Draw the square detection box
            drawRect(
                color = Color.Transparent,
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
            
            // Draw the border
            drawRect(
                color = boxColor,
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }
        
        // Status text
        Text(
            text = if (isObjectDetected) "Object Detected!" else "Position object in box",
            color = if (isObjectDetected) Color.Green else Color.White,
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
        )
    }
} 