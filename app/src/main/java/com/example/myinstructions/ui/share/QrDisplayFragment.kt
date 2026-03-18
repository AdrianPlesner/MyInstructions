package com.example.myinstructions.ui.share

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myinstructions.databinding.FragmentQrDisplayBinding
import com.example.myinstructions.util.QrCodeHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class QrDisplayFragment : Fragment() {

    private var _binding: FragmentQrDisplayBinding? = null
    private val binding get() = _binding!!
    private var qrBitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrDisplayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tasksJson = arguments?.getString("tasksJson") ?: run {
            findNavController().popBackStack()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                val tasks = QrCodeHelper.fromJson(tasksJson) ?: return@withContext null
                QrCodeHelper.encode(tasks)
            }

            if (bitmap == null) {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(com.example.myinstructions.R.string.qr_payload_too_large)
                    .setPositiveButton(android.R.string.ok) { _, _ -> findNavController().popBackStack() }
                    .show()
                return@launch
            }

            qrBitmap = bitmap
            binding.imageQr.setImageBitmap(bitmap)
        }

        binding.buttonShareImage.setOnClickListener {
            val bmp = qrBitmap ?: return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val file = File(requireContext().cacheDir, "qr_share.png")
                    FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    withContext(Dispatchers.Main) {
                        startActivity(Intent.createChooser(intent, null))
                    }
                } catch (_: Exception) {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
