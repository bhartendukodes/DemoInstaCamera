package com.wastesamaritan.demoinstacamera.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "images")
data class ImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filePath: String,
    val yaw: Float? = null,
    val pitch: Float? = null,
    val roll: Float? = null,
    val rotationMatrix: String? = null,
    val horizontalFov: Float? = null,
    val comment: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) 