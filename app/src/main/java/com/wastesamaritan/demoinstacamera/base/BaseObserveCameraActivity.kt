package com.wastesamaritan.demoinstacamera.base

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.wastesamaritan.demoinstacamera.R
import com.arashivision.sdkcamera.camera.InstaCameraManager
import com.arashivision.sdkcamera.camera.callback.ICameraChangedCallback
import com.arashivision.sdkcamera.camera.model.TemperatureLevel

abstract class BaseObserveCameraActivity : BaseActivity(), ICameraChangedCallback {
    
    protected val TAG = this.javaClass.simpleName
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InstaCameraManager.getInstance().registerCameraChangedCallback(this)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        InstaCameraManager.getInstance().unregisterCameraChangedCallback(this)
    }
    
    /**
     * Camera status changed
     *
     * @param enabled Whether the camera is available
     */
    override fun onCameraStatusChanged(enabled: Boolean, connectType: Int) {
        // Override in subclasses if needed
    }
    
    /**
     * Camera connection failed
     *
     * A common situation is that other phones or other applications of this phone have already
     * established a connection with this camera, resulting in this establishment failure,
     * and other phones need to disconnect from this camera first.
     *
     * @param errorCode
     */
    override fun onCameraConnectError(errorCode: Int) {
        // Override in subclasses if needed
    }
    
    /**
     * SD card insertion notification
     *
     * @param enabled Whether the current SD card is available
     */
    override fun onCameraSDCardStateChanged(enabled: Boolean) {
        // Override in subclasses if needed
    }
    
    /**
     * SD card storage status changed
     *
     * @param freeSpace  Currently available size
     * @param totalSpace Total size
     */
    override fun onCameraStorageChanged(freeSpace: Long, totalSpace: Long) {
        // Override in subclasses if needed
    }
    
    /**
     * Low battery notification
     */
    override fun onCameraBatteryLow() {
        // Override in subclasses if needed
    }
    
    /**
     * Camera power change notification
     *
     * @param batteryLevel Current power (0-100, always returns 100 when charging)
     * @param isCharging   Whether the camera is charging
     */
    override fun onCameraBatteryUpdate(batteryLevel: Int, isCharging: Boolean) {
        // Override in subclasses if needed
    }
    
    /**
     * Just for OneX2, when change its camera sensor
     *
     * @param cameraSensorMode equals to InstaCameraManager.getInstance().getCurrentCameraMode();
     */
    override fun onCameraSensorModeChanged(cameraSensorMode: Int) {
    }
    
    override fun onCameraTemperatureChanged(tempLevel: TemperatureLevel) {
        Log.d(TAG, "onCameraTemperatureChanged  ${tempLevel.name}")
        when (tempLevel) {
            TemperatureLevel.HIGH -> {
                Toast.makeText(this, R.string.common_toast_temperature_high, Toast.LENGTH_SHORT).show()
            }
            TemperatureLevel.HIGH_SHUTDOWN -> {
                InstaCameraManager.getInstance().closeCamera()
                Toast.makeText(this, R.string.common_toast_temperature_high_shutdown, Toast.LENGTH_SHORT).show()
            }
            else -> {
                // Handle other temperature levels if needed
            }
        }
    }
} 