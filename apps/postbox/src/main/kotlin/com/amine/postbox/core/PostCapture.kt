package com.amine.postbox.core

import kotlinx.serialization.Serializable

@Serializable
data class PostCapture(
    val id: String,
    val title: String,
    val text: String,
    val sourceUrl: String,
    val imagesJpegBase64: List<String>,
    val capturedAtEpochMs: Long,
)

@Serializable
data class StoredCapture(
    val capture: PostCapture,
    val archived: Boolean = false,
    val sent: Boolean = false,
)

enum class SaveResult {
    ADDED,
    ALREADY_PRESENT,
    CONFLICT,
    FULL,
}

@Serializable
data class PairedTv(
    val host: String,
    val port: Int,
    val pin: String,
    val token: String,
    val name: String,
)

data class ReceiverSession(val pairingLink: String, val expiresAtEpochMs: Long)
