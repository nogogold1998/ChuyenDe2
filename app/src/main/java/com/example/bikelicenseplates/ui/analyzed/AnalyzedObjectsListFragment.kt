package com.example.bikelicenseplates.ui.analyzed

import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.bikelicenseplates.MainViewModel
import com.example.bikelicenseplates.R
import com.example.bikelicenseplates.databinding.FragmentAnalyzedObjectsListBinding
import com.example.bikelicenseplates.model.AnalyzedObject
import com.google.mlkit.vision.objects.DetectedObject

class AnalyzedObjectsListFragment : Fragment() {
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding = FragmentAnalyzedObjectsListBinding.inflate(inflater, container, false)

        val adapter = AnalyzedObjectsAdapter()
        val bitmap = BitmapFactory.decodeResource(requireContext().resources, R.drawable.android)
        val detectedObject =
            DetectedObject(Rect(1, 2, 3, 4), null, listOf(DetectedObject.Label("Label", 0.8f, 1)))
        val list = listOf(
            AnalyzedObject(bitmap, detectedObject, "Hello"),
            AnalyzedObject(bitmap, detectedObject, "World")
        )
        adapter.submitList(list)

        binding.recyclerView.run {
            this.adapter = adapter
        }

        mainViewModel.analyzedObjects.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                binding.loading.visibility = if(it.isEmpty()) View.VISIBLE else View.GONE
                adapter.submitList(it)
            }
        })
        return binding.root
    }
}

