package com.bilocan.sifreyoneticisi.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.bilocan.sifreyoneticisi.databinding.ItemPasswordBinding
import com.bilocan.sifreyoneticisi.model.Password

class PasswordAdapter(
    private val onItemClick: (Password) -> Unit,
    private val onCopyClick: (Password) -> Unit,
    private val onDeleteClick: (Password) -> Unit,
    private val onMoveItem: (Int, Int) -> Unit
) : ListAdapter<Password, PasswordAdapter.PasswordViewHolder>(PasswordDiffCallback()) {

    private var touchHelper: ItemTouchHelper? = null

    fun setTouchHelper(itemTouchHelper: ItemTouchHelper) {
        touchHelper = itemTouchHelper
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PasswordViewHolder {
        val binding = ItemPasswordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PasswordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PasswordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PasswordViewHolder(
        private val binding: ItemPasswordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.editButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            binding.copyButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onCopyClick(getItem(position))
                }
            }

            binding.deleteButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(position))
                }
            }

            binding.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    touchHelper?.startDrag(this@PasswordViewHolder)
                }
                false
            }
        }

        fun bind(password: Password) {
            binding.titleText.text = password.title
            binding.usernameText.text = password.username
            
            // Kullanıcı adı boş ise ilgili view'ları gizle
            if (password.username.isNullOrEmpty()) {
                binding.usernameText.visibility = View.GONE
                binding.separatorText.visibility = View.GONE
            } else {
                binding.usernameText.visibility = View.VISIBLE
                binding.separatorText.visibility = View.VISIBLE
            }
            
            binding.passwordText.text = password.password
        }
    }

    private class PasswordDiffCallback : DiffUtil.ItemCallback<Password>() {
        override fun areItemsTheSame(oldItem: Password, newItem: Password): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Password, newItem: Password): Boolean {
            return oldItem == newItem
        }
    }
} 