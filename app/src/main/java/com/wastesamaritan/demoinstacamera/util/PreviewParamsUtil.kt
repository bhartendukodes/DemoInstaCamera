package com.wastesamaritan.demoinstacamera.util

import com.arashivision.graphicpath.render.source.AssetInfo
import com.arashivision.onecamera.camerarequest.WindowCropInfo
import com.arashivision.sdkcamera.camera.InstaCameraManager
import com.arashivision.sdkmedia.player.capture.CaptureParamsBuilder

object PreviewParamsUtil {
    
    /**
     * Get capture params builder with basic camera parameters
     * Using CaptureParamsBuilder with proper OffsetData handling
     */
    fun getCaptureParamsBuilder(): CaptureParamsBuilder {
        val cameraType = InstaCameraManager.getInstance().cameraType
        val mediaOffset = InstaCameraManager.getInstance().mediaOffset
        val isSelfie = InstaCameraManager.getInstance().isCameraSelfie
        
        return CaptureParamsBuilder()
            .setCameraType(cameraType)
            .setMediaOffset(mediaOffset ?: "")
            .setCameraSelfie(isSelfie)
            .setLive(false)
    }
    
    /**
     * Convert window crop info from camera format to player format
     */
    fun windowCropInfoConversion(cameraWindowCropInfo: WindowCropInfo?): com.arashivision.insta360.basemedia.asset.WindowCropInfo? {
        return cameraWindowCropInfo?.let { info ->
            com.arashivision.insta360.basemedia.asset.WindowCropInfo().apply {
                srcWidth = info.srcWidth
                srcHeight = info.srcHeight
                offsetX = info.offsetX
                offsetY = info.offsetY
            }
        }
    }
} 