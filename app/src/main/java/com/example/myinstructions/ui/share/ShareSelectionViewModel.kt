package com.example.myinstructions.ui.share

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.myinstructions.data.AppDatabase
import com.example.myinstructions.data.repository.CategoryRepository
import com.example.myinstructions.data.repository.TaskRepository
import com.example.myinstructions.data.relation.CategoryWithTasks
import com.example.myinstructions.data.relation.TaskWithInstructions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

class ShareSelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = TaskRepository(db.taskDao(), db.instructionDao(), db.categoryDao())
    private val categoryRepository = CategoryRepository(db.categoryDao())

    val categoriesWithTasks: Flow<List<CategoryWithTasks>> = categoryRepository.getAllCategoriesWithTasks()
    val uncategorizedTaskIds: Flow<List<Long>> = categoryRepository.getUncategorizedTaskIds()
    val allTasksWithInstructions: Flow<List<TaskWithInstructions>> = repository.getAllTasksWithInstructions()

    private val _selectedTaskIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTaskIds: StateFlow<Set<Long>> = _selectedTaskIds

    fun preselectTask(taskId: Long) {
        _selectedTaskIds.value = setOf(taskId)
    }

    fun preselectTasks(taskIds: Set<Long>) {
        _selectedTaskIds.value = taskIds
    }

    fun toggleTask(taskId: Long) {
        _selectedTaskIds.value = _selectedTaskIds.value.let { set ->
            if (taskId in set) set - taskId else set + taskId
        }
    }

    fun toggleCategory(taskIds: List<Long>) {
        val current = _selectedTaskIds.value
        val allSelected = taskIds.all { it in current }
        _selectedTaskIds.value = if (allSelected) {
            current - taskIds.toSet()
        } else {
            current + taskIds.toSet()
        }
    }

    suspend fun buildShareableTasks(): List<ShareableTask> {
        val allTasks = allTasksWithInstructions.first()
        val selected = _selectedTaskIds.value
        return allTasks
            .filter { it.task.id in selected }
            .map { twi ->
                ShareableTask(
                    name = twi.task.name,
                    instructions = twi.sortedInstructions
                        .filter { it.text.isNotBlank() }
                        .map { it.text }
                )
            }
    }
}
