package com.example.bikelicenseplates.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.example.bikelicenseplates.R
import com.example.bikelicenseplates.databinding.FragmentCameraxPreviewBinding
import com.example.bikelicenseplates.util.DetectorUtils
import com.example.bikelicenseplates.util.PreferenceUtils
import com.example.bikelicenseplates.util.analyzeCroppedPlate
import com.example.bikelicenseplates.util.analyzeImage
import com.example.bikelicenseplates.util.toBitmap
import com.example.bikelicenseplates.view.graphic.ObjectGraphic
import com.example.bikelicenseplates.view.graphic.TextGraphic
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
    private lateinit var cameraSelector: CameraSelector
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var previewUseCase: Preview? = null
    private var analyzeUseCase: ImageAnalysis? = null
    private var captureUseCase: ImageCapture? = null

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentCameraxPreviewBinding.inflate(inflater)

        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        cameraXViewModel.processCameraProvider.observe(viewLifecycleOwner, Observer { provider ->
            cameraProvider = provider
            bindUseCases()
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonSettings.setOnClickListener(
            Navigation.createNavigateOnClickListener(
                CameraXPreviewFragmentDirections.actionCameraXPreviewToSettings()
            )
        )

        binding.buttonCapture.setOnClickListener {
            if (binding.imageView.visibility == View.VISIBLE) {
                binding.imageView.visibility = View.GONE
                binding.buttonCapture.setBackgroundResource(R.drawable.ic_shutter)

                bindUseCases()
            } else {
                binding.buttonCapture.isEnabled = false
                captureUseCase!!.takePicture(cameraExecutor,
                    object : ImageCapture.OnImageCapturedCallback() {
                        @SuppressLint("UnsafeExperimentalUsageError")
                        override fun onCaptureSuccess(imageProxy: ImageProxy) {
                            imageProxy.image?.let {
                                val bitmap = it.toBitmap()
                                analyzeCapturedImage(imageProxy, bitmap)
                                requireActivity().runOnUiThread {
                                    binding.imageView.run {
                                        setImageBitmap(bitmap)
                                        visibility = View.VISIBLE
                                    }
                                    binding.buttonCapture.run {
                                        setBackgroundResource(R.drawable.ic_cancel)
                                        isEnabled = true
                                    }
                                    cameraProvider!!.unbindAll()
                                }
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            super.onError(exception)
                            Log.e(TAG, "capture failed: ", exception)
                            requireActivity().runOnUiThread {
                                Toast.makeText(
                                    requireContext(), "Capture failed", Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    })
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        // Shut down our background executor
        cameraExecutor.shutdown()
    }

    private fun bindUseCases() {
        if (cameraProvider == null) {
            Log.d(TAG, "bindUseCases: cameraProvider == null")
            return
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }

        previewUseCase = Preview.Builder().apply {
            val size = PreferenceUtils.getTargetAnalysisSize(requireContext())
            setTargetRotation(binding.previewView.display.rotation)
            if (size != null) {
                setTargetResolution(size)
            }
        }.build()
        previewUseCase!!.setSurfaceProvider(binding.previewView.createSurfaceProvider())

        captureUseCase = ImageCapture.Builder().apply {
            setTargetRotation(binding.previewView.display.rotation)
            val size = PreferenceUtils.getTargetAnalysisSize(requireContext())
            if (size != null) {
                setTargetResolution(Size(1280, 960))
            }
            setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        }.build()


        if (analyzeUseCase != null) {
            cameraProvider!!.unbind(analyzeUseCase)
        }

        analyzeUseCase = ImageAnalysis.Builder().apply {
            val size = PreferenceUtils.getTargetAnalysisSize(requireContext())
            if (size != null) {
                setTargetResolution(size)
            }
        }.build()

        analyzeUseCase?.setAnalyzer(
            cameraExecutor,
            ImageAnalyzer(requireContext(), binding.graphicsOverlay).also {
                lifecycle.addObserver(it.objectDetector)
            }
        )

        // camera provides access to CameraControl & CameraInfo
        camera = cameraProvider!!.bindToLifecycle(
            viewLifecycleOwner, cameraSelector,
            previewUseCase, analyzeUseCase, captureUseCase
        )
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    fun analyzeCapturedImage(imageProxy: ImageProxy, bitmap: Bitmap) =
        lifecycleScope.launchWhenResumed {
            val objectDetector = DetectorUtils.getObjectDetector(requireContext())
            val textDetector = TextRecognition.getClient()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            if (rotationDegrees == 0 || rotationDegrees == 180) {
                binding.graphicsOverlay.setImageSourceInfo(
                    imageProxy.width, imageProxy.height, false
                )
            } else {
                binding.graphicsOverlay.setImageSourceInfo(
                    imageProxy.height, imageProxy.width, false
                )
            }
            imageProxy.image?.let { mediaImage ->
                // val bitmap = mediaImage.toBitmap()

                // create an InputImage object from mediaImage specifying the correct rotation
                val inputImage =
                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                val detectedObjects = objectDetector.analyzeImage(inputImage) {
                    imageProxy.close()
                }
                binding.graphicsOverlay.clear()
                detectedObjects.forEach { detectedObject ->
                    val rect = detectedObject.boundingBox
                    val text = async(Dispatchers.Default) {
                        textDetector.analyzeCroppedPlate(
                            bitmap,
                            detectedObject,
                            inputImage.rotationDegrees
                        )
                    }
                    binding.graphicsOverlay.run {
                        add(ObjectGraphic(binding.graphicsOverlay, detectedObject))
                        add(TextGraphic(binding.graphicsOverlay, text.await(), rect))
                        postInvalidate()
                    }
                }
            }
        }

    companion object {
        private const val TAG = "CameraXPreviewFragment"
    }
}
