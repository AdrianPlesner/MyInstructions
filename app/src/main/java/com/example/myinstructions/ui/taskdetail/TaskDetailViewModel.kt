package com.example.myinstructions.ui.taskdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myinstructions.data.AppDatabase
import com.example.myinstructions.data.relation.TaskWithInstructions
import com.example.myinstructions.data.repository.TaskRepository
import com.example.myinstructions.util.ImageStorageHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TaskDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = TaskRepository(db.taskDao(), db.instructionDao())

    fun getTaskWithInstructions(taskId: Long): Flow<TaskWithInstructions?> =
        repository.getTaskWithInstructions(taskId)

    fun markViewed(taskId: Long) {
        viewModelScope.launch {
            repository.touchLastViewed(taskId)
        }
    }

    fun deleteTask(taskId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            val imageUris = repository.getImageUrisForTask(taskId)
            ImageStorageHelper.deleteImages(getApplication(), imageUris)
            repository.deleteTask(taskId)
            onDone()
        }
    }
}
