package com.bilocan.sifreyoneticisi

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var passwordAdapter: PasswordAdapter
    private lateinit var db: AppDatabase
    private var categoryId: Int = 0
    private var categoryName: String = ""
    private var categoryIcon: Int = 0
    private var isFinishing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityCategoryDetailBinding.inflate(layoutInflater)
            setContentView(binding.root)

            categoryId = intent.getIntExtra("categoryId", 0)
            categoryName = intent.getStringExtra("categoryName") ?: ""
            categoryIcon = intent.getIntExtra("categoryIcon", 0)

            if (categoryId == 0) {
                Log.e(TAG, "Geçersiz category_id: $categoryId")
                showMessage("Geçersiz kategori bilgisi")
                finish()
                return
            }

            initializeComponents()
        } catch (e: Exception) {
            Log.e(TAG, "CategoryDetailActivity başlatılırken hata", e)
            showMessage("Sayfa başlatılırken bir hata oluştu")
            finish()
        }
    }

    private fun initializeComponents() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
        binding.toolbarTitle.text = categoryName

        db = AppDatabase.getDatabase(this)
        setupRecyclerView()
        setupClickListeners()
        observePasswords()
    }

    private fun observePasswords() {
        if (isFinishing) return

        lifecycleScope.launch {
            try {
                db.passwordDao().getPasswordsForCategory(categoryId).collectLatest { passwords ->
                    if (!isFinishing) {
                        passwordAdapter.submitList(passwords)
                    }
                }
            } catch (e: Exception) {
                if (!isFinishing) {
                    Log.e(TAG, "Şifreler yüklenirken hata", e)
                    if (e !is IllegalStateException) {
                        showMessage("Şifreler yüklenirken bir hata oluştu")
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        passwordAdapter = PasswordAdapter(
            onItemClick = { password ->
                showEditPasswordDialog(password)
            },
            onCopyClick = { password ->
                copyPasswordToClipboard(password)
            },
            onDeleteClick = { password ->
                showDeletePasswordConfirmation(password)
            },
            onMoveItem = { fromPosition, toPosition ->
                movePassword(fromPosition, toPosition)
            }
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

            override fun isLongPressDragEnabled(): Boolean = false
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
                showMessage("Sıralama güncellenirken bir hata oluştu")
            }
        }
    }

    private fun setupClickListeners() {
        binding.addPasswordFab.setOnClickListener {
            showAddPasswordDialog()
        }
        
        binding.sortButton.setOnClickListener {
            sortPasswordsAlphabetically()
        }
    }

    private fun sortPasswordsAlphabetically() {
        val currentList = passwordAdapter.currentList.toMutableList()
        val sortedList = currentList.sortedBy { it.title.uppercase() }
        
        if (currentList == sortedList) {
            // Eğer liste zaten sıralıysa, ters çevir
            binding.sortButton.setImageResource(R.drawable.ic_sort_alpha)
            passwordAdapter.submitList(currentList.sortedByDescending { it.title.uppercase() }) {
                showMessage("Z'den A'ya sıralandı")
            }
        } else {
            // Normal sıralama yap
            binding.sortButton.setImageResource(R.drawable.ic_sort_alpha_reverse)
            passwordAdapter.submitList(sortedList) {
                showMessage("A'dan Z'ye sıralandı")
            }
        }
    }

    private fun showMessage(message: String) {
        try {
            val view = layoutInflater.inflate(R.layout.custom_toast, null).apply {
                findViewById<TextView>(R.id.toastText).text = message
                alpha = 0f
                animate().alpha(1f).setDuration(200).start()
            }
            
            Toast(this).apply {
                setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 150)
                duration = Toast.LENGTH_SHORT
                this.view = view
                show()
            }
            
            Log.d(TAG, "Mesaj gösterildi: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Mesaj gösterilirken hata", e)
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddPasswordDialog() {
        val dialogBinding = DialogAddPasswordBinding.inflate(layoutInflater)

        dialogBinding.dialogTitle.text = "Yeni Şifre"
        dialogBinding.saveButton.text = "Kaydet"

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
            .apply {
                dialogBinding.saveButton.setOnClickListener {
                    handlePasswordSave(dialogBinding, null, this)
                }
                dialogBinding.cancelButton.setOnClickListener { dismiss() }
                show()
            }
    }

    private fun handlePasswordSave(
        dialogBinding: DialogAddPasswordBinding,
        existingPassword: Password?,
        dialog: AlertDialog
    ) {
        val title = dialogBinding.titleEditText.text.toString()
        val username = dialogBinding.usernameEditText.text.toString()
        val password = dialogBinding.passwordEditText.text.toString()

        when {
            title.isEmpty() -> {
                showMessage("Lütfen başlık giriniz")
            }
            password.isEmpty() -> {
                showMessage("Lütfen şifre giriniz")
            }
            else -> {
                if (existingPassword == null) {
                    addPassword(title, username, password)
                } else {
                    updatePassword(existingPassword.copy(
                        title = title,
                        username = username,
                        password = password
                    ))
                }
                dialog.dismiss()
            }
        }
    }

    private fun addPassword(title: String, username: String, password: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nextOrderIndex = db.passwordDao().getNextOrderIndex(categoryId) ?: 0
                val newPassword = Password(
                    title = title,
                    username = username,
                    password = password,
                    categoryId = categoryId,
                    orderIndex = nextOrderIndex
                )
                db.passwordDao().insertPassword(newPassword)

                // Kategori şifre sayısını güncelle
                val category = db.categoryDao().getCategoryById(categoryId)
                if (category != null) {
                    val passwordCount = db.passwordDao().getPasswordCountForCategory(categoryId)
                    val updatedCategory = category.copy(passwordCount = passwordCount)
                    db.categoryDao().updateCategory(updatedCategory)
                }

                withContext(Dispatchers.Main) {
                    showMessage("$title şifresi eklendi")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Şifre eklenirken hata", e)
                withContext(Dispatchers.Main) {
                    showMessage("Şifre eklenirken bir hata oluştu")
                }
            }
        }
    }

    private fun showEditPasswordDialog(password: Password) {
        val dialogBinding = DialogAddPasswordBinding.inflate(layoutInflater)

        dialogBinding.titleEditText.setText(password.title)
        dialogBinding.usernameEditText.setText(password.username)
        dialogBinding.passwordEditText.setText(password.password)

        dialogBinding.dialogTitle.text = "Şifreyi Düzenle"
        dialogBinding.saveButton.text = "Güncelle"

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
            .apply {
                dialogBinding.saveButton.setOnClickListener {
                    handlePasswordSave(dialogBinding, password, this)
                }
                dialogBinding.cancelButton.setOnClickListener { dismiss() }
                show()
            }
    }

    private fun updatePassword(password: Password) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val updatedPassword = password.copy(
                    title = password.title,
                    username = password.username,
                    password = password.password,
                    categoryId = password.categoryId,
                    orderIndex = password.orderIndex
                )
                db.passwordDao().updatePassword(updatedPassword)
                withContext(Dispatchers.Main) {
                    showMessage("${password.title} şifresi güncellendi")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Şifre güncellenirken hata", e)
                withContext(Dispatchers.Main) {
                    showMessage("Şifre güncellenirken bir hata oluştu")
                }
            }
        }
    }

    private fun showDeletePasswordConfirmation(password: Password) {
        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle("Şifreyi Sil")
            .setMessage("${password.title} şifresini silmek istediğinize emin misiniz?")
            .setPositiveButton("SİL") { _, _ ->
                lifecycleScope.launch {
                    db.passwordDao().deletePassword(password)

                    // Kategori şifre sayısını güncelle
                    val category = db.categoryDao().getCategoryById(categoryId)
                    if (category != null) {
                        val passwordCount = db.passwordDao().getPasswordCountForCategory(categoryId)
                        val updatedCategory = category.copy(passwordCount = passwordCount)
                        db.categoryDao().updateCategory(updatedCategory)
                    }

                    showMessage("${password.title} şifresi silindi")
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun copyPasswordToClipboard(password: Password) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Şifre", password.password)
        clipboard.setPrimaryClip(clip)
        showMessage("${password.title} şifresi panoya kopyalandı")
    }

    private fun movePassword(fromPosition: Int, toPosition: Int) {
        val currentList = passwordAdapter.currentList.toMutableList()
        val item = currentList.removeAt(fromPosition)
        currentList.add(toPosition, item)
        passwordAdapter.submitList(currentList)
    }

    override fun onDestroy() {
        isFinishing = true
        super.onDestroy()
    }
} 