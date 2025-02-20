package com.bilocan.sifreyoneticisi.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bilocan.sifreyoneticisi.R
import com.bilocan.sifreyoneticisi.databinding.ItemCategoryBinding
import com.bilocan.sifreyoneticisi.model.Category

class CategoryAdapter(
    private val onItemClick: (Category) -> Unit,
    private val onLongClick: (Category) -> Boolean,
    val onMoveItem: (Int, Int) -> Unit
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    private var selectedCategoryId: Int? = null

    fun setSelectedCategory(category: Category?) {
        val oldSelectedId = selectedCategoryId
        selectedCategoryId = category?.id
        
        if (oldSelectedId != null) {
            notifyItemChanged(currentList.indexOfFirst { it.id == oldSelectedId })
        }
        if (selectedCategoryId != null) {
            notifyItemChanged(currentList.indexOfFirst { it.id == selectedCategoryId })
        }
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            binding.apply {
                categoryIcon.setImageResource(category.icon)
                categoryName.text = category.name
                passwordCount.text = "${category.passwordCount} kayıt"
                
                // Seçili kategori kontrolü
                val isSelected = category.id == selectedCategoryId
                root.setCardBackgroundColor(
                    ContextCompat.getColor(
                        root.context,
                        if (isSelected) android.R.color.white else category.color
                    )
                )
                
                // Tik işaretini göster/gizle
                checkIcon.visibility = if (isSelected) View.VISIBLE else View.GONE
                
                // Metin rengini her zaman siyah yap
                categoryName.setTextColor(ContextCompat.getColor(root.context, android.R.color.black))
                passwordCount.setTextColor(ContextCompat.getColor(root.context, android.R.color.black))
                categoryIcon.setColorFilter(ContextCompat.getColor(root.context, android.R.color.black))
                dragHandle.setColorFilter(ContextCompat.getColor(root.context, android.R.color.black))

                dragHandle.setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        itemView.parent.requestDisallowInterceptTouchEvent(true)
                    }
                    false
                }

                root.setOnClickListener {
                    if (selectedCategoryId != null) {
                        val oldSelectedId = selectedCategoryId
                        selectedCategoryId = null
                        notifyItemChanged(currentList.indexOfFirst { it.id == oldSelectedId })
                        onItemClick(category)
                    } else {
                        onItemClick(category)
                    }
                }

                root.setOnLongClickListener {
                    if (selectedCategoryId == category.id) {
                        selectedCategoryId = null
                        notifyItemChanged(adapterPosition)
                        false
                    } else {
                        onLongClick(category)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
} 