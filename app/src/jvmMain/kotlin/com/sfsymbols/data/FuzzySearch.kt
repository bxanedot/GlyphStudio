package com.sfsymbols.data

import com.composables.sfsymbols.SfSymbolMetadata
import com.composables.sfsymbols.SfSymbolsCatalog
import kotlin.math.abs
import kotlin.math.min

/**
 * Lightweight fuzzy, typo-tolerant, ranked search over the SF Symbols catalog.
 *
 * Symbols are matched against their apple name (e.g. "0.circle.fill"),
 * its dot/hyphen/space-separated segments, the camel-cased pascal name and
 * their categories and far-guess [SymbolTags]. Queries are tokenized the same
 * way; scores combine exact/prefix/substring, subsequence, tag and category
 * matches, with a bounded Levenshtein edit-distance fallback for typos.
 */
public object FuzzySearch {

    private data class Indexed(
        val apple: String,
        val segments: List<String>,
        val pascalWords: List<String>,
        val normalized: String,
        val categories: List<String>,
        val tags: List<String>,
    )

    private val index: List<Indexed> by lazy {
        SfSymbolsCatalog.all.map { meta ->
            val apple = meta.appleName.lowercase()
            Indexed(
                apple = apple,
                segments = apple.split('.', '-', '_', ' ').filter { it.isNotEmpty() },
                pascalWords = camelWords(meta.pascalName),
                normalized = apple.replace(".", " ").replace("-", " ").replace("_", " "),
                categories = meta.categories.map { it.lowercase() },
                tags = SymbolTags.tagsFor(meta.appleName),
            )
        }
    }

    /** Every real word in the catalog. A query word found here is never treated as a typo of another word. */
    private val vocabulary: Set<String> by lazy {
        index.flatMapTo(HashSet()) { it.segments + it.pascalWords + it.tags }
    }

    private const val STRONG_TOKEN = 120

    private val FILLER_WORDS = setOf(
        "a", "an", "the", "of", "on", "in", "at", "to", "for", "with", "and", "or", "each", "other",
        "top", "some", "like", "that", "is", "are", "my", "me", "icon", "symbol", "something", "thing",
    )
    private const val CACHE_LIMIT = 256

    /** Access-ordered so the least recently used query is evicted first. */
    private val cache = LinkedHashMap<String, List<SfSymbolMetadata>>(64, 0.75f, true)

    private fun camelWords(pascal: String): List<String> {
        if (pascal.isEmpty()) return emptyList()
        val sb = StringBuilder(pascal[0].lowercaseChar().toString())
        for (c in pascal.drop(1)) {
            if (c.isUpperCase()) sb.append(' ')
            sb.append(c.lowercaseChar())
        }
        return sb.toString().split(' ').filter { it.isNotEmpty() }
    }

    public fun search(query: String, limit: Int = 200): List<SfSymbolMetadata> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return SfSymbolsCatalog.all.take(limit)

        synchronized(cache) { cache[q] }?.let { return it.take(limit) }

        // Drop filler so a description like "cubes stacked on top of each other" is scored on its real words.
        val tokens = q.split('.', '-', '_', ' ').filter { it.isNotEmpty() }
            .let { all -> all.filterNot { it in FILLER_WORDS }.ifEmpty { all } }
            // In a sentence ("4 cubes stacked") a number is a count, not part of a name like "4.circle".
            .let { words -> if (words.size >= 3) words.filterNot { w -> w.all(Char::isDigit) } else words }

        val scored = ArrayList<Pair<Int, Int>>(index.size) // (score, symbolIndex)
        val all = SfSymbolsCatalog.all
        for (i in index.indices) {
            val score = score(index[i], tokens, q)
            if (score > 0) scored.add(score to i)
        }
        // Higher score first; among equals the plainest symbol wins, so "trash" beats "arrow.up.trash.fill".
        scored.sortWith(
            compareByDescending<Pair<Int, Int>> { it.first }
                .thenBy { index[it.second].segments.size }
                .thenBy { index[it.second].apple.length }
                .thenBy { index[it.second].apple }
        )
        val result = scored.map { all[it.second] }
        synchronized(cache) {
            if (cache.size >= CACHE_LIMIT) cache.clear()
            cache[q] = result
        }
        return result.take(limit)
    }

    private fun score(ix: Indexed, tokens: List<String>, q: String): Int {
        var total = 0

        // Exact names rank first; exact tags outrank incidental name prefixes.
        if (ix.apple == q) total += 1000
        else if (q in ix.tags) total += 700
        else if (ix.apple.startsWith(q)) total += 700
        else if (ix.apple.contains(q)) total += 350
        else if (ix.normalized.contains(q)) total += 300

        var anyStrongToken = false
        for (t in tokens) {
            val best = tokenScore(ix, t)
            if (best >= STRONG_TOKEN) anyStrongToken = true
            total += best
        }

        if (!anyStrongToken) return 0

        // Multi-segment queries (e.g. "arrow.right"): require most segments hit.
        val dotCount = q.count { it == '.' }
        if (dotCount >= 2 && tokens.size == dotCount + 1) {
            val segmentMatches = tokens.count { t ->
                ix.segments.any { it == t || it.startsWith(t) || editDistanceMatch(ix, t) > 0 }
            }
            if (segmentMatches < tokens.size) total /= 2
        }

        val qNoDots = q.replace(".", "")
        if (qNoDots.isNotEmpty() && ix.pascalWords.isNotEmpty() &&
            ix.pascalWords.joinToString("").contains(qNoDots)
        ) {
            total = max(total, 220)
        }
        return total
    }

    private fun tokenScore(ix: Indexed, t: String): Int {
        var best = 0
        for (seg in ix.segments) {
            val s = when {
                seg == t -> 600
                seg.startsWith(t) -> 450
                else -> 0
            }
            if (s > best) best = s
        }
        for (w in ix.pascalWords) {
            val s = when {
                w == t -> 550
                w.startsWith(t) -> 380
                else -> 0
            }
            if (s > best) best = s
        }
        if (best == 0 && ix.normalized.contains(t)) best = 250
        // A close misspelling of a real word beats scattered letters that merely appear in order.
        if (best < 250 && t.length in 3..14 && t !in vocabulary) best = max(best, editDistanceMatch(ix, t))
        if (best == 0 && t.length >= 3 && isSubsequence(t, ix.normalized)) best = 150
        for (cat in ix.categories) {
            val s = when {
                cat == t -> 500
                cat.startsWith(t) -> 320
                t.length >= 3 && cat.contains(t) -> 200
                else -> 0
            }
            if (s > best) best = s
        }
        // Tags sit just below a direct name-word hit and above category hits.
        for (tag in ix.tags) {
            val s = when {
                tag == t -> 560
                t.length >= 3 && tag.startsWith(t) -> 300
                else -> 0
            }
            if (s > best) best = s
        }
        return best
    }

    /**
     * Typo match against the symbol's name words and its tags ("tradh" -> trash,
     * "garbgae" -> the garbage tag). Fewer edits score higher: 1 -> 260, 2 -> 220, 3 -> 180.
     */
    private fun editDistanceMatch(ix: Indexed, t: String): Int {
        val budget = editBudget(t.length)
        var best = budget + 1
        for (word in ix.segments.asSequence() + ix.pascalWords + ix.tags) {
            if (word.length in 2..14 && abs(word.length - t.length) <= budget) {
                best = min(best, boundedDistance(word, t, budget))
            }
        }
        return if (best in 1..budget) 300 - 40 * best else 0
    }

    /** Short words tolerate one edit; longer words tolerate proportionally more. */
    private fun editBudget(len: Int): Int = when {
        len <= 4 -> 1
        len <= 7 -> 2
        else -> 3
    }

    private fun isSubsequence(needle: String, haystack: String): Boolean {
        var i = 0
        for (c in haystack) {
            if (i < needle.length && c == needle[i]) i++
        }
        return i == needle.length
    }

    /**
     * Edit distance where a wrong, missing or extra letter and two swapped
     * neighbours ("hosue") each count as one edit (optimal string alignment).
     * Gives up once two consecutive rows exceed [maxDist], returning [maxDist] + 1.
     */
    internal fun boundedDistance(a: String, b: String, maxDist: Int): Int {
        val la = a.length
        val lb = b.length
        if (abs(la - lb) > maxDist) return maxDist + 1
        if (la == 0) return lb
        if (lb == 0) return la
        var prev2 = IntArray(lb + 1)
        var prev = IntArray(lb + 1) { it }
        var curr = IntArray(lb + 1)
        var prevMin = 0
        for (i in 1..la) {
            curr[0] = i
            var rowMin = i
            for (j in 1..lb) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var d = min(min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) d = min(d, prev2[j - 2] + 1)
                curr[j] = d
                if (d < rowMin) rowMin = d
            }
            // A swap can reach back two rows, so stop only when both rows are out of budget.
            if (rowMin > maxDist && prevMin > maxDist) return maxDist + 1
            prevMin = rowMin
            val tmp = prev2; prev2 = prev; prev = curr; curr = tmp
        }
        return min(prev[lb], maxDist + 1)
    }

    private fun max(a: Int, b: Int): Int = if (a > b) a else b
}
