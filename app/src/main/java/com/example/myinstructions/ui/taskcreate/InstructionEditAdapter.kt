package com.example.myinstructions.ui.taskcreate

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.myinstructions.databinding.ItemInstructionEditBinding
import com.example.myinstructions.util.ImageStorageHelper

class InstructionEditAdapter(
    private val onAddImage: (position: Int) -> Unit,
    private val onRemoveImage: (position: Int) -> Unit,
    private val onRemoveInstruction: (position: Int) -> Unit
) : RecyclerView.Adapter<InstructionEditAdapter.ViewHolder>() {

    val items = mutableListOf<InstructionDraft>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInstructionEditBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun addInstruction() {
        items.add(InstructionDraft())
        notifyItemInserted(items.size - 1)
    }

    fun removeInstruction(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, items.size - position)
        }
    }

    fun setImage(position: Int, imageUri: String?) {
        if (position in items.indices) {
            items[position].imageUri = imageUri
            notifyItemChanged(position)
        }
    }

    inner class ViewHolder(private val binding: ItemInstructionEditBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var textWatcher: TextWatcher? = null

        fun bind(draft: InstructionDraft) {
            textWatcher?.let { binding.editInstructionText.removeTextChangedListener(it) }

            binding.editInstructionText.setText(draft.text)

            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val pos = this@ViewHolder.adapterPosition
                    if (pos != RecyclerView.NO_POSITION && pos < items.size) {
                        items[pos].text = s?.toString() ?: ""
                    }
                }
            }
            binding.editInstructionText.addTextChangedListener(textWatcher)

            if (draft.imageUri != null) {
                binding.imagePreview.visibility = View.VISIBLE
                binding.buttonRemoveImage.visibility = View.VISIBLE
                val file = ImageStorageHelper.getAbsolutePath(
                    binding.root.context, draft.imageUri!!
                )
                binding.imagePreview.load(file)
            } else {
                binding.imagePreview.visibility = View.GONE
                binding.buttonRemoveImage.visibility = View.GONE
            }

            binding.buttonAddImage.setOnClickListener {
                val pos = this@ViewHolder.adapterPosition
                if (pos != RecyclerView.NO_POSITION) onAddImage(pos)
            }
            binding.buttonRemoveImage.setOnClickListener {
                val pos = this@ViewHolder.adapterPosition
                if (pos != RecyclerView.NO_POSITION) onRemoveImage(pos)
            }
            binding.buttonRemoveInstruction.setOnClickListener {
                val pos = this@ViewHolder.adapterPosition
                if (pos != RecyclerView.NO_POSITION) onRemoveInstruction(pos)
            }
        }
    }
}
