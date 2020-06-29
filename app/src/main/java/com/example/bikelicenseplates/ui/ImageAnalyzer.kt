package com.example.bikelicenseplates.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.bikelicenseplates.util.PreferenceUtils
import com.example.bikelicenseplates.util.rotateAndCrop
import com.example.bikelicenseplates.util.toBitmap
import com.example.bikelicenseplates.view.GraphicOverlay
import com.example.bikelicenseplates.view.graphic.ObjectGraphic
import com.example.bikelicenseplates.view.graphic.TextGraphic
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition

class ImageAnalyzer(context: Context, private val graphicsOverlay: GraphicOverlay) :
    ImageAnalysis.Analyzer {

    // todo 3: setup the local model
    private val localModel = LocalModel.Builder()
        // .setAssetFilePath("aiy_vision_classifier_birds_V1_2.tflite")
        // .setAssetFilePath("bird_classifier.tflite")
        .setAssetFilePath("automl_flowers.tflite")
        .build()

    /* todo 4: create an options object using a Building by specifying:
    * local model
    * stream mode
    * enable classification
    * establish confidence threshold (at least 50%)
    * maximum label count per object (setting to 1 because we can only really display one)
    * build the option object
    */
    private val customObjectDetectorOptions =
        CustomObjectDetectorOptions.Builder(
            localModel
        ).apply {
            setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
            if (PreferenceUtils.getEnableMultipleObjects(context)) {
                enableMultipleObjects()
            }
            if (PreferenceUtils.getEnableClassification(context)) {
                enableClassification()
            }
            val threshold = PreferenceUtils.getClassificationConfidenceThreshold(context)
            setClassificationConfidenceThreshold(threshold.toFloat() / 100)
            setMaxPerObjectLabelCount(1)
        }.build()

    // todo 5: create an object detector instance
    val objectDetector = ObjectDetection.getClient(customObjectDetectorOptions)

    val textDetector = TextRecognition.getClient()

    var needUpdateGraphicOverlayImageSourceInfo = true

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {

        // todo place bug here to see imageProxy infos
        if (needUpdateGraphicOverlayImageSourceInfo) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            // todo inject isImageFlipped
            if (rotationDegrees == 0 || rotationDegrees == 180) {
                graphicsOverlay.setImageSourceInfo(
                    imageProxy.width, imageProxy.height, false
                )
            } else {
                graphicsOverlay.setImageSourceInfo(
                    imageProxy.height, imageProxy.width, false
                )
            }
            needUpdateGraphicOverlayImageSourceInfo = false
        }

        val mediaImage = imageProxy.image ?: return
        val bitmap = mediaImage.toBitmap()

        // todo 6: create an InputImage object from mediaImage specifying the correct rotation
        val inputImage =
            InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        /* todo 7: process the image adding
        * failure listener
        * success listener (update the screen)
        * completed listener - close the image proxy
        */
        objectDetector.process(inputImage)
            .addOnFailureListener {
                Log.d(
                    TAG,
                    "analyze object: ${it.printStackTrace()}"
                )
            }
            .addOnSuccessListener { it ->
                graphicsOverlay.clear()
                for (detectedObject: DetectedObject in it) {
                    graphicsOverlay.add(
                        ObjectGraphic(graphicsOverlay, detectedObject)
                    )
                    // todo pipeline text detection
                    val rect = detectedObject.boundingBox
                    val croppedBitmap = bitmap.rotateAndCrop(
                        inputImage.rotationDegrees,
                        rect.top, rect.left,
                        rect.height(), rect.width()
                    )
                    textDetector.process(InputImage.fromBitmap(croppedBitmap, 0))
                        .addOnFailureListener { exc ->
                            Log.d(
                                TAG,
                                "analyze text: ${detectedObject.labels[0].text}, ${exc.printStackTrace()}"
                            )
                        }.addOnSuccessListener { text ->
                        Log.d(
                            TAG,
                            "analyze text: ${detectedObject.labels[0].text}, text: ${text.text}"
                        )
                        graphicsOverlay.add(TextGraphic(graphicsOverlay, text, rect))
                    }
                }
                graphicsOverlay.postInvalidate()
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    companion object {
        private const val TAG = "ImageAnalyzer"
    }
}
