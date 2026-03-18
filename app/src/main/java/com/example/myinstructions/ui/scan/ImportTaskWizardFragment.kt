package com.example.myinstructions.ui.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myinstructions.R
import com.example.myinstructions.data.entity.CategoryEntity
import com.example.myinstructions.databinding.FragmentImportWizardBinding
import com.example.myinstructions.util.QrCodeHelper
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ImportTaskWizardFragment : Fragment() {

    private var _binding: FragmentImportWizardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ImportTaskWizardViewModel by viewModels()
    private lateinit var instructionAdapter: InstructionPreviewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImportWizardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tasksJson = arguments?.getString("tasksJson") ?: run {
            findNavController().popBackStack()
            return
        }

        val tasks = QrCodeHelper.fromJson(tasksJson)
        if (tasks == null || tasks.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.invalid_qr)
                .setPositiveButton(android.R.string.ok) { _, _ -> findNavController().popBackStack() }
                .show()
            return
        }

        viewModel.init(tasks)

        instructionAdapter = InstructionPreviewAdapter()
        binding.recyclerInstructions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = instructionAdapter
            isNestedScrollingEnabled = false
        }

        binding.progressIndicator.max = viewModel.totalCount

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.currentIndex,
                    viewModel.allCategories,
                    viewModel.selectedCategoryIds
                ) { index, categories, selectedIds ->
                    Triple(index, categories, selectedIds)
                }.collect { (index, categories, selectedIds) ->
                    val task = viewModel.currentTask ?: return@collect
                    binding.progressIndicator.progress = index + 1
                    binding.textProgress.text = getString(R.string.task_progress, index + 1, viewModel.totalCount)
                    binding.textTaskName.text = task.name
                    instructionAdapter.submitList(task.instructions)
                    rebuildChips(categories, selectedIds)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.wizardDone.collect { done ->
                    if (done) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.import_done, viewModel.importedTaskCount),
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().popBackStack(R.id.TaskListFragment, false)
                    }
                }
            }
        }

        binding.buttonImport.setOnClickListener { viewModel.importCurrentTask() }
        binding.buttonSkip.setOnClickListener { viewModel.skipCurrentTask() }

        binding.buttonAddCategory.setOnClickListener {
            val name = binding.editNewCategory.text?.toString()?.trim() ?: ""
            if (name.isEmpty()) return@setOnClickListener
            viewModel.createCategoryInline(name) {
                binding.editNewCategory.text?.clear()
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
