package com.wastesamaritan.demoinstacamera.data

import com.arashivision.sdkcamera.camera.InstaCameraManager
import com.arashivision.sdkmedia.work.WorkUtils
import com.arashivision.sdkmedia.work.WorkWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CameraMediaRepository {
    suspend fun getAllCameraWorks(): List<WorkWrapper> = withContext(Dispatchers.IO) {
        val manager = InstaCameraManager.getInstance()
        val httpPrefix = manager.cameraHttpPrefix
        val infoMap = manager.cameraInfoMap
        val allUrlList = manager.allUrlList
        val rawUrlList = manager.rawUrlList
        val allInsDataList = manager.allInsDataList
        if (httpPrefix == null || infoMap == null || allUrlList == null || rawUrlList == null || allInsDataList == null) {
            return@withContext emptyList<WorkWrapper>()
        }
        WorkUtils.getAllCameraWorks(
            httpPrefix,
            infoMap,
            allUrlList,
            rawUrlList,
            allInsDataList
        ) ?: emptyList()
    }

    fun filterImages(works: List<WorkWrapper>): List<WorkWrapper> =
        works.filter { it.isPhoto }

    fun filterVideos(works: List<WorkWrapper>): List<WorkWrapper> =
        works.filter { it.isVideo }
}