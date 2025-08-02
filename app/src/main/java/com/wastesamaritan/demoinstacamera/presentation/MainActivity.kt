package com.wastesamaritan.demoinstacamera.presentation

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wastesamaritan.demoinstacamera.ui.theme.DemoInstaCameraTheme
import com.wastesamaritan.demoinstacamera.util.PermissionManager
import com.wastesamaritan.demoinstacamera.util.SDKMemoryManager
import com.wastesamaritan.demoinstacamera.util.NetworkManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

class MainActivity : FragmentActivity() {
    
    private lateinit var permissionManager: PermissionManager
    private lateinit var memoryManager: SDKMemoryManager
    private lateinit var networkManager: NetworkManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        permissionManager = PermissionManager(this)
        memoryManager = SDKMemoryManager(this)
        networkManager = NetworkManager(this)
        requestPermissions()
        
        setContent {
            DemoInstaCameraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: Insta360ViewModel = viewModel()
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    
                    // Monitor memory and network
                    LaunchedEffect(Unit) {
                        // Check device suitability
                        val suitability = memoryManager.isDeviceSuitable()
                        if (!suitability.recommended) {
                            Log.w("MainActivity", "Device suitability warnings: ${suitability.warnings}")
                        }
                        
                        // Start network monitoring
                        networkManager.startNetworkMonitoring { networkInfo ->
                            Log.d("MainActivity", "Network changed: ${networkInfo}")
                        }
                        
                        // Periodic memory cleanup
                        while (true) {
                            kotlinx.coroutines.delay(30000) // Every 30 seconds
                            memoryManager.performMemoryCleanup(scope)
                        }
                    }
                    
                    // Use the existing MainScreen composable
                    MainScreen()
                }
            }
        }
    }
    
    private fun requestPermissions() {
        val missingPermissions = permissionManager.getMissingPermissions()
        
        if (missingPermissions.isNotEmpty()) {
            val permissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val allGranted = permissions.values.all { it }
                if (!allGranted) {
                    // Show permission rationale dialog
                    showPermissionRationaleDialog()
                }
            }
            
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
    
    private fun showPermissionRationaleDialog() {
        // This would show a dialog explaining why permissions are needed
        // Implementation depends on your UI framework
    }
    
    companion object {
        private const val TAG = "MainActivity"
    }
} 