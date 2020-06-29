package com.example.bikelicenseplates.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Size
import androidx.preference.PreferenceManager
import com.example.bikelicenseplates.R

object PreferenceUtils {
    private lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context) {
        sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun getEnableMultipleObjects(context: Context): Boolean = sharedPreferences.getBoolean(
        context.getString(R.string.pref_key_object_detector_enable_multiple_objects),
        false
    )

    fun getEnableClassification(context: Context): Boolean = sharedPreferences.getBoolean(
        context.getString(R.string.pref_key_object_detector_enable_classification),
        true
    )

    fun getClassificationConfidenceThreshold(context: Context): Int = sharedPreferences.getInt(
        context.getString(R.string.pref_key_object_detector_classification_confidence_threshold),
        0
    )

    fun getTargetAnalysisSize(context: Context): Size? {
        val prefKey = context.getString(R.string.pref_key_camerax_target_analysis_size)
        return Size.parseSize(sharedPreferences.getString(prefKey, null))
    }
}
