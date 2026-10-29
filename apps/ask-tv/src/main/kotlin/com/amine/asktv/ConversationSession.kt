package com.amine.asktv

import kotlinx.serialization.json.*

data class ConversationPair(val question: String, val answer: String)
class ConversationSession {
    private val completed = mutableListOf<ConversationPair>()
    val pairs: List<ConversationPair> get() = completed.toList()
    fun complete(question: String, answer: String) {
        require(question.length <= 4000)
        if (answer.isBlank()) return
        completed += ConversationPair(question, answer)
        while (completed.size > 6 || completed.sumOf { it.question.length + it.answer.length } > 24000) completed.removeAt(0)
    }
    fun input(question: String): JsonArray {
        require(question.isNotBlank() && question.length <= 4000) { "Use 4,000 characters or fewer." }
        return buildJsonArray {
            completed.forEach { pair -> add(message("user", pair.question)); add(message("assistant", pair.answer)) }
            add(message("user", question))
        }
    }
    private fun message(role: String, text: String) = buildJsonObject { put("role", role); put("content", text) }
    fun clear() { completed.clear() }
}
