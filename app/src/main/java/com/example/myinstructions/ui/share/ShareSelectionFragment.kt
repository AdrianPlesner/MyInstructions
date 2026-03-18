package com.example.myinstructions.ui.share

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
import com.example.myinstructions.databinding.FragmentShareSelectionBinding
import com.example.myinstructions.ui.tasklist.ListItem
import com.example.myinstructions.ui.tasklist.TaskItem
import com.example.myinstructions.util.QrCodeHelper
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ShareSelectionFragment : Fragment() {

    private var _binding: FragmentShareSelectionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ShareSelectionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShareSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply preselection from arguments (only once, not on config change)
        if (savedInstanceState == null) {
            val preselectedTaskId = arguments?.getLong("preselectedTaskId", -1L) ?: -1L
            val preselectedTaskIds = arguments?.getLongArray("preselectedTaskIds")
            when {
                preselectedTaskId != -1L -> viewModel.preselectTask(preselectedTaskId)
                preselectedTaskIds != null && preselectedTaskIds.isNotEmpty() ->
                    viewModel.preselectTasks(preselectedTaskIds.toHashSet())
            }
        }

        val adapter = ShareSelectionAdapter(
            onTaskToggle = { taskId -> viewModel.toggleTask(taskId) },
            onCategoryToggle = { taskIds -> viewModel.toggleCategory(taskIds) }
        )

        binding.recyclerShareTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerShareTasks.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.categoriesWithTasks,
                    viewModel.uncategorizedTaskIds,
                    viewModel.allTasksWithInstructions,
                    viewModel.selectedTaskIds
                ) { categories, uncategorizedIds, allTasks, selected ->
                    val taskInfoMap = allTasks.associateBy({ it.task.id }, { it })
                    val items = mutableListOf<ListItem>()

                    for (cwt in categories) {
                        if (cwt.tasks.isEmpty()) continue
                        val taskIds = cwt.tasks.map { it.id }
                        items.add(
                            ListItem.CategoryHeader(
                                cwt.category.id, cwt.category.name,
                                cwt.tasks.size, isExpanded = true, taskIds = taskIds
                            )
                        )
                        for (task in cwt.tasks.sortedBy { it.name }) {
                            val count = taskInfoMap[task.id]?.instructions?.size ?: 0
                            items.add(ListItem.TaskRow(TaskItem(
                                id = task.id,
                                name = task.name,
                                instructionCount = count
                            )))
                        }
                    }

                    val uncategorizedSet = uncategorizedIds.toSet()
                    if (uncategorizedSet.isNotEmpty()) {
                        items.add(ListItem.UncategorizedHeader(uncategorizedSet.size, isExpanded = true))
                        uncategorizedIds.forEach { taskId ->
                            val twi = taskInfoMap[taskId]
                            items.add(ListItem.TaskRow(TaskItem(
                                id = taskId,
                                name = twi?.task?.name ?: "",
                                instructionCount = twi?.instructions?.size ?: 0
                            )))
                        }
                    }

                    items to selected
                }.collect { (items, selected) ->
                    adapter.submitList(items)
                    adapter.updateSelection(selected)
                    binding.textEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        binding.buttonGenerateQr.setOnClickListener {
            if (viewModel.selectedTaskIds.value.isEmpty()) {
                Toast.makeText(requireContext(), R.string.no_tasks_selected, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewLifecycleOwner.lifecycleScope.launch {
                val tasks = viewModel.buildShareableTasks()
                val bitmap = QrCodeHelper.encode(tasks)
                if (bitmap == null) {
                    Toast.makeText(requireContext(), R.string.qr_payload_too_large, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val json = QrCodeHelper.toJson(tasks)
                val bundle = Bundle().apply { putString("tasksJson", json) }
                findNavController().navigate(R.id.action_ShareSelection_to_QrDisplay, bundle)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
