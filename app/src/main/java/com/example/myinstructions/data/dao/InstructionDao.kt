package com.example.myinstructions.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.myinstructions.data.entity.InstructionEntity

@Dao
interface InstructionDao {
    @Insert
    suspend fun insertAll(instructions: List<InstructionEntity>)

    @Query("SELECT imageUri FROM instruction WHERE taskId = :taskId AND imageUri IS NOT NULL")
    suspend fun getImageUrisForTask(taskId: Long): List<String>

    @Query("DELETE FROM instruction WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: Long)
}
