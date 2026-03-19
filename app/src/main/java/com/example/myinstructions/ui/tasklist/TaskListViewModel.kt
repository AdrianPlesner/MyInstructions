package com.example.myinstructions.ui.tasklist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myinstructions.data.AppDatabase
import com.example.myinstructions.data.entity.CategoryEntity
import com.example.myinstructions.data.repository.CategoryRepository
import com.example.myinstructions.data.repository.TaskRepository
import com.example.myinstructions.ui.share.ShareableTask
import com.example.myinstructions.util.ImageStorageHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

    // Selection state
    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode

    private val _selectedTaskIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTaskIds: StateFlow<Set<Long>> = _selectedTaskIds

    fun toggleSortMode() {
        _sortMode.value = when (_sortMode.value) {
            SortMode.CATEGORY -> SortMode.RECENT
            SortMode.RECENT -> SortMode.CATEGORY
        }
    }

    /** Base list items without selection state annotations. */
    private val _baseListItems: Flow<List<ListItem>> = combine(
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
                val matchingInstrs = twi.instructions
                    .filter { it.text.contains(query, ignoreCase = true) }
                    .sortedBy { it.orderIndex }
                    .map { it.text }
                when {
                    titleMatch -> Triple(2, twi, matchingInstrs)
                    matchingInstrs.isNotEmpty() -> Triple(1, twi, matchingInstrs)
                    else -> null
                }
            }
                .sortedByDescending { it.first }
                .map { (_, twi, matchTexts) ->
                    ListItem.TaskRow(
                        TaskItem(
                            id = twi.task.id,
                            name = twi.task.name,
                            instructionCount = twi.instructions.size,
                            matchingInstructions = matchTexts,
                            searchQuery = query
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
            items.add(
                ListItem.CategoryHeader(
                    cwt.category.id, cwt.category.name, taskCount, isExpanded,
                    taskIds = cwt.tasks.map { it.id }
                )
            )
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
            items.add(
                ListItem.UncategorizedHeader(
                    uncategorizedTasks.size, isExpanded,
                    taskIds = uncategorizedTasks.map { it.task.id }
                )
            )
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

    /** Combined list with selection state annotations. */
    val listItems: Flow<List<ListItem>> = _baseListItems.combine(
        combine(_selectionMode, _selectedTaskIds) { mode, ids -> mode to ids }
    ) { items, (mode, selectedIds) ->
        if (!mode) return@combine items
        items.map { item ->
            when (item) {
                is ListItem.CategoryHeader -> item.copy(
                    isSelectionMode = true,
                    isAllSelected = item.taskIds.isNotEmpty() && item.taskIds.all { it in selectedIds }
                )
                is ListItem.UncategorizedHeader -> item.copy(
                    isSelectionMode = true,
                    isAllSelected = item.taskIds.isNotEmpty() && item.taskIds.all { it in selectedIds }
                )
                is ListItem.TaskRow -> item.copy(
                    isSelectionMode = true,
                    isSelected = item.task.id in selectedIds
                )
            }
        }
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

    // ─── Selection mode ──────────────────────────────────────────────────────

    fun enterSelectionMode(taskId: Long) {
        _selectedTaskIds.value = setOf(taskId)
        _selectionMode.value = true
    }

    fun toggleTaskSelection(taskId: Long) {
        val updated = _selectedTaskIds.value.let { current ->
            if (taskId in current) current - taskId else current + taskId
        }
        _selectedTaskIds.value = updated
        if (updated.isEmpty()) exitSelectionMode()
    }

    fun toggleCategorySelection(taskIds: List<Long>) {
        val current = _selectedTaskIds.value
        val updated = if (taskIds.all { it in current }) {
            current - taskIds.toSet()
        } else {
            current + taskIds.toSet()
        }
        _selectedTaskIds.value = updated
        if (updated.isEmpty()) exitSelectionMode()
    }

    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedTaskIds.value = emptySet()
    }

    fun deleteSelectedTasks() {
        val ids = _selectedTaskIds.value.toList()
        viewModelScope.launch {
            for (id in ids) {
                val imageUris = repository.getImageUrisForTask(id)
                ImageStorageHelper.deleteImages(getApplication(), imageUris)
                repository.deleteTask(id)
            }
            exitSelectionMode()
        }
    }

    /**
     * Returns all categories with nothing pre-checked.
     * The dialog is for choosing NEW categories to add; existing
     * assignments on each task are unrelated to this selection.
     */
    suspend fun getCategoriesForDialog(): Pair<List<CategoryEntity>, BooleanArray> {
        val allCategories = categoryRepository.getAllCategoriesOnce()
        val checked = BooleanArray(allCategories.size) { false }
        return allCategories to checked
    }

    /**
     * Adds [categoryIds] to every selected task without removing any
     * categories those tasks already have.
     */
    fun addCategoriesToSelected(categoryIds: List<Long>) {
        val ids = _selectedTaskIds.value.toList()
        viewModelScope.launch {
            for (taskId in ids) {
                val existing = categoryRepository.getCategoriesForTask(taskId).map { it.id }
                val merged = (existing + categoryIds).distinct()
                categoryRepository.setTaskCategories(taskId, merged)
            }
            exitSelectionMode()
        }
    }

    /**
     * Builds [ShareableTask] objects for every currently selected task,
     * preserving instruction order. Call from a coroutine scope.
     */
    suspend fun buildShareableTasksForSelected(): List<ShareableTask> {
        val ids = _selectedTaskIds.value
        return repository.getAllTasksWithInstructions()
            .first()
            .filter { it.task.id in ids }
            .map { twi ->
                ShareableTask(
                    name = twi.task.name,
                    instructions = twi.instructions
                        .sortedBy { it.orderIndex }
                        .map { it.text }
                )
            }
    }

    companion object {
        const val UNCATEGORIZED_ID = -1L
    }
}
