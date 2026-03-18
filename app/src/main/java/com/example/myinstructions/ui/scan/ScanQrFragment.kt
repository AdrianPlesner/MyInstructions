package com.example.myinstructions.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myinstructions.R
import com.example.myinstructions.databinding.FragmentScanQrBinding
import com.example.myinstructions.util.QrCodeHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class ScanQrFragment : Fragment() {

    private var _binding: FragmentScanQrBinding? = null
    private val binding get() = _binding!!

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            handleScannedJson(result.contents)
        } else {
            findNavController().popBackStack()
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchScanner()
        } else {
            Toast.makeText(requireContext(), R.string.camera_permission_denied, Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkCameraPermissionAndScan()
    }

    private fun checkCameraPermissionAndScan() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> launchScanner()

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.camera_permission_title)
                    .setMessage(R.string.camera_permission_rationale)
                    .setPositiveButton(R.string.grant) { _, _ ->
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton(R.string.cancel) { _, _ -> findNavController().popBackStack() }
                    .show()
            }

            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchScanner() {
        scanLauncher.launch(ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt(getString(R.string.scan_prompt))
            setCameraId(0)
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
        })
    }

    private fun handleScannedJson(json: String) {
        val tasks = QrCodeHelper.decode(json)
        if (tasks == null || tasks.isEmpty()) {
            Toast.makeText(requireContext(), R.string.invalid_qr, Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }
        val tasksJson = QrCodeHelper.toJson(tasks)
        val bundle = Bundle().apply { putString("tasksJson", tasksJson) }
        findNavController().navigate(R.id.action_ScanQr_to_ImportWizard, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
