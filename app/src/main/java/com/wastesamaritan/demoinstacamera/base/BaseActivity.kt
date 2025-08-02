package com.wastesamaritan.demoinstacamera.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initActivity()
    }
    
    protected open fun initActivity() {
        // Override in subclasses if needed
    }
} 