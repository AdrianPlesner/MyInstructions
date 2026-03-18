package com.example.myinstructions.ui.share

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myinstructions.R
import com.example.myinstructions.databinding.ItemShareCategoryHeaderBinding
import com.example.myinstructions.databinding.ItemShareTaskBinding
import com.example.myinstructions.ui.tasklist.ListItem
import com.example.myinstructions.ui.tasklist.TaskItem

class ShareSelectionAdapter(
    private val onTaskToggle: (taskId: Long) -> Unit,
    private val onCategoryToggle: (taskIds: List<Long>) -> Unit
) : ListAdapter<ListItem, RecyclerView.ViewHolder>(DIFF) {

    private var selectedTaskIds: Set<Long> = emptySet()

    fun updateSelection(selected: Set<Long>) {
        selectedTaskIds = selected
        notifyDataSetChanged()
    }

    companion object {
        private const val TYPE_CATEGORY_HEADER = 0
        private const val TYPE_TASK = 1
        private const val TYPE_UNCATEGORIZED_HEADER = 2

        private val DIFF = object : DiffUtil.ItemCallback<ListItem>() {
            override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
                return when {
                    oldItem is ListItem.CategoryHeader && newItem is ListItem.CategoryHeader ->
                        oldItem.id == newItem.id
                    oldItem is ListItem.UncategorizedHeader && newItem is ListItem.UncategorizedHeader ->
                        true
                    oldItem is ListItem.TaskRow && newItem is ListItem.TaskRow ->
                        oldItem.task.id == newItem.task.id
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem) = oldItem == newItem
        }
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is ListItem.CategoryHeader -> TYPE_CATEGORY_HEADER
        is ListItem.UncategorizedHeader -> TYPE_UNCATEGORIZED_HEADER
        is ListItem.TaskRow -> TYPE_TASK
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_CATEGORY_HEADER, TYPE_UNCATEGORIZED_HEADER -> {
                val binding = ItemShareCategoryHeaderBinding.inflate(inflater, parent, false)
                CategoryHeaderViewHolder(binding)
            }
            else -> {
                val binding = ItemShareTaskBinding.inflate(inflater, parent, false)
                TaskViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ListItem.CategoryHeader -> (holder as CategoryHeaderViewHolder).bind(
                item.name, item.taskCount, item.taskIds, selectedTaskIds
            )
            is ListItem.UncategorizedHeader -> (holder as CategoryHeaderViewHolder).bind(
                holder.itemView.context.getString(R.string.uncategorized),
                item.taskCount, emptyList(), selectedTaskIds
            )
            is ListItem.TaskRow -> (holder as TaskViewHolder).bind(item.task, selectedTaskIds)
        }
    }

    inner class CategoryHeaderViewHolder(private val binding: ItemShareCategoryHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(name: String, taskCount: Int, taskIds: List<Long>, selected: Set<Long>) {
            binding.textCategoryName.text = name
            binding.textTaskCount.text = binding.root.context.getString(
                R.string.category_task_count, taskCount
            )
            val allSelected = taskIds.isNotEmpty() && taskIds.all { it in selected }
            binding.checkboxCategory.isChecked = allSelected
            binding.root.setOnClickListener {
                if (taskIds.isNotEmpty()) onCategoryToggle(taskIds)
            }
        }
    }

    inner class TaskViewHolder(private val binding: ItemShareTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TaskItem, selected: Set<Long>) {
            binding.textTaskName.text = item.name
            binding.textInstructionCount.text = binding.root.context.getString(
                R.string.instructions_count, item.instructionCount
            )
            binding.checkboxTask.isChecked = item.id in selected
            binding.root.setOnClickListener { onTaskToggle(item.id) }
        }
    }
}
