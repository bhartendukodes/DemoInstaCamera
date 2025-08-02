package com.wastesamaritan.demoinstacamera.data

import com.arashivision.sdkcamera.camera.InstaCameraManager
import com.arashivision.sdkcamera.camera.callback.ICameraChangedCallback

class Insta360Repository {

    fun openCameraViaWifi() {
        InstaCameraManager.getInstance().openCamera(InstaCameraManager.CONNECT_TYPE_WIFI)
    }
    fun isCameraConnected(): Boolean {
        val type = InstaCameraManager.getInstance().cameraConnectedType
        return type != InstaCameraManager.CONNECT_TYPE_NONE
    }
    fun registerCameraChangedCallback(callback: ICameraChangedCallback) {
        InstaCameraManager.getInstance().registerCameraChangedCallback(callback)
    }
    fun unregisterCameraChangedCallback(callback: ICameraChangedCallback) {
        InstaCameraManager.getInstance().unregisterCameraChangedCallback(callback)
    }
    fun getCameraType(): String = InstaCameraManager.getInstance().cameraType
    fun getCameraVersion(): String = InstaCameraManager.getInstance().cameraVersion
    fun getCameraSerial(): String = InstaCameraManager.getInstance().cameraSerial

    fun closeCamera() {
        InstaCameraManager.getInstance().closeCamera()
    }
}