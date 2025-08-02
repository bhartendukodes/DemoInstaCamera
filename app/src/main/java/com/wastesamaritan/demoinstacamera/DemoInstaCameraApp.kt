package com.wastesamaritan.demoinstacamera

import android.app.Application
import com.arashivision.sdkcamera.InstaCameraSDK
import com.arashivision.sdkmedia.InstaMediaSDK

class DemoInstaCameraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        InstaCameraSDK.init(this)
        InstaMediaSDK.init(this)
    }
} 