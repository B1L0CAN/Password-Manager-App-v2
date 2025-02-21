package com.bilocan.sifreyoneticisi.repository

import com.bilocan.sifreyoneticisi.data.PasswordDao
import com.bilocan.sifreyoneticisi.model.Password
import kotlinx.coroutines.flow.Flow

class PasswordRepository(private val passwordDao: PasswordDao) {

    fun getPasswordsForCategory(categoryId: Int): Flow<List<Password>> =
        passwordDao.getPasswordsForCategory(categoryId)

    suspend fun getPasswordsForCategoryAsList(categoryId: Int): List<Password> =
        passwordDao.getPasswordsForCategoryAsList(categoryId)

    suspend fun getPasswordById(passwordId: Int): Password? =
        passwordDao.getPasswordById(passwordId)

    suspend fun insertPassword(password: Password): Long =
        passwordDao.insertPassword(password)

    suspend fun updatePassword(password: Password) =
        passwordDao.updatePassword(password)

    suspend fun deletePassword(password: Password) =
        passwordDao.deletePassword(password)

    suspend fun deletePasswordsForCategory(categoryId: Int) =
        passwordDao.deletePasswordsForCategory(categoryId)

    suspend fun deleteAllPasswords() =
        passwordDao.deleteAllPasswords()

    suspend fun getNextOrderIndex(categoryId: Int): Int? =
        passwordDao.getNextOrderIndex(categoryId)

    suspend fun updatePasswordOrder(passwordId: Int, newIndex: Int) =
        passwordDao.updatePasswordOrder(passwordId, newIndex)
} 