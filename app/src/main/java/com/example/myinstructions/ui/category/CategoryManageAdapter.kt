package com.example.myinstructions.ui.category

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myinstructions.data.entity.CategoryEntity
import com.example.myinstructions.databinding.ItemCategoryManageBinding

class CategoryManageAdapter(
    private val onDelete: (CategoryEntity) -> Unit
) : ListAdapter<CategoryEntity, CategoryManageAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryManageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCategoryManageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: CategoryEntity) {
            binding.textCategoryName.text = category.name
            binding.buttonDelete.setOnClickListener { onDelete(category) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<CategoryEntity>() {
            override fun areItemsTheSame(oldItem: CategoryEntity, newItem: CategoryEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: CategoryEntity, newItem: CategoryEntity) =
                oldItem == newItem
        }
    }
}
