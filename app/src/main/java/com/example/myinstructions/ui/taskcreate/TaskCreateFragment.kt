package com.example.myinstructions.ui.taskcreate

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myinstructions.R
import com.example.myinstructions.data.entity.CategoryEntity
import com.example.myinstructions.databinding.FragmentTaskCreateBinding
import com.example.myinstructions.util.ImageStorageHelper
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File

class TaskCreateFragment : Fragment() {

    private var _binding: FragmentTaskCreateBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TaskCreateViewModel by viewModels()

    private var taskId: Long = -1L
    private var pendingImagePosition: Int = -1
    private var pendingCameraRelativePath: String? = null
    private var pendingCropDestRelativePath: String? = null
    private lateinit var adapter: InstructionEditAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && pendingImagePosition >= 0) {
            launchCrop(uri)
        } else {
            pendingImagePosition = -1
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && pendingImagePosition >= 0 && pendingCameraRelativePath != null) {
            val sourceFile = ImageStorageHelper.getAbsolutePath(requireContext(), pendingCameraRelativePath!!)
            val sourceUri = Uri.fromFile(sourceFile)
            launchCrop(sourceUri)
        } else {
            if (pendingCameraRelativePath != null) {
                ImageStorageHelper.deleteImage(requireContext(), pendingCameraRelativePath!!)
            }
            pendingImagePosition = -1
            pendingCameraRelativePath = null
        }
    }

    private val cropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val croppedUri = UCrop.getOutput(result.data!!)
            if (croppedUri != null && pendingImagePosition >= 0 && pendingCropDestRelativePath != null) {
                // Clean up the camera source file if it was different from the crop dest
                cleanUpCameraSourceIfNeeded()
                adapter.setImage(pendingImagePosition, pendingCropDestRelativePath!!)
            }
        } else {
            // User cancelled crop — clean up
            if (pendingCropDestRelativePath != null) {
                ImageStorageHelper.deleteImage(requireContext(), pendingCropDestRelativePath!!)
            }
            cleanUpCameraSourceIfNeeded()
        }
        pendingImagePosition = -1
        pendingCameraRelativePath = null
        pendingCropDestRelativePath = null
    }

    private fun cleanUpCameraSourceIfNeeded() {
        if (pendingCameraRelativePath != null && pendingCameraRelativePath != pendingCropDestRelativePath) {
            ImageStorageHelper.deleteImage(requireContext(), pendingCameraRelativePath!!)
        }
    }

    private fun launchCrop(sourceUri: Uri) {
        val (destUri, destRelativePath) = ImageStorageHelper.createImageFileUri(requireContext())
        pendingCropDestRelativePath = destRelativePath
        val destFile = ImageStorageHelper.getAbsolutePath(requireContext(), destRelativePath)
        val options = UCrop.Options().apply {
            setFreeStyleCropEnabled(true)
            setCompressionQuality(90)
        }
        val intent = UCrop.of(sourceUri, Uri.fromFile(destFile))
            .withOptions(options)
            .getIntent(requireContext())
        cropLauncher.launch(intent)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskId = arguments?.getLong("taskId", -1L) ?: -1L

        val touchCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                adapter.moveInstruction(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun isLongPressDragEnabled(): Boolean = false
        }
        itemTouchHelper = ItemTouchHelper(touchCallback)

        adapter = InstructionEditAdapter(
            onAddImage = { position ->
                pendingImagePosition = position
                showImageSourceDialog()
            },
            onRemoveImage = { position ->
                val draft = adapter.items.getOrNull(position)
                if (draft?.imageUri != null) {
                    ImageStorageHelper.deleteImage(requireContext(), draft.imageUri!!)
                }
                adapter.setImage(position, null)
            },
            onRemoveInstruction = { position ->
                val draft = adapter.items.getOrNull(position)
                if (draft?.imageUri != null) {
                    ImageStorageHelper.deleteImage(requireContext(), draft.imageUri!!)
                }
                adapter.removeInstruction(position)
            },
            onStartDrag = { viewHolder ->
                itemTouchHelper.startDrag(viewHolder)
            }
        )

        binding.recyclerInstructionsEdit.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerInstructionsEdit.adapter = adapter
        itemTouchHelper.attachToRecyclerView(binding.recyclerInstructionsEdit)

        // When a text field gains focus, scroll its card to the top of the visible area
        binding.recyclerInstructionsEdit.addOnChildAttachStateChangeListener(
            object : RecyclerView.OnChildAttachStateChangeListener {
                override fun onChildViewAttachedToWindow(view: View) {
                    val editText = view.findViewById<View>(R.id.edit_instruction_text)
                    editText?.setOnFocusChangeListener { v, hasFocus ->
                        if (hasFocus) {
                            v.post {
                                val scrollView = binding.root
                                val location = IntArray(2)
                                view.getLocationInWindow(location)
                                val scrollViewLocation = IntArray(2)
                                scrollView.getLocationInWindow(scrollViewLocation)
                                val scrollY = location[1] - scrollViewLocation[1] + scrollView.scrollY
                                scrollView.smoothScrollTo(0, scrollY)
                            }
                        }
                    }
                }
                override fun onChildViewDetachedFromWindow(view: View) {}
            }
        )

        // Category chips
        setupCategoryChips()

        binding.buttonAddCategory.setOnClickListener {
            val name = binding.editNewCategory.text?.toString()?.trim() ?: ""
            if (name.isEmpty()) return@setOnClickListener
            viewModel.createCategoryInline(name) {
                binding.editNewCategory.text?.clear()
            }
        }

        binding.buttonAddInstruction.setOnClickListener {
            adapter.addInstruction()
        }

        binding.buttonSave.setOnClickListener {
            val name = binding.editTaskName.text?.toString()?.trim() ?: ""
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), R.string.task_name_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.saveTask(taskId, name, adapter.items.toList()) {
                findNavController().popBackStack()
            }
        }

        if (taskId != -1L) {
            viewModel.loadTask(taskId) { name, instructions ->
                binding.editTaskName.setText(name)
                adapter.items.clear()
                adapter.items.addAll(instructions)
                adapter.notifyDataSetChanged()
            }
        } else {
            adapter.addInstruction()
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf(
            getString(R.string.image_source_gallery),
            getString(R.string.image_source_camera)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.image_source_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageLauncher.launch("image/*")
                    1 -> {
                        val (uri, relativePath) = ImageStorageHelper.createImageFileUri(requireContext())
                        pendingCameraRelativePath = relativePath
                        takePictureLauncher.launch(uri)
                    }
                }
            }
            .show()
    }

    private fun setupCategoryChips() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.allCategories,
                    viewModel.selectedCategoryIds
                ) { categories, selectedIds ->
                    categories to selectedIds
                }.collect { (categories, selectedIds) ->
                    rebuildChips(categories, selectedIds)
                }
            }
        }
    }

    private fun rebuildChips(categories: List<CategoryEntity>, selectedIds: Set<Long>) {
        binding.chipGroupCategories.removeAllViews()
        for (category in categories) {
            val chip = Chip(requireContext()).apply {
                text = category.name
                isCheckable = true
                isChecked = category.id in selectedIds
                setOnCheckedChangeListener { _, _ ->
                    viewModel.toggleCategory(category.id)
                }
            }
            binding.chipGroupCategories.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
