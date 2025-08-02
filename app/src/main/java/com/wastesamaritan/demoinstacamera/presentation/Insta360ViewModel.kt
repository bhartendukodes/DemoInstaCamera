package com.wastesamaritan.demoinstacamera.presentation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import com.arashivision.sdkcamera.camera.InstaCameraManager
import com.arashivision.sdkcamera.camera.callback.ICameraChangedCallback
import com.arashivision.sdkcamera.camera.callback.ICameraOperateCallback
import com.arashivision.sdkcamera.camera.callback.IPreviewStatusListener
import com.arashivision.sdkcamera.camera.model.TemperatureLevel
import com.arashivision.sdkcamera.camera.resolution.PreviewStreamResolution
import com.arashivision.sdkmedia.player.capture.CaptureParamsBuilder
import com.arashivision.sdkmedia.player.capture.InstaCapturePlayerView
import com.arashivision.sdkmedia.player.listener.PlayerViewListener
import com.wastesamaritan.demoinstacamera.data.Insta360Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PreviewState {
    object Idle : PreviewState()
    object Opening : PreviewState()
    data class Error(val message: String? = null) : PreviewState()
}

class Insta360ViewModel(
    private val repository: Insta360Repository = Insta360Repository()
) : ViewModel(), ICameraChangedCallback {
    private val _previewState = MutableStateFlow<PreviewState>(PreviewState.Idle)

    private val _cameraConnected = MutableStateFlow(false)
    val cameraConnected: StateFlow<Boolean> = _cameraConnected
    private val _cameraType = MutableStateFlow("")
    val cameraType: StateFlow<String> = _cameraType
    private val _cameraVersion = MutableStateFlow("")
    val cameraVersion: StateFlow<String> = _cameraVersion
    private val _cameraSerial = MutableStateFlow("")
    val cameraSerial: StateFlow<String> = _cameraSerial

    private val _networkBindError = MutableStateFlow<String?>(null)
    val networkBindError = _networkBindError.asStateFlow()
    private var boundNetwork: Network? = null

    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        repository.registerCameraChangedCallback(this)
        updateCameraInfo()
    }

    private fun updateCameraInfo() {
        viewModelScope.launch {
            _cameraConnected.value = repository.isCameraConnected()
            if (_cameraConnected.value) {
                _cameraType.value = repository.getCameraType()
                _cameraVersion.value = repository.getCameraVersion()
                _cameraSerial.value = repository.getCameraSerial()
            } else {
                _cameraType.value = ""
                _cameraVersion.value = ""
                _cameraSerial.value = ""
            }
        }
    }

    override fun onCameraStatusChanged(enabled: Boolean, connectType: Int) {
        _cameraConnected.value = enabled
        if (enabled){
            updateCameraInfo()
        }
        else{
            _cameraType.value = ""
            _cameraSerial.value = ""
            _cameraVersion.value = " "
        }
    }


    override fun onCameraConnectError(errorCode: Int) {
        _cameraConnected.value = false
        _cameraType.value = ""
        _cameraVersion.value = ""
        _cameraSerial.value = ""
    }
    override fun onCameraSDCardStateChanged(enabled: Boolean) {}
    override fun onCameraStorageChanged(freeSpace: Long, totalSpace: Long) {}
    override fun onCameraBatteryLow() {}
    override fun onCameraBatteryUpdate(batteryLevel: Int, isCharging: Boolean) {}
    override fun onCameraTemperatureChanged(tempLevel: TemperatureLevel) {}

    override fun onCleared() {
        repository.unregisterCameraChangedCallback(this)
        super.onCleared()
    }

    fun bindToCameraWifi(context: Context, onBound: (Boolean) -> Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val success = connectivityManager.bindProcessToNetwork(network)
                    if (success) {
                        boundNetwork = network
                        _networkBindError.value = null
                        onBound(true)
                    } else {
                        _networkBindError.value = "Failed to bind to camera Wi-Fi network."
                        onBound(false)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val success = ConnectivityManager.setProcessDefaultNetwork(network)
                    if (success) {
                        boundNetwork = network
                        _networkBindError.value = null
                        onBound(true)
                    } else {
                        _networkBindError.value = "Failed to bind to camera Wi-Fi network."
                        onBound(false)
                    }
                }
                connectivityManager.unregisterNetworkCallback(this)
            }
            override fun onUnavailable() {
                _networkBindError.value = "Camera Wi-Fi network unavailable."
                onBound(false)
                connectivityManager.unregisterNetworkCallback(this)
            }
        })
    }
    fun getCameraType(): String = repository.getCameraType()
    fun getCameraVersion(): String = repository.getCameraVersion()
    fun getCameraSerial(): String = repository.getCameraSerial()

    fun connectCamera(context: Context) {
        _previewState.value = PreviewState.Opening
        bindToCameraWifi(context) { bound ->
            if (!bound) {
                _previewState.value = PreviewState.Error(_networkBindError.value)
                return@bindToCameraWifi
            }
            repository.openCameraViaWifi()
        }
    }

    fun disconnectCamera(){
        repository.closeCamera()
    }
}