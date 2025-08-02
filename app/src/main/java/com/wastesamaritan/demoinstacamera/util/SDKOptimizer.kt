package com.wastesamaritan.demoinstacamera.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Comprehensive SDK optimizer for Arashivision camera SDK
 * Combines permission, memory, and network management for optimal performance
 */
class SDKOptimizer(
    private val context: Context,
    private val scope: CoroutineScope
) {
    
    companion object {
        private const val TAG = "SDKOptimizer"
    }
    
    private val permissionManager = if (context is androidx.fragment.app.FragmentActivity) {
        PermissionManager(context)
    } else {
        throw IllegalArgumentException("Context must be a FragmentActivity for permission management")
    }
    private val memoryManager = SDKMemoryManager(context)
    private val networkManager = NetworkManager(context)
    
    /**
     * Initialize and optimize SDK environment
     */
    fun initializeSDK(onComplete: (SDKStatus) -> Unit) {
        scope.launch {
            Log.i(TAG, "Initializing SDK optimization...")
            
            val status = withContext(Dispatchers.IO) {
                // Check permissions
                val permissionStatus = checkPermissions()
                
                // Check device suitability
                val deviceStatus = checkDeviceSuitability()
                
                // Check network connectivity
                val networkStatus = checkNetworkConnectivity()
                
                // Perform initial memory cleanup
                memoryManager.performMemoryCleanup(scope)
                
                SDKStatus(
                    permissions = permissionStatus,
                    device = deviceStatus,
                    network = networkStatus,
                    isReady = permissionStatus.allGranted && deviceStatus.recommended && networkStatus.canReachCamera
                )
            }
            
            Log.i(TAG, "SDK optimization complete: ${status}")
            onComplete(status)
        }
    }
    
    /**
     * Monitor SDK health and perform optimizations
     */
    fun startHealthMonitoring(onStatusChanged: (SDKStatus) -> Unit) {
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(10000) // Check every 10 seconds
                
                val status = withContext(Dispatchers.IO) {
                    val permissionStatus = checkPermissions()
                    val deviceStatus = checkDeviceSuitability()
                    val networkStatus = checkNetworkConnectivity()
                    
                    // Perform memory cleanup if needed
                    if (deviceStatus.memoryUsagePercent > 80) {
                        memoryManager.performMemoryCleanup(scope)
                    }
                    
                    SDKStatus(
                        permissions = permissionStatus,
                        device = deviceStatus,
                        network = networkStatus,
                        isReady = permissionStatus.allGranted && deviceStatus.recommended && networkStatus.canReachCamera
                    )
                }
                
                onStatusChanged(status)
            }
        }
    }
    
    /**
     * Get comprehensive SDK diagnostics
     */
    fun getDiagnostics(): SDKDiagnostics {
        val permissionStatus = checkPermissions()
        val deviceStatus = checkDeviceSuitability()
        val networkDiagnostics = networkManager.getNetworkDiagnostics()
        
        return SDKDiagnostics(
            permissions = permissionStatus,
            device = deviceStatus,
            network = networkDiagnostics,
            recommendations = buildRecommendations(permissionStatus, deviceStatus, networkDiagnostics),
            isProductionReady = isProductionReady(permissionStatus, deviceStatus, networkDiagnostics)
        )
    }
    
    private fun checkPermissions(): PermissionStatus {
        return PermissionStatus(
            allGranted = permissionManager.hasAllPermissions(),
            cameraGranted = permissionManager.hasCameraPermissions(),
            storageGranted = permissionManager.hasStoragePermissions(),
            networkGranted = permissionManager.hasNetworkPermissions(),
            missingPermissions = permissionManager.getMissingPermissions()
        )
    }
    
    private fun checkDeviceSuitability(): SDKMemoryManager.DeviceSuitability {
        return memoryManager.isDeviceSuitable()
    }
    
    private fun checkNetworkConnectivity(): NetworkConnectivityStatus {
        val networkInfo = networkManager.getCurrentNetworkInfo()
        return NetworkConnectivityStatus(
            isConnected = networkInfo.isConnected,
            isWifi = networkInfo.isWifi,
            canReachCamera = networkManager.canReachCameraNetwork(),
            ipAddress = networkInfo.ipAddress
        )
    }
    
    private fun buildRecommendations(
        permissionStatus: PermissionStatus,
        deviceStatus: SDKMemoryManager.DeviceSuitability,
        networkDiagnostics: NetworkManager.NetworkDiagnostics
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Permission recommendations
        if (!permissionStatus.allGranted) {
            recommendations.add("Grant all required permissions for camera functionality")
        }
        
        // Device recommendations
        deviceStatus.warnings.forEach { warning ->
            recommendations.add("Device: $warning")
        }
        
        // Network recommendations
        networkDiagnostics.recommendations.forEach { recommendation ->
            recommendations.add("Network: $recommendation")
        }
        
        return recommendations
    }
    
    private fun isProductionReady(
        permissionStatus: PermissionStatus,
        deviceStatus: SDKMemoryManager.DeviceSuitability,
        networkDiagnostics: NetworkManager.NetworkDiagnostics
    ): Boolean {
        return permissionStatus.allGranted &&
               deviceStatus.recommended &&
               networkDiagnostics.canReachCamera &&
               deviceStatus.memoryUsagePercent < 80
    }
    
    data class SDKStatus(
        val permissions: PermissionStatus,
        val device: SDKMemoryManager.DeviceSuitability,
        val network: NetworkConnectivityStatus,
        val isReady: Boolean
    )
    
    data class PermissionStatus(
        val allGranted: Boolean,
        val cameraGranted: Boolean,
        val storageGranted: Boolean,
        val networkGranted: Boolean,
        val missingPermissions: List<String>
    )
    
    data class NetworkConnectivityStatus(
        val isConnected: Boolean,
        val isWifi: Boolean,
        val canReachCamera: Boolean,
        val ipAddress: String?
    )
    
    data class SDKDiagnostics(
        val permissions: PermissionStatus,
        val device: SDKMemoryManager.DeviceSuitability,
        val network: NetworkManager.NetworkDiagnostics,
        val recommendations: List<String>,
        val isProductionReady: Boolean
    )
} 