package com.bilocan.sifreyoneticisi

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bilocan.sifreyoneticisi.data.AppDatabase
import com.bilocan.sifreyoneticisi.databinding.ActivityAppPasswordBinding
import com.bilocan.sifreyoneticisi.databinding.ActivityAppPasswordRegisterBinding
import com.bilocan.sifreyoneticisi.model.AppPassword
import kotlinx.coroutines.launch

class AppPasswordActivity : AppCompatActivity() {
    private var _binding: Any? = null
    private val loginBinding get() = _binding as ActivityAppPasswordBinding
    private val registerBinding get() = _binding as ActivityAppPasswordRegisterBinding
    private lateinit var db: AppDatabase
    private var isSettingPassword = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.getDatabase(this)

        lifecycleScope.launch {
            isSettingPassword = !db.appPasswordDao().hasPassword()
            setupUI()
            if (!isSettingPassword) {
                showKeyboard()
            }
        }
    }
    
    private fun showKeyboard() {
        loginBinding.pinEditText.postDelayed({
            loginBinding.pinEditText.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(loginBinding.pinEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 200)
    }

    private fun setupUI() {
        if (isSettingPassword) {
            _binding = ActivityAppPasswordRegisterBinding.inflate(layoutInflater)
            setContentView(registerBinding.root)
            setupRegisterPinEditText()
        } else {
            _binding = ActivityAppPasswordBinding.inflate(layoutInflater)
            setContentView(loginBinding.root)
            setupLoginPinEditText()
            loginBinding.pinEditText.requestFocus()
        }
    }

    private fun setupRegisterPinEditText() {
        var pin = ""
        var confirmPin = ""

        registerBinding.pinEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                pin = s?.toString() ?: ""
                if (pin.length == 6 && confirmPin.length == 6) {
                    handlePinRegistration(pin, confirmPin)
                }
                registerBinding.errorText.visibility = View.GONE
            }
        })

        registerBinding.confirmPinEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                confirmPin = s?.toString() ?: ""
                if (pin.length == 6 && confirmPin.length == 6) {
                    handlePinRegistration(pin, confirmPin)
                }
                registerBinding.errorText.visibility = View.GONE
            }
        })
    }

    private fun setupLoginPinEditText() {
        loginBinding.pinEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 6) {
                    handlePinLogin(s.toString())
                }
                loginBinding.errorText.visibility = View.GONE
            }
        })
    }

    private fun handlePinRegistration(pin: String, confirmPin: String) {
        if (pin != confirmPin) {
            showError("Şifreler eşleşmiyor")
            registerBinding.confirmPinEditText.text?.clear()
            return
        }

        lifecycleScope.launch {
            db.appPasswordDao().setAppPassword(AppPassword(password = pin))
            startMainActivity()
        }
    }

    private fun handlePinLogin(pin: String) {
        lifecycleScope.launch {
            val appPassword = db.appPasswordDao().getAppPassword()
            if (appPassword?.password == pin) {
                startMainActivity()
            } else {
                showError("Yanlış şifre, lütfen tekrar deneyin")
                loginBinding.pinEditText.text?.clear()
            }
        }
    }

    private fun showError(message: String) {
        if (isSettingPassword) {
            registerBinding.errorText.text = message
            registerBinding.errorText.visibility = View.VISIBLE
        } else {
            loginBinding.errorText.text = message
            loginBinding.errorText.visibility = View.VISIBLE
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
} 