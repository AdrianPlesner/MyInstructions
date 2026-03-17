package com.example.myinstructions.ui.tasklist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myinstructions.data.AppDatabase
import com.example.myinstructions.data.repository.CategoryRepository
import com.example.myinstructions.data.repository.TaskRepository
import com.example.myinstructions.util.ImageStorageHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class SortMode { CATEGORY, RECENT }

class TaskListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = TaskRepository(db.taskDao(), db.instructionDao(), db.categoryDao())
    private val categoryRepository = CategoryRepository(db.categoryDao())

    private val _searchQuery = MutableStateFlow("")
    private val _expandedCategories = MutableStateFlow<Set<Long>>(emptySet())
    private val _sortMode = MutableStateFlow(SortMode.CATEGORY)
    val sortMode: StateFlow<SortMode> = _sortMode

    fun toggleSortMode() {
        _sortMode.value = when (_sortMode.value) {
            SortMode.CATEGORY -> SortMode.RECENT
            SortMode.RECENT -> SortMode.CATEGORY
        }
    }

    /** Grouped list for category mode, flat list for recent mode, filtered list for search. */
    val listItems: Flow<List<ListItem>> = combine(
        categoryRepository.getAllCategoriesWithTasks(),
        repository.getAllTasksWithInstructions(),
        categoryRepository.getUncategorizedTaskIds(),
        _expandedCategories,
        combine(_searchQuery, _sortMode) { q, s -> q to s }
    ) { categoriesWithTasks, allTasksWithInstructions, uncategorizedIds, expanded, (query, sortMode) ->

        // Search mode: flat filtered list by relevance
        if (query.isNotBlank()) {
            return@combine allTasksWithInstructions.mapNotNull { twi ->
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
                    ListItem.TaskRow(
                        TaskItem(
                            id = twi.task.id,
                            name = twi.task.name,
                            instructionCount = twi.instructions.size
                        )
                    )
                }
        }

        // Recent mode: flat list sorted by lastViewedAt
        if (sortMode == SortMode.RECENT) {
            return@combine allTasksWithInstructions
                .sortedByDescending { it.task.lastViewedAt }
                .map { twi ->
                    ListItem.TaskRow(
                        TaskItem(
                            id = twi.task.id,
                            name = twi.task.name,
                            instructionCount = twi.instructions.size
                        )
                    )
                }
        }

        // Category mode: grouped list
        val taskInstructionMap = allTasksWithInstructions.associateBy { it.task.id }
        val uncategorizedIdSet = uncategorizedIds.toSet()
        val items = mutableListOf<ListItem>()

        for (cwt in categoriesWithTasks) {
            val taskCount = cwt.tasks.size
            if (taskCount == 0) continue
            val isExpanded = cwt.category.id in expanded
            items.add(ListItem.CategoryHeader(cwt.category.id, cwt.category.name, taskCount, isExpanded))
            if (isExpanded) {
                for (task in cwt.tasks.sortedByDescending { it.updatedAt }) {
                    val twi = taskInstructionMap[task.id]
                    items.add(
                        ListItem.TaskRow(
                            TaskItem(
                                id = task.id,
                                name = task.name,
                                instructionCount = twi?.instructions?.size ?: 0
                            )
                        )
                    )
                }
            }
        }

        // Uncategorized tasks at the bottom
        val uncategorizedTasks = allTasksWithInstructions
            .filter { it.task.id in uncategorizedIdSet }
            .sortedByDescending { it.task.lastViewedAt }
        if (uncategorizedTasks.isNotEmpty()) {
            val isExpanded = UNCATEGORIZED_ID in expanded
            items.add(ListItem.UncategorizedHeader(uncategorizedTasks.size, isExpanded))
            if (isExpanded) {
                for (twi in uncategorizedTasks) {
                    items.add(
                        ListItem.TaskRow(
                            TaskItem(
                                id = twi.task.id,
                                name = twi.task.name,
                                instructionCount = twi.instructions.size
                            )
                        )
                    )
                }
            }
        }

        items
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleCategory(categoryId: Long) {
        _expandedCategories.value = _expandedCategories.value.let { set ->
            if (categoryId in set) set - categoryId else set + categoryId
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            val imageUris = repository.getImageUrisForTask(taskId)
            ImageStorageHelper.deleteImages(getApplication(), imageUris)
            repository.deleteTask(taskId)
        }
    }

    companion object {
        const val UNCATEGORIZED_ID = -1L
    }
}
