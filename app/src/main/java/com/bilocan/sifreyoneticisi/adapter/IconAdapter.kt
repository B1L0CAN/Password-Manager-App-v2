package com.bilocan.sifreyoneticisi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bilocan.sifreyoneticisi.databinding.ItemIconBinding

class IconAdapter(
    private val icons: List<Int>,
    private val onIconSelected: (Int) -> Unit
) : RecyclerView.Adapter<IconAdapter.IconViewHolder>() {

    private var selectedPosition = 0

    fun setSelectedIcon(position: Int) {
        if (position in icons.indices) {
            val previousSelected = selectedPosition
            selectedPosition = position
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
        }
    }

    inner class IconViewHolder(
        private val binding: ItemIconBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(icon: Int, position: Int) {
            binding.root.isSelected = position == selectedPosition
            binding.iconImage.setImageResource(icon)
            
            binding.root.setOnClickListener {
                val previousSelected = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)
                onIconSelected(icon)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val binding = ItemIconBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IconViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        holder.bind(icons[position], position)
    }

    override fun getItemCount() = icons.size
} 