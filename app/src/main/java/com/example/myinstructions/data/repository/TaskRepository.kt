package com.example.myinstructions.data.repository

import com.example.myinstructions.data.dao.CategoryDao
import com.example.myinstructions.data.dao.InstructionDao
import com.example.myinstructions.data.dao.TaskDao
import com.example.myinstructions.data.entity.InstructionEntity
import com.example.myinstructions.data.entity.TaskEntity
import com.example.myinstructions.data.relation.TaskWithInstructions
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val instructionDao: InstructionDao,
    private val categoryDao: CategoryDao? = null
) {
    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    fun getAllTasksWithInstructions(): Flow<List<TaskWithInstructions>> =
        taskDao.getAllTasksWithInstructions()

    fun getTaskWithInstructions(taskId: Long): Flow<TaskWithInstructions?> =
        taskDao.getTaskWithInstructions(taskId)

    suspend fun createTask(
        name: String,
        instructions: List<Pair<String, String?>>,
        categoryIds: List<Long> = emptyList()
    ): Long {
        val now = System.currentTimeMillis()
        val taskId = taskDao.insert(TaskEntity(name = name, createdAt = now, updatedAt = now))
        val entities = instructions.mapIndexed { index, (text, imageUri) ->
            InstructionEntity(taskId = taskId, orderIndex = index, text = text, imageUri = imageUri)
        }
        instructionDao.insertAll(entities)
        if (categoryIds.isNotEmpty()) {
            categoryDao?.setTaskCategories(taskId, categoryIds)
        }
        return taskId
    }

    suspend fun updateTask(
        taskId: Long,
        name: String,
        instructions: List<Pair<String, String?>>,
        categoryIds: List<Long> = emptyList()
    ) {
        taskDao.update(TaskEntity(id = taskId, name = name, updatedAt = System.currentTimeMillis()))
        instructionDao.deleteByTaskId(taskId)
        val entities = instructions.mapIndexed { index, (text, imageUri) ->
            InstructionEntity(taskId = taskId, orderIndex = index, text = text, imageUri = imageUri)
        }
        instructionDao.insertAll(entities)
        categoryDao?.setTaskCategories(taskId, categoryIds)
    }

    suspend fun getImageUrisForTask(taskId: Long): List<String> =
        instructionDao.getImageUrisForTask(taskId)

    suspend fun deleteTask(taskId: Long) {
        taskDao.deleteById(taskId)
    }
}
