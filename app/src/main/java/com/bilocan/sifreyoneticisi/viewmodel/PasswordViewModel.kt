package com.bilocan.sifreyoneticisi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bilocan.sifreyoneticisi.model.Password
import com.bilocan.sifreyoneticisi.repository.PasswordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PasswordViewModel(private val repository: PasswordRepository) : ViewModel() {

    fun getPasswordsForCategory(categoryId: Int): Flow<List<Password>> =
        repository.getPasswordsForCategory(categoryId)

    fun addPassword(title: String, username: String, password: String, categoryId: Int) {
        viewModelScope.launch {
            val nextOrderIndex = repository.getNextOrderIndex(categoryId) ?: 0
            val newPassword = Password(
                title = title,
                username = username,
                password = password,
                categoryId = categoryId,
                orderIndex = nextOrderIndex
            )
            repository.insertPassword(newPassword)
        }
    }

    fun updatePassword(password: Password) {
        viewModelScope.launch {
            repository.updatePassword(password)
        }
    }

    fun deletePassword(password: Password) {
        viewModelScope.launch {
            repository.deletePassword(password)
        }
    }

    fun deletePasswordsForCategory(categoryId: Int) {
        viewModelScope.launch {
            repository.deletePasswordsForCategory(categoryId)
        }
    }

    fun deleteAllPasswords() {
        viewModelScope.launch {
            repository.deleteAllPasswords()
        }
    }

    fun updatePasswordOrder(passwordId: Int, newIndex: Int) {
        viewModelScope.launch {
            repository.updatePasswordOrder(passwordId, newIndex)
        }
    }
}

class PasswordViewModelFactory(private val repository: PasswordRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PasswordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PasswordViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 