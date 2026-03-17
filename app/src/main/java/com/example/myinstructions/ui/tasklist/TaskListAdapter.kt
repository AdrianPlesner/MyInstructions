package com.example.myinstructions.ui.tasklist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myinstructions.R
import com.example.myinstructions.databinding.ItemTaskBinding

data class TaskItem(
    val id: Long,
    val name: String,
    val instructionCount: Int,
    val matchingInstruction: String? = null
)

class TaskListAdapter(
    private val onClick: (TaskItem) -> Unit
) : ListAdapter<TaskItem, TaskListAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TaskItem) {
            binding.textTaskName.text = item.name
            binding.textInstructionCount.text =
                binding.root.context.getString(R.string.instructions_count, item.instructionCount)
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TaskItem>() {
            override fun areItemsTheSame(oldItem: TaskItem, newItem: TaskItem) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: TaskItem, newItem: TaskItem) =
                oldItem == newItem
        }
    }
}
