package com.bilocan.sifreyoneticisi.data

import androidx.room.*
import com.bilocan.sifreyoneticisi.model.Password
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    @Query("SELECT * FROM passwords WHERE categoryId = :categoryId ORDER BY orderIndex ASC")
    fun getPasswordsForCategory(categoryId: Int): Flow<List<Password>>

    @Insert
    suspend fun insertPassword(password: Password): Long

    @Delete
    suspend fun deletePassword(password: Password)

    @Update
    suspend fun updatePassword(password: Password)

    @Query("SELECT COUNT(*) FROM passwords WHERE categoryId = :categoryId")
    suspend fun getPasswordCountForCategory(categoryId: Int): Int

    @Query("SELECT COALESCE(MAX(orderIndex), -1) + 1 FROM passwords WHERE categoryId = :categoryId")
    suspend fun getNextOrderIndex(categoryId: Int): Int?

    @Query("UPDATE passwords SET orderIndex = :orderIndex WHERE id = :passwordId")
    suspend fun updatePasswordOrder(passwordId: Int, orderIndex: Int)

    @Query("SELECT * FROM passwords WHERE categoryId IN (:categoryIds)")
    suspend fun getAllPasswordsForCategories(categoryIds: List<Int>): List<Password>
} 