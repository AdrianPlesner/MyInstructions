package com.example.myinstructions.data.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.myinstructions.data.entity.CategoryEntity
import com.example.myinstructions.data.entity.TaskCategoryCrossRef
import com.example.myinstructions.data.entity.TaskEntity

data class CategoryWithTasks(
    @Embedded val category: CategoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TaskCategoryCrossRef::class,
            parentColumn = "categoryId",
            entityColumn = "taskId"
        )
    )
    val tasks: List<TaskEntity>
)
