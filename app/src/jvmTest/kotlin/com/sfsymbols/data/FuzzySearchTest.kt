package com.sfsymbols.data

import com.composables.sfsymbols.SfSymbolsCatalog
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class FuzzySearchTest {

    @Test
    fun `home finds house`() {
        val names = FuzzySearch.search("home", limit = 200).map { it.appleName }
        assertTrue(
            names.contains("house"),
            "expected \"house\" for query \"home\", got ${names.take(10)}",
        )
    }

    @Test
    fun `direct query outranks alias match`() {
        val names = FuzzySearch.search("house", limit = 200).map { it.appleName }
        assertEquals("house", names.first(), "exact apple name should rank first")
    }

    @Test
    fun `phone finds telephone`() {
        val names = FuzzySearch.search("phone", limit = 200).map { it.appleName }
        assertTrue(
            names.any { it.contains("phone") },
            "expected a phone symbol for \"phone\", got ${names.take(10)}",
        )
    }

    @Test
    fun `far-guess tags reveal symbols`() {
        val expected = mapOf(
            "garbage" to "trash",
            "logout" to "rectangle.portrait.and.arrow.right",
            "ai" to "sparkles",
            "cog" to "gear",
            "money" to "dollarsign",
        )
        for ((query, symbol) in expected) {
            val names = FuzzySearch.search(query, limit = 50).map { it.appleName }
            assertTrue(symbol in names, "expected \"$symbol\" for \"$query\", got ${names.take(10)}")
        }
    }

    @Test
    fun `typos still match`() {
        val names = FuzzySearch.search("chekmark", limit = 200).map { it.appleName }
        assertTrue(
            names.any { it.contains("checkmark") },
            "expected \"checkmark\" for typo \"chekmark\", got ${names.take(10)}",
        )
    }

    @Test
    fun `categories are searchable`() {
        val names = FuzzySearch.search("weather", limit = 200).map { it.appleName }
        assertTrue(
            names.isNotEmpty(),
            "expected the Weather category to be searchable, got no results",
        )
    }

    @Test
    fun `subsequence matching works`() {
        val names = FuzzySearch.search("hsf", limit = 200).map { it.appleName }
        assertTrue(
            names.any { it.contains("house") },
            "expected house symbols for \"hsf\", got ${names.take(10)}",
        )
    }

    @Test
    fun `empty query returns catalog head`() {
        assertEquals(
            SfSymbolsCatalog.all.take(200).map { it.appleName },
            FuzzySearch.search("   ", limit = 200).map { it.appleName },
        )
    }

    @Test
    fun `repeated queries are consistent`() {
        val first = FuzzySearch.search("home", limit = 200).map { it.appleName }
        val second = FuzzySearch.search("home", limit = 200).map { it.appleName }
        assertEquals(first, second, "cached result must match a fresh evaluation")
    }

    private fun top(query: String) = FuzzySearch.search(query, limit = 5).map { it.appleName }

    /** Checks every query, then reports all misses at once. */
    private fun assertFirst(expected: Map<String, String>) {
        val misses = expected.mapNotNull { (query, symbol) ->
            top(query).takeIf { it.firstOrNull() != symbol }?.let { "\"$query\" -> want $symbol, got $it" }
        }
        assertTrue(misses.isEmpty(), misses.joinToString("; "))
    }

    @Test
    fun `plain symbols outrank their variants`() {
        val expected = mapOf("garbage" to "trash", "cog" to "gear", "home" to "house", "trash" to "trash")
        assertFirst(expected)
    }

    @Test
    fun `misspellings suggest the intended symbol first`() {
        val expected = mapOf(
            "tradh" to "trash",     // wrong key
            "trsh" to "trash",      // missing letter
            "hosue" to "house",     // swapped letters
            "gearshap" to "gearshape",
            "garbgae" to "trash",   // typo in a tag word
            "settigns" to "gear",
        )
        assertFirst(expected)
    }

    @Test
    fun `shorthand and everyday words find symbols`() {
        assertFirst(mapOf(
            "msg" to "message", "pw" to "lock", "pics" to "photo",
            "bunny" to "hare", "zap" to "bolt", "doggy paw" to "pawprint", "four boxes" to "square.grid.2x2",
        ))
    }

    @Test
    fun `descriptions ignore filler words and counts`() {
        assertFirst(mapOf(
            "4 cubes stacked on top of each other" to "square.stack.3d.up",
            "a yucky can" to "trash",
            "4 circle" to "4.circle",
        ))
    }

    @Test
    fun `real words are not corrected into other words`() {
        // "heart" is a real word, so it must not be read as a typo of "cart".
        assertTrue(FuzzySearch.search("heart", limit = 20).none { it.appleName.startsWith("cart") })
    }
}
