package com.example.bikelicenseplates

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bikelicenseplates.model.AnalyzedObject
import com.example.bikelicenseplates.util.DetectorUtils
import com.example.bikelicenseplates.util.analyzeCroppedPlate
import com.example.bikelicenseplates.util.analyzeImage
import com.example.bikelicenseplates.util.rotateAndCrop
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val textDetector = TextRecognition.getClient()

    val analyzedObjects: LiveData<List<AnalyzedObject>> get() = _analyzedObjects

    private val _analyzedObjects =
        MutableLiveData<List<AnalyzedObject>>().apply { value = emptyList() }

    @SuppressLint("UnsafeExperimentalUsageError")
    fun analyzeCapturedImage(inputImage: InputImage, bitmap: Bitmap, context: Context) =
        viewModelScope.launch {
            val objectDetector =
                DetectorUtils.getObjectDetector(
                    context,
                    CustomObjectDetectorOptions.SINGLE_IMAGE_MODE
                )

            _analyzedObjects.value = emptyList()

            val result = mutableListOf<AnalyzedObject>()

            val detectedObjects = objectDetector.analyzeImage(inputImage)
            detectedObjects.forEach { detectedObject ->
                val rect = detectedObject.boundingBox
                val rotationDegrees = inputImage.rotationDegrees
                val croppedBitmap = bitmap.rotateAndCrop(
                    rotationDegrees,
                    rect.left, rect.top,
                    rect.width(), rect.height()
                )
                val text = textDetector.analyzeCroppedPlate(croppedBitmap, detectedObject)
                result.add(AnalyzedObject(croppedBitmap, detectedObject, text.text.trim()))
            }
            _analyzedObjects.value = result
        }

    class Factory : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel() as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
