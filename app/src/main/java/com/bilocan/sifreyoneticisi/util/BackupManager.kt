package com.bilocan.sifreyoneticisi.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.bilocan.sifreyoneticisi.data.AppDatabase
import com.bilocan.sifreyoneticisi.model.Category
import com.bilocan.sifreyoneticisi.model.Password
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import java.security.SecureRandom

private const val TAG = "BackupManager"
private const val ALGORITHM = "AES/CBC/PKCS5Padding"
private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
private const val ITERATIONS = 10000
private const val KEY_LENGTH = 256

data class BackupData(
    val categories: List<Category>,
    val passwords: List<Password>,
    val version: Int = 1
)

data class EncryptedBackupData(
    val salt: String,
    val iv: String,
    val encryptedData: String
)

class BackupManager(private val context: Context) {
    private val gson: Gson = GsonBuilder().create()
    private val db = AppDatabase.getDatabase(context)
    private var backupPassword: String? = null

    fun setBackupPassword(password: String) {
        backupPassword = password
    }

    private fun generateKey(password: String, salt: ByteArray): SecretKeySpec {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val keyFactory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
        val secretKey = keyFactory.generateSecret(keySpec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }

    private fun encrypt(data: String, password: String): EncryptedBackupData {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)

        val key = generateKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))

        val encryptedBytes = cipher.doFinal(data.toByteArray())

        return EncryptedBackupData(
            salt = Base64.encodeToString(salt, Base64.NO_WRAP),
            iv = Base64.encodeToString(iv, Base64.NO_WRAP),
            encryptedData = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        )
    }

    private fun decrypt(encryptedData: EncryptedBackupData, password: String): String {
        val salt = Base64.decode(encryptedData.salt, Base64.NO_WRAP)
        val iv = Base64.decode(encryptedData.iv, Base64.NO_WRAP)
        val data = Base64.decode(encryptedData.encryptedData, Base64.NO_WRAP)

        val key = generateKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

        return String(cipher.doFinal(data))
    }

    suspend fun createBackup(uri: Uri) {
        try {
            withContext(Dispatchers.IO) {
                val password = backupPassword ?: throw BackupException("Yedekleme şifresi belirlenmemiş")
                
                val categories = db.categoryDao().getCategoriesAsList()
                val passwords = db.passwordDao().getAllPasswordsForCategories(categories.map { it.id })

                val backupData = BackupData(categories, passwords)
                val jsonData = gson.toJson(backupData)
                
                val encryptedData = encrypt(jsonData, password)
                val encryptedJson = gson.toJson(encryptedData)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(encryptedJson.toByteArray())
                } ?: throw BackupException("Yedekleme dosyası oluşturulamadı")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Yedekleme sırasında hata", e)
            throw BackupException(e.message ?: "Yedekleme sırasında bir hata oluştu")
        }
    }

    suspend fun restoreBackup(uri: Uri) {
        try {
            withContext(Dispatchers.IO) {
                val password = backupPassword ?: throw BackupException("Yedekleme şifresi girilmemiş")
                
                val encryptedJson = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    String(inputStream.readBytes())
                } ?: throw BackupException("Yedek dosyası açılamadı")

                val encryptedData = try {
                    gson.fromJson(encryptedJson, EncryptedBackupData::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Yedek dosyası okuma hatası", e)
                    throw BackupException("Geçersiz yedek dosyası")
                }

                val jsonData = try {
                    decrypt(encryptedData, password)
                } catch (e: Exception) {
                    Log.e(TAG, "Şifre çözme hatası", e)
                    throw BackupException("Yedek şifresi yanlış")
                }

                val backupData = try {
                    gson.fromJson(jsonData, BackupData::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "JSON dönüştürme hatası: ${e.message}", e)
                    throw BackupException("Yedek dosyası bozuk")
                }

                if (backupData.categories.isEmpty()) {
                    throw BackupException("Yedek dosyası boş veya geçersiz")
                }

                // Mevcut verileri temizle
                db.categoryDao().deleteAllCategories()
                db.passwordDao().deleteAllPasswords()

                // Kategori ID'lerini eşleştirmek için map oluştur
                val categoryIdMap = mutableMapOf<Int, Int>()

                // Önce kategorileri ekle
                backupData.categories.forEach { category ->
                    try {
                        val oldId = category.id
                        val newId = db.categoryDao().insertCategory(
                            category.copy(
                                id = 0,
                                passwordCount = 0,
                                orderIndex = category.orderIndex
                            )
                        ).toInt()
                        categoryIdMap[oldId] = newId
                    } catch (e: Exception) {
                        Log.e(TAG, "Kategori yükleme hatası", e)
                    }
                }

                // Sonra şifreleri ekle
                backupData.passwords.forEach { password ->
                    try {
                        val newCategoryId = categoryIdMap[password.categoryId]
                        if (newCategoryId != null) {
                            db.passwordDao().insertPassword(
                                password.copy(
                                    id = 0,
                                    categoryId = newCategoryId,
                                    orderIndex = password.orderIndex
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Şifre yükleme hatası", e)
                    }
                }

                // Kategori şifre sayılarını güncelle
                categoryIdMap.values.forEach { categoryId ->
                    try {
                        val category = db.categoryDao().getCategoryById(categoryId)
                        if (category != null) {
                            val passwordCount = db.passwordDao().getPasswordCountForCategory(categoryId)
                            db.categoryDao().updateCategory(category.copy(passwordCount = passwordCount))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Kategori güncelleme hatası", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Yedekten yükleme sırasında hata: ${e.message}", e)
            throw BackupException(e.message ?: "Yedekten yükleme sırasında bir hata oluştu")
        }
    }
}

class BackupException(message: String) : Exception(message) 