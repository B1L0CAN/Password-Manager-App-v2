package com.bilocan.sifreyoneticisi.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.bilocan.sifreyoneticisi.data.AppDatabase
import com.bilocan.sifreyoneticisi.model.Category
import com.bilocan.sifreyoneticisi.model.Password
import com.bilocan.sifreyoneticisi.data.User
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "BackupManager"

data class BackupData(
    val users: List<User>,
    val categories: List<Category>,
    val passwords: List<Password>,
    val version: Int = 1
)

class BackupManager(private val context: Context) {
    private val gson: Gson = GsonBuilder().create()
    private val db = AppDatabase.getDatabase(context)

    suspend fun createBackup(uri: Uri, userId: Int) {
        try {
            withContext(Dispatchers.IO) {
                val user = db.userDao().getUserById(userId) ?: throw BackupException("Kullanıcı bulunamadı")
                val categories = db.categoryDao().getCategoriesForUserAsList(userId)
                val passwords = db.passwordDao().getAllPasswordsForCategories(categories.map { it.id })

                val backupData = BackupData(listOf(user), categories, passwords)
                val jsonData = gson.toJson(backupData)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonData.toByteArray())
                } ?: throw BackupException("Yedekleme dosyası oluşturulamadı")
            }
        } catch (e: Exception) {
            throw BackupException(e.message ?: "Yedekleme sırasında bir hata oluştu")
        }
    }

    suspend fun restoreBackup(uri: Uri, currentUserId: Int) {
        try {
            withContext(Dispatchers.IO) {
                val jsonData = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    String(inputStream.readBytes())
                } ?: throw BackupException("Yedek dosyası açılamadı")

                val backupData = try {
                    gson.fromJson(jsonData, BackupData::class.java)
                } catch (e: Exception) {
                    throw BackupException("Yedek dosyası geçersiz format içeriyor")
                }

                val currentUser = db.userDao().getUserById(currentUserId)
                    ?: throw BackupException("Kullanıcı bilgisi alınamadı")

                val backupUser = backupData.users.firstOrNull()
                    ?: throw BackupException("Yedek dosyasında kullanıcı bilgisi bulunamadı")

                if (backupUser.username != currentUser.username) {
                    throw BackupException("Bu yedek dosyası başka bir kullanıcıya ait")
                }

                db.categoryDao().deleteAllCategoriesForUser(currentUserId)
                
                val categoryIdMap = mutableMapOf<Int, Int>()
                
                backupData.categories.forEach { category ->
                    val oldId = category.id
                    val newCategory = category.copy(id = 0, userId = currentUserId)
                    val newId = db.categoryDao().insertCategory(newCategory).toInt()
                    categoryIdMap[oldId] = newId
                }

                backupData.passwords.forEach { password ->
                    val newCategoryId = categoryIdMap[password.categoryId] ?: return@forEach
                    val newPassword = password.copy(id = 0, categoryId = newCategoryId)
                    db.passwordDao().insertPassword(newPassword)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Yedek geri yüklenirken hata", e)
            throw BackupException(e.message ?: "Geri yükleme sırasında bir hata oluştu")
        }
    }
}

class BackupException(message: String) : Exception(message) 