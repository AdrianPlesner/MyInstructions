package com.example.myinstructions.ui.tasklist

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.Toast
import com.example.myinstructions.R
import com.example.myinstructions.databinding.FragmentTaskListBinding
import com.example.myinstructions.util.QrCodeHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class TaskListFragment : Fragment() {

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TaskListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back press exits selection mode
        val backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                viewModel.exitSelectionMode()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                val inSelection = viewModel.selectionMode.value
                menu.findItem(R.id.action_manage_categories)?.isVisible = !inSelection
                menu.findItem(R.id.action_sort)?.isVisible = !inSelection
                menu.findItem(R.id.action_share_tasks)?.isVisible = !inSelection
                menu.findItem(R.id.action_scan_qr)?.isVisible = !inSelection
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_manage_categories -> {
                        findNavController().navigate(R.id.action_TaskList_to_CategoryManage)
                        true
                    }
                    R.id.action_sort -> {
                        viewModel.toggleSortMode()
                        requireActivity().invalidateOptionsMenu()
                        true
                    }
                    R.id.action_share_tasks -> {
                        findNavController().navigate(R.id.action_TaskList_to_ShareSelection)
                        true
                    }
                    R.id.action_scan_qr -> {
                        findNavController().navigate(R.id.action_TaskList_to_ScanQr)
                        true
                    }
                    else -> false
                }
            }

            override fun onPrepareMenu(menu: Menu) {
                val inSelection = viewModel.selectionMode.value
                menu.findItem(R.id.action_manage_categories)?.isVisible = !inSelection
                menu.findItem(R.id.action_share_tasks)?.isVisible = !inSelection
                menu.findItem(R.id.action_scan_qr)?.isVisible = !inSelection
                menu.findItem(R.id.action_sort)?.apply {
                    isVisible = !inSelection
                    title = when (viewModel.sortMode.value) {
                        SortMode.CATEGORY -> getString(R.string.sort_by_recent)
                        SortMode.RECENT -> getString(R.string.sort_by_category)
                    }
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val adapter = CategoryTaskListAdapter(
            onTaskClick = { task ->
                if (viewModel.selectionMode.value) {
                    viewModel.toggleTaskSelection(task.id)
                } else {
                    val bundle = Bundle().apply { putLong("taskId", task.id) }
                    findNavController().navigate(R.id.action_TaskList_to_TaskDetail, bundle)
                }
            },
            onTaskLongClick = { task ->
                viewModel.enterSelectionMode(task.id)
            },
            onHeaderClick = { categoryId, taskIds ->
                if (viewModel.selectionMode.value) {
                    viewModel.toggleCategorySelection(taskIds)
                } else {
                    viewModel.toggleCategory(categoryId)
                }
            },
            onHeaderLongClick = { _, taskIds ->
                if (!viewModel.selectionMode.value) {
                    val bundle = Bundle().apply {
                        putLongArray("preselectedTaskIds", taskIds.toLongArray())
                    }
                    findNavController().navigate(R.id.action_TaskList_to_ShareSelection, bundle)
                }
            }
        )

        binding.recyclerTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTasks.adapter = adapter

        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
        })

        // Bottom action bar buttons
        binding.buttonDeleteSelected.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_task_title)
                .setMessage(
                    getString(R.string.delete_tasks_message, viewModel.selectedTaskIds.value.size)
                )
                .setPositiveButton(R.string.delete) { _, _ ->
                    viewModel.deleteSelectedTasks()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        binding.buttonAssignCategories.setOnClickListener {
            showAssignCategoriesDialog()
        }

        binding.buttonShareQr.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val tasks = viewModel.buildShareableTasksForSelected()
                if (tasks.isEmpty()) return@launch
                val json = QrCodeHelper.toJson(tasks)
                val bundle = Bundle().apply { putString("tasksJson", json) }
                viewModel.exitSelectionMode()
                findNavController().navigate(R.id.action_TaskList_to_QrDisplay, bundle)
            }
        }

        // Observe list items
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.listItems.collect { items ->
                    adapter.submitList(items)
                    binding.textEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        // Observe selection mode
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectionMode.collect { inSelectionMode ->
                    backCallback.isEnabled = inSelectionMode
                    binding.selectionActionBar.visibility =
                        if (inSelectionMode) View.VISIBLE else View.GONE
                    requireActivity().invalidateOptionsMenu()
                }
            }
        }

        // Observe selected count
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedTaskIds.collect { ids ->
                    binding.textSelectedCount.text =
                        getString(R.string.selected_count, ids.size)
                }
            }
        }
    }

    private fun showAssignCategoriesDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val (categories, checked) = viewModel.getCategoriesForDialog()
            if (categories.isEmpty()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.assign_categories_label)
                    .setMessage(R.string.no_categories)
                    .setPositiveButton(R.string.cancel, null)
                    .show()
                return@launch
            }
            val names = categories.map { it.name }.toTypedArray()
            val checkedMutable = checked.copyOf()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.assign_categories_label)
                .setMultiChoiceItems(names, checkedMutable) { _, index, isChecked ->
                    checkedMutable[index] = isChecked
                }
                .setPositiveButton(R.string.save) { _, _ ->
                    val selectedCategoryIds = categories
                        .filterIndexed { i, _ -> checkedMutable[i] }
                        .map { it.id }
                    viewModel.addCategoriesToSelected(selectedCategoryIds)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
