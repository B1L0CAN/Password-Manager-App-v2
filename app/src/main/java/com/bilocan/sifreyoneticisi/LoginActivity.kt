package com.bilocan.sifreyoneticisi

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bilocan.sifreyoneticisi.data.AppDatabase
import com.bilocan.sifreyoneticisi.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "LoginActivity"
private const val PREFS_NAME = "login_prefs"
private const val KEY_USERNAME = "saved_username"
private const val KEY_PASSWORD = "saved_password"

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate başladı")
        
        try {
            binding = ActivityLoginBinding.inflate(layoutInflater)
            setContentView(binding.root)
            db = AppDatabase.getDatabase(this)
            setupUI()
        } catch (e: Exception) {
            Log.e(TAG, "onCreate'de hata", e)
            showMessage("Uygulama başlatılırken bir hata oluştu")
            finish()
        }
    }

    private fun setupUI() {
        loadSavedCredentials()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener { handleLogin() }
        binding.registerButton.setOnClickListener { navigateToRegister() }
    }

    private fun handleLogin() {
        val username = binding.usernameEditText.text.toString()
        val password = binding.passwordEditText.text.toString()

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Lütfen tüm alanları doldurun")
            return
        }

        Log.d(TAG, "Giriş denemesi başlıyor - Kullanıcı: $username")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val user = db.userDao().getUser(username, password)
                Log.d(TAG, "Veritabanı sorgusu tamamlandı - Kullanıcı bulundu: ${user != null}")
                
                withContext(Dispatchers.Main) {
                    if (user != null) {
                        handleSuccessfulLogin(user.id, username, password)
                    } else {
                        showMessage("Kullanıcı adı veya şifre hatalı")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Giriş yaparken hata", e)
                withContext(Dispatchers.Main) {
                    showMessage("Giriş yapılırken bir hata oluştu")
                }
            }
        }
    }

    private fun handleSuccessfulLogin(userId: Int, username: String, password: String) {
        try {
            if (binding.rememberPasswordCheckbox.isChecked) {
                saveCredentials(username, password)
            } else {
                clearSavedCredentials()
            }
            
            Log.d(TAG, "MainActivity'ye geçiş yapılıyor - User ID: $userId")
            startMainActivity(userId)
        } catch (e: Exception) {
            Log.e(TAG, "MainActivity başlatılırken hata", e)
            showMessage("Ana ekran açılırken bir hata oluştu")
        }
    }

    private fun startMainActivity(userId: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("user_id", userId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToRegister() {
        try {
            startActivity(Intent(this, RegisterActivity::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Kayıt ekranına geçerken hata", e)
            showMessage("Kayıt ekranı açılırken bir hata oluştu")
        }
    }

    private fun saveCredentials(username: String, password: String) {
        try {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
                putString(KEY_USERNAME, username)
                putString(KEY_PASSWORD, password)
                apply()
            }
            Log.d(TAG, "Kimlik bilgileri kaydedildi")
        } catch (e: Exception) {
            Log.e(TAG, "Kimlik bilgileri kaydedilirken hata", e)
        }
    }

    private fun loadSavedCredentials() {
        try {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val savedUsername = prefs.getString(KEY_USERNAME, "")
            val savedPassword = prefs.getString(KEY_PASSWORD, "")
            
            if (!savedUsername.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
                binding.apply {
                    usernameEditText.setText(savedUsername)
                    passwordEditText.setText(savedPassword)
                    rememberPasswordCheckbox.isChecked = true
                }
                Log.d(TAG, "Kayıtlı kimlik bilgileri yüklendi")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Kayıtlı kimlik bilgileri yüklenirken hata", e)
        }
    }

    private fun clearSavedCredentials() {
        try {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply()
            Log.d(TAG, "Kayıtlı kimlik bilgileri temizlendi")
        } catch (e: Exception) {
            Log.e(TAG, "Kimlik bilgileri temizlenirken hata", e)
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
                duration = Toast.LENGTH_LONG
                this.view = view
                show()
            }
            
            Log.d(TAG, "Mesaj gösterildi: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Mesaj gösterilirken hata", e)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
} 