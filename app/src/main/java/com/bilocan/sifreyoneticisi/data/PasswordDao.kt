package com.bilocan.sifreyoneticisi.data

import androidx.room.*
import com.bilocan.sifreyoneticisi.model.Password
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    @Query("SELECT * FROM passwords WHERE categoryId = :categoryId ORDER BY orderIndex ASC")
    fun getPasswordsForCategory(categoryId: Int): Flow<List<Password>>

    @Query("SELECT * FROM passwords WHERE categoryId = :categoryId ORDER BY orderIndex ASC")
    suspend fun getPasswordsForCategoryAsList(categoryId: Int): List<Password>

    @Query("SELECT * FROM passwords WHERE id = :passwordId")
    suspend fun getPasswordById(passwordId: Int): Password?

    @Insert
    suspend fun insertPassword(password: Password): Long

    @Update
    suspend fun updatePassword(password: Password)

    @Delete
    suspend fun deletePassword(password: Password)

    @Query("DELETE FROM passwords WHERE categoryId = :categoryId")
    suspend fun deletePasswordsForCategory(categoryId: Int)

    @Query("DELETE FROM passwords")
    suspend fun deleteAllPasswords()

    @Query("SELECT COUNT(*) FROM passwords WHERE categoryId = :categoryId")
    suspend fun getPasswordCountForCategory(categoryId: Int): Int

    @Query("SELECT * FROM passwords WHERE categoryId IN (:categoryIds)")
    suspend fun getAllPasswordsForCategories(categoryIds: List<Int>): List<Password>

    @Query("SELECT COALESCE(MAX(orderIndex), -1) + 1 FROM passwords WHERE categoryId = :categoryId")
    suspend fun getNextOrderIndex(categoryId: Int): Int?

    @Query("UPDATE passwords SET orderIndex = :orderIndex WHERE id = :passwordId")
    suspend fun updatePasswordOrder(passwordId: Int, orderIndex: Int)
} 