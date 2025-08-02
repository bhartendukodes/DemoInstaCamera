package com.wastesamaritan.demoinstacamera.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Network manager for Arashivision camera SDK
 * Handles WiFi connectivity and network configuration for camera connection
 */
class NetworkManager(private val context: Context) {
    
    companion object {
        private const val TAG = "NetworkManager"
        private const val CAMERA_IP_PREFIX = "192.168.42"
        private const val CAMERA_PORT = 80
        private const val CONNECTION_TIMEOUT_MS = 5000
    }
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    
    /**
     * Check if device is connected to camera WiFi network
     */
    fun isConnectedToCameraNetwork(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        return networkCapabilities?.let { capabilities ->
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } ?: false
    }
    
    /**
     * Check if WiFi is enabled
     */
    fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }
    
    /**
     * Get current network information
     */
    fun getCurrentNetworkInfo(): NetworkInfo {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
        
        return NetworkInfo(
            isConnected = activeNetwork != null,
            isWifi = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false,
            isCellular = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false,
            hasInternet = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false,
            ipAddress = linkProperties?.linkAddresses?.firstOrNull()?.address?.hostAddress,
            networkName = getNetworkName(activeNetwork)
        )
    }
    
    /**
     * Check if device can reach camera IP range
     */
    fun canReachCameraNetwork(): Boolean {
        return getCurrentNetworkInfo().let { info ->
            info.isConnected && info.isWifi && info.ipAddress?.startsWith(CAMERA_IP_PREFIX) == true
        }
    }
    
    /**
     * Monitor network changes for camera connectivity
     */
    fun startNetworkMonitoring(onNetworkChanged: (NetworkInfo) -> Unit) {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available: ${network}")
                onNetworkChanged(getCurrentNetworkInfo())
            }
            
            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost: ${network}")
                onNetworkChanged(getCurrentNetworkInfo())
            }
            
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                Log.d(TAG, "Network capabilities changed: ${networkCapabilities}")
                onNetworkChanged(getCurrentNetworkInfo())
            }
        }
        
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }
    
    /**
     * Test camera connectivity
     */
    fun testCameraConnectivity(scope: CoroutineScope, onResult: (Boolean) -> Unit) {
        scope.launch {
            val isReachable = withContext(Dispatchers.IO) {
                try {
                    // Test connection to camera IP
                    val socket = java.net.Socket()
                    socket.connect(java.net.InetSocketAddress("192.168.42.1", CAMERA_PORT), CONNECTION_TIMEOUT_MS)
                    socket.close()
                    true
                } catch (e: Exception) {
                    Log.w(TAG, "Camera connectivity test failed: ${e.message}")
                    false
                }
            }
            
            onResult(isReachable)
        }
    }
    
    /**
     * Get network diagnostics for troubleshooting
     */
    fun getNetworkDiagnostics(): NetworkDiagnostics {
        val networkInfo = getCurrentNetworkInfo()
        val wifiInfo = wifiManager.connectionInfo
        
        return NetworkDiagnostics(
            networkInfo = networkInfo,
            wifiEnabled = isWifiEnabled(),
            wifiConnected = networkInfo.isWifi,
            wifiSSID = wifiInfo.ssid,
            wifiSignalStrength = wifiInfo.rssi,
            canReachCamera = canReachCameraNetwork(),
            recommendations = buildRecommendations(networkInfo)
        )
    }
    
    private fun getNetworkName(network: Network?): String? {
        return try {
            val linkProperties = connectivityManager.getLinkProperties(network)
            linkProperties?.interfaceName
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get network name: ${e.message}")
            null
        }
    }
    
    private fun buildRecommendations(networkInfo: NetworkInfo): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (!networkInfo.isConnected) {
            recommendations.add("No network connection detected")
        }
        
        if (!networkInfo.isWifi) {
            recommendations.add("Camera requires WiFi connection")
        }
        
        if (!networkInfo.hasInternet) {
            recommendations.add("Network has no internet access")
        }
        
        if (!canReachCameraNetwork()) {
            recommendations.add("Not connected to camera WiFi network (192.168.42.x)")
        }
        
        return recommendations
    }
    
    data class NetworkInfo(
        val isConnected: Boolean,
        val isWifi: Boolean,
        val isCellular: Boolean,
        val hasInternet: Boolean,
        val ipAddress: String?,
        val networkName: String?
    )
    
    data class NetworkDiagnostics(
        val networkInfo: NetworkInfo,
        val wifiEnabled: Boolean,
        val wifiConnected: Boolean,
        val wifiSSID: String?,
        val wifiSignalStrength: Int,
        val canReachCamera: Boolean,
        val recommendations: List<String>
    )
} 