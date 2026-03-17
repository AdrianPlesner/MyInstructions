package com.example.myinstructions.ui.taskcreate

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myinstructions.data.AppDatabase
import com.example.myinstructions.data.entity.CategoryEntity
import com.example.myinstructions.data.repository.CategoryRepository
import com.example.myinstructions.data.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TaskCreateViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = TaskRepository(db.taskDao(), db.instructionDao(), db.categoryDao())
    private val categoryRepository = CategoryRepository(db.categoryDao())

    val allCategories: Flow<List<CategoryEntity>> = categoryRepository.getAllCategories()

    private val _selectedCategoryIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedCategoryIds: StateFlow<Set<Long>> = _selectedCategoryIds

    fun toggleCategory(categoryId: Long) {
        _selectedCategoryIds.value = _selectedCategoryIds.value.let { set ->
            if (categoryId in set) set - categoryId else set + categoryId
        }
    }

    fun createCategoryInline(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = categoryRepository.createCategory(name)
            _selectedCategoryIds.value = _selectedCategoryIds.value + id
            onCreated(id)
        }
    }

    fun loadTask(taskId: Long, onLoaded: (name: String, instructions: List<InstructionDraft>) -> Unit) {
        viewModelScope.launch {
            val taskWithInstructions = repository.getTaskWithInstructions(taskId).first()
            if (taskWithInstructions != null) {
                val drafts = taskWithInstructions.sortedInstructions.map {
                    InstructionDraft(text = it.text, imageUri = it.imageUri)
                }
                val categories = categoryRepository.getCategoriesForTask(taskId)
                _selectedCategoryIds.value = categories.map { it.id }.toSet()

                onLoaded(taskWithInstructions.task.name, drafts)
            }
        }
    }

    fun saveTask(
        taskId: Long,
        name: String,
        instructions: List<InstructionDraft>,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val pairs = instructions
                .filter { it.text.isNotBlank() || it.imageUri != null }
                .map { it.text to it.imageUri }
            val categoryIds = _selectedCategoryIds.value.toList()
            if (taskId == -1L) {
                repository.createTask(name, pairs, categoryIds)
            } else {
                repository.updateTask(taskId, name, pairs, categoryIds)
            }
            onDone()
        }
    }
}
