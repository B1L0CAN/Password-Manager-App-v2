package com.bilocan.sifreyoneticisi.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bilocan.sifreyoneticisi.R
import com.bilocan.sifreyoneticisi.data.AppDatabase
import com.bilocan.sifreyoneticisi.model.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    init {
        loadCategories()
    }

    fun addCategory(name: String, icon: Int = R.drawable.ic_folder, color: Int = R.color.category_yellow) {
        viewModelScope.launch {
            val category = Category(
                name = name,
                icon = icon,
                color = color,
                passwordCount = 0
            )
            db.categoryDao().insertCategory(category)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            db.categoryDao().updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            db.categoryDao().deleteCategory(category)
        }
    }

    fun updateCategoryOrder(categoryId: Int, newIndex: Int) {
        viewModelScope.launch {
            db.categoryDao().updateCategoryOrder(categoryId, newIndex)
        }
    }

    fun deleteAllCategories() {
        viewModelScope.launch {
            db.categoryDao().deleteAllCategories()
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            db.categoryDao().getAllCategories().collect { categories ->
                _categories.value = categories
            }
        }
    }
} 