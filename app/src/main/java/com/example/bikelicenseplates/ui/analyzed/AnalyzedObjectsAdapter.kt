package com.example.bikelicenseplates.ui.analyzed

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bikelicenseplates.databinding.ItemAnalyzedObjectBinding
import com.example.bikelicenseplates.model.AnalyzedObject

class AnalyzedObjectsAdapter :
    ListAdapter<AnalyzedObject, AnalyzedObjectsAdapter.ViewHolder>(
        diffUtils
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding =
            ItemAnalyzedObjectBinding.inflate(
                inflater,
                parent,
                false
            )
        return ViewHolder(
            binding
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val analyzedObject = getItem(position)
        holder.bind(analyzedObject)
    }

    class ViewHolder(private val binding: ItemAnalyzedObjectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(analyzedObject: AnalyzedObject) {
            binding.resultImage.setImageBitmap(analyzedObject.bitmap)
            binding.resultText.text = analyzedObject.text.replace('\n', ' ')
            analyzedObject.detectedObject.labels.getOrNull(0)?.let {
                binding.index.text = "index: ${it.index}"
                val confidence = it.confidence.times(100)
                binding.confidence.text = "conf: $confidence%"
                binding.label.text = "label: ${it.text}"
            }
        }
    }

    companion object {
        private val diffUtils = object : DiffUtil.ItemCallback<AnalyzedObject>() {
            override fun areItemsTheSame(
                oldItem: AnalyzedObject,
                newItem: AnalyzedObject
            ): Boolean {
                return oldItem === newItem
            }

            override fun areContentsTheSame(
                oldItem: AnalyzedObject,
                newItem: AnalyzedObject
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
