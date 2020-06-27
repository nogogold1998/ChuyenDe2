package com.example.bikelicenseplates.ui

import android.app.Application
import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * ViewModel for interacting with CameraX
 */
class CameraXPreviewViewModel(application: Application) : AndroidViewModel(application) {
    val processCameraProvider: LiveData<ProcessCameraProvider>
        get() = _processCameraProvider

    private val _processCameraProvider = MutableLiveData<ProcessCameraProvider>()

    init {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(application)
        cameraProviderFuture.addListener(
            Runnable {
                try {
                    _processCameraProvider.value = cameraProviderFuture.get()
                } catch (e: Exception) {
                    // Handle any errors (including cancellation)
                    Log.e(TAG, "Unhandled exception", e)
                }
            },
            ContextCompat.getMainExecutor(application)
        )
    }

    companion object {
        private const val TAG = "CameraXViewModel"
    }
}
