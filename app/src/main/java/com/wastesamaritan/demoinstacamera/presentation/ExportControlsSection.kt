package com.wastesamaritan.demoinstacamera.presentation

import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arashivision.sdkmedia.export.ExportImageParamsBuilder
import com.arashivision.sdkmedia.export.ExportUtils
import com.arashivision.sdkmedia.export.ExportVideoParamsBuilder
import com.arashivision.sdkmedia.export.IExportCallback
import com.arashivision.sdkmedia.work.WorkWrapper
import com.arashivision.sdkmedia.player.offset.OffsetType
import java.util.Locale

@Composable
fun ExportControlsSection(
    workWrapper: WorkWrapper,
    isImageFusion: Boolean,
    mStabType: Int,
    mOffsetType: OffsetType,
    mHDROutputPath: String,
    mPureShotOutputPath: String,
    mIsStitchHDRSuccessful: Boolean,
    mIsStitchPureShotSuccessful: Boolean,
    mBtnHDRChecked: Boolean,
    mBtnPureShotChecked: Boolean,
) {
    val EXPORT_DIR_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + "/SDK_DEMO_EXPORT/"
    var exportDialogState by remember { mutableStateOf<ExportDialogState?>(null) }
    var currentExportId by remember { mutableStateOf(-1) }

    fun stopExport() {
        if (currentExportId != -1) {
            ExportUtils.stopExport(currentExportId)
            currentExportId = -1
        }
    }

    fun exportVideoOriginal() {
        val builder = ExportVideoParamsBuilder()
            .setExportMode(ExportUtils.ExportMode.PANORAMA)
            .setTargetPath(EXPORT_DIR_PATH + System.currentTimeMillis() + ".mp4")
            .setImageFusion(isImageFusion)
            .setStabType(mStabType)
            .setOffsetType(mOffsetType)
            .setWidth(2048).setHeight(1024)
        currentExportId = ExportUtils.exportVideo(workWrapper, builder, object : IExportCallback {
            override fun onSuccess() {
                exportDialogState = ExportDialogState.Success(EXPORT_DIR_PATH)
                currentExportId = -1
            }
            override fun onFail(errorCode: Int, errorMsg: String?) {
                exportDialogState = ExportDialogState.Failure(errorCode, errorMsg)
                currentExportId = -1
            }
            override fun onCancel() {
                exportDialogState = ExportDialogState.Cancelled
                currentExportId = -1
            }
            override fun onProgress(progress: Float) {
                exportDialogState = ExportDialogState.Progress(progress)
            }
        })
    }

    fun exportImageOriginal() {
        val builder = ExportImageParamsBuilder()
            .setExportMode(ExportUtils.ExportMode.PANORAMA)
            .setImageFusion(isImageFusion)
            .setStabType(mStabType)
            .setOffsetType(mOffsetType)
            .setTargetPath(EXPORT_DIR_PATH + System.currentTimeMillis() + ".jpg")
        if (mIsStitchHDRSuccessful && mBtnHDRChecked) {
            builder.setUrlForExport(mHDROutputPath)
        } else if (mIsStitchPureShotSuccessful && mBtnPureShotChecked) {
            builder.setUrlForExport(mPureShotOutputPath)
        }
        currentExportId = ExportUtils.exportImage(workWrapper, builder, object : IExportCallback {
            override fun onSuccess() {
                exportDialogState = ExportDialogState.Success(EXPORT_DIR_PATH)
                currentExportId = -1
            }
            override fun onFail(errorCode: Int, errorMsg: String?) {
                exportDialogState = ExportDialogState.Failure(errorCode, errorMsg)
                currentExportId = -1
            }
            override fun onCancel() {
                exportDialogState = ExportDialogState.Cancelled
                currentExportId = -1
            }
            override fun onProgress(progress: Float) {
                exportDialogState = ExportDialogState.Progress(progress)
            }
        })
    }

    fun exportVideoThumbnail() {
        val builder = ExportImageParamsBuilder()
            .setExportMode(ExportUtils.ExportMode.SPHERE)
            .setTargetPath(EXPORT_DIR_PATH + System.currentTimeMillis() + ".jpg")
            .setWidth(512)
            .setHeight(512)
            .setStabType(mStabType)
            .setOffsetType(mOffsetType)
            .setImageFusion(isImageFusion)
        currentExportId = ExportUtils.exportVideoToImage(workWrapper, builder, object : IExportCallback {
            override fun onSuccess() {
                exportDialogState = ExportDialogState.Success(EXPORT_DIR_PATH)
                currentExportId = -1
            }
            override fun onFail(errorCode: Int, errorMsg: String?) {
                exportDialogState = ExportDialogState.Failure(errorCode, errorMsg)
                currentExportId = -1
            }
            override fun onCancel() {
                exportDialogState = ExportDialogState.Cancelled
                currentExportId = -1
            }
            override fun onProgress(progress: Float) {
                exportDialogState = ExportDialogState.Progress(progress)
            }
        })
    }

    fun exportImageThumbnail() {
        val builder = ExportImageParamsBuilder()
            .setExportMode(ExportUtils.ExportMode.SPHERE)
            .setTargetPath(EXPORT_DIR_PATH + System.currentTimeMillis() + ".jpg")
            .setWidth(512)
            .setHeight(512)
            .setImageFusion(isImageFusion)
            .setOffsetType(mOffsetType)
            .setStabType(mStabType)
        if (mIsStitchHDRSuccessful && mBtnHDRChecked) {
            builder.setUrlForExport(mHDROutputPath)
        } else if (mIsStitchPureShotSuccessful && mBtnPureShotChecked) {
            builder.setUrlForExport(mPureShotOutputPath)
        }
        currentExportId = ExportUtils.exportImage(workWrapper, builder, object : IExportCallback {
            override fun onSuccess() {
                exportDialogState = ExportDialogState.Success(EXPORT_DIR_PATH)
                currentExportId = -1
            }
            override fun onFail(errorCode: Int, errorMsg: String?) {
                exportDialogState = ExportDialogState.Failure(errorCode, errorMsg)
                currentExportId = -1
            }
            override fun onCancel() {
                exportDialogState = ExportDialogState.Cancelled
                currentExportId = -1
            }
            override fun onProgress(progress: Float) {
                exportDialogState = ExportDialogState.Progress(progress)
            }
        })
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = {
                if (workWrapper.isVideo) exportVideoOriginal() else exportImageOriginal()
                exportDialogState = ExportDialogState.Progress(0f)
            }) { Text("Export Original") }
            Button(onClick = {
                if (workWrapper.isVideo) exportVideoThumbnail() else exportImageThumbnail()
                exportDialogState = ExportDialogState.Progress(0f)
            }) { Text("Export Thumbnail") }
        }
        exportDialogState?.let { dialogState ->
            ExportProgressDialog(
                state = dialogState,
                onCancel = { stopExport() },
                onDismiss = { exportDialogState = null }
            )
        }
    }
}

sealed class ExportDialogState {
    data class Progress(val progress: Float) : ExportDialogState()
    data class Success(val exportDir: String) : ExportDialogState()
    data class Failure(val errorCode: Int, val errorMsg: String?) : ExportDialogState()
    object Cancelled : ExportDialogState()
}

@Composable
fun ExportProgressDialog(
    state: ExportDialogState,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (state is ExportDialogState.Success || state is ExportDialogState.Failure || state is ExportDialogState.Cancelled) {
                Button(onClick = onDismiss) { Text("OK") }
            }
        },
        dismissButton = {
            if (state is ExportDialogState.Progress) {
                Button(onClick = onCancel) { Text("Cancel") }
            }
        },
        title = {
            Text(
                when (state) {
                    is ExportDialogState.Progress -> "Exporting..."
                    is ExportDialogState.Success -> "Export Success"
                    is ExportDialogState.Failure -> "Export Failed"
                    is ExportDialogState.Cancelled -> "Export Cancelled"
                }
            )
        },
        text = {
            when (state) {
                is ExportDialogState.Progress -> Text("Progress: ${String.format(Locale.CHINA, "%.1f", state.progress * 100)}%")
                is ExportDialogState.Success -> Text("Exported to: ${state.exportDir}")
                is ExportDialogState.Failure -> Text("Error code: ${state.errorCode}\n${state.errorMsg ?: ""}")
                is ExportDialogState.Cancelled -> Text("Export was cancelled.")
            }
        }
    )
} 