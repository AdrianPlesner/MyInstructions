package com.example.myinstructions.ui.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareableTaskTest {

    @Test
    fun `two instances with same name and instructions are equal`() {
        val a = ShareableTask("Task A", listOf("Step 1", "Step 2"))
        val b = ShareableTask("Task A", listOf("Step 1", "Step 2"))
        assertEquals(a, b)
    }

    @Test
    fun `instances with different names are not equal`() {
        assertNotEquals(
            ShareableTask("Task A", listOf("Step 1")),
            ShareableTask("Task B", listOf("Step 1"))
        )
    }

    @Test
    fun `instances with different instructions are not equal`() {
        assertNotEquals(
            ShareableTask("Task", listOf("Step 1")),
            ShareableTask("Task", listOf("Step 2"))
        )
    }

    @Test
    fun `instances with different instruction counts are not equal`() {
        assertNotEquals(
            ShareableTask("Task", listOf("Step 1")),
            ShareableTask("Task", listOf("Step 1", "Step 2"))
        )
    }

    @Test
    fun `copy with changed name produces new instance with original instructions`() {
        val original = ShareableTask("Original", listOf("Step 1", "Step 2"))
        val copy = original.copy(name = "Modified")
        assertEquals("Modified", copy.name)
        assertEquals(original.instructions, copy.instructions)
    }

    @Test
    fun `copy with changed instructions produces new instance with original name`() {
        val original = ShareableTask("Task", listOf("Old step"))
        val copy = original.copy(instructions = listOf("New step"))
        assertEquals("Task", copy.name)
        assertEquals(listOf("New step"), copy.instructions)
    }

    @Test
    fun `task with empty instructions has empty list`() {
        val task = ShareableTask("Empty task", emptyList())
        assertTrue(task.instructions.isEmpty())
    }

    @Test
    fun `instructions are returned in insertion order`() {
        val instructions = listOf("First", "Second", "Third")
        val task = ShareableTask("Task", instructions)
        assertEquals(instructions, task.instructions)
    }

    @Test
    fun `equal instances have equal hash codes`() {
        val a = ShareableTask("Task", listOf("Step"))
        val b = ShareableTask("Task", listOf("Step"))
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `toString contains task name`() {
        val task = ShareableTask("My Task", listOf("Step"))
        assertTrue(task.toString().contains("My Task"))
    }
}
