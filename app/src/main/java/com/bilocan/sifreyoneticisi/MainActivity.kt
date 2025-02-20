package com.bilocan.sifreyoneticisi

import android.content.Intent
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.bilocan.sifreyoneticisi.adapter.CategoryAdapter
import com.bilocan.sifreyoneticisi.adapter.ColorAdapter
import com.bilocan.sifreyoneticisi.adapter.IconAdapter
import com.bilocan.sifreyoneticisi.data.AppDatabase
import com.bilocan.sifreyoneticisi.databinding.ActivityMainBinding
import com.bilocan.sifreyoneticisi.databinding.DialogAddCategoryBinding
import com.bilocan.sifreyoneticisi.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bilocan.sifreyoneticisi.util.BackupManager
import com.bilocan.sifreyoneticisi.util.BackupData
import com.bilocan.sifreyoneticisi.util.BackupException
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import android.net.Uri
import java.io.File
import com.google.android.material.textfield.TextInputLayout

private const val TAG = "MainActivity"
private const val GRID_SPAN_COUNT = 5
private const val EXTRA_USER_ID = "user_id"

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var db: AppDatabase
    private var userId: Int = 0
    private val categories = mutableListOf<Category>()
    private var isFinishing = false
    private lateinit var backupManager: BackupManager
    private var selectedCategory: Category? = null
    private val gson: Gson = GsonBuilder().create()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            showMessage("İzinler verildi")
            // İzinler verildikten sonra beklemeden dosya seçiciyi aç
            createBackupLauncher.launch("sifreyoneticisi_yedek.json")
        } else {
            showMessage("Yedekleme için gerekli izinler verilmedi")
        }
    }

    private val createBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            lifecycleScope.launch {
                try {
                    backupManager.createBackup(it, userId)
                    showMessage("Yedekleme başarıyla tamamlandı")
                } catch (e: BackupException) {
                    showMessage(e.message ?: "Yedekleme sırasında bir hata oluştu")
                }
            }
        }
    }

    private val restoreBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            lifecycleScope.launch {
                try {
                    // Önce dosyanın geçerli bir yedek dosyası olup olmadığını kontrol et
                    val jsonData = this@MainActivity.contentResolver.openInputStream(it)?.use { inputStream ->
                        String(inputStream.readBytes())
                    } ?: throw BackupException("Yedek dosyası açılamadı")

                    // JSON formatını kontrol et
                    val backupData = try {
                        gson.fromJson(jsonData, BackupData::class.java)
                    } catch (e: Exception) {
                        throw BackupException("Geçersiz yedek dosyası seçildi")
                    }

                    // Yedekteki kullanıcı adını kontrol et
                    val backupUser = backupData.users.firstOrNull()
                        ?: throw BackupException("Yedek dosyasında kullanıcı bilgisi bulunamadı")

                    val currentUser = db.userDao().getUserById(userId)
                        ?: throw BackupException("Kullanıcı bilgisi alınamadı")

                    // Yedekteki kullanıcı adı ile mevcut kullanıcı adı aynı mı kontrol et
                    if (backupUser.username == currentUser.username) {
                        // Kullanıcı adları aynıysa geri yükleme onayı iste
                        showRestoreConfirmationDialog(it)
                    } else {
                        // Kullanıcı adları farklıysa erişim reddet
                        throw BackupException("Bu yedek dosyası başka bir kullanıcıya ait")
                    }
                } catch (e: BackupException) {
                    showMessage(e.message ?: "Yedekten yükleme sırasında bir hata oluştu")
                } catch (e: Exception) {
                    showMessage("Geçersiz yedek dosyası seçildi")
                }
            }
        }
    }

    private fun showRestoreConfirmationDialog(uri: Uri) {
        AlertDialog.Builder(this@MainActivity, R.style.CustomAlertDialog)
            .setTitle("Yedekten Yükleme")
            .setMessage("Bu işlem mevcut verilerin üzerine yazacak. Devam etmek istiyor musunuz?")
            .setPositiveButton("Evet") { _, _ ->
                lifecycleScope.launch {
                    try {
                        backupManager.restoreBackup(uri, userId)
                        showMessage("Yedekten yükleme başarıyla tamamlandı")
                        isFinishing = true
                        val intent = intent
                        finish()
                        startActivity(intent)
                    } catch (e: BackupException) {
                        showMessage(e.message ?: "Yedekten yükleme sırasında bir hata oluştu")
                    }
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private val iconOptions = listOf(
        R.drawable.ic_folder, R.drawable.ic_lock, R.drawable.ic_star,
        R.drawable.ic_key, R.drawable.ic_card, R.drawable.ic_bank,
        R.drawable.ic_social, R.drawable.ic_email, R.drawable.ic_game,
        R.drawable.ic_shop
    )

    private val colorOptions = listOf(
        R.color.category_yellow, R.color.category_orange, R.color.category_red,
        R.color.category_pink, R.color.category_purple, R.color.category_deep_purple,
        R.color.category_light_green, R.color.category_green, R.color.category_teal,
        R.color.category_amber
    )

    private val defaultCategoryColor = R.color.category_yellow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            userId = intent.getIntExtra(EXTRA_USER_ID, 0)
            if (userId == 0) {
                Log.e(TAG, "Geçersiz user_id: $userId")
                showMessage("Geçersiz kullanıcı bilgisi")
                finish()
                return
            }

            initializeComponents()
        } catch (e: Exception) {
            Log.e(TAG, "MainActivity başlatılırken hata", e)
            showMessage("Uygulama başlatılırken bir hata oluştu")
            finish()
        }
    }

    private fun initializeComponents() {
        // Toolbar'ı ayarla
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Toolbar butonlarının tıklama işleyicilerini ayarla
        binding.settingsButton.setOnClickListener {
            showSettingsDialog()
        }
        
        binding.logoutButton.setOnClickListener {
            showLogoutDialog()
        }

        Log.d(TAG, "MainActivity başlatılıyor. User ID: $userId")
        
        // Veritabanı bağlantısını yeniden oluştur
        AppDatabase.clearInstance() // Önceki instance'ı temizle
        db = AppDatabase.getDatabase(this)
        
        backupManager = BackupManager(this)
        setupRecyclerView()
        setupClickListeners()
        observeCategories()
    }

    private fun observeCategories() {
        if (isFinishing) return
        
        lifecycleScope.launch {
            try {
                db.categoryDao().getCategoriesForUser(userId).collectLatest { categoryList ->
                    if (!isFinishing) {
                        updateCategories(categoryList)
                    }
                }
            } catch (e: Exception) {
                if (!isFinishing) {
                    Log.e(TAG, "Kategoriler yüklenirken hata", e)
                    if (e !is IllegalStateException) {
                        showMessage("Kategoriler yüklenirken bir hata oluştu")
                    }
                }
            }
        }
    }

    private suspend fun updateCategories(categoryList: List<Category>) {
        try {
            categories.clear()
            categories.addAll(categoryList)
            updatePasswordCounts()
        } catch (e: Exception) {
            Log.e(TAG, "Kategoriler güncellenirken hata", e)
        }
    }

    private suspend fun updatePasswordCounts() {
        if (isFinishing) return
        
        try {
            val updatedCategories = withContext(Dispatchers.IO) {
                categories.map { category ->
                    category.copy(
                        passwordCount = db.passwordDao().getPasswordCountForCategory(category.id)
                    )
                }
            }
            if (!isFinishing) {
                categoryAdapter.submitList(updatedCategories)
            }
        } catch (e: Exception) {
            if (!isFinishing) {
                Log.e(TAG, "Şifre sayıları güncellenirken hata", e)
                if (!isFinishing && e !is IllegalStateException) {
                    showMessage("Şifre sayıları güncellenirken bir hata oluştu")
                }
            }
        }
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(
            onItemClick = { category ->
                handleCategoryClick(category)
            },
            onLongClick = { category ->
                handleCategoryLongClick(category)
            },
            onMoveItem = { fromPosition, toPosition ->
                moveCategory(fromPosition, toPosition)
            }
        )

        setupItemTouchHelper()
        
        binding.categoriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = categoryAdapter
            setHasFixedSize(true)
        }
    }

    private fun handleCategoryClick(category: Category) {
        if (selectedCategory != null) {
            selectedCategory = null
            categoryAdapter.setSelectedCategory(null)
            showMessage("Kategori seçimi iptal edildi")
        } else {
            navigateToCategoryDetail(category)
        }
    }

    private fun handleCategoryLongClick(category: Category): Boolean {
        selectedCategory = category
        categoryAdapter.setSelectedCategory(category)
        showMessage("${category.name} kategorisi seçildi")
        return true
    }

    private fun setupItemTouchHelper() {
        val callback = createItemTouchHelperCallback()
        ItemTouchHelper(callback).attachToRecyclerView(binding.categoriesRecyclerView)
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

                val currentList = categoryAdapter.currentList.toMutableList()
                val item = currentList.removeAt(fromPosition)
                currentList.add(toPosition, item)
                categoryAdapter.submitList(currentList)

                return true
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
                    updateCategoryOrder()
                }

                dragFrom = -1
                dragTo = -1
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun isLongPressDragEnabled(): Boolean = true
        }
    }

    private fun updateCategoryOrder() {
        lifecycleScope.launch {
            try {
                val currentList = categoryAdapter.currentList
                currentList.forEachIndexed { index, category ->
                    db.categoryDao().updateCategoryOrder(category.id, index)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sıralama güncellenirken hata", e)
                showMessage("Sıralama güncellenirken bir hata oluştu")
            }
        }
    }

    private fun setupColorSelection(dialogBinding: DialogAddCategoryBinding, onColorSelected: (Int) -> Unit) {
        dialogBinding.colorRecyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, GRID_SPAN_COUNT)
            adapter = ColorAdapter(this@MainActivity, colorOptions) { color ->
                onColorSelected(color)
                (adapter as ColorAdapter).setSelectedColor(colorOptions.indexOf(color))
            }
            setHasFixedSize(true)
        }
    }

    private fun setupIconSelection(dialogBinding: DialogAddCategoryBinding, onIconSelected: (Int) -> Unit) {
        dialogBinding.iconRecyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, GRID_SPAN_COUNT)
            adapter = IconAdapter(iconOptions) { icon ->
                onIconSelected(icon)
                (adapter as IconAdapter).setSelectedIcon(iconOptions.indexOf(icon))
            }
            setHasFixedSize(true)
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

    override fun onDestroy() {
        isFinishing = true
        super.onDestroy()
    }

    private fun checkPermissionsAndBackup() {
        if (checkAndRequestPermissions()) {
            createBackupLauncher.launch("sifreyoneticisi_yedek.json")
        }
    }

    private fun checkPermissionsAndRestore() {
        if (checkAndRequestPermissions()) {
            restoreBackupLauncher.launch(arrayOf("application/json"))
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        val notGrantedPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        return if (notGrantedPermissions.isEmpty()) {
            true
        } else {
            requestPermissionLauncher.launch(notGrantedPermissions.toTypedArray())
            false
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle("Çıkış Yap")
            .setMessage("Çıkış yapmak istediğinizden emin misiniz?")
            .setPositiveButton("Çıkış Yap") { _, _ -> handleLogout() }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun handleLogout() {
        isFinishing = true
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Veritabanı bağlantısını kapat
                AppDatabase.clearInstance()
                // Kaydedilmiş kimlik bilgilerini temizle
                clearSavedCredentials()
                withContext(Dispatchers.Main) {
                    // LoginActivity'ye yönlendir
                    val intent = Intent(this@MainActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Çıkış yapılırken hata", e)
                withContext(Dispatchers.Main) {
                    showMessage("Çıkış yapılırken bir hata oluştu")
                }
            }
        }
    }

    private fun clearSavedCredentials() {
        getSharedPreferences("login_prefs", MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun showDeleteCategoryConfirmation(category: Category) {
        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle("Kategoriyi Sil")
            .setMessage("${category.name} kategorisini silmek istediğinize emin misiniz?")
            .setPositiveButton("SİL") { _, _ ->
                lifecycleScope.launch {
                    db.categoryDao().deleteCategory(category)
                    selectedCategory = null
                    categoryAdapter.setSelectedCategory(null)
                    showMessage("${category.name} kategorisi silindi")
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun navigateToCategoryDetail(category: Category) {
        val intent = Intent(this, CategoryDetailActivity::class.java).apply {
            putExtra("categoryId", category.id)
            putExtra("categoryName", category.name)
            putExtra("categoryIcon", category.icon)
        }
        startActivity(intent)
    }

    private fun moveCategory(fromPosition: Int, toPosition: Int) {
        val currentList = categoryAdapter.currentList.toMutableList()
        val item = currentList.removeAt(fromPosition)
        currentList.add(toPosition, item)
        categoryAdapter.submitList(currentList)
    }

    private fun setupClickListeners() {
        binding.addCategoryFab.setOnClickListener { showAddCategoryDialog() }
        binding.backupButton.setOnClickListener { checkPermissionsAndBackup() }
        binding.restoreButton.setOnClickListener { checkPermissionsAndRestore() }
        
        binding.editCategoryButton.setOnClickListener {
            if (selectedCategory == null) {
                showMessage("Lütfen yukarıdan bir kategori seçiniz")
            } else {
                showEditCategoryDialog(selectedCategory!!)
            }
        }
        
        binding.deleteCategoryButton.setOnClickListener {
            if (selectedCategory == null) {
                showMessage("Lütfen yukarıdan bir kategori seçiniz")
            } else {
                showDeleteCategoryConfirmation(selectedCategory!!)
            }
        }
    }

    private fun showAddCategoryDialog() {
        val dialogBinding = DialogAddCategoryBinding.inflate(layoutInflater)
        var selectedIcon = iconOptions[0]
        var selectedColor = defaultCategoryColor

        // Dialog başlığını ayarla
        dialogBinding.dialogTitle.text = "Yeni Kategori"
        dialogBinding.saveButton.text = "Kaydet"

        // Önce adaptörleri kur
        setupIconSelection(dialogBinding) { selectedIcon = it }
        setupColorSelection(dialogBinding) { selectedColor = it }

        // İlk seçimleri yap
        (dialogBinding.iconRecyclerView.adapter as IconAdapter).setSelectedIcon(0)
        (dialogBinding.colorRecyclerView.adapter as ColorAdapter).setSelectedColor(0)

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
            .apply {
                dialogBinding.saveButton.setOnClickListener {
                    handleCategorySave(dialogBinding, selectedIcon, selectedColor, this)
                }
                dialogBinding.cancelButton.setOnClickListener { dismiss() }
                show()
            }
    }

    private fun handleCategorySave(
        dialogBinding: DialogAddCategoryBinding,
        selectedIcon: Int,
        selectedColor: Int,
        dialog: AlertDialog
    ) {
        val categoryName = dialogBinding.categoryNameEditText.text.toString()
        if (categoryName.isNotEmpty()) {
            addCategory(categoryName.uppercase(java.util.Locale.forLanguageTag("tr")), selectedIcon, selectedColor)
            dialog.dismiss()
        } else {
            showMessage("Kategori adı boş olamaz")
        }
    }

    private fun addCategory(name: String, icon: Int, color: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nextOrderIndex = db.categoryDao().getNextOrderIndex(userId) ?: 0
                val newCategory = Category(
                    userId = userId,
                    name = name,
                    icon = icon,
                    color = color,
                    orderIndex = nextOrderIndex
                )
                val categoryId = db.categoryDao().insertCategory(newCategory)
                Log.d(TAG, "Yeni kategori eklendi. ID: $categoryId, UserID: $userId")
                withContext(Dispatchers.Main) {
                    showMessage("$name kategorisi eklendi")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Kategori eklenirken hata. UserID: $userId", e)
                withContext(Dispatchers.Main) {
                    showMessage("Kategori eklenirken bir hata oluştu")
                }
            }
        }
    }

    private fun showEditCategoryDialog(category: Category) {
        val dialogBinding = DialogAddCategoryBinding.inflate(layoutInflater)
        var selectedIcon = category.icon
        var selectedColor = category.color

        // Mevcut kategori bilgilerini doldur
        dialogBinding.categoryNameEditText.setText(category.name)
        
        // Dialog başlığını güncelle
        dialogBinding.dialogTitle.text = "Kategoriyi Düzenle"
        dialogBinding.saveButton.text = "Güncelle"

        // Önce adaptörleri kur
        setupIconSelection(dialogBinding) { selectedIcon = it }
        setupColorSelection(dialogBinding) { selectedColor = it }

        // Mevcut kategori değerlerini seç
        val iconIndex = iconOptions.indexOf(category.icon)
        val colorIndex = colorOptions.indexOf(category.color)
        
        if (iconIndex != -1) {
            (dialogBinding.iconRecyclerView.adapter as IconAdapter).setSelectedIcon(iconIndex)
            selectedIcon = category.icon
        }
        
        if (colorIndex != -1) {
            (dialogBinding.colorRecyclerView.adapter as ColorAdapter).setSelectedColor(colorIndex)
            selectedColor = category.color
        }

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
            .apply {
                dialogBinding.saveButton.setOnClickListener {
                    handleCategoryUpdate(dialogBinding, category, selectedIcon, selectedColor, this)
                }
                dialogBinding.cancelButton.setOnClickListener { dismiss() }
                show()
            }
    }

    private fun handleCategoryUpdate(
        dialogBinding: DialogAddCategoryBinding,
        category: Category,
        selectedIcon: Int,
        selectedColor: Int,
        dialog: AlertDialog
    ) {
        val categoryName = dialogBinding.categoryNameEditText.text.toString()
        if (categoryName.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val updatedCategory = category.copy(
                        name = categoryName.uppercase(java.util.Locale.forLanguageTag("tr")),
                        icon = selectedIcon,
                        color = selectedColor
                    )
                    db.categoryDao().updateCategory(updatedCategory)
                    withContext(Dispatchers.Main) {
                        selectedCategory = null
                        categoryAdapter.setSelectedCategory(null)
                        showMessage("$categoryName kategorisi güncellendi")
                        dialog.dismiss()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Kategori güncellenirken hata", e)
                    withContext(Dispatchers.Main) {
                        showMessage("Kategori güncellenirken bir hata oluştu")
                    }
                }
            }
        } else {
            showMessage("Kategori adı boş olamaz")
        }
    }

    private fun showSettingsDialog() {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_settings, null)
        val usernameEditText = dialogBinding.findViewById<TextInputEditText>(R.id.usernameEditText)
        val passwordEditText = dialogBinding.findViewById<TextInputEditText>(R.id.passwordEditText)
        val confirmPasswordEditText = dialogBinding.findViewById<TextInputEditText>(R.id.confirmPasswordEditText)
        val saveButton = dialogBinding.findViewById<MaterialButton>(R.id.saveButton)
        val cancelButton = dialogBinding.findViewById<MaterialButton>(R.id.cancelButton)

        lifecycleScope.launch {
            val currentUser = db.userDao().getUserById(userId)
            currentUser?.let {
                usernameEditText.setText(it.username)
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding)
            .create()

        saveButton.setOnClickListener {
            val newPassword = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            when {
                newPassword.isEmpty() -> {
                    showMessage("Lütfen yeni şifrenizi giriniz")
                }
                newPassword != confirmPassword -> {
                    showMessage("Şifreler eşleşmiyor")
                }
                newPassword.length < 6 -> {
                    showMessage("Şifre en az 6 karakter olmalıdır")
                }
                else -> {
                    lifecycleScope.launch {
                        try {
                            val currentUser = db.userDao().getUserById(userId)
                            if (currentUser != null) {
                                db.userDao().updateUser(userId, currentUser.username, newPassword)
                                showMessage("Şifre başarıyla güncellendi")
                                dialog.dismiss()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Şifre güncellenirken hata", e)
                            showMessage("Şifre güncellenirken bir hata oluştu")
                        }
                    }
                }
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}

private fun TextView.addTextWatcher(afterTextChanged: (Editable) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) = afterTextChanged(s)
    })
}
