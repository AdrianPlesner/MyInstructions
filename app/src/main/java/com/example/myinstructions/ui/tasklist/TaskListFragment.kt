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
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myinstructions.R
import com.example.myinstructions.databinding.FragmentTaskListBinding
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

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.findItem(R.id.action_manage_categories)?.isVisible = true
                menu.findItem(R.id.action_sort)?.isVisible = true
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
                    else -> false
                }
            }

            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.action_manage_categories)?.isVisible = true
                menu.findItem(R.id.action_sort)?.apply {
                    isVisible = true
                    // Show the option to switch TO the other mode
                    title = when (viewModel.sortMode.value) {
                        SortMode.CATEGORY -> getString(R.string.sort_by_recent)
                        SortMode.RECENT -> getString(R.string.sort_by_category)
                    }
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val adapter = CategoryTaskListAdapter(
            onTaskClick = { task ->
                val bundle = Bundle().apply { putLong("taskId", task.id) }
                findNavController().navigate(R.id.action_TaskList_to_TaskDetail, bundle)
            },
            onHeaderClick = { categoryId ->
                viewModel.toggleCategory(categoryId)
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.listItems.collect { items ->
                    adapter.submitList(items)
                    binding.textEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
