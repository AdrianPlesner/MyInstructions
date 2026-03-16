package com.example.myinstructions.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.myinstructions.data.entity.TaskEntity
import com.example.myinstructions.data.relation.TaskWithInstructions
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM task ORDER BY updatedAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Transaction
    @Query("SELECT * FROM task ORDER BY updatedAt DESC")
    fun getAllTasksWithInstructions(): Flow<List<TaskWithInstructions>>

    @Transaction
    @Query("SELECT * FROM task WHERE id = :taskId")
    fun getTaskWithInstructions(taskId: Long): Flow<TaskWithInstructions?>

    @Query("SELECT COUNT(*) FROM instruction WHERE taskId = :taskId")
    fun getInstructionCount(taskId: Long): Flow<Int>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM task WHERE id = :taskId")
    suspend fun deleteById(taskId: Long)
}
