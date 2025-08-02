package com.wastesamaritan.demoinstacamera.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.wastesamaritan.demoinstacamera.data.ImageEntity
import java.text.SimpleDateFormat
import java.util.*
import android.os.Environment
import java.io.File
import java.io.FileWriter
import android.content.Context

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseGalleryScreen(navController: NavController, onClose: (() -> Unit)? = null) {
    val context = LocalContext.current
    val imageRepository = remember { com.wastesamaritan.demoinstacamera.data.ImageRepository(context) }
    val images by imageRepository.getAllImages().collectAsState(initial = emptyList())

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("My Snaps", fontSize = MaterialTheme.typography.titleLarge.fontSize) },
                navigationIcon = {
                    IconButton(onClick = {
                        onClose?.invoke() ?: navController.popBackStack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
            if (images.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No snaps found.", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(120.dp),
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    items(images) { image ->
                        Box(
                            Modifier
                                .aspectRatio(1f)
                                .padding(4.dp)
                                .background(Color.LightGray)
                                .clickable { navController.navigate("image_preview/${image.id}") }
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(image.filePath),
                                contentDescription = "Snap",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Float?.format(digits: Int) = this?.let { String.format("%.${digits}f", it) } ?: "--" 