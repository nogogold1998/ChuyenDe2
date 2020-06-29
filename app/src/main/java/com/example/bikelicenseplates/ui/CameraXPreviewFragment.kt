package com.example.bikelicenseplates.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.bikelicenseplates.databinding.FragmentCameraxPreviewBinding
import com.example.bikelicenseplates.util.PreferenceUtils
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
    private var cameraSelector: CameraSelector? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

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

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonSettings.setOnClickListener(
            Navigation.createNavigateOnClickListener(
                CameraXPreviewFragmentDirections.actionCameraXPreviewToSettings()
            )
        )

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onResume() {
        super.onResume()
        // bindPreviewUseCase()
        // bindAnalysisUseCase()
    }

    override fun onPause() {
        super.onPause()
        // todo stop image processor
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        // Shut down our background executor
        cameraExecutor.shutdown()
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
            val size = PreferenceUtils.getTargetAnalysisSize(requireContext())
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

        analysisUseCase = ImageAnalysis.Builder().apply {
            val size = PreferenceUtils.getTargetAnalysisSize(requireContext())
            if (size != null) {
                setTargetResolution(size)
            }
        }.build()

        analysisUseCase?.setAnalyzer(
            cameraExecutor,
            ImageAnalyzer(requireContext(), binding.graphicOverlay).also {
                lifecycle.addObserver(it.objectDetector)
                lifecycle.addObserver(it.textDetector)
            }
        )

        cameraProvider!!.bindToLifecycle(viewLifecycleOwner, cameraSelector!!, analysisUseCase)
    }

    companion object {
        private const val TAG = "CameraXPreviewFragment"
    }
}
