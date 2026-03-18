package com.example.myinstructions.ui.scan

import android.app.Application
import androidx.room.RoomDatabase
import com.example.myinstructions.data.AppDatabase
import com.example.myinstructions.ui.share.ShareableTask
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Tests the wizard state machine in [ImportTaskWizardViewModel].
 * Only pure state transitions are exercised (no database calls).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImportTaskWizardViewModelTest {

    private lateinit var viewModel: ImportTaskWizardViewModel

    private val sampleTasks = listOf(
        ShareableTask("Task 1", listOf("Step A", "Step B")),
        ShareableTask("Task 2", listOf("Step C")),
        ShareableTask("Task 3", emptyList())
    )

    @Before
    fun setUp() {
        resetDbSingleton()
        val app = RuntimeEnvironment.getApplication() as Application
        viewModel = ImportTaskWizardViewModel(app)
    }

    @After
    fun tearDown() {
        resetDbSingleton()
    }

    // ── pre-init state ────────────────────────────────────────────────────────

    @Test
    fun `currentTask is null before init`() {
        assertNull(viewModel.currentTask)
    }

    @Test
    fun `totalCount is zero before init`() {
        assertEquals(0, viewModel.totalCount)
    }

    @Test
    fun `wizardDone is false before init`() {
        assertFalse(viewModel.wizardDone.value)
    }

    @Test
    fun `importedTaskCount is zero before init`() {
        assertEquals(0, viewModel.importedTaskCount)
    }

    // ── init ──────────────────────────────────────────────────────────────────

    @Test
    fun `init sets currentIndex to 0`() {
        viewModel.init(sampleTasks)
        assertEquals(0, viewModel.currentIndex.value)
    }

    @Test
    fun `init sets totalCount to task list size`() {
        viewModel.init(sampleTasks)
        assertEquals(3, viewModel.totalCount)
    }

    @Test
    fun `init exposes the first task as currentTask`() {
        viewModel.init(sampleTasks)
        assertEquals(sampleTasks[0], viewModel.currentTask)
    }

    @Test
    fun `init sets wizardDone to false`() {
        viewModel.init(sampleTasks)
        assertFalse(viewModel.wizardDone.value)
    }

    @Test
    fun `init clears selected category ids`() {
        viewModel.init(sampleTasks)
        assertTrue(viewModel.selectedCategoryIds.value.isEmpty())
    }

    @Test
    fun `init with empty list marks wizard as not done`() {
        viewModel.init(emptyList())
        assertFalse(viewModel.wizardDone.value)
    }

    @Test
    fun `init is idempotent - second call is ignored`() {
        val firstTasks = listOf(ShareableTask("First", emptyList()))
        val secondTasks = listOf(ShareableTask("Second", emptyList()))
        viewModel.init(firstTasks)
        viewModel.init(secondTasks)
        assertEquals(firstTasks[0], viewModel.currentTask)
        assertEquals(1, viewModel.totalCount)
    }

    // ── toggleCategory ────────────────────────────────────────────────────────

    @Test
    fun `toggleCategory adds category id when not selected`() {
        viewModel.init(sampleTasks)
        viewModel.toggleCategory(42L)
        assertTrue(42L in viewModel.selectedCategoryIds.value)
    }

    @Test
    fun `toggleCategory removes category id when already selected`() {
        viewModel.init(sampleTasks)
        viewModel.toggleCategory(42L)
        viewModel.toggleCategory(42L)
        assertFalse(42L in viewModel.selectedCategoryIds.value)
    }

    @Test
    fun `toggleCategory can manage multiple category ids independently`() {
        viewModel.init(sampleTasks)
        viewModel.toggleCategory(10L)
        viewModel.toggleCategory(20L)
        viewModel.toggleCategory(10L) // remove 10
        assertFalse(10L in viewModel.selectedCategoryIds.value)
        assertTrue(20L in viewModel.selectedCategoryIds.value)
    }

    // ── skipCurrentTask ───────────────────────────────────────────────────────

    @Test
    fun `skipCurrentTask advances to next task`() {
        viewModel.init(sampleTasks)
        viewModel.skipCurrentTask()
        assertEquals(1, viewModel.currentIndex.value)
        assertEquals(sampleTasks[1], viewModel.currentTask)
    }

    @Test
    fun `skipCurrentTask advances through all tasks`() {
        viewModel.init(sampleTasks)
        viewModel.skipCurrentTask() // → index 1
        viewModel.skipCurrentTask() // → index 2
        assertEquals(2, viewModel.currentIndex.value)
        assertEquals(sampleTasks[2], viewModel.currentTask)
    }

    @Test
    fun `skipCurrentTask on last task marks wizard as done`() {
        viewModel.init(listOf(ShareableTask("Only task", emptyList())))
        viewModel.skipCurrentTask()
        assertTrue(viewModel.wizardDone.value)
    }

    @Test
    fun `skipCurrentTask clears selected category ids for next task`() {
        viewModel.init(sampleTasks)
        viewModel.toggleCategory(99L)
        viewModel.skipCurrentTask()
        assertTrue(viewModel.selectedCategoryIds.value.isEmpty())
    }

    @Test
    fun `skipCurrentTask does not increment importedTaskCount`() {
        viewModel.init(sampleTasks)
        viewModel.skipCurrentTask()
        assertEquals(0, viewModel.importedTaskCount)
    }

    @Test
    fun `wizardDone is false while tasks remain after skip`() {
        viewModel.init(sampleTasks)
        viewModel.skipCurrentTask()
        assertFalse(viewModel.wizardDone.value)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun resetDbSingleton() {
        val field = AppDatabase::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        (field.get(null) as? RoomDatabase)?.close()
        field.set(null, null)
    }
}
