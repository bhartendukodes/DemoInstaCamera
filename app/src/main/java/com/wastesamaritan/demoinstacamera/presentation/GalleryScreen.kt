package com.wastesamaritan.demoinstacamera.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.layout.ContentScale
import com.arashivision.sdkmedia.work.WorkWrapper
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.ui.viewinterop.AndroidView
import coil.request.ImageRequest
import coil.size.Scale
import androidx.compose.animation.Crossfade
import com.arashivision.sdkmedia.player.image.InstaImagePlayerView
import com.arashivision.sdkmedia.player.video.InstaVideoPlayerView
import com.arashivision.sdkmedia.player.image.ImageParamsBuilder
import com.arashivision.sdkmedia.player.video.VideoParamsBuilder
import androidx.lifecycle.LifecycleOwner
import com.wastesamaritan.demoinstacamera.R
import com.wastesamaritan.demoinstacamera.data.CameraMediaRepository
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import android.util.Log
import androidx.compose.runtime.DisposableEffect
import com.arashivision.sdkmedia.player.listener.PlayerViewListener

enum class PreviewType(val label: String) {
    Normal("Normal"),
    Fisheye("Fisheye"),
    TinyPlanet("Tiny Planet"),
    Plane("Plane")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(onBack: () -> Unit) {
    val cameraMediaRepository = remember { CameraMediaRepository() }
    var allWorks by remember { mutableStateOf<List<WorkWrapper>>(emptyList()) }
    var shownWorks by remember { mutableStateOf<List<WorkWrapper>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedWork by remember { mutableStateOf<WorkWrapper?>(null) }
    var loading by remember { mutableStateOf(true) }
    val cardShape = RoundedCornerShape(20.dp)
    var previewType by remember { mutableStateOf(PreviewType.Normal) }
    var showExportControls by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            loading = true
            Log.d("GalleryScreen", "Starting to load camera works...")
            val works = cameraMediaRepository.getAllCameraWorks()
            Log.d("GalleryScreen", "Found ${works.size} camera works")
            val loadedWorks = mutableListOf<WorkWrapper>()
            for (work in works) {
                try {
                    loadedWorks.add(work)
                    allWorks = loadedWorks.toList()
                    shownWorks = loadedWorks.toList()
                    Log.d("GalleryScreen", "Loaded work: ${work.getUrls(false).firstOrNull()}")
                    delay(30)
                } catch (e: Exception) {
                    Log.e("GalleryScreen", "Error loading individual work", e)
                }
            }
            loading = false
            Log.d("GalleryScreen", "Finished loading ${loadedWorks.size} works")
        } catch (e: Exception) {
            Log.e("GalleryScreen", "Error loading camera works", e)
            loading = false
        }
    }

    fun applyFilter(filter: String) {
        selectedFilter = filter
        shownWorks = when (filter) {
            "Images" -> cameraMediaRepository.filterImages(allWorks)
            "Videos" -> cameraMediaRepository.filterVideos(allWorks)
            else -> allWorks
        }
    }

    fun isImageFile(url: String): Boolean {
        val lower = url.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
    }
    fun isVideoFile(url: String): Boolean {
        val lower = url.lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".insv")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gallery", fontSize = MaterialTheme.typography.titleLarge.fontSize) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val filters = listOf("All", "Images", "Videos")
                    filters.forEach { filter ->
                        val selected = selectedFilter == filter
                        Button(
                            onClick = { applyFilter(filter) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(filter)
                        }
                    }
                }
                if (loading && shownWorks.isEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(160.dp),
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        items(10) {
                            Box(
                                Modifier
                                    .size(160.dp)
                                    .padding(8.dp)
                                    .clip(cardShape)
                                    .background(Color.LightGray.copy(alpha = 0.3f))
                            )
                        }
                    }
                } else if (shownWorks.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                            Spacer(Modifier.height(8.dp))
                            Text("No media found.", color = Color.Gray)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(160.dp),
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        items(shownWorks) { work: WorkWrapper ->
                            val thumbUrl = work.getUrls(false).getOrNull(0) ?: ""
                            val isVideo = isVideoFile(thumbUrl)
                            val isSupported = thumbUrl.endsWith(".jpg", true) || thumbUrl.endsWith(".jpeg", true) || thumbUrl.endsWith(".png", true) || thumbUrl.endsWith(".mp4", true)
                            Card(
                                modifier = Modifier
                                    .size(160.dp)
                                    .padding(8.dp)
                                    .clip(cardShape)
                                    .clickable { selectedWork = work },
                                shape = cardShape,
                                elevation = CardDefaults.cardElevation(0.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Box(Modifier.fillMaxSize()) {
                                    if (isSupported) {
                                        val painter = rememberAsyncImagePainter(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(thumbUrl)
                                                .crossfade(true)
                                                .scale(Scale.FILL)
                                                .build(),
                                            placeholder = rememberVectorPainter(Icons.Filled.PhotoLibrary),
                                            error = rememberVectorPainter(Icons.Filled.BrokenImage)
                                        )
                                        val state = painter.state
                                        Image(
                                            painter = painter,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (state is AsyncImagePainter.State.Loading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.align(Alignment.Center),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            if (isVideo) {
                                                Icon(
                                                    imageVector = Icons.Filled.PlayArrow,
                                                    contentDescription = "Video",
                                                    tint = Color.Gray,
                                                    modifier = Modifier.size(48.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Filled.PhotoLibrary,
                                                    contentDescription = "Image",
                                                    tint = Color.Gray,
                                                    modifier = Modifier.size(48.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (isVideo) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(48.dp)
                                                .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(24.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (selectedWork != null) {
                val url = selectedWork?.getUrls(true)?.getOrNull(0) ?: ""
                val isVideo = isVideoFile(url)
                var showExportDialog by remember { mutableStateOf(false) }
                var showPreviewTypeDialog by remember { mutableStateOf(false) }
                var previewType by remember { mutableStateOf(PreviewType.Normal) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Preview", color = Color.White) },
                            navigationIcon = {
                                IconButton(onClick = { selectedWork = null }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                            },
                            actions = {
                                IconButton(onClick = { showExportDialog = true }) {
                                    Icon(
                                        Icons.Filled.Upload, 
                                        contentDescription = "Export", 
                                        tint = Color.White
                                    )
                                }
                                // Preview Type button
                                IconButton(onClick = { showPreviewTypeDialog = true }) {
                                    Icon(
                                        Icons.Filled.Tune, 
                                        contentDescription = "Preview Type", 
                                        tint = Color.White
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Black,
                                titleContentColor = Color.White,
                                navigationIconContentColor = Color.White,
                                actionIconContentColor = Color.White
                            )
                        )
                    }
                ) { padding ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Crossfade(targetState = previewType, label = "PreviewTypeCrossfade") { type ->
                            when (type) {
                                PreviewType.Normal ->
                                    Insta360PlayerView(
                                        workWrapper = selectedWork!!,
                                        isVideo = isVideo,
                                        renderMode = "NORMAL",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                PreviewType.Fisheye ->
                                    Insta360PlayerView(
                                        workWrapper = selectedWork!!,
                                        isVideo = isVideo,
                                        renderMode = "FISHEYE",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                PreviewType.TinyPlanet ->
                                    Insta360PlayerView(
                                        workWrapper = selectedWork!!,
                                        isVideo = isVideo,
                                        renderMode = "TINY_PLANET",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                PreviewType.Plane ->
                                    Insta360PlayerView(
                                        workWrapper = selectedWork!!,
                                        isVideo = isVideo,
                                        renderMode = "PLANE",
                                        modifier = Modifier.fillMaxSize()
                                    )
                            }
                        }
                        if (showExportDialog) {
                            AlertDialog(
                                onDismissRequest = { showExportDialog = false },
                                title = { Text("Export Options") },
                                text = {
                                    Column {
                                        Button(
                                            onClick = { 
                                                // TODO: Call export original
                                                showExportDialog = false 
                                            }, 
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Export Original")
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Button(
                                            onClick = { 
                                                // TODO: Call export thumbnail
                                                showExportDialog = false 
                                            }, 
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Export Thumbnail")
                                        }
                                    }
                                },
                                confirmButton = {},
                                dismissButton = {
                                    Button(onClick = { showExportDialog = false }) { 
                                        Text("Cancel") 
                                    }
                                }
                            )
                        }
                        if (showPreviewTypeDialog) {
                            AlertDialog(
                                onDismissRequest = { showPreviewTypeDialog = false },
                                title = { Text("Select Preview Type") },
                                text = {
                                    Column {
                                        PreviewType.values().forEach { type ->
                                            Button(
                                                onClick = { 
                                                    previewType = type
                                                    showPreviewTypeDialog = false 
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (type == previewType) 
                                                        MaterialTheme.colorScheme.primary 
                                                    else 
                                                        MaterialTheme.colorScheme.surface
                                                )
                                            ) {
                                                Text(type.label)
                                            }
                                        }
                                    }
                                },
                                confirmButton = {},
                                dismissButton = {
                                    Button(onClick = { showPreviewTypeDialog = false }) { 
                                        Text("Cancel") 
                                    }
                                }
                            )
                        }
                    }
                }
            }
            if (showExportControls) {
                AlertDialog(
                    onDismissRequest = { showExportControls = false },
                    confirmButton = {
                        Button(onClick = { showExportControls = false }) { Text("Close") }
                    },
                    title = { Text("Export Controls") },
                    text = { Text("Use the export buttons in the detail view.") }
                )
            }
        }
    }
}

@Composable
fun Insta360PlayerView(
    workWrapper: WorkWrapper,
    isVideo: Boolean,
    renderMode: String,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalContext.current as? LifecycleOwner
    var videoPlayer by remember { mutableStateOf<InstaVideoPlayerView?>(null) }
    var imagePlayer by remember { mutableStateOf<InstaImagePlayerView?>(null) }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            try {
                videoPlayer?.destroy()
                imagePlayer?.destroy()
            } catch (e: Exception) {
                Log.e("PlayerCleanup", "Error during cleanup", e)
            }
        }
    }
    
    AndroidView(
        factory = { ctx ->
            val inflater = android.view.LayoutInflater.from(ctx)
            val root = inflater.inflate(R.layout.insta_player_view, null, false)
            val imagePlayerView = root.findViewById<InstaImagePlayerView>(R.id.insta_image_player)
            val videoPlayerView = root.findViewById<InstaVideoPlayerView>(R.id.insta_video_player)
            
            // Store references for cleanup
            imagePlayer = imagePlayerView
            videoPlayer = videoPlayerView
            
            if (isVideo) {
                // Video Player Setup
                videoPlayerView.visibility = android.view.View.VISIBLE
                imagePlayerView.visibility = android.view.View.GONE
                
                try {
                    // Set lifecycle for video player
                    lifecycleOwner?.let { videoPlayerView.setLifecycle(it.lifecycle) }
                    
                    // Set player view listener for video
                    videoPlayerView.setPlayerViewListener(object : PlayerViewListener {
                        override fun onLoadingStatusChanged(isLoading: Boolean) {
                            Log.d("VideoPlayer", "Loading status changed: $isLoading")
                        }
                        
                        override fun onLoadingFinish() {
                            Log.d("VideoPlayer", "Loading finished")
                        }
                    })
                    
                    // Use basic VideoParamsBuilder without problematic methods
                    val videoParamsBuilder = VideoParamsBuilder()
                    
                    // Log the work wrapper info for debugging
                    Log.d("VideoPlayer", "WorkWrapper URLs: ${workWrapper.getUrls(true)}")
                    Log.d("VideoPlayer", "WorkWrapper isVideo: ${workWrapper.isVideo}")
                    
                    // Prepare and play video with additional error handling
                    videoPlayerView.prepare(workWrapper, videoParamsBuilder)
                    
                    // Add a small delay before playing to ensure preparation is complete
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            videoPlayerView.play()
                        } catch (e: Exception) {
                            Log.e("VideoPlayer", "Error playing video", e)
                        }
                    }, 100)
                    
                } catch (e: Exception) {
                    Log.e("VideoPlayer", "Error preparing video player", e)
                }
                
            } else {
                // Image Player Setup
                imagePlayerView.visibility = android.view.View.VISIBLE
                videoPlayerView.visibility = android.view.View.GONE
                
                try {
                    // Set lifecycle for image player
                    lifecycleOwner?.let { imagePlayerView.setLifecycle(it.lifecycle) }
                    
                    // Set player view listener for image
                    imagePlayerView.setPlayerViewListener(object : PlayerViewListener {
                        override fun onLoadingStatusChanged(isLoading: Boolean) {
                            Log.d("ImagePlayer", "Loading status changed: $isLoading")
                        }
                        
                        override fun onLoadingFinish() {
                            Log.d("ImagePlayer", "Loading finished")
                        }
                    })
                    
                    // Use basic ImageParamsBuilder without problematic methods
                    val imageParamsBuilder = ImageParamsBuilder()
                    
                    // Log the work wrapper info for debugging
                    Log.d("ImagePlayer", "WorkWrapper URLs: ${workWrapper.getUrls(true)}")
                    Log.d("ImagePlayer", "WorkWrapper isPhoto: ${workWrapper.isPhoto}")
                    
                    // Prepare and play image with additional error handling
                    imagePlayerView.prepare(workWrapper, imageParamsBuilder)
                    
                    // Add a small delay before playing to ensure preparation is complete
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            imagePlayerView.play()
                        } catch (e: Exception) {
                            Log.e("ImagePlayer", "Error playing image", e)
                        }
                    }, 100)
                    
                } catch (e: Exception) {
                    Log.e("ImagePlayer", "Error preparing image player", e)
                }
            }
            
            root
        },
        modifier = modifier
    )
}