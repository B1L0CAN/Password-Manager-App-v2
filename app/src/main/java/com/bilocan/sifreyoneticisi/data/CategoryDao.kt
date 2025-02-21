package com.bilocan.sifreyoneticisi.data

import androidx.room.*
import com.bilocan.sifreyoneticisi.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY orderIndex ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY orderIndex ASC")
    suspend fun getCategoriesAsList(): List<Category>

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

    @Query("SELECT MAX(orderIndex) + 1 FROM categories")
    suspend fun getNextOrderIndex(): Int?

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
} 