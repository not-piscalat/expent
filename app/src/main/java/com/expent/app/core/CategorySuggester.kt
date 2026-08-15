package com.expent.app.core

import com.expent.app.data.local.dao.TransactionWithCategory
import com.expent.app.data.local.entity.CategoryEntity
import com.expent.app.data.local.entity.TransactionType

/** A category the suggester thinks the note belongs to, with its confidence score. */
data class CategorySuggestion(
    val category: CategoryEntity,
    val score: Int
)

/**
 * Suggests a category from a note, learning from the user's own history:
 *
 *  1. tokens the user has already categorized (strongest over time);
 *  2. a built-in keyword map matched against the *user's* category names
 *     ("grab" boosts any category whose name mentions transport/ride);
 *  3. exact name-token matches ("coffee" boosts a category named "Coffee").
 *
 * Suggestions are hints only — the UI applies them on tap.
 */
object CategorySuggester {

    private val KEYWORDS = mapOf(
        "grab" to listOf("transport", "ride"),
        "taxi" to listOf("transport", "ride"),
        "lalamove" to listOf("transport", "delivery"),
        "angkas" to listOf("transport", "ride"),
        "gas" to listOf("transport", "fuel"),
        "fuel" to listOf("transport", "fuel"),
        "parking" to listOf("transport", "park"),
        "meralco" to listOf("utilit", "electric"),
        "electric" to listOf("utilit", "electric"),
        "water" to listOf("utilit", "water"),
        "internet" to listOf("utilit", "internet"),
        "wifi" to listOf("utilit", "internet"),
        "phone" to listOf("utilit", "phone"),
        "lunch" to listOf("food", "meal", "eat"),
        "dinner" to listOf("food", "meal", "eat"),
        "breakfast" to listOf("food", "meal", "eat"),
        "coffee" to listOf("food", "coffee", "cafe"),
        "grocer" to listOf("food", "grocer"),
        "rent" to listOf("housing", "rent"),
        "movie" to listOf("entertainment", "movie"),
        "cinema" to listOf("entertainment", "movie"),
        "netflix" to listOf("entertainment", "stream"),
        "spotify" to listOf("entertainment", "stream"),
        "game" to listOf("entertainment", "game"),
        "school" to listOf("education", "school"),
        "tuition" to listOf("education", "school"),
        "gym" to listOf("health", "fitness", "gym"),
        "doctor" to listOf("health", "medical", "doctor"),
        "medicin" to listOf("health", "medical"),
        "hospital" to listOf("health", "medical"),
        "shopping" to listOf("shopping", "mall"),
        "mall" to listOf("shopping", "mall"),
        "clothes" to listOf("shopping", "cloth"),
        "salary" to listOf("salary"),
        "pay" to listOf("salary", "pay"),
        "allowance" to listOf("income", "allowance"),
        "bonus" to listOf("income", "salary")
    )

    fun suggest(
        note: String,
        history: List<TransactionWithCategory>,
        categories: List<CategoryEntity>,
        type: TransactionType,
        limit: Int = 2
    ): List<CategorySuggestion> {
        val tokens = tokenize(note)
        val candidates = categories.filter { it.type == type }
        if (tokens.isEmpty() || candidates.isEmpty()) return emptyList()

        // 1) Learned: count how often the user has categorized these tokens.
        val tokenSet = tokens.toSet()
        val historyScores = mutableMapOf<Long, Int>()
        for (item in history) {
            val historyNote = item.transaction.note?.takeIf { it.isNotBlank() } ?: continue
            val categoryId = item.transaction.categoryId ?: continue
            val shared = tokenize(historyNote).intersect(tokenSet)
            if (shared.isNotEmpty()) {
                historyScores[categoryId] = (historyScores[categoryId] ?: 0) + shared.size
            }
        }
        // The exact same note before is the strongest learned signal.
        val exactNote = note.trim().lowercase()
        for (item in history) {
            val historyNote = item.transaction.note?.trim()?.lowercase() ?: continue
            val categoryId = item.transaction.categoryId ?: continue
            if (historyNote == exactNote) {
                historyScores[categoryId] = (historyScores[categoryId] ?: 0) + 5
            }
        }

        return candidates.map { category ->
            var score = historyScores[category.id] ?: 0
            val name = category.name.lowercase()
            // 2) Built-in keywords, matched against the user's category names.
            for (token in tokens) {
                val hints = KEYWORDS[token] ?: continue
                if (hints.any { name.contains(it) }) score += 2
            }
            // 3) The note names the category directly.
            val nameTokens = tokenize(category.name)
            if (tokens.any { it in nameTokens }) score += 3
            CategorySuggestion(category, score)
        }
            .filter { it.score > 0 }
            .sortedByDescending { it.score }
            .take(limit)
    }

    private fun tokenize(text: String): List<String> =
        text.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 2 }
}
