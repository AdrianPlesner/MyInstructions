package com.example.myinstructions.ui.taskdetail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.myinstructions.data.entity.InstructionEntity
import com.example.myinstructions.databinding.ItemInstructionDetailBinding
import com.example.myinstructions.util.ImageStorageHelper

class InstructionDetailAdapter :
    ListAdapter<InstructionEntity, InstructionDetailAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInstructionDetailBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(private val binding: ItemInstructionDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InstructionEntity, position: Int) {
            binding.textNumber.text = "${position + 1}."
            binding.textInstruction.text = item.text
            if (item.imageUri != null) {
                binding.imageInstruction.visibility = View.VISIBLE
                val file = ImageStorageHelper.getAbsolutePath(
                    binding.root.context, item.imageUri
                )
                binding.imageInstruction.load(file)
            } else {
                binding.imageInstruction.visibility = View.GONE
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<InstructionEntity>() {
            override fun areItemsTheSame(oldItem: InstructionEntity, newItem: InstructionEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: InstructionEntity,
                newItem: InstructionEntity
            ) = oldItem == newItem
        }
    }
}
