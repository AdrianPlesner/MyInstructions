package com.example.myinstructions.util

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan

object HighlightHelper {

    private val HIGHLIGHT_COLOR = Color.parseColor("#FFFF00")

    /**
     * Returns a SpannableString with all case-insensitive occurrences of [query]
     * highlighted with a yellow background. Returns plain text if query is blank.
     */
    fun highlight(text: String, query: String?): CharSequence {
        if (query.isNullOrBlank()) return text
        val spannable = SpannableString(text)
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        var start = lowerText.indexOf(lowerQuery)
        while (start >= 0) {
            spannable.setSpan(
                BackgroundColorSpan(HIGHLIGHT_COLOR),
                start,
                start + lowerQuery.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            start = lowerText.indexOf(lowerQuery, start + lowerQuery.length)
        }
        return spannable
    }
}
