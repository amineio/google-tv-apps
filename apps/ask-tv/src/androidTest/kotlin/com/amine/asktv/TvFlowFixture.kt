package com.amine.asktv

import android.app.Application
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.platform.app.InstrumentationRegistry
import com.amine.asktv.phoneauth.*
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/** Synthetic rendering fixture. It does not establish MainActivity window/lifecycle correctness. */
internal class TvFlowFixture(initial: CredentialRecord = CredentialRecord(HOST)) : AutoCloseable {
    private val worker = Executors.newSingleThreadExecutor()
    private val modelStore = androidx.lifecycle.ViewModelStore()
    private val records = object : RecordStorage {
        @Volatile var value = initial
        override fun read() = value
        override fun write(record: CredentialRecord) { value = record }
    }
    val hosts = CopyOnWriteArrayList<Host>()
    private val tickers = CopyOnWriteArrayList<() -> Unit>()
    var permissionAllowed = true
    var deferPermissionResult = false
    var pendingPermissionGeneration: Long? = null
    var requestConsent = false
    var exchangeScopes = REQUIRED_SCOPES
    var modelFailure:Exception? = null
    var exits = 0
    val model:AskViewModel = run {
        lateinit var created:AskViewModel
        InstrumentationRegistry.getInstrumentation().runOnMainSync { created = AskViewModel(
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application,
        AskDependencies(
            store = SessionStore(records),
            worker = worker,
            exchange = { grant, current, _ ->
                CredentialRecord(current.hostId, grant.clientId, "synthetic-subject", ISSUER,
                    "viewer@example.test", "synthetic-access", "synthetic-refresh", "synthetic-id",
                    System.currentTimeMillis() / 1000 + 3600, exchangeScopes)
            },
            models = { _, _ -> modelFailure?.let { throw it }; listOf(Model("synthetic-model", "Example model")) },
            revoke = { _, _ -> true },
            phoneFactory = { onGrant, onUi ->
                PhoneSignInCoordinator(onGrant, onUi,
                    hostFactory = { relay -> Host(relay).also(hosts::add) },
                    worker = worker,
                    monotonicMs = android.os.SystemClock::elapsedRealtime,
                    startTicker = { action -> tickers.add(action); AutoCloseable { tickers.remove(action) } })
            },
            macFactory = { _, _, _ -> object : MacSignInHost {
                override fun start() = Unit
                override fun close() = Unit
            } },
        ),
    ).also { modelStore.put("synthetic-tv-flow", it) } }
        created
    }

    fun render(rule: ComposeContentTestRule) {
        rule.setContent {
            AskTheme {
                AskScreen(model, onAsk = {}, onExit = { exits++ }, onPhoneSignIn = { consent ->
                    requestConsent = consent
                    val generation = model.beginPhonePermission(consent)
                    pendingPermissionGeneration = generation
                    if (!deferPermissionResult) model.phonePermissionResult(generation, permissionAllowed)
                })
            }
        }
    }
    fun pulse() { tickers.toList().forEach { it() } }
    fun claim(): RelayClaim = hosts.last().claim()
    fun callback(claim: RelayClaim) {
        val query = URI(claim.authorizationUrl).rawQuery.split('&').associate {
            val part = it.split('=', limit = 2)
            URLDecoder.decode(part[0], "UTF-8") to URLDecoder.decode(part[1], "UTF-8")
        }
        hosts.last().relay.callback(claim.relayToken,
            "/auth/callback?state=${query.getValue("state")}&code=synthetic&client_id=issued")
    }
    fun cancelFromPhone(claim: RelayClaim) { hosts.last().relay.cancel(claim.relayToken); pulse() }
    fun workerAction(action: () -> Unit) { worker.submit(action).get(3, java.util.concurrent.TimeUnit.SECONDS) }
    override fun close() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { modelStore.clear() }
        worker.shutdown()
        check(worker.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS))
    }

    class Host(val relay: PhoneRelayProtocol) : PhoneRelayHost {
        lateinit var link: PhonePairingLink
        @Volatile var closed = false
        override fun start(): PhonePairingLink =
            PhonePairingLink("192.168.1.2", 4444, "a".repeat(64), relay.open()).also { link = it }
        override fun close() { closed = true; relay.close() }
        fun claim() = relay.claim(link.token)
    }

    companion object {
        const val HOST = "urn:uuid:00000000-0000-0000-0000-000000000001"
        fun connected(scopes: Set<String> = REQUIRED_SCOPES) = CredentialRecord(
            HOST, "issued", "synthetic-subject", ISSUER, "viewer@example.test",
            "synthetic-access", "synthetic-refresh", "synthetic-id",
            System.currentTimeMillis() / 1000 + 3600, scopes)
    }
}
