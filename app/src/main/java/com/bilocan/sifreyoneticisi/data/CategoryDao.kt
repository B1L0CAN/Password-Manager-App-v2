package com.bilocan.sifreyoneticisi.data

import androidx.room.*
import com.bilocan.sifreyoneticisi.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY orderIndex ASC")
    fun getCategoriesForUser(userId: Int): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY orderIndex ASC")
    suspend fun getCategoriesForUserAsList(userId: Int): List<Category>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Int): Category?

    @Insert
    suspend fun insertCategory(category: Category): Long

    @Delete
    suspend fun deleteCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)

    @Query("UPDATE categories SET orderIndex = :newIndex WHERE id = :categoryId")
    suspend fun updateCategoryOrder(categoryId: Int, newIndex: Int)

    @Query("SELECT MAX(orderIndex) + 1 FROM categories WHERE userId = :userId")
    suspend fun getNextOrderIndex(userId: Int): Int?

    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<Category>

    @Query("DELETE FROM categories WHERE userId = :userId")
    suspend fun deleteAllCategoriesForUser(userId: Int)
} 