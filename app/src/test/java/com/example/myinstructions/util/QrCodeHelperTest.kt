package com.example.myinstructions.util

import android.util.Base64
import com.example.myinstructions.ui.share.ShareableTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QrCodeHelperTest {

    // ── toJson ────────────────────────────────────────────────────────────────

    @Test
    fun `toJson includes format version field`() {
        val json = QrCodeHelper.toJson(listOf(ShareableTask("T", emptyList())))
        assertTrue(json.contains("\"v\""))
    }

    @Test
    fun `toJson includes all task names`() {
        val tasks = listOf(
            ShareableTask("Pasta recipe", listOf("Boil water")),
            ShareableTask("Laundry guide", listOf("Sort clothes"))
        )
        val json = QrCodeHelper.toJson(tasks)
        assertTrue(json.contains("Pasta recipe"))
        assertTrue(json.contains("Laundry guide"))
    }

    @Test
    fun `toJson includes all instruction texts`() {
        val task = ShareableTask("Task", listOf("Step A", "Step B", "Step C"))
        val json = QrCodeHelper.toJson(listOf(task))
        assertTrue(json.contains("Step A"))
        assertTrue(json.contains("Step B"))
        assertTrue(json.contains("Step C"))
    }

    @Test
    fun `toJson with empty task list produces valid JSON with tasks key`() {
        val json = QrCodeHelper.toJson(emptyList())
        assertTrue(json.contains("tasks"))
    }

    @Test
    fun `toJson with task with no instructions produces valid JSON`() {
        val json = QrCodeHelper.toJson(listOf(ShareableTask("Empty task", emptyList())))
        assertNotNull(json)
        assertTrue(json.isNotBlank())
    }

    // ── fromJson ──────────────────────────────────────────────────────────────

    @Test
    fun `fromJson round-trips a single task`() {
        val original = listOf(ShareableTask("Cook pasta", listOf("Boil water", "Add pasta")))
        val result = QrCodeHelper.fromJson(QrCodeHelper.toJson(original))
        assertEquals(original, result)
    }

    @Test
    fun `fromJson round-trips multiple tasks`() {
        val original = listOf(
            ShareableTask("Task 1", listOf("Step A", "Step B")),
            ShareableTask("Task 2", listOf("Step C")),
            ShareableTask("Task 3", emptyList())
        )
        val result = QrCodeHelper.fromJson(QrCodeHelper.toJson(original))
        assertEquals(original, result)
    }

    @Test
    fun `fromJson preserves empty instructions list`() {
        val original = listOf(ShareableTask("No steps", emptyList()))
        val result = QrCodeHelper.fromJson(QrCodeHelper.toJson(original))
        assertEquals(original, result)
    }

    @Test
    fun `fromJson preserves instruction order`() {
        val instructions = listOf("First", "Second", "Third", "Fourth")
        val original = listOf(ShareableTask("Ordered task", instructions))
        val result = QrCodeHelper.fromJson(QrCodeHelper.toJson(original))
        assertEquals(instructions, result!![0].instructions)
    }

    @Test
    fun `fromJson returns null for version 0`() {
        assertNull(QrCodeHelper.fromJson("""{"v":0,"tasks":[{"n":"T","i":[]}]}"""))
    }

    @Test
    fun `fromJson returns null for unsupported future version`() {
        assertNull(QrCodeHelper.fromJson("""{"v":99,"tasks":[{"n":"T","i":[]}]}"""))
    }

    @Test
    fun `fromJson returns null when version field is absent`() {
        assertNull(QrCodeHelper.fromJson("""{"tasks":[{"n":"T","i":[]}]}"""))
    }

    @Test
    fun `fromJson returns null when tasks array is absent`() {
        assertNull(QrCodeHelper.fromJson("""{"v":1}"""))
    }

    @Test
    fun `fromJson returns empty list for empty tasks array`() {
        val result = QrCodeHelper.fromJson("""{"v":1,"tasks":[]}""")
        assertNotNull(result)
        assertTrue(result!!.isEmpty())
    }

    @Test
    fun `fromJson skips tasks with blank name`() {
        val json = """{"v":1,"tasks":[{"n":"","i":[]},{"n":"Valid","i":["step"]}]}"""
        val result = QrCodeHelper.fromJson(json)
        assertNotNull(result)
        assertEquals(1, result!!.size)
        assertEquals("Valid", result[0].name)
    }

    @Test
    fun `fromJson skips tasks with whitespace-only name`() {
        val json = """{"v":1,"tasks":[{"n":"   ","i":[]},{"n":"Real task","i":[]}]}"""
        val result = QrCodeHelper.fromJson(json)
        assertNotNull(result)
        assertEquals(1, result!!.size)
    }

    @Test
    fun `fromJson returns null for completely malformed JSON`() {
        assertNull(QrCodeHelper.fromJson("not json at all"))
        assertNull(QrCodeHelper.fromJson("{invalid{"))
    }

    @Test
    fun `fromJson returns null for empty string`() {
        assertNull(QrCodeHelper.fromJson(""))
    }

    // ── decode ────────────────────────────────────────────────────────────────

    @Test
    fun `decode handles legacy plain-JSON format`() {
        val tasks = listOf(ShareableTask("Legacy task", listOf("Do this")))
        val legacyJson = QrCodeHelper.toJson(tasks)
        // plain JSON (no "z:" prefix) is the legacy path
        val result = QrCodeHelper.decode(legacyJson)
        assertEquals(tasks, result)
    }

    @Test
    fun `decode returns null for invalid legacy JSON`() {
        assertNull(QrCodeHelper.decode("not json"))
    }

    @Test
    fun `decode handles compressed z-prefix format`() {
        val tasks = listOf(
            ShareableTask("Compressed task", listOf("Step one", "Step two"))
        )
        // Build compressed payload the same way QrCodeHelper.toQrString does internally
        val json = QrCodeHelper.toJson(tasks)
        val compressed = deflate(json)
        val payload = "z:" + Base64.encodeToString(compressed, Base64.NO_WRAP)

        val result = QrCodeHelper.decode(payload)
        assertEquals(tasks, result)
    }

    @Test
    fun `decode returns null for truncated compressed payload`() {
        assertNull(QrCodeHelper.decode("z:notvalidbase64!!!@@##"))
    }

    @Test
    fun `decode returns null for z-prefix with empty payload`() {
        // "z:" followed by valid Base64 of random bytes that decompress to garbage
        assertNull(QrCodeHelper.decode("z:"))
    }

    @Test
    fun `decode is consistent with toJson for round-trip`() {
        val tasks = listOf(
            ShareableTask("Task A", listOf("Inst 1", "Inst 2")),
            ShareableTask("Task B", emptyList())
        )
        val json = QrCodeHelper.toJson(tasks)
        val decoded = QrCodeHelper.decode(json) // legacy path
        assertEquals(tasks, decoded)
    }

    // ── helper ────────────────────────────────────────────────────────────────

    /** Mirrors the private compress() in QrCodeHelper. */
    private fun deflate(input: String): ByteArray {
        val bos = ByteArrayOutputStream()
        DeflaterOutputStream(bos).use { it.write(input.toByteArray(Charsets.UTF_8)) }
        return bos.toByteArray()
    }
}
