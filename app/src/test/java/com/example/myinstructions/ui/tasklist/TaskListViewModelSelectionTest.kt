package com.example.myinstructions.ui.tasklist

import android.app.Application
import androidx.room.RoomDatabase
import com.example.myinstructions.data.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Tests the multi-select state logic in [TaskListViewModel].
 * All methods under test operate on in-memory StateFlow values only
 * (except [TaskListViewModel.getCategoriesForDialog] which reads the DB).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskListViewModelSelectionTest {

    private lateinit var viewModel: TaskListViewModel

    @Before
    fun setUp() {
        resetDbSingleton()
        val app = RuntimeEnvironment.getApplication() as Application
        viewModel = TaskListViewModel(app)
    }

    @After
    fun tearDown() {
        resetDbSingleton()
    }

    // ── initial state ──────────────────────────────────────────────────────────

    @Test
    fun `selection mode is off on creation`() {
        assertFalse(viewModel.selectionMode.value)
    }

    @Test
    fun `selected task ids is empty on creation`() {
        assertTrue(viewModel.selectedTaskIds.value.isEmpty())
    }

    // ── enterSelectionMode ─────────────────────────────────────────────────────

    @Test
    fun `enterSelectionMode activates selection mode`() {
        viewModel.enterSelectionMode(1L)
        assertTrue(viewModel.selectionMode.value)
    }

    @Test
    fun `enterSelectionMode selects exactly the given task`() {
        viewModel.enterSelectionMode(7L)
        assertEquals(setOf(7L), viewModel.selectedTaskIds.value)
    }

    @Test
    fun `enterSelectionMode replaces any previous selection`() {
        viewModel.enterSelectionMode(1L)
        viewModel.enterSelectionMode(2L)
        assertEquals(setOf(2L), viewModel.selectedTaskIds.value)
    }

    // ── toggleTaskSelection ────────────────────────────────────────────────────

    @Test
    fun `toggleTaskSelection adds an unselected task`() {
        viewModel.enterSelectionMode(1L)
        viewModel.toggleTaskSelection(2L)
        assertTrue(2L in viewModel.selectedTaskIds.value)
    }

    @Test
    fun `toggleTaskSelection removes an already-selected task`() {
        viewModel.enterSelectionMode(1L)
        viewModel.toggleTaskSelection(2L)
        viewModel.toggleTaskSelection(2L)
        assertFalse(2L in viewModel.selectedTaskIds.value)
    }

    @Test
    fun `toggleTaskSelection does not affect other selected tasks`() {
        viewModel.enterSelectionMode(1L)
        viewModel.toggleTaskSelection(2L)
        viewModel.toggleTaskSelection(3L)
        // remove 2L only
        viewModel.toggleTaskSelection(2L)
        assertEquals(setOf(1L, 3L), viewModel.selectedTaskIds.value)
    }

    @Test
    fun `toggleTaskSelection exits selection mode when the last task is deselected`() {
        viewModel.enterSelectionMode(1L)
        viewModel.toggleTaskSelection(1L)
        assertFalse(viewModel.selectionMode.value)
        assertTrue(viewModel.selectedTaskIds.value.isEmpty())
    }

    // ── toggleCategorySelection ────────────────────────────────────────────────

    @Test
    fun `toggleCategorySelection selects all tasks when none of them are currently selected`() {
        viewModel.enterSelectionMode(99L) // unrelated task already selected
        viewModel.toggleCategorySelection(listOf(1L, 2L, 3L))
        assertTrue(listOf(1L, 2L, 3L).all { it in viewModel.selectedTaskIds.value })
    }

    @Test
    fun `toggleCategorySelection deselects all tasks when all are already selected`() {
        viewModel.enterSelectionMode(1L)
        viewModel.toggleTaskSelection(2L)
        viewModel.toggleTaskSelection(3L)
        viewModel.toggleCategorySelection(listOf(1L, 2L, 3L))
        assertFalse(listOf(1L, 2L, 3L).any { it in viewModel.selectedTaskIds.value })
    }

    @Test
    fun `toggleCategorySelection selects all when only a subset is currently selected`() {
        viewModel.enterSelectionMode(1L) // only 1L selected from [1,2,3]
        viewModel.toggleCategorySelection(listOf(1L, 2L, 3L))
        assertEquals(setOf(1L, 2L, 3L), viewModel.selectedTaskIds.value)
    }

    @Test
    fun `toggleCategorySelection does not affect tasks outside the toggled category`() {
        viewModel.enterSelectionMode(99L)
        viewModel.toggleTaskSelection(1L)
        viewModel.toggleTaskSelection(2L)
        // 1L and 2L are all selected in this category → deselect them; 99L must remain
        viewModel.toggleCategorySelection(listOf(1L, 2L))
        assertEquals(setOf(99L), viewModel.selectedTaskIds.value)
    }

    @Test
    fun `toggleCategorySelection with empty list leaves selection unchanged`() {
        viewModel.enterSelectionMode(5L)
        viewModel.toggleCategorySelection(emptyList())
        assertEquals(setOf(5L), viewModel.selectedTaskIds.value)
    }

    @Test
    fun `toggleCategorySelection exits selection mode when deselecting leaves no tasks selected`() {
        viewModel.enterSelectionMode(1L)
        viewModel.toggleTaskSelection(2L)
        // deselect all tasks, including those outside the explicit list
        viewModel.toggleCategorySelection(listOf(1L, 2L))
        assertFalse(viewModel.selectionMode.value)
        assertTrue(viewModel.selectedTaskIds.value.isEmpty())
    }

    // ── exitSelectionMode ──────────────────────────────────────────────────────

    @Test
    fun `exitSelectionMode clears the selection and deactivates selection mode`() {
        viewModel.enterSelectionMode(1L)
        viewModel.toggleTaskSelection(2L)
        viewModel.exitSelectionMode()
        assertFalse(viewModel.selectionMode.value)
        assertTrue(viewModel.selectedTaskIds.value.isEmpty())
    }

    @Test
    fun `exitSelectionMode is a no-op when selection mode is already inactive`() {
        viewModel.exitSelectionMode()
        assertFalse(viewModel.selectionMode.value)
        assertTrue(viewModel.selectedTaskIds.value.isEmpty())
    }

    // ── getCategoriesForDialog ─────────────────────────────────────────────────

    @Test
    fun `getCategoriesForDialog returns an empty list when no categories exist`() = runBlocking {
        val (categories, checked) = viewModel.getCategoriesForDialog()
        assertTrue(categories.isEmpty())
        assertEquals(0, checked.size)
    }

    @Test
    fun `getCategoriesForDialog never pre-checks any category`() = runBlocking {
        val (_, checked) = viewModel.getCategoriesForDialog()
        checked.forEach { assertFalse("Expected all entries to be false", it) }
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private fun resetDbSingleton() {
        val field = AppDatabase::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        (field.get(null) as? RoomDatabase)?.close()
        field.set(null, null)
    }
}
