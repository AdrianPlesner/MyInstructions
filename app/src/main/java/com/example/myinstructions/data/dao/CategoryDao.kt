package com.example.myinstructions.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.myinstructions.data.entity.CategoryEntity
import com.example.myinstructions.data.entity.TaskCategoryCrossRef
import com.example.myinstructions.data.relation.CategoryWithTasks
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM category ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Transaction
    @Query("SELECT * FROM category ORDER BY name ASC")
    fun getAllCategoriesWithTasks(): Flow<List<CategoryWithTasks>>

    @Query(
        "SELECT c.* FROM category c INNER JOIN task_category tc ON c.id = tc.categoryId WHERE tc.taskId = :taskId"
    )
    suspend fun getCategoriesForTask(taskId: Long): List<CategoryEntity>

    @Query(
        "SELECT id FROM task WHERE id NOT IN (SELECT DISTINCT taskId FROM task_category)"
    )
    fun getUncategorizedTaskIds(): Flow<List<Long>>

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Query("DELETE FROM category WHERE id = :categoryId")
    suspend fun deleteById(categoryId: Long)

    @Insert
    suspend fun insertCrossRef(crossRef: TaskCategoryCrossRef)

    @Query("DELETE FROM task_category WHERE taskId = :taskId")
    suspend fun deleteCrossRefsForTask(taskId: Long)

    @Transaction
    suspend fun setTaskCategories(taskId: Long, categoryIds: List<Long>) {
        deleteCrossRefsForTask(taskId)
        categoryIds.forEach { categoryId ->
            insertCrossRef(TaskCategoryCrossRef(taskId = taskId, categoryId = categoryId))
        }
    }
}
