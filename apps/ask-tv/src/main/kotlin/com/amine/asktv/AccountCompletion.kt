package com.amine.asktv

/** Delivers durable account state and operation-specific actions on the UI queue. */
internal class AccountCompletion(
    private val operations: AskController,
    private val enqueue: (() -> Unit) -> Unit,
    private val currentRecord: () -> CredentialRecord?,
    private val refreshAccount: (CredentialRecord?) -> Unit,
) {
    fun committed(token: Long, activeActions: () -> Unit) {
        enqueue {
            refreshAccount(currentRecord())
            if (operations.isCurrent(token)) activeActions()
        }
    }
}
