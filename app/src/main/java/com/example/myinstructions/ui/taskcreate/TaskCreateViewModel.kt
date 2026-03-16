package com.example.myinstructions.ui.taskcreate

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myinstructions.data.AppDatabase
import com.example.myinstructions.data.repository.TaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TaskCreateViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = TaskRepository(db.taskDao(), db.instructionDao())

    fun loadTask(taskId: Long, onLoaded: (name: String, instructions: List<InstructionDraft>) -> Unit) {
        viewModelScope.launch {
            val taskWithInstructions = repository.getTaskWithInstructions(taskId).first()
            if (taskWithInstructions != null) {
                val drafts = taskWithInstructions.sortedInstructions.map {
                    InstructionDraft(text = it.text, imageUri = it.imageUri)
                }
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
            if (taskId == -1L) {
                repository.createTask(name, pairs)
            } else {
                repository.updateTask(taskId, name, pairs)
            }
            onDone()
        }
    }
}
