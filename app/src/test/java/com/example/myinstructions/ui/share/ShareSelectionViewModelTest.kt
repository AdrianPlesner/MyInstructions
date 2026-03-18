package com.example.myinstructions.ui.share

import android.app.Application
import androidx.room.RoomDatabase
import com.example.myinstructions.data.AppDatabase
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
 * Tests the pure selection state logic of [ShareSelectionViewModel].
 * No database operations are exercised; all tested methods only update [selectedTaskIds].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShareSelectionViewModelTest {

    private lateinit var viewModel: ShareSelectionViewModel

    @Before
    fun setUp() {
        resetDbSingleton()
        val app = RuntimeEnvironment.getApplication() as Application
        viewModel = ShareSelectionViewModel(app)
    }

    @After
    fun tearDown() {
        resetDbSingleton()
    }

    // ── initial state ─────────────────────────────────────────────────────────

    @Test
    fun `selected task ids is empty on creation`() {
        assertTrue(viewModel.selectedTaskIds.value.isEmpty())
    }

    // ── preselectTask ─────────────────────────────────────────────────────────

    @Test
    fun `preselectTask selects exactly the given task`() {
        viewModel.preselectTask(5L)
        assertEquals(setOf(5L), viewModel.selectedTaskIds.value)
    }

    @Test
    fun `preselectTask replaces any previous selection`() {
        viewModel.preselectTask(1L)
        viewModel.preselectTask(2L)
        assertEquals(setOf(2L), viewModel.selectedTaskIds.value)
    }

    // ── preselectTasks ────────────────────────────────────────────────────────

    @Test
    fun `preselectTasks selects all provided task ids`() {
        viewModel.preselectTasks(setOf(10L, 20L, 30L))
        assertEquals(setOf(10L, 20L, 30L), viewModel.selectedTaskIds.value)
    }

    @Test
    fun `preselectTasks with empty set clears selection`() {
        viewModel.preselectTask(1L)
        viewModel.preselectTasks(emptySet())
        assertTrue(viewModel.selectedTaskIds.value.isEmpty())
    }

    // ── toggleTask ────────────────────────────────────────────────────────────

    @Test
    fun `toggleTask adds task when not currently selected`() {
        viewModel.toggleTask(10L)
        assertTrue(10L in viewModel.selectedTaskIds.value)
    }

    @Test
    fun `toggleTask removes task when already selected`() {
        viewModel.toggleTask(10L)
        viewModel.toggleTask(10L)
        assertFalse(10L in viewModel.selectedTaskIds.value)
    }

    @Test
    fun `toggleTask does not affect other selected tasks`() {
        viewModel.preselectTasks(setOf(1L, 2L, 3L))
        viewModel.toggleTask(2L)
        assertEquals(setOf(1L, 3L), viewModel.selectedTaskIds.value)
    }

    @Test
    fun `toggleTask adds to existing selection`() {
        viewModel.preselectTask(1L)
        viewModel.toggleTask(2L)
        assertEquals(setOf(1L, 2L), viewModel.selectedTaskIds.value)
    }

    // ── toggleCategory ────────────────────────────────────────────────────────

    @Test
    fun `toggleCategory selects all tasks when none are selected`() {
        viewModel.toggleCategory(listOf(1L, 2L, 3L))
        assertEquals(setOf(1L, 2L, 3L), viewModel.selectedTaskIds.value)
    }

    @Test
    fun `toggleCategory deselects all tasks when all are already selected`() {
        viewModel.preselectTasks(setOf(1L, 2L, 3L))
        viewModel.toggleCategory(listOf(1L, 2L, 3L))
        assertTrue(viewModel.selectedTaskIds.value.isEmpty())
    }

    @Test
    fun `toggleCategory selects all when only a subset is selected`() {
        viewModel.preselectTasks(setOf(1L))
        viewModel.toggleCategory(listOf(1L, 2L, 3L))
        // Not all selected → add the rest
        assertEquals(setOf(1L, 2L, 3L), viewModel.selectedTaskIds.value)
    }

    @Test
    fun `toggleCategory does not affect tasks from other categories`() {
        viewModel.preselectTasks(setOf(1L, 2L, 99L))
        // 1L and 2L are all selected → deselect them; 99L is untouched
        viewModel.toggleCategory(listOf(1L, 2L))
        assertEquals(setOf(99L), viewModel.selectedTaskIds.value)
    }

    @Test
    fun `toggleCategory with empty task list leaves selection unchanged`() {
        viewModel.preselectTasks(setOf(5L))
        viewModel.toggleCategory(emptyList())
        assertEquals(setOf(5L), viewModel.selectedTaskIds.value)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun resetDbSingleton() {
        val field = AppDatabase::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        (field.get(null) as? RoomDatabase)?.close()
        field.set(null, null)
    }
}
