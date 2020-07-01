package com.example.bikelicenseplates.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.bikelicenseplates.util.DetectorUtils
import com.example.bikelicenseplates.util.analyzeImage
import com.example.bikelicenseplates.view.GraphicOverlay
import com.example.bikelicenseplates.view.graphic.ObjectGraphic
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.coroutines.runBlocking

class ImageAnalyzer(context: Context, private val graphicsOverlay: GraphicOverlay) :
    ImageAnalysis.Analyzer {

    // 3: create an object detector instance
    val objectDetector = DetectorUtils.getObjectDetector(context)

    val textDetector = TextRecognition.getClient()

    private var needUpdateGraphicOverlayImageSourceInfo = true

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {

        if (needUpdateGraphicOverlayImageSourceInfo) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

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

        // 4: create an InputImage object from mediaImage specifying the correct rotation
        val inputImage =
            InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        /* 5: process the image adding
        * failure listener
        * success listener (update the screen)
        * completed listener - close the image proxy
        */
        runBlocking {
            val detectedObjects = objectDetector.analyzeImage(inputImage) {
                imageProxy.close()
            }
            graphicsOverlay.clear()
            detectedObjects.forEach { detectedObject ->
                graphicsOverlay.add(ObjectGraphic(graphicsOverlay, detectedObject))
                graphicsOverlay.postInvalidate()
            }
        }
    }

    companion object {
        private const val TAG = "ImageAnalyzer"
    }
}
