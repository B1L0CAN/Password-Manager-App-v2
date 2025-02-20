package com.bilocan.sifreyoneticisi.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bilocan.sifreyoneticisi.databinding.ItemColorBinding

class ColorAdapter(
    private val context: Context,
    private val colors: List<Int>,
    private val onColorSelected: (Int) -> Unit
) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

    private var selectedPosition = 0

    fun setSelectedColor(position: Int) {
        if (position in colors.indices) {
            val previousSelected = selectedPosition
            selectedPosition = position
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
        }
    }

    inner class ColorViewHolder(
        private val binding: ItemColorBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(colorResId: Int, position: Int) {
            binding.root.isSelected = position == selectedPosition
            binding.colorView.setBackgroundColor(ContextCompat.getColor(context, colorResId))
            
            binding.root.setOnClickListener {
                val previousSelected = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)
                onColorSelected(colorResId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val binding = ItemColorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ColorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(colors[position], position)
    }

    override fun getItemCount() = colors.size
} 