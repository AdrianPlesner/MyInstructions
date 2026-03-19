package com.example.myinstructions.data.repository

import com.example.myinstructions.data.dao.CategoryDao
import com.example.myinstructions.data.entity.CategoryEntity
import com.example.myinstructions.data.relation.CategoryWithTasks
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {

    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    fun getAllCategoriesWithTasks(): Flow<List<CategoryWithTasks>> =
        categoryDao.getAllCategoriesWithTasks()

    fun getUncategorizedTaskIds(): Flow<List<Long>> = categoryDao.getUncategorizedTaskIds()

    suspend fun getCategoriesForTask(taskId: Long): List<CategoryEntity> =
        categoryDao.getCategoriesForTask(taskId)

    suspend fun createCategory(name: String): Long =
        categoryDao.insert(CategoryEntity(name = name))

    suspend fun deleteCategory(categoryId: Long) =
        categoryDao.deleteById(categoryId)

    suspend fun setTaskCategories(taskId: Long, categoryIds: List<Long>) =
        categoryDao.setTaskCategories(taskId, categoryIds)

    suspend fun getAllCategoriesOnce(): List<CategoryEntity> = categoryDao.getAllCategoriesOnce()
}
