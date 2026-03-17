package com.example.myinstructions.ui.category

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myinstructions.data.AppDatabase
import com.example.myinstructions.data.entity.CategoryEntity
import com.example.myinstructions.data.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class CategoryManageViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = CategoryRepository(db.categoryDao())

    val categories: Flow<List<CategoryEntity>> = repository.getAllCategories()

    fun addCategory(name: String) {
        viewModelScope.launch {
            repository.createCategory(name)
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
        }
    }
}
