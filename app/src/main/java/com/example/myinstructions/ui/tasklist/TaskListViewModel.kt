package com.example.myinstructions.ui.tasklist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myinstructions.data.AppDatabase
import com.example.myinstructions.data.repository.TaskRepository
import com.example.myinstructions.util.ImageStorageHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TaskListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = TaskRepository(db.taskDao(), db.instructionDao())

    val tasks: Flow<List<TaskItem>> = repository.getAllTasksWithInstructions().map { list ->
        list.map { twi ->
            TaskItem(
                id = twi.task.id,
                name = twi.task.name,
                instructionCount = twi.instructions.size
            )
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            val imageUris = repository.getImageUrisForTask(taskId)
            ImageStorageHelper.deleteImages(getApplication(), imageUris)
            repository.deleteTask(taskId)
        }
    }
}
