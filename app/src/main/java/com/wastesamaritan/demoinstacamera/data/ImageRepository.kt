package com.wastesamaritan.demoinstacamera.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class ImageRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val dao = db.imageDao()

    fun getAllImages(): Flow<List<ImageEntity>> = dao.getAllImages()
    suspend fun insertImage(image: ImageEntity) = dao.insertImage(image)
    fun getImageById(id: Int) = db.imageDao().getImageById(id)
    suspend fun deleteImage(image: ImageEntity) = db.imageDao().deleteImage(image)
} 