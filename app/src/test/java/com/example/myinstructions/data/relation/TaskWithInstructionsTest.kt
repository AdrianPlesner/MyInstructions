package com.example.myinstructions.data.relation

import com.example.myinstructions.data.entity.InstructionEntity
import com.example.myinstructions.data.entity.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskWithInstructionsTest {

    private fun task() = TaskEntity(id = 1L, name = "Test Task")

    private fun instruction(id: Long, orderIndex: Int, text: String) =
        InstructionEntity(id = id, taskId = 1L, orderIndex = orderIndex, text = text)

    // ── sortedInstructions ────────────────────────────────────────────────────

    @Test
    fun `sortedInstructions returns instructions ordered by orderIndex ascending`() {
        val instructions = listOf(
            instruction(1, 2, "Third"),
            instruction(2, 0, "First"),
            instruction(3, 1, "Second")
        )
        val sorted = TaskWithInstructions(task(), instructions).sortedInstructions
        assertEquals("First", sorted[0].text)
        assertEquals("Second", sorted[1].text)
        assertEquals("Third", sorted[2].text)
    }

    @Test
    fun `sortedInstructions with already-sorted list preserves order`() {
        val instructions = listOf(
            instruction(1, 0, "Step 1"),
            instruction(2, 1, "Step 2"),
            instruction(3, 2, "Step 3")
        )
        val sorted = TaskWithInstructions(task(), instructions).sortedInstructions
        assertEquals(listOf("Step 1", "Step 2", "Step 3"), sorted.map { it.text })
    }

    @Test
    fun `sortedInstructions with empty list returns empty list`() {
        val sorted = TaskWithInstructions(task(), emptyList()).sortedInstructions
        assertTrue(sorted.isEmpty())
    }

    @Test
    fun `sortedInstructions with single instruction returns it unchanged`() {
        val only = instruction(1, 0, "Only step")
        val sorted = TaskWithInstructions(task(), listOf(only)).sortedInstructions
        assertEquals(1, sorted.size)
        assertEquals("Only step", sorted[0].text)
    }

    @Test
    fun `sortedInstructions does not mutate the original instructions list`() {
        val instructions = listOf(
            instruction(1, 2, "C"),
            instruction(2, 0, "A"),
            instruction(3, 1, "B")
        )
        val twi = TaskWithInstructions(task(), instructions)
        twi.sortedInstructions // trigger sort

        // Original list order must be unchanged
        assertEquals(2, twi.instructions[0].orderIndex)
        assertEquals(0, twi.instructions[1].orderIndex)
        assertEquals(1, twi.instructions[2].orderIndex)
    }

    @Test
    fun `sortedInstructions is stable for equal orderIndex values`() {
        val instructions = listOf(
            instruction(1, 0, "Alpha"),
            instruction(2, 0, "Beta")
        )
        val sorted = TaskWithInstructions(task(), instructions).sortedInstructions
        assertEquals(2, sorted.size)
        // Both have orderIndex 0; sorted list must contain both
        assertTrue(sorted.any { it.text == "Alpha" })
        assertTrue(sorted.any { it.text == "Beta" })
    }

    @Test
    fun `sortedInstructions preserves all instruction fields`() {
        val original = instruction(42L, 0, "Check valve")
            .copy(imageUri = "content://media/image.jpg")
        val twi = TaskWithInstructions(task(), listOf(original))
        val sorted = twi.sortedInstructions

        assertEquals(42L, sorted[0].id)
        assertEquals("Check valve", sorted[0].text)
        assertEquals("content://media/image.jpg", sorted[0].imageUri)
    }
}
