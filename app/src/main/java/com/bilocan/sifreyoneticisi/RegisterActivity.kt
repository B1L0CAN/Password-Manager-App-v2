package com.bilocan.sifreyoneticisi

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bilocan.sifreyoneticisi.data.AppDatabase
import com.bilocan.sifreyoneticisi.data.User
import com.bilocan.sifreyoneticisi.databinding.ActivityRegisterBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "RegisterActivity"
private const val MIN_PASSWORD_LENGTH = 6

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)
        setupUI()

        val alertDialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle("Bilgilendirme")
            .setMessage("Kullanıcı ismi değiştirilememektedir, lütfen unutmayacağınız bir kullanıcı adı seçiniz. \n" +
                    "Ayrıca alacağınız yedekler de bu kullanıcı adı ile kayıt altına alınacaktır. " +
                    "Aldığınız yedekleri başka cihazda kullanmak isterseniz yine aynı kullanıcı adı ile kayıt olmanız gerekmektedir. \n" +
                    "Aldığınız yedeklerin .json dosya uzantısına sahip olmasına lütfen dikkat ediniz.")
            .setPositiveButton("Okudum") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()

        // Butonun rengini değiştirme
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.accent))
    }

    private fun setupUI() {
        setupBackButton()
        setupRegisterButton()
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener { 
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { 
                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                finish()
            }
        })
    }

    private fun setupRegisterButton() {
        binding.registerButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()

            when {
                username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                    showMessage("Lütfen tüm alanları doldurun")
                }
                password != confirmPassword -> {
                    showMessage("Şifreler eşleşmiyor")
                }
                password.length < MIN_PASSWORD_LENGTH -> {
                    showMessage("Şifre en az $MIN_PASSWORD_LENGTH karakter olmalıdır")
                }
                else -> {
                    handleRegistration(username, password)
                }
            }
        }
    }

    private fun handleRegistration(username: String, password: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (isUsernameAvailable(username)) {
                    registerUser(username, password)
                } else {
                    withContext(Dispatchers.Main) {
                        showMessage("Bu kullanıcı adı zaten kullanılıyor")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Kayıt işlemi sırasında hata", e)
                withContext(Dispatchers.Main) {
                    showMessage("Kayıt işlemi sırasında bir hata oluştu")
                }
            }
        }
    }

    private suspend fun isUsernameAvailable(username: String): Boolean =
        db.userDao().getUserByUsername(username) == null

    private suspend fun registerUser(username: String, password: String) {
        val newUser = User(username = username, password = password)
        db.userDao().insertUser(newUser)
        
        withContext(Dispatchers.Main) {
            showMessage("Kayıt başarıyla oluşturuldu")
            finish()
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
} 