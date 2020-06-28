package com.example.bikelicenseplates.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import com.example.bikelicenseplates.R
import com.example.bikelicenseplates.databinding.FragmentCameraxPreviewBinding
import com.example.bikelicenseplates.objectdetector.ObjectGraphic
import com.example.bikelicenseplates.view.GraphicOverlay
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions

class CameraXPreviewFragment : Fragment() {

    private var _binding: FragmentCameraxPreviewBinding? = null
    private val binding get() = _binding!!
    private val cameraXViewModel: CameraXPreviewViewModel by lazy {
        ViewModelProvider(
            viewModelStore,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        ).get(CameraXPreviewViewModel::class.java)
    }

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var cameraSelector: CameraSelector? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentCameraxPreviewBinding.inflate(inflater)

        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        cameraXViewModel.processCameraProvider.observe(viewLifecycleOwner, Observer { provider ->
            cameraProvider = provider
            bindPreviewUseCase()
            bindAnalysisUseCase()
        })

        // return inflater.inflate(R.layout.fragment_camerax_preview, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonSettings.setOnClickListener(
            Navigation.createNavigateOnClickListener(
                CameraXPreviewFragmentDirections.actionCameraXPreviewToSettings()
            )
        )
    }

    override fun onResume() {
        super.onResume()
        bindPreviewUseCase()
        bindAnalysisUseCase()
    }

    override fun onPause() {
        super.onPause()
        // todo stop image processor
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // todo stop image processor
    }

    private fun bindPreviewUseCase() {
        // todo kiem tra neu can lay LiveViewPort
        if (cameraProvider == null) {
            Log.d(TAG, "bindPreviewUseCase: cameraProvider == null")
            return
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }

        previewUseCase = Preview.Builder().apply {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val prefKey = getString(R.string.pref_key_camerax_target_analysis_size)
            val size: Size? = Size.parseSize(sharedPreferences.getString(prefKey, null))
            if (size != null) {
                setTargetResolution(size)
            }
        }.build()
        previewUseCase!!.setSurfaceProvider(binding.previewView.createSurfaceProvider())
        cameraProvider!!.bindToLifecycle(viewLifecycleOwner, cameraSelector!!, previewUseCase)
    }

    private fun bindAnalysisUseCase() {
        if (cameraProvider == null) {
            Log.d(TAG, "bindAnalysisUseCase: cameraProvider == null")
            return
        }
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }

        analysisUseCase = ImageAnalysis.Builder().build()

        analysisUseCase?.setAnalyzer(
            ContextCompat.getMainExecutor(requireContext()),
            ImageAnalyzer(requireContext(), binding.graphicOverlay)
        )

        cameraProvider!!.bindToLifecycle(viewLifecycleOwner, cameraSelector!!, analysisUseCase)
    }

    private class ImageAnalyzer(context: Context, private val graphicsOverlay: GraphicOverlay) :
        ImageAnalysis.Analyzer {
        private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        // todo 3: setup the local model
        private val localModel = LocalModel.Builder()
            // .setAssetFilePath("aiy_vision_classifier_birds_V1_2.tflite")
            .setAssetFilePath("bird_classifier.tflite")
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
            CustomObjectDetectorOptions.Builder(localModel).apply {
                setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                if (sharedPreferences.getBoolean(
                        context.getString(R.string.pref_key_object_detector_enable_multiple_objects),
                        false
                    )
                ) {
                    enableMultipleObjects()
                }
                if (sharedPreferences.getBoolean(
                        context.getString(R.string.pref_key_object_detector_enable_classification),
                        true
                    )
                ) {
                    enableClassification()
                }
                val threshold = sharedPreferences.getInt(
                    context.getString(R.string.pref_key_object_detector_classification_confidence_threshold),
                    0
                )
                setClassificationConfidenceThreshold(threshold.toFloat() / 100)
                // setMaxPerObjectLabelCount(1)
            }
                .build()

        // todo 5: create an object detector instance
        private val objectDetector = ObjectDetection.getClient(customObjectDetectorOptions)

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

            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                // todo 6: create an InputImage object from mediaImage specifying the correct rotation
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                /* todo 7: process the image adding
                * failure listener
                * success listener (update the screen)
                * completed listener - close the image proxy
                */
                objectDetector.process(image)
                    .addOnFailureListener { Log.d(TAG, "analyze: ${it.printStackTrace()}") }
                    .addOnSuccessListener {
                        graphicsOverlay.clear()
                        for (detectedObject: DetectedObject in it) {
                            // graphicsOverlay.addObjectGraphic(graphicsOverlay, detectedObject)
                            graphicsOverlay.add(ObjectGraphic(graphicsOverlay, detectedObject))
                        }
                        graphicsOverlay.postInvalidate()
                    }
                    .addOnCompleteListener { imageProxy.close() }
            }
        }

        companion object {
            private const val TAG = "ImageAnalyzer"
        }
    }

    companion object {
        private const val TAG = "CameraXPreviewFragment"
    }
}
