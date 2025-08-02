package com.wastesamaritan.demoinstacamera.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wastesamaritan.demoinstacamera.data.ImageRepository

sealed class MainNavScreen(val route: String, val label: String, val icon: ImageVector) {
    object Home : MainNavScreen("home", "Home", Icons.Filled.Home)
    object Capture : MainNavScreen("capture", "Capture", Icons.Filled.CameraAlt)
    object Gallery : MainNavScreen("gallery", "Gallery", Icons.Filled.PhotoLibrary)
    object FullCamera : MainNavScreen("full_camera", "Full Camera", Icons.Filled.CameraAlt)
    object DbGallery : MainNavScreen("db_gallery", "DB Gallery", Icons.Filled.PhotoLibrary)
    object CaptureOrientation : MainNavScreen("capture_orientation", "Capture Orientation", Icons.Filled.CameraAlt)
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val imageRepository = ImageRepository(LocalContext.current)

    NavHost(
        navController = navController,
        startDestination = MainNavScreen.Home.route
    ) {
        composable(MainNavScreen.Home.route) {
            Insta360PreviewScreen(navController)
        }
        composable(MainNavScreen.Capture.route) {
            Insta360CaptureScreen(navController, onBack = { navController.popBackStack() })
        }
        composable(MainNavScreen.Gallery.route) {
            GalleryScreen(onBack = { navController.popBackStack() })
        }
        composable(MainNavScreen.FullCamera.route) {
            Insta360FullCameraScreen(navController)
        }
        composable(MainNavScreen.DbGallery.route) {
            DatabaseGalleryScreen(navController)
        }
        composable(MainNavScreen.CaptureOrientation.route) {
            OrientationCaptureScreen(navController)
        }
        composable("image_preview/{imageId}") { backStackEntry ->
            val imageId = backStackEntry.arguments?.getString("imageId")?.toIntOrNull() ?: return@composable
            FullScreenImagePreviewScreen(
                imageId = imageId,
                navController = navController,
                imageRepository = imageRepository
            )
        }
    }
}