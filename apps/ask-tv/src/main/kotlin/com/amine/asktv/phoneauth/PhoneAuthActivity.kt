package com.amine.asktv.phoneauth

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Retained only inside this process. No pairing data is written to saved instance state. */
internal class PhoneHelperMemory : ViewModel() {
    var candidate by mutableStateOf<PhonePairingLink?>(null)
    var paste by mutableStateOf("")
    var message by mutableStateOf<String?>(null)
    var inProgress by mutableStateOf(false)
    var continueRequested by mutableStateOf(false)
    var permissionsReady = false
    var notificationsRequested = false
    private var confirmedCandidate by mutableStateOf<PhonePairingLink?>(null)
    val hasActiveFlow: Boolean get() = inProgress || confirmedCandidate != null
    fun offerCandidate(link: PhonePairingLink): Boolean {
        if (hasActiveFlow) return false
        candidate = link
        return true
    }
    fun confirmCandidate(): Boolean {
        if (hasActiveFlow) return false
        confirmedCandidate = candidate ?: return false
        continueRequested = true
        permissionsReady = false
        return true
    }
    fun consumeConfirmedCandidate(): PhonePairingLink? {
        val confirmed = confirmedCandidate
        confirmedCandidate = null
        continueRequested = false
        permissionsReady = false
        return confirmed
    }
    fun cancelConfirmation() {
        confirmedCandidate = null
        continueRequested = false
        permissionsReady = false
    }
    var photo: File? = null
    fun discardPhoto() {
        val previous = photo
        photo = null
        if (previous != null) viewModelScope.launch(Dispatchers.IO) { previous.delete() }
    }
    override fun onCleared() {
        val previous = photo
        photo = null
        if (previous != null) CoroutineScope(Dispatchers.IO).launch { previous.delete() }
    }
}

class PhoneAuthActivity : ComponentActivity() {
    private lateinit var memory: PhoneHelperMemory
    private val binding = MutableStateFlow<PhoneAuthService.LocalBinder?>(null)
    private var bound = false
    private var ui by mutableStateOf(PhoneAuthUi())
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            binding.value = service as PhoneAuthService.LocalBinder
            continueWhenReady()
        }
        override fun onServiceDisconnected(name: ComponentName) {
            binding.value = null
            memory.inProgress = false
            memory.cancelConfirmation()
            ui = PhoneAuthUi(PhoneAuthPhase.FINISHED, "The phone helper stopped. Get a new code on your TV.")
        }
    }
    private val networkPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) requestNotifications()
        else {
            memory.cancelConfirmation()
            memory.message = "Local network access is needed to reach your TV. Select Continue to try again."
        }
    }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Android permits a foreground service after notification permission is denied.
        memory.permissionsReady = true
        continueWhenReady()
    }
    private val photograph = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = memory.photo
        memory.photo = null
        if (file != null) memory.viewModelScope.launch {
            val decoded = withContext(Dispatchers.IO) {
                if (!success) { file.delete(); null }
                else runCatching { PhonePairingPhotoReader.read(file) }
            }
            if (success) {
                val link = decoded?.getOrNull()
                if (link == null) memory.message = "Could not read the TV code. Photograph the complete code again, or paste the link."
                else if (memory.offerCandidate(link)) {
                    memory.paste = ""
                    memory.message = null
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        memory = ViewModelProvider(this)[PhoneHelperMemory::class.java]
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                binding.collectLatest { service ->
                    if (service == null) return@collectLatest
                    service.snapshot.collect { update ->
                        ui = update
                        memory.inProgress = update.phase in ACTIVE_PHASES
                        if (memory.inProgress && memory.candidate != null) {
                            memory.candidate = null
                            memory.paste = ""
                            memory.message = "Sign-in is already active. Cancel it before opening another TV code."
                        }
                        if (update.canOpenBrowser) {
                            val authorization = service.takeBrowserLaunch() ?: return@collect
                            try {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(authorization.url)).addCategory(Intent.CATEGORY_BROWSABLE))
                            } catch (_: Exception) {
                                service.cancel()
                                memory.message = "A browser could not open. Install or enable a browser, then get a new TV code."
                            }
                        }
                    }
                }
            }
        }
        receiveAndClear(intent)
        // Remove private leftovers from a process that stopped before a camera result arrived.
        val createdAt = System.currentTimeMillis()
        if (memory.photo == null) lifecycleScope.launch(Dispatchers.IO) {
            File(cacheDir, "phone_pairing").listFiles()?.filter { it.lastModified() < createdAt }?.forEach { it.delete() }
        }
        setContent {
            PhoneAuthScreen(
                ui = if (memory.continueRequested) PhoneAuthUi(PhoneAuthPhase.STARTING, "Allow the requested permissions to continue.") else ui,
                hasPairingLink = memory.candidate != null,
                paste = memory.paste,
                message = memory.message,
                inProgress = memory.hasActiveFlow,
                onPaste = { value ->
                    memory.paste = value.take(2049)
                    memory.candidate = null
                    memory.message = null
                },
                onUsePaste = {
                    val parsed = runCatching { PhonePairingLink.parse(memory.paste.trim()) }.getOrNull()
                    if (parsed != null) memory.offerCandidate(parsed)
                    memory.message = if (parsed == null) "This is not a valid AI Chat TV code. Get a new code on your TV." else null
                    memory.paste = ""
                },
                onPhotograph = ::takePhotograph,
                onContinue = ::continueFromVisibleUi,
                onCancel = ::cancelFlow,
            )
        }
    }
    override fun onStart() {
        super.onStart()
        if (!bound) bound = bindService(Intent(this, PhoneAuthService::class.java), connection, Context.BIND_AUTO_CREATE)
        if (!bound) memory.message = "The phone helper could not start. Reopen AI Chat and try a new TV code."
    }
    override fun onResume() {
        super.onResume()
        continueWhenReady()
    }
    override fun onDestroy() {
        // Keep the binding while the browser is visible. After foreground completion,
        // the bound service retains only its safe result until this activity is destroyed.
        if (bound) unbindService(connection)
        bound = false
        binding.value = null
        super.onDestroy()
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        receiveAndClear(intent)
    }
    private fun receiveAndClear(incoming: Intent) {
        val raw = incoming.dataString
        incoming.data = null
        incoming.clipData = null
        incoming.replaceExtras(Bundle())
        setIntent(Intent(this, PhoneAuthActivity::class.java))
        if (raw == null) return
        val parsed = runCatching { PhonePairingLink.parse(raw) }.getOrNull()
        if (memory.hasActiveFlow) {
            memory.message = "Sign-in is already active. Cancel it before opening another TV code."
        } else {
            if (parsed != null) memory.offerCandidate(parsed) else memory.candidate = null
            memory.paste = ""
            memory.message = if (parsed == null) "This is not a valid AI Chat TV code. Get a new code on your TV." else null
        }
    }
    private fun continueFromVisibleUi() {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || !memory.confirmCandidate()) return
        memory.message = null
        if (!hasLocalNetworkPermission()) networkPermission.launch(LOCAL_NETWORK_PERMISSION)
        else requestNotifications()
    }
    private fun requestNotifications() {
        if (!memory.continueRequested) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && !memory.notificationsRequested) {
            memory.notificationsRequested = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            memory.permissionsReady = true
            continueWhenReady()
        }
    }
    private fun continueWhenReady() {
        if (!::memory.isInitialized || !memory.continueRequested || !memory.permissionsReady || !hasLocalNetworkPermission() ||
            !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        val service = binding.value
        if (service == null) {
            if (!bound) {
                memory.cancelConfirmation()
                memory.message = "The phone helper could not start. Reopen AI Chat and try a new TV code."
            }
            return
        }
        val link = memory.consumeConfirmedCandidate() ?: return
        try {
            checkNotNull(startForegroundService(Intent(this, PhoneAuthService::class.java)))
            service.start(link)
            memory.inProgress = true
            memory.candidate = null
            memory.paste = ""
        } catch (_: Exception) {
            service.cancel()
            stopService(Intent(this, PhoneAuthService::class.java))
            memory.inProgress = false
            memory.message = "The phone helper could not start. Reopen AI Chat and try a new TV code."
        }
    }
    private fun hasLocalNetworkPermission(): Boolean = Build.VERSION.SDK_INT < 37 ||
        checkSelfPermission(LOCAL_NETWORK_PERMISSION) == PackageManager.PERMISSION_GRANTED
    private fun cancelFlow() {
        memory.cancelConfirmation()
        memory.candidate = null
        memory.paste = ""
        memory.message = null
        memory.inProgress = false
        memory.discardPhoto()
        binding.value?.cancel()
    }
    private fun takePhotograph() {
        if (memory.hasActiveFlow) return
        memory.discardPhoto()
        lifecycleScope.launch {
            var file: File? = null
            try {
                file = withContext(Dispatchers.IO) {
                    val directory = File(cacheDir, "phone_pairing")
                    check(directory.isDirectory || directory.mkdirs())
                    File.createTempFile("tv-code-", ".jpg", directory)
                }
                memory.photo = file
                val uri = FileProvider.getUriForFile(this@PhoneAuthActivity, "$packageName.phone-pairing-files", file)
                photograph.launch(uri)
            } catch (_: Exception) {
                memory.photo = null
                val abandoned = file
                if (abandoned != null) CoroutineScope(Dispatchers.IO).launch { abandoned.delete() }
                memory.message = "Camera unavailable. Use Paste pairing link instead."
            }
        }
    }
    private companion object {
        const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
        val ACTIVE_PHASES = setOf(PhoneAuthPhase.STARTING, PhoneAuthPhase.READY_FOR_BROWSER, PhoneAuthPhase.WAITING_FOR_CALLBACK, PhoneAuthPhase.VERIFYING)
    }
}
