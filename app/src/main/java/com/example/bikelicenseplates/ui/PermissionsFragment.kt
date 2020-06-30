package com.example.bikelicenseplates.ui

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.bikelicenseplates.R

class PermissionsFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when {
            allPermissionsGranted() -> {
                findNavController().navigate(
                    PermissionsFragmentDirections.actionPermissionsToCameraXPreview()
                )
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("App needs access to camera")
                    .setMessage("to take live camera feed to detect and analyze license plates.")
                    .setPositiveButton("Allow access") { _: DialogInterface, _: Int ->
                        requestPermissions(PERMISSIONS_REQUIRED, PERMISSION_REQUEST_CODE)
                    }
                    .setNegativeButton("Cancel", null)
                    .create()
                    .show()
            }
            else -> {
                requestPermissions(PERMISSIONS_REQUIRED, PERMISSION_REQUEST_CODE)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_permissions, container, false)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                findNavController().navigate(
                    PermissionsFragmentDirections.actionPermissionsToCameraXPreview()
                )
            } else {
                Toast.makeText(requireContext(), "Permissions not granted", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun allPermissionsGranted() = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 10
        private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)
    }
}
