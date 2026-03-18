package com.example.myinstructions.ui.tasklist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myinstructions.R
import com.example.myinstructions.databinding.ItemCategoryHeaderBinding
import com.example.myinstructions.databinding.ItemTaskBinding
import com.example.myinstructions.util.HighlightHelper

sealed class ListItem {
    data class CategoryHeader(
        val id: Long,
        val name: String,
        val taskCount: Int,
        val isExpanded: Boolean,
        val taskIds: List<Long> = emptyList()
    ) : ListItem()

    data class UncategorizedHeader(
        val taskCount: Int,
        val isExpanded: Boolean
    ) : ListItem()

    data class TaskRow(val task: TaskItem) : ListItem()
}

class CategoryTaskListAdapter(
    private val onTaskClick: (TaskItem) -> Unit,
    private val onHeaderClick: (Long) -> Unit,
    private val onHeaderLongClick: ((categoryId: Long, taskIds: List<Long>) -> Unit)? = null
) : ListAdapter<ListItem, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_CATEGORY_HEADER = 0
        private const val TYPE_TASK = 1
        private const val TYPE_UNCATEGORIZED_HEADER = 2
        private const val UNCATEGORIZED_ID = -1L

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

            override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
                return oldItem == newItem
            }
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
                val binding = ItemCategoryHeaderBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }
            else -> {
                val binding = ItemTaskBinding.inflate(inflater, parent, false)
                TaskViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ListItem.CategoryHeader -> (holder as HeaderViewHolder).bind(
                item.name, item.taskCount, item.isExpanded, item.id, item.taskIds
            )
            is ListItem.UncategorizedHeader -> (holder as HeaderViewHolder).bind(
                holder.itemView.context.getString(R.string.uncategorized),
                item.taskCount, item.isExpanded, UNCATEGORIZED_ID, emptyList()
            )
            is ListItem.TaskRow -> (holder as TaskViewHolder).bind(item.task)
        }
    }

    inner class HeaderViewHolder(private val binding: ItemCategoryHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(name: String, taskCount: Int, isExpanded: Boolean, categoryId: Long, taskIds: List<Long> = emptyList()) {
            binding.textCategoryName.text = name
            binding.textTaskCount.text = binding.root.context.getString(
                R.string.category_task_count, taskCount
            )
            binding.iconExpand.setImageResource(
                if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )
            binding.root.setOnClickListener { onHeaderClick(categoryId) }
            binding.root.setOnLongClickListener {
                onHeaderLongClick?.invoke(categoryId, taskIds)
                onHeaderLongClick != null
            }
        }
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TaskItem) {
            binding.textTaskName.text = HighlightHelper.highlight(item.name, item.searchQuery)
            binding.textInstructionCount.text =
                binding.root.context.getString(R.string.instructions_count, item.instructionCount)

            val container = binding.containerMatchingInstructions
            container.removeAllViews()
            if (item.matchingInstructions.isNotEmpty()) {
                container.visibility = View.VISIBLE
                val context = binding.root.context
                val prefix = context.getString(R.string.matching_instruction_prefix, "")
                for (text in item.matchingInstructions) {
                    val fullText = context.getString(R.string.matching_instruction_prefix, text)
                    val highlighted = HighlightHelper.highlight(fullText, item.searchQuery)
                    val tv = TextView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { topMargin = 4 }
                        setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
                        setTypeface(typeface, android.graphics.Typeface.ITALIC)
                        maxLines = 2
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        this.text = highlighted
                    }
                    container.addView(tv)
                }
            } else {
                container.visibility = View.GONE
            }

            binding.root.setOnClickListener { onTaskClick(item) }
        }
    }
}
