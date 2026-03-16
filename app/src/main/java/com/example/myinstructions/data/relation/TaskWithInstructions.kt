package com.example.myinstructions.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.myinstructions.data.entity.InstructionEntity
import com.example.myinstructions.data.entity.TaskEntity

data class TaskWithInstructions(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val instructions: List<InstructionEntity>
) {
    val sortedInstructions: List<InstructionEntity>
        get() = instructions.sortedBy { it.orderIndex }
}
