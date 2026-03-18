package com.example.myinstructions.ui.scan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myinstructions.data.AppDatabase
import com.example.myinstructions.data.entity.CategoryEntity
import com.example.myinstructions.data.repository.CategoryRepository
import com.example.myinstructions.data.repository.TaskRepository
import com.example.myinstructions.ui.share.ShareableTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ImportTaskWizardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = TaskRepository(db.taskDao(), db.instructionDao(), db.categoryDao())
    private val categoryRepository = CategoryRepository(db.categoryDao())

    private var tasks: List<ShareableTask> = emptyList()
    private var initialized = false
    private var importedCount = 0

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    val totalCount: Int get() = tasks.size
    val currentTask: ShareableTask? get() = tasks.getOrNull(_currentIndex.value)

    val allCategories: Flow<List<CategoryEntity>> = categoryRepository.getAllCategories()

    private val _selectedCategoryIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedCategoryIds: StateFlow<Set<Long>> = _selectedCategoryIds

    private val _wizardDone = MutableStateFlow(false)
    val wizardDone: StateFlow<Boolean> = _wizardDone

    val importedTaskCount: Int get() = importedCount

    fun init(tasks: List<ShareableTask>) {
        if (initialized) return
        this.tasks = tasks
        initialized = true
        _currentIndex.value = 0
        _selectedCategoryIds.value = emptySet()
        _wizardDone.value = false
        importedCount = 0
    }

    fun toggleCategory(categoryId: Long) {
        _selectedCategoryIds.value = _selectedCategoryIds.value.let { set ->
            if (categoryId in set) set - categoryId else set + categoryId
        }
    }

    fun importCurrentTask() {
        val task = currentTask ?: return
        viewModelScope.launch {
            repository.createTask(
                name = task.name,
                instructions = task.instructions.map { it to null },
                categoryIds = _selectedCategoryIds.value.toList()
            )
            importedCount++
            advance()
        }
    }

    fun skipCurrentTask() {
        advance()
    }

    fun createCategoryInline(name: String, onCreated: () -> Unit) {
        viewModelScope.launch {
            val id = categoryRepository.createCategory(name)
            _selectedCategoryIds.value = _selectedCategoryIds.value + id
            onCreated()
        }
    }

    private fun advance() {
        val next = _currentIndex.value + 1
        if (next >= tasks.size) {
            _wizardDone.value = true
        } else {
            _currentIndex.value = next
            _selectedCategoryIds.value = emptySet()
        }
    }
}
