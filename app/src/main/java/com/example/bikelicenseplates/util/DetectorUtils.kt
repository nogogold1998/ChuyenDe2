package com.example.bikelicenseplates.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognizer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "DetectorUtils"

object DetectorUtils {
    private const val CUSTOM_MODEL_ASSET_PATH = "bird_classifier.tflite"
    fun getObjectDetector(context: Context): ObjectDetector {
        // 1: setup the local model
        val localModel = LocalModel.Builder()
            // .setAssetFilePath("aiy_vision_classifier_birds_V1_2.tflite")
            // .setAssetFilePath("automl_flowers.tflite")
            .setAssetFilePath(CUSTOM_MODEL_ASSET_PATH)
            .build()

        // 2: create an option object
        val customObjectDetectorOptions =
            CustomObjectDetectorOptions.Builder(localModel)
                .apply {
                    setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
                    if (PreferenceUtils.getEnableMultipleObjects(context)) {
                        enableMultipleObjects()
                    }
                    if (PreferenceUtils.getEnableClassification(context)) {
                        enableClassification()
                    }
                    val threshold = PreferenceUtils.getClassificationConfidenceThreshold(context)
                    setClassificationConfidenceThreshold(threshold)
                    setMaxPerObjectLabelCount(1)
                }
                .build()

        // 3: create an object detector instance
        return ObjectDetection.getClient(customObjectDetectorOptions)
    }

}

suspend fun ObjectDetector.analyzeImage(inputImage: InputImage,
    completeCallback: () -> Unit
): List<DetectedObject> = suspendCancellableCoroutine { continuation ->
    this.process(inputImage)
        .addOnFailureListener {
            continuation.resumeWithException(it)
        }.addOnSuccessListener {
            continuation.resume(it)
        }.addOnCompleteListener { completeCallback.invoke() }
}

suspend fun TextRecognizer.analyzeCroppedPlate(
    bitmap: Bitmap,
    detectedObject: DetectedObject,
    rotationDegrees: Int,
    completeCallback: (() -> Unit)? = null
) = suspendCancellableCoroutine<Text> { continuation ->
    val rect = detectedObject.boundingBox
    val croppedBitmap = bitmap.rotateAndCrop(
        rotationDegrees,
        rect.top, rect.left,
        rect.height(), rect.width()
    )
    try {
        this.process(InputImage.fromBitmap(croppedBitmap, 0))
            .addOnFailureListener { exc ->
                Log.d(TAG, "labels: ${detectedObject.labels}, ${exc.printStackTrace()}")
                continuation.resumeWithException(exc)
            }.addOnSuccessListener { text ->
                Log.d(TAG, "labels: ${detectedObject.labels}, text: ${text.text}")
                continuation.resume(text)
            }.addOnCompleteListener { completeCallback?.invoke() }
    } catch (e: MlKitException) {
        Log.e(TAG, "analyzePlate: ", e)
    }
}
