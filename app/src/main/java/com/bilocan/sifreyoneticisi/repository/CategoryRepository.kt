package com.bilocan.sifreyoneticisi.repository

import com.bilocan.sifreyoneticisi.data.CategoryDao
import com.bilocan.sifreyoneticisi.model.Category
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {

    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun getCategoriesAsList(): List<Category> = categoryDao.getCategoriesAsList()

    suspend fun getCategoryById(categoryId: Int): Category? = categoryDao.getCategoryById(categoryId)

    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)

    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)

    suspend fun updateCategoryOrder(categoryId: Int, newIndex: Int) = 
        categoryDao.updateCategoryOrder(categoryId, newIndex)

    suspend fun getNextOrderIndex(): Int = categoryDao.getNextOrderIndex() ?: 0

    suspend fun deleteAllCategories() = categoryDao.deleteAllCategories()
} 