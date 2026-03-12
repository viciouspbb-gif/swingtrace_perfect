package com.golftrajectory.app.ai

/**
 * Chat message data class for AI coaching conversation
 */
data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val options: List<ConversationOption>? = null
)

/**
 * Conversation option for interactive AI coaching
 */
data class ConversationOption(
    val id: String,
    val text: String,
    val action: String
)
