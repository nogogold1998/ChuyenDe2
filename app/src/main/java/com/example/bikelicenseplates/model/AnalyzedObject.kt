package com.example.bikelicenseplates.model

import android.graphics.Bitmap
import com.google.mlkit.vision.objects.DetectedObject

data class AnalyzedObject(val bitmap: Bitmap, val detectedObject: DetectedObject, val text: String)
