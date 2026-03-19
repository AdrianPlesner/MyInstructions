package com.example.myinstructions.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
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
import com.google.zxing.BinaryBitmap
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class ScanQrFragment : Fragment() {

    private var _binding: FragmentScanQrBinding? = null
    private val binding get() = _binding!!

    // ── Camera scanner ────────────────────────────────────────────────────────

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            handleScannedJson(result.contents)
        }
        // cancelled → stay on this screen so the user can try gallery instead
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraScanner()
        } else {
            Toast.makeText(requireContext(), R.string.camera_permission_denied, Toast.LENGTH_SHORT)
                .show()
        }
    }

    // ── Gallery picker ────────────────────────────────────────────────────────

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) decodeQrFromUri(uri)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnScanCamera.setOnClickListener { checkCameraPermissionAndScan() }
        binding.btnScanGallery.setOnClickListener { galleryLauncher.launch("image/*") }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Camera helpers ────────────────────────────────────────────────────────

    private fun checkCameraPermissionAndScan() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> launchCameraScanner()

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.camera_permission_title)
                    .setMessage(R.string.camera_permission_rationale)
                    .setPositiveButton(R.string.grant) { _, _ ->
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }

            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCameraScanner() {
        scanLauncher.launch(ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt(getString(R.string.scan_prompt))
            setCameraId(0)
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
        })
    }

    // ── Gallery QR decode ─────────────────────────────────────────────────────

    private fun decodeQrFromUri(uri: Uri) {
        try {
            val stream = requireContext().contentResolver.openInputStream(uri)
                ?: run { showInvalidQrToast(); return }

            val bitmap = BitmapFactory.decodeStream(stream)
            stream.close()

            if (bitmap == null) { showInvalidQrToast(); return }

            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = QRCodeReader().decode(binaryBitmap)
            handleScannedJson(result.text)

        } catch (_: NotFoundException) {
            Toast.makeText(requireContext(), R.string.no_qr_found, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            showInvalidQrToast()
        }
    }

    private fun showInvalidQrToast() {
        Toast.makeText(requireContext(), R.string.invalid_qr, Toast.LENGTH_SHORT).show()
    }

    // ── Common ────────────────────────────────────────────────────────────────

    private fun handleScannedJson(json: String) {
        val tasks = QrCodeHelper.decode(json)
        if (tasks == null || tasks.isEmpty()) {
            showInvalidQrToast()
            return
        }
        val tasksJson = QrCodeHelper.toJson(tasks)
        val bundle = Bundle().apply { putString("tasksJson", tasksJson) }
        findNavController().navigate(R.id.action_ScanQr_to_ImportWizard, bundle)
    }
}
