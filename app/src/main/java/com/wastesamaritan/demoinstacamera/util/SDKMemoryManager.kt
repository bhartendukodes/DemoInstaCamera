package com.wastesamaritan.demoinstacamera.util

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Memory manager for Arashivision SDK
 * Handles memory optimization and cleanup for the extensive native libraries
 */
class SDKMemoryManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SDKMemoryManager"
        private const val MEMORY_THRESHOLD_PERCENT = 80 // Trigger cleanup at 80% memory usage
        private const val MIN_MEMORY_MB = 512 // Minimum required memory in MB
    }
    
    /**
     * Check if device has sufficient memory for SDK
     */
    fun hasSufficientMemory(): Boolean {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)
        
        val availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
        val totalMemoryMB = memoryInfo.totalMem / (1024 * 1024)
        val memoryUsagePercent = ((totalMemoryMB - availableMemoryMB) / totalMemoryMB.toFloat()) * 100
        
        Log.d(TAG, "Memory: ${availableMemoryMB}MB available, ${totalMemoryMB}MB total, ${memoryUsagePercent}% used")
        
        return availableMemoryMB >= MIN_MEMORY_MB && memoryUsagePercent < MEMORY_THRESHOLD_PERCENT
    }
    
    /**
     * Get current memory usage statistics
     */
    fun getMemoryStats(): MemoryStats {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)
        
        val availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
        val totalMemoryMB = memoryInfo.totalMem / (1024 * 1024)
        val usedMemoryMB = totalMemoryMB - availableMemoryMB
        val memoryUsagePercent = (usedMemoryMB / totalMemoryMB.toFloat()) * 100
        
        return MemoryStats(
            availableMemoryMB = availableMemoryMB,
            totalMemoryMB = totalMemoryMB,
            usedMemoryMB = usedMemoryMB,
            memoryUsagePercent = memoryUsagePercent,
            isLowMemory = memoryInfo.lowMemory
        )
    }
    
    /**
     * Perform memory cleanup when needed
     */
    fun performMemoryCleanup(scope: CoroutineScope) {
        scope.launch {
            val stats = getMemoryStats()
            
            if (stats.memoryUsagePercent > MEMORY_THRESHOLD_PERCENT || stats.isLowMemory) {
                Log.w(TAG, "Memory usage high (${stats.memoryUsagePercent}%), performing cleanup")
                
                withContext(Dispatchers.IO) {
                    System.gc()
                    try {
                        val coilImageLoader = coil.ImageLoader.Builder(context).build()
                        coilImageLoader.memoryCache?.clear()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to clear Coil cache: ${e.message}")
                    }
                    
                    // Clear other caches
                    context.cacheDir.deleteRecursively()
                }
                
                Log.i(TAG, "Memory cleanup completed")
            }
        }
    }
    
    /**
     * Check if device is suitable for SDK based on memory and performance
     */
    fun isDeviceSuitable(): DeviceSuitability {
        val stats = getMemoryStats()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        
        return DeviceSuitability(
            hasSufficientMemory = stats.availableMemoryMB >= MIN_MEMORY_MB,
            memoryUsagePercent = stats.memoryUsagePercent,
            isLowMemory = stats.isLowMemory,
            recommended = stats.availableMemoryMB >= MIN_MEMORY_MB * 2 && stats.memoryUsagePercent < 70,
            warnings = buildWarnings(stats, activityManager)
        )
    }
    
    private fun buildWarnings(stats: MemoryStats, activityManager: ActivityManager): List<String> {
        val warnings = mutableListOf<String>()
        
        if (stats.availableMemoryMB < MIN_MEMORY_MB) {
            warnings.add("Insufficient memory: ${stats.availableMemoryMB}MB available, ${MIN_MEMORY_MB}MB required")
        }
        
        if (stats.memoryUsagePercent > MEMORY_THRESHOLD_PERCENT) {
            warnings.add("High memory usage: ${stats.memoryUsagePercent}%")
        }
        
        if (stats.isLowMemory) {
            warnings.add("Device is in low memory state")
        }
        
        return warnings
    }
    
    data class MemoryStats(
        val availableMemoryMB: Long,
        val totalMemoryMB: Long,
        val usedMemoryMB: Long,
        val memoryUsagePercent: Float,
        val isLowMemory: Boolean
    )
    
    data class DeviceSuitability(
        val hasSufficientMemory: Boolean,
        val memoryUsagePercent: Float,
        val isLowMemory: Boolean,
        val recommended: Boolean,
        val warnings: List<String>
    )
} 