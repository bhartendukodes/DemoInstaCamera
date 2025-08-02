package com.wastesamaritan.demoinstacamera.presentation

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import com.arashivision.insta360.basecamera.camera.BaseCamera
import com.arashivision.insta360.basecamera.camera.CameraType
import com.arashivision.sdkcamera.camera.InstaCameraManager
import com.arashivision.sdkcamera.camera.callback.IPreviewStatusListener
import com.arashivision.sdkcamera.camera.resolution.PreviewStreamResolution
import com.arashivision.sdkmedia.player.capture.InstaCapturePlayerView
import com.arashivision.sdkmedia.player.listener.PlayerViewListener
import com.wastesamaritan.demoinstacamera.R
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ToggleButton
import com.arashivision.sdkmedia.player.config.InstaStabType
import com.wastesamaritan.demoinstacamera.base.BaseObserveCameraActivity
import com.wastesamaritan.demoinstacamera.util.PreviewParamsUtil
import com.arashivision.sdkmedia.player.capture.CaptureParamsBuilder
import com.wastesamaritan.demoinstacamera.util.SDKErrorHandler

class PreviewActivity : BaseObserveCameraActivity(), IPreviewStatusListener {

    private lateinit var layoutContent: ViewGroup
    private lateinit var playerView: InstaCapturePlayerView
    private lateinit var btnSwitch: ToggleButton
    private lateinit var rbNormal: RadioButton
    private lateinit var rbFisheye: RadioButton
    private lateinit var rbPerspective: RadioButton
    private lateinit var spinnerResolution: Spinner
    private lateinit var spinnerStabType: Spinner
    private var currentResolution: PreviewStreamResolution? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_preview)
        bindViews()
        InstaCameraManager.getInstance().setPreviewStatusChangedListener(this)
        
        Log.d(TAG, "Starting preview automatically")
        val cameraType = InstaCameraManager.getInstance().cameraType
        val isConnected = InstaCameraManager.getInstance().cameraConnectedType != InstaCameraManager.CONNECT_TYPE_NONE
        Log.d(TAG, "Camera type: $cameraType, Connected: $isConnected")
        
        if (isConnected) {
            InstaCameraManager.getInstance().startPreviewStream()
        } else {
            Log.w(TAG, "Camera not connected, cannot start preview")
        }
    }

    private fun bindViews() {
        layoutContent = findViewById(R.id.layout_content)
        playerView = findViewById(R.id.player_capture)
        playerView.setLifecycle(lifecycle)

        btnSwitch = findViewById(R.id.btn_switch)
        btnSwitch.setOnClickListener {
            if (btnSwitch.isChecked) {
                if (currentResolution == null) {
                    InstaCameraManager.getInstance().startPreviewStream()
                } else {
                    InstaCameraManager.getInstance().startPreviewStream(currentResolution)
                }
            } else {
                InstaCameraManager.getInstance().closePreviewStream()
            }
        }

        rbNormal = findViewById(R.id.rb_normal)
        rbFisheye = findViewById(R.id.rb_fisheye)
        rbPerspective = findViewById(R.id.rb_perspective)
        val radioGroup = findViewById<RadioGroup>(R.id.rg_preview_mode)
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_normal -> {
                    playerView.switchNormalMode()
                }
                R.id.rb_fisheye -> {
                    playerView.switchFisheyeMode()
                }
                R.id.rb_perspective -> {
                    playerView.switchPerspectiveMode()
                }
            }
        }

        spinnerResolution = findViewById(R.id.spinner_resolution)
        val adapter1 = ArrayAdapter<PreviewStreamResolution>(this, android.R.layout.simple_spinner_dropdown_item)
        adapter1.addAll(InstaCameraManager.getInstance().getSupportedPreviewStreamResolution(InstaCameraManager.PREVIEW_TYPE_NORMAL))
        spinnerResolution.adapter = adapter1
        spinnerResolution.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentResolution = adapter1.getItem(position)
                InstaCameraManager.getInstance().closePreviewStream()
                InstaCameraManager.getInstance().startPreviewStream(currentResolution)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerStabType = findViewById(R.id.spinner_stab_type)
        val adapter2 = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item)
        adapter2.add(getString(R.string.stab_type_off))
        adapter2.add(getString(R.string.stab_type_panorama))
        adapter2.add(getString(R.string.stab_type_calibrate_horizon))
        adapter2.add(getString(R.string.stab_type_footage_motion_smooth))
        spinnerStabType.adapter = adapter2
        spinnerStabType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position == 0 && playerView.isStabEnabled || position != 0 && !playerView.isStabEnabled) {
                    InstaCameraManager.getInstance().closePreviewStream()
                    if (currentResolution == null) {
                        InstaCameraManager.getInstance().startPreviewStream()
                    } else {
                        if (CameraType.X4 == CameraType.getForType(InstaCameraManager.getInstance().cameraType)) {
                            val firstStreamResolution = InstaCameraManager.getInstance().curFirstStreamResolution
                            if (firstStreamResolution != null) {
                                currentResolution = firstStreamResolution
                            }
                        }
                        InstaCameraManager.getInstance().startPreviewStream(currentResolution)
                    }
                } else {
                    playerView.setStabType(getStabType())
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        
        val isNanoS = TextUtils.equals(InstaCameraManager.getInstance().cameraType, CameraType.NANOS.type)
        spinnerStabType.visibility = if (isNanoS) View.GONE else View.VISIBLE
    }

    private fun getStabType(): Int {
        return when (spinnerStabType.selectedItemPosition) {
            0 -> InstaStabType.STAB_TYPE_OFF
            1 -> InstaStabType.STAB_TYPE_PANORAMA
            2 -> InstaStabType.STAB_TYPE_CALIBRATE_HORIZON
            3 -> InstaStabType.STAB_TYPE_FOOTAGE_MOTION_SMOOTH
            else -> InstaStabType.STAB_TYPE_OFF
        }
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            InstaCameraManager.getInstance().setPreviewStatusChangedListener(null)
            InstaCameraManager.getInstance().closePreviewStream()
            playerView.destroy()
        }
    }

    override fun onOpening() {
        Log.d(TAG, "Preview Opening")
        btnSwitch.isChecked = true
    }

    override fun onOpened() {
        Log.d(TAG, "Preview Opened")
        InstaCameraManager.getInstance().setStreamEncode()
        playerView.setPlayerViewListener(object : PlayerViewListener {
            override fun onLoadingFinish() {
                Log.d(TAG, "Player Loading Finished")
                InstaCameraManager.getInstance().setPipeline(playerView.pipeline)
            }
            override fun onReleaseCameraPipeline() {
                Log.d(TAG, "Releasing Camera Pipeline")
                InstaCameraManager.getInstance().setPipeline(null)
            }
        })
        Log.d(TAG, "Preparing player with params")
        playerView.prepare(createParams())
        Log.d(TAG, "Starting player")
        
        // Add comprehensive error handling for SDK issues with retry mechanism
        startPlayerWithRetry()
    }
    
    private fun startPlayerWithRetry(maxRetries: Int = 3) {
        var retryCount = 0
        
        fun attemptPlay() {
            try {
                playerView.play()
                playerView.keepScreenOn = true
                Log.d(TAG, "Preview setup complete")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting player preview (attempt ${retryCount + 1}): ${e.message}", e)
                val handled = SDKErrorHandler.handlePlayerPlayError(playerView, e)
                if (handled && retryCount < maxRetries) {
                    retryCount++
                    Log.i(TAG, "Retrying player start (attempt $retryCount)")
                    Thread.sleep(500)
                    attemptPlay()
                } else {
                    Log.e(TAG, "Failed to start player after $maxRetries attempts")
                    SDKErrorHandler.logSDKDiagnostics()
                }
            }
        }
        attemptPlay()
    }


    private fun createParams(): CaptureParamsBuilder {
        val builder = PreviewParamsUtil.getCaptureParamsBuilder()
            .setStabType(getStabType())
            .setStabEnabled(spinnerStabType.selectedItemPosition != 4)

        currentResolution?.let {
            builder.setResolutionParams(it.width, it.height, it.fps)
        }
        
        return builder
    }

    override fun onIdle() {
        playerView.destroy()
        playerView.keepScreenOn = false
    }

    override fun onError() {
        btnSwitch.isChecked = false
    }

    override fun onCameraPreviewStreamParamsChanged(baseCamera: BaseCamera, isPreviewStreamParamsChanged: Boolean) {
        Log.d(TAG, "liveStreamParams isPreviewStreamParamsChanged: $isPreviewStreamParamsChanged")
        if (!isPreviewStreamParamsChanged) {
            Log.d(TAG, "liveStreamParams has nothing changed, ignored")
            return
        }
        
        val curWindowCropInfo = playerView.windowCropInfo
        val cameraWindowCropInfo = PreviewParamsUtil.windowCropInfoConversion(baseCamera.windowCropInfo)
        
        if (playerView.isPlaying && curWindowCropInfo != null && cameraWindowCropInfo != null) {
            if (curWindowCropInfo.srcWidth != cameraWindowCropInfo.srcWidth
                || curWindowCropInfo.srcHeight != cameraWindowCropInfo.srcHeight
                || curWindowCropInfo.offsetX != cameraWindowCropInfo.offsetX
                || curWindowCropInfo.offsetY != cameraWindowCropInfo.offsetY) {
                
                Log.d(TAG, "liveStreamParams changed windowCropInfo: ${baseCamera.windowCropInfo}")
                playerView.windowCropInfo = cameraWindowCropInfo
            }
        }
    }
}