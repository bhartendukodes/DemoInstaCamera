package com.wastesamaritan.demoinstacamera.util

import android.util.Log
import com.arashivision.sdkcamera.camera.InstaCameraManager
import com.arashivision.sdkmedia.player.capture.InstaCapturePlayerView

/**
 * SDK Error Handler for Arashivision camera SDK
 * Handles common SDK errors and provides recovery mechanisms
 */
object SDKErrorHandler {
    
    private const val TAG = "SDKErrorHandler"
    
    /**
     * Known SDK error patterns
     */
    private val KNOWN_ERRORS = mapOf(
        "OffsetData" to "Known SDK issue with OffsetData null pointer",
        "NullPointerException" to "Null pointer exception in SDK",
        "IllegalStateException" to "Illegal state in SDK",
        "NetworkException" to "Network connectivity issue",
        "CameraNotConnected" to "Camera not connected or disconnected"
    )
    
    /**
     * Handle player play errors with recovery mechanisms
     */
    fun handlePlayerPlayError(playerView: InstaCapturePlayerView, error: Exception): Boolean {
        Log.e(TAG, "Player play error: ${error.message}", error)
        
        val errorMessage = error.message ?: ""
        val causeMessage = error.cause?.message ?: ""
        
        // Check for known error patterns
        for ((pattern, description) in KNOWN_ERRORS) {
            if (errorMessage.contains(pattern) || causeMessage.contains(pattern)) {
                Log.w(TAG, "Detected known error: $description")
                
                when (pattern) {
                    "OffsetData" -> {
                        return handleOffsetDataError(playerView)
                    }
                    "CameraNotConnected" -> {
                        return handleCameraConnectionError()
                    }
                    else -> {
                        Log.w(TAG, "Known error pattern detected: $pattern")
                        return false
                    }
                }
            }
        }
        
        // Unknown error
        Log.e(TAG, "Unknown SDK error: $errorMessage")
        return false
    }
    
    /**
     * Handle OffsetData null pointer error
     */
    private fun handleOffsetDataError(playerView: InstaCapturePlayerView): Boolean {
        Log.w(TAG, "Handling OffsetData error - attempting recovery")
        
        try {
            // Try to destroy and recreate the player view
            playerView.destroy()
            Thread.sleep(100) // Small delay
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle OffsetData error: ${e.message}")
            return false
        }
    }
    
    /**
     * Handle camera connection error
     */
    private fun handleCameraConnectionError(): Boolean {
        Log.w(TAG, "Handling camera connection error")
        
        val cameraManager = InstaCameraManager.getInstance()
        val isConnected = cameraManager.cameraConnectedType != InstaCameraManager.CONNECT_TYPE_NONE
        
        if (!isConnected) {
            Log.w(TAG, "Camera not connected, attempting to reconnect")
            // You could implement reconnection logic here
            return false
        }
        
        return true
    }
    
    /**
     * Check if error is recoverable
     */
    fun isRecoverableError(error: Exception): Boolean {
        val errorMessage = error.message ?: ""
        val causeMessage = error.cause?.message ?: ""
        
        return KNOWN_ERRORS.keys.any { pattern ->
            errorMessage.contains(pattern) || causeMessage.contains(pattern)
        }
    }
    
    /**
     * Get error description for user-friendly messages
     */
    fun getErrorDescription(error: Exception): String {
        val errorMessage = error.message ?: ""
        val causeMessage = error.cause?.message ?: ""
        
        for ((pattern, description) in KNOWN_ERRORS) {
            if (errorMessage.contains(pattern) || causeMessage.contains(pattern)) {
                return description
            }
        }
        
        return "Unknown SDK error: ${error.message}"
    }
    
    /**
     * Log SDK diagnostics for debugging
     */
    fun logSDKDiagnostics() {
        val cameraManager = InstaCameraManager.getInstance()
        
        Log.d(TAG, "SDK Diagnostics:")
        Log.d(TAG, "  Camera Type: ${cameraManager.cameraType}")
        Log.d(TAG, "  Camera Version: ${cameraManager.cameraVersion}")
        Log.d(TAG, "  Camera Serial: ${cameraManager.cameraSerial}")
        Log.d(TAG, "  Connected Type: ${cameraManager.cameraConnectedType}")
        Log.d(TAG, "  Media Offset: ${cameraManager.mediaOffset}")
        Log.d(TAG, "  Is Selfie: ${cameraManager.isCameraSelfie}")
    }
} 