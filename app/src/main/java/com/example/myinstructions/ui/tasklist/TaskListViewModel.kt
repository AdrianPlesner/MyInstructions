package com.example.myinstructions.ui.tasklist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myinstructions.data.AppDatabase
import com.example.myinstructions.data.repository.TaskRepository
import com.example.myinstructions.util.ImageStorageHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class TaskListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = TaskRepository(db.taskDao(), db.instructionDao())

    private val _searchQuery = MutableStateFlow("")

    val tasks: Flow<List<TaskItem>> = combine(
        repository.getAllTasksWithInstructions(),
        _searchQuery
    ) { list, query ->
        if (query.isBlank()) {
            list.map { twi ->
                TaskItem(
                    id = twi.task.id,
                    name = twi.task.name,
                    instructionCount = twi.instructions.size
                )
            }
        } else {
            list.mapNotNull { twi ->
                val titleMatch = twi.task.name.contains(query, ignoreCase = true)
                val instructionMatch = twi.instructions.any {
                    it.text.contains(query, ignoreCase = true)
                }
                when {
                    titleMatch -> 2 to twi
                    instructionMatch -> 1 to twi
                    else -> null
                }
            }
                .sortedByDescending { it.first }
                .map { (_, twi) ->
                    TaskItem(
                        id = twi.task.id,
                        name = twi.task.name,
                        instructionCount = twi.instructions.size
                    )
                }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            val imageUris = repository.getImageUrisForTask(taskId)
            ImageStorageHelper.deleteImages(getApplication(), imageUris)
            repository.deleteTask(taskId)
        }
    }
}
