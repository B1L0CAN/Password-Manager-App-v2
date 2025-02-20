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
    private val onCopyClick: (Password) -> Unit,
    private val onDeleteClick: (Password) -> Unit,
    private val onEditClick: (Password) -> Unit,
    val onMoveItem: suspend (Int, Int) -> Unit
) : ListAdapter<Password, PasswordAdapter.PasswordViewHolder>(PasswordDiffCallback()) {

    private var touchHelper: ItemTouchHelper? = null

    fun setTouchHelper(itemTouchHelper: ItemTouchHelper) {
        touchHelper = itemTouchHelper
    }

    inner class PasswordViewHolder(
        private val binding: ItemPasswordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(password: Password) {
            binding.apply {
                appNameText.text = password.appName
                
                // Kullanıcı adı boş ise ilgili view'ları gizle
                if (password.username.isNullOrEmpty()) {
                    usernameText.visibility = View.GONE
                    separatorText.visibility = View.GONE
                } else {
                    usernameText.visibility = View.VISIBLE
                    separatorText.visibility = View.VISIBLE
                    usernameText.text = password.username
                }
                
                passwordText.text = password.password

                copyButton.setOnClickListener {
                    onCopyClick(password)
                }

                deleteButton.setOnClickListener {
                    onDeleteClick(password)
                }

                editButton.setOnClickListener {
                    onEditClick(password)
                }

                dragHandle.setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        touchHelper?.startDrag(this@PasswordViewHolder)
                    }
                    false
                }
            }
        }
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
}

private class PasswordDiffCallback : DiffUtil.ItemCallback<Password>() {
    override fun areItemsTheSame(oldItem: Password, newItem: Password): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Password, newItem: Password): Boolean {
        return oldItem == newItem
    }
} 