package com.example.myinstructions.util

import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HighlightHelperTest {

    // ── Null / blank query ────────────────────────────────────────────────────

    @Test
    fun `highlight returns plain String for null query`() {
        val result = HighlightHelper.highlight("Hello world", null)
        assertTrue("Expected String, got ${result.javaClass}", result is String)
        assertEquals("Hello world", result.toString())
    }

    @Test
    fun `highlight returns plain String for empty query`() {
        val result = HighlightHelper.highlight("Hello world", "")
        assertTrue(result is String)
    }

    @Test
    fun `highlight returns plain String for whitespace-only query`() {
        val result = HighlightHelper.highlight("Hello world", "   ")
        assertTrue(result is String)
    }

    // ── Matching behaviour ────────────────────────────────────────────────────

    @Test
    fun `highlight applies one span for a single match`() {
        val result = HighlightHelper.highlight("Hello world", "Hello") as SpannableString
        val spans = result.getSpans(0, result.length, BackgroundColorSpan::class.java)
        assertEquals(1, spans.size)
    }

    @Test
    fun `highlight span covers the correct character range`() {
        val result = HighlightHelper.highlight("Hello world", "Hello") as SpannableString
        val spans = result.getSpans(0, result.length, BackgroundColorSpan::class.java)
        assertEquals(0, result.getSpanStart(spans[0]))
        assertEquals(5, result.getSpanEnd(spans[0]))
    }

    @Test
    fun `highlight applies multiple spans for repeated occurrences`() {
        val result = HighlightHelper.highlight("abc abc abc", "abc") as SpannableString
        val spans = result.getSpans(0, result.length, BackgroundColorSpan::class.java)
        assertEquals(3, spans.size)
    }

    @Test
    fun `highlight is case-insensitive`() {
        val result = HighlightHelper.highlight("Hello World", "hello") as SpannableString
        val spans = result.getSpans(0, result.length, BackgroundColorSpan::class.java)
        assertEquals(1, spans.size)
    }

    @Test
    fun `highlight is case-insensitive with uppercase query`() {
        val result = HighlightHelper.highlight("step by step", "STEP") as SpannableString
        val spans = result.getSpans(0, result.length, BackgroundColorSpan::class.java)
        assertEquals(2, spans.size)
    }

    @Test
    fun `highlight returns SpannableString with no spans when query does not match`() {
        val result = HighlightHelper.highlight("Hello world", "xyz") as SpannableString
        val spans = result.getSpans(0, result.length, BackgroundColorSpan::class.java)
        assertEquals(0, spans.size)
    }

    @Test
    fun `highlight preserves the full original text content`() {
        val text = "Some instruction text to search within"
        val result = HighlightHelper.highlight(text, "text")
        assertEquals(text, result.toString())
    }

    @Test
    fun `highlight handles query that equals the full text`() {
        val text = "exact"
        val result = HighlightHelper.highlight(text, "exact") as SpannableString
        val spans = result.getSpans(0, result.length, BackgroundColorSpan::class.java)
        assertEquals(1, spans.size)
        assertEquals(0, result.getSpanStart(spans[0]))
        assertEquals(text.length, result.getSpanEnd(spans[0]))
    }

    @Test
    fun `highlight handles empty source text`() {
        val result = HighlightHelper.highlight("", "query") as SpannableString
        assertEquals("", result.toString())
        val spans = result.getSpans(0, result.length, BackgroundColorSpan::class.java)
        assertEquals(0, spans.size)
    }

    @Test
    fun `highlight span positions are correct for mid-string match`() {
        // "01234 56789"  → "world" starts at index 6
        val text = "Hello world"
        val result = HighlightHelper.highlight(text, "world") as SpannableString
        val spans = result.getSpans(0, result.length, BackgroundColorSpan::class.java)
        assertEquals(1, spans.size)
        assertEquals(6, result.getSpanStart(spans[0]))
        assertEquals(11, result.getSpanEnd(spans[0]))
    }
}
