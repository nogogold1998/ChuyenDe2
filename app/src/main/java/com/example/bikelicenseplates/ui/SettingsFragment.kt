package com.example.bikelicenseplates.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import com.example.bikelicenseplates.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_settings, rootKey)

        setupPreferences()
    }

    private fun setupPreferences() {
        val confidencePref = findPreference<SeekBarPreference>(
            getString(R.string.pref_key_object_detector_classification_confidence_threshold)
        )
    }
}
