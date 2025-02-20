package com.bilocan.sifreyoneticisi

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.bilocan.sifreyoneticisi.adapter.PasswordAdapter
import com.bilocan.sifreyoneticisi.data.AppDatabase
import com.bilocan.sifreyoneticisi.databinding.ActivityCategoryDetailBinding
import com.bilocan.sifreyoneticisi.databinding.DialogAddPasswordBinding
import com.bilocan.sifreyoneticisi.model.Password
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "CategoryDetailActivity"

class CategoryDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCategoryDetailBinding
    private lateinit var db: AppDatabase
    private lateinit var passwordAdapter: PasswordAdapter
    private var categoryId: Int = 0
    private var categoryName: String = ""
    private var categoryIconResId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
    }

    private fun initializeComponents() {
        categoryId = intent.getIntExtra("categoryId", 0)
        categoryName = intent.getStringExtra("categoryName") ?: ""
        categoryIconResId = intent.getIntExtra("categoryIcon", 0)

        if (categoryId == 0) {
            showCustomToast("Kategori bilgisi alınamadı")
            finish()
            return
        }

        db = AppDatabase.getDatabase(this)
        setupUI()
        setupRecyclerView()
        observePasswords()
    }

    private fun setupUI() {
        with(binding) {
            categoryTitleText.text = categoryName
            categoryIcon.setImageResource(categoryIconResId)
            backButton.setOnClickListener { onBackPressed() }
            addPasswordFab.setOnClickListener { showAddPasswordDialog() }
        }
    }

    private fun setupRecyclerView() {
        passwordAdapter = PasswordAdapter(
            onCopyClick = { password -> copyToClipboard(password.password) },
            onDeleteClick = { password -> showDeleteConfirmationDialog(password) },
            onEditClick = { password -> showEditPasswordDialog(password) },
            onMoveItem = { fromPosition, toPosition -> movePassword(fromPosition, toPosition) }
        )

        setupItemTouchHelper()

        binding.passwordsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CategoryDetailActivity)
            adapter = passwordAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupItemTouchHelper() {
        val callback = createItemTouchHelperCallback()
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.passwordsRecyclerView)
        passwordAdapter.setTouchHelper(itemTouchHelper)
    }

    private fun createItemTouchHelperCallback(): ItemTouchHelper.SimpleCallback {
        return object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END, 0
        ) {
            private var dragFrom = -1
            private var dragTo = -1

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition

                if (fromPosition < 0 || toPosition < 0) return false

                if (dragFrom == -1) {
                    dragFrom = fromPosition
                }
                dragTo = toPosition

                val currentList = passwordAdapter.currentList.toMutableList()
                val item = currentList.removeAt(fromPosition)
                currentList.add(toPosition, item)
                passwordAdapter.submitList(currentList)

                return true
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
                    updatePasswordOrder()
                }

                dragFrom = -1
                dragTo = -1
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun isLongPressDragEnabled(): Boolean = true
        }
    }

    private fun updatePasswordOrder() {
        lifecycleScope.launch {
            try {
                val currentList = passwordAdapter.currentList
                currentList.forEachIndexed { index, password ->
                    db.passwordDao().updatePasswordOrder(password.id, index)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sıralama güncellenirken hata", e)
                showCustomToast("Sıralama güncellenirken bir hata oluştu")
            }
        }
    }

    private fun observePasswords() {
        lifecycleScope.launch {
            try {
                db.passwordDao().getPasswordsForCategory(categoryId).collectLatest { passwords ->
                    try {
                        if (!isFinishing) {
                            passwordAdapter.submitList(passwords.sortedBy { it.orderIndex })
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Şifre listesi güncellenirken hata", e)
                    }
                }
            } catch (e: Exception) {
                if (!isFinishing) {
                    Log.e(TAG, "Şifreler yüklenirken hata", e)
                }
            }
        }
    }

    private fun showAddPasswordDialog() {
        val dialogBinding = DialogAddPasswordBinding.inflate(layoutInflater)

        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle("Şifre Ekle")
            .setView(dialogBinding.root)
            .setPositiveButton("Kaydet") { _, _ ->
                handlePasswordSave(dialogBinding)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun handlePasswordSave(dialogBinding: DialogAddPasswordBinding) {
        val appName = dialogBinding.appNameEditText.text.toString()
        val username = dialogBinding.usernameEditText.text.toString()
        val password = dialogBinding.passwordEditText.text.toString()

        if (appName.isEmpty() || password.isEmpty()) {
            showCustomToast("Uygulama adı ve şifre zorunludur")
            return
        }

        savePassword(appName, username, password)
    }

    private fun savePassword(appName: String, username: String, password: String) {
        lifecycleScope.launch {
            try {
                val nextOrderIndex = db.passwordDao().getNextOrderIndex(categoryId) ?: 0
                val newPassword = Password(
                    categoryId = categoryId,
                    appName = appName,
                    username = if (username.isEmpty()) null else username,
                    password = password,
                    orderIndex = nextOrderIndex
                )
                db.passwordDao().insertPassword(newPassword)
                updateCategoryPasswordCount()
                showCustomToast("Kayıt başarıyla eklendi")
            } catch (e: Exception) {
                showCustomToast("Kayıt işlemi sırasında bir hata oluştu")
            }
        }
    }

    private fun showDeleteConfirmationDialog(password: Password) {
        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle("Kaydı Sil")
            .setMessage("${password.appName} uygulamasının kaydını silmek istediğinize emin misiniz?")
            .setPositiveButton("SİL") { _, _ ->
                deletePassword(password)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun deletePassword(password: Password) {
        lifecycleScope.launch {
            try {
                db.passwordDao().deletePassword(password)
                updateCategoryPasswordCount()
                showCustomToast("Kayıt başarıyla silindi")
            } catch (e: Exception) {
                showCustomToast("Silme işlemi sırasında bir hata oluştu")
            }
        }
    }

    private fun showEditPasswordDialog(password: Password) {
        val dialogBinding = DialogAddPasswordBinding.inflate(layoutInflater)
        
        // Mevcut verileri doldur
        dialogBinding.apply {
            appNameEditText.setText(password.appName)
            usernameEditText.setText(password.username)
            passwordEditText.apply {
                setText(password.password)
                inputType = android.text.InputType.TYPE_CLASS_TEXT
            }
        }
        
        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle("Şifreyi Güncelle")
            .setView(dialogBinding.root)
            .setPositiveButton("Güncelle") { _, _ ->
                handlePasswordUpdate(dialogBinding, password)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun handlePasswordUpdate(dialogBinding: DialogAddPasswordBinding, password: Password) {
        val appName = dialogBinding.appNameEditText.text.toString()
        val username = dialogBinding.usernameEditText.text.toString()
        val newPassword = dialogBinding.passwordEditText.text.toString()

        if (appName.isEmpty() || newPassword.isEmpty()) {
            showCustomToast("Uygulama adı ve şifre zorunludur")
            return
        }

        updatePassword(password, appName, username, newPassword)
    }

    private fun updatePassword(password: Password, appName: String, username: String, newPassword: String) {
        lifecycleScope.launch {
            try {
                val updatedPassword = password.copy(
                    appName = appName,
                    username = if (username.isEmpty()) null else username,
                    password = newPassword
                )
                db.passwordDao().updatePassword(updatedPassword)
                showCustomToast("Kayıt başarıyla güncellendi")
            } catch (e: Exception) {
                showCustomToast("Güncelleme işlemi sırasında bir hata oluştu")
            }
        }
    }

    private suspend fun updateCategoryPasswordCount() {
        val count = db.passwordDao().getPasswordCountForCategory(categoryId)
        val category = db.categoryDao().getCategoryById(categoryId)
        if (category != null) {
            category.passwordCount = count
            db.categoryDao().updateCategory(category)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("password", text)
        clipboard.setPrimaryClip(clip)
        showCustomToast("Şifre kopyalandı")
    }

    private fun showCustomToast(message: String) {
        val toast = Toast(this)
        val view = layoutInflater.inflate(R.layout.custom_toast, null)
        val textView = view.findViewById<TextView>(R.id.toastText)
        textView.text = message
        
        toast.apply {
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 150)
            duration = Toast.LENGTH_SHORT
            this.view = view
        }
        
        // Toast'un animasyonlu görünmesi için
        view.alpha = 0f
        view.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
            
        toast.show()
    }

    private fun movePassword(fromPosition: Int, toPosition: Int) {
        val currentList = passwordAdapter.currentList.toMutableList()
        val item = currentList.removeAt(fromPosition)
        currentList.add(toPosition, item)
        passwordAdapter.submitList(currentList)
    }
} 