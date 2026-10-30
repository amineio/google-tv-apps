package com.amine.postbox

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.amine.postbox.core.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal data class CaptureDraft(
    val title: String = "",
    val text: String = "",
    val sourceUrl: String = "",
    val images: List<String> = emptyList(),
    val replacesId: String? = null,
) {
    val linkOnly: Boolean
        get() = text.isBlank() && images.isEmpty() && sourceUrl.isNotBlank()
}

internal class PostboxViewModel(application: Application, private val phone: Boolean) :
    AndroidViewModel(application) {
    var items by mutableStateOf<List<StoredCapture>>(emptyList())
        private set

    var loaded by mutableStateOf(false)
        private set

    var busy by mutableStateOf(false)
        private set

    var message by mutableStateOf("")
        private set

    var draft by mutableStateOf(CaptureDraft())
        private set

    var phoneScreen by mutableStateOf("library")
        private set

    var selectedPhoneId by mutableStateOf<String?>(null)
        private set

    var pairedTv by mutableStateOf<PairedTv?>(null)
        private set

    var pairingInput by mutableStateOf("")
    var showPairingInput by mutableStateOf(false)
    private var beforeConnect = "library"
    var tvScreen by mutableStateOf("library")
        private set

    var archived by mutableStateOf(false)
        private set

    var selectedTvId by mutableStateOf<String?>(null)
        private set

    var example by mutableStateOf(false)
        private set

    var session by mutableStateOf<ReceiverSession?>(null)
        private set

    var remainingSeconds by mutableStateOf(0)
        private set

    var resumeGeneration by mutableStateOf(0)
        private set

    private var resumed = false
    private var lanAllowed = false
    private var receiver: CaptureReceiver? = null
    private val receiverMutex = Mutex()
    private var receiverGeneration = 0
    private var receiverDeadlineElapsedMs = 0L
    private var refreshGeneration = 0
    private var mutationRevision = 0
    private var pairRevision = 0
    private val app
        get() = getApplication<Application>()

    private val directory
        get() = File(app.filesDir, if (phone) "phone-captures" else "tv-captures")

    private val pairedFile
        get() = File(app.filesDir, "paired-tv.json")

    private val store by lazy { PostboxStore(directory) }
    private val json = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
    }
    val selectedPhone: StoredCapture?
        get() = items.firstOrNull { it.capture.id == selectedPhoneId }

    val tvItems: List<StoredCapture>
        get() = items.filter { it.archived == archived }

    val selectedTv: StoredCapture?
        get() = items.firstOrNull { it.capture.id == selectedTvId }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val generation = ++refreshGeneration
            val mutation = mutationRevision
            val pairVersion = pairRevision
            try {
                val result =
                    withContext(Dispatchers.IO) {
                        val list = store.list()
                        val savedPair =
                            if (phone && pairedFile.exists())
                                runCatching {
                                        json
                                            .decodeFromString<PairedTv>(pairedFile.readText())
                                            .also {
                                                PairingLink.parse(
                                                    PairingLink(it.host, it.port, it.pin, it.token)
                                                        .encode()
                                                )
                                            }
                                    }
                                    .getOrNull()
                            else null
                        Triple(list, store.hasStorageIssue, savedPair)
                    }
                if (generation == refreshGeneration && mutation == mutationRevision) {
                    items = result.first
                    if (result.second) message = "Some local captures could not be read."
                }
                if (phone && pairVersion == pairRevision) pairedTv = result.third
            } catch (_: Exception) {
                message = "Local storage unavailable. Your existing captures are kept."
            }
            loaded = true
        }
    }

    fun clearMessage() {
        message = ""
    }

    fun showMessage(value: String) {
        message = value
    }

    fun newCapture() {
        draft = CaptureDraft()
        phoneScreen = "editor"
        message = ""
    }

    fun updateDraft(
        title: String = draft.title,
        text: String = draft.text,
        source: String = draft.sourceUrl,
    ) {
        draft = draft.copy(title = title, text = text, sourceUrl = source)
    }

    fun removeImage(index: Int) {
        draft = draft.copy(images = draft.images.filterIndexed { i, _ -> i != index })
    }

    fun receiveIntent(intent: Intent) {
        if (busy) {
            val pending = Intent(intent)
            viewModelScope.launch {
                while (busy) delay(25)
                receiveIntent(pending)
            }
            return
        }
        if (intent.action == Intent.ACTION_VIEW) {
            connect()
            pairingInput = intent.dataString.orEmpty()
            showPairingInput = true
            return
        }
        if (intent.action !in listOf(Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE)) return
        newCapture()
        val shared = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()
        val url = Regex("https?://[^\\s]+").find(shared)?.value.orEmpty()
        val onlyUrl = url.isNotEmpty() && shared.trim() == url
        draft =
            CaptureDraft(
                title = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty(),
                text = if (onlyUrl) "" else shared,
                sourceUrl = url,
            )
        val streams =
            if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
            } else listOfNotNull(intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
        val clipUris =
            intent.clipData
                ?.let { clip -> (0 until clip.itemCount).mapNotNull { clip.getItemAt(it).uri } }
                .orEmpty()
        addImages((streams + clipUris).distinct())
    }

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (draft.images.size + uris.size > 4) {
            message = "Use up to four images."
            return
        }
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                val values =
                    withContext(Dispatchers.IO) {
                        uris.map { ImageNormalizer.read(app.contentResolver, it) }
                    }
                draft = draft.copy(images = draft.images + values)
            } catch (_: Exception) {
                message = "Image unavailable. Choose the image again."
            }
            busy = false
        }
    }

    fun savePhone() {
        if (busy) return
        val submitted = draft
        mutationRevision++
        busy = true
        message = ""
        viewModelScope.launch {
            try {
                val capture =
                    withContext(Dispatchers.IO) {
                        val raw =
                            PostCapture(
                                UUID.randomUUID().toString(),
                                submitted.title,
                                submitted.text,
                                submitted.sourceUrl,
                                submitted.images,
                                System.currentTimeMillis(),
                            )
                        CaptureCodec().decode(CaptureCodec().canonicalBytes(raw))
                    }
                val result = withContext(Dispatchers.IO) { store.save(capture) }
                if (result == SaveResult.ADDED || result == SaveResult.ALREADY_PRESENT) {
                    // The replacement is durable before the old draft is removed.
                    if (submitted.replacesId != null)
                        withContext(Dispatchers.IO) { store.delete(submitted.replacesId) }
                    items = withContext(Dispatchers.IO) { store.list() }
                    mutationRevision++
                    selectedPhoneId = capture.id
                    phoneScreen = "saved"
                } else message = saveFailure(result)
            } catch (_: IllegalArgumentException) {
                message =
                    "Check the text, source link, and images. Use up to 160 title characters, 20,000 text characters, four images, and 4 MiB per capture."
            } catch (_: Exception) {
                message = "Could not save on phone. The previous draft is kept."
            }
            busy = false
        }
    }

    fun openPhone(id: String) {
        selectedPhoneId = id
        phoneScreen = "saved"
        message = ""
    }

    fun editPhone() {
        selectedPhone?.capture?.let {
            draft = CaptureDraft(it.title, it.text, it.sourceUrl, it.imagesJpegBase64, it.id)
            phoneScreen = "editor"
            message = ""
        }
    }

    fun phoneLibrary() {
        phoneScreen = "library"
        message = ""
    }

    fun connect() {
        beforeConnect = phoneScreen
        phoneScreen = "connect"
        message = ""
    }

    fun closeConnect() {
        phoneScreen = beforeConnect
    }

    fun deletePhone() {
        val id = selectedPhoneId ?: return
        operation { store.delete(id) }
        phoneScreen = "library"
    }

    fun pair(value: String) {
        if (busy) return
        val link =
            try {
                PairingLink.parse(value.trim())
            } catch (_: Exception) {
                message = "Invalid pairing link."
                return
            }
        busy = true
        pairRevision++
        message = ""
        viewModelScope.launch {
            try {
                val paired =
                    withContext(Dispatchers.IO) {
                        val result = PostboxClient().pair(link)
                        val temp = File(app.filesDir, "paired-tv.tmp")
                        try {
                            java.io.FileOutputStream(temp).use {
                                it.write(json.encodeToString(result).toByteArray())
                                it.flush()
                                it.fd.sync()
                            }
                            java.nio.file.Files.move(
                                temp.toPath(),
                                pairedFile.toPath(),
                                java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                            )
                        } finally {
                            temp.delete()
                        }
                        result
                    }
                pairedTv = paired
                pairRevision++
                phoneScreen = beforeConnect
                message = "Connected to Postbox TV. Open Add posts on TV before sending."
                pairingInput = ""
                showPairingInput = false
            } catch (_: Exception) {
                message =
                    "TV connection unavailable. Keep this draft and retry while Add posts is open on TV."
            }
            busy = false
        }
    }

    fun sendPhone() {
        val capture = selectedPhone?.capture ?: return
        val tv =
            pairedTv
                ?: run {
                    connect()
                    return
                }
        if (busy) return
        busy = true
        message = ""
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { PostboxClient().send(tv, capture) }
                if (result == SaveResult.ADDED || result == SaveResult.ALREADY_PRESENT) {
                    withContext(Dispatchers.IO) { store.markSent(capture.id) }
                    items = withContext(Dispatchers.IO) { store.list() }
                    mutationRevision++
                    message = "Received on TV. Your phone copy is kept."
                } else message = saveFailure(result)
            } catch (_: Exception) {
                message = "TV unavailable. Your draft stays saved. Open Add posts on TV and retry."
            }
            busy = false
        }
    }

    private fun saveFailure(result: SaveResult) =
        when (result) {
            SaveResult.FULL -> "Library full. Delete captures to make room. Your draft is kept."
            SaveResult.CONFLICT ->
                "This capture conflicts with a saved item. Edit and save a new copy."
            else -> "Capture unavailable. Your draft is kept."
        }

    fun permissionDenied() {
        message = "Local network access is required. Allow it in Android app settings, then retry."
        if (!phone) {
            session = null
            syncReceiver()
        }
    }

    fun setResumed(value: Boolean, allowed: Boolean) {
        resumed = value
        lanAllowed = allowed
        if (value) {
            resumeGeneration++
            refresh()
        }
        if (!phone) syncReceiver()
    }

    fun permissionChanged(allowed: Boolean) {
        lanAllowed = allowed
        if (!allowed) permissionDenied()
        if (!phone) syncReceiver()
    }

    fun enterAdd() {
        tvScreen = "add"
        message = ""
        syncReceiver()
    }

    fun retryReceiver() {
        message = ""
        syncReceiver()
    }

    fun library() {
        tvScreen = "library"
        example = false
        syncReceiver()
    }

    fun setArchive(value: Boolean) {
        archived = value
        selectedTvId = null
    }

    fun selectTv(id: String) {
        selectedTvId = id
    }

    fun readTv(id: String) {
        selectedTvId = id
        example = false
        tvScreen = "reader"
    }

    fun readExample() {
        example = true
        tvScreen = "reader"
    }

    fun archiveTv() {
        val item = selectedTv ?: return
        operation { store.archive(item.capture.id, !item.archived) }
        library()
    }

    fun deleteTv() {
        val id = selectedTvId ?: return
        operation { store.delete(id) }
        selectedTvId = null
        library()
    }

    fun forgetPhone() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { receiverMutex.withLock { receiver?.forgetPhone() } }
                message = "Phone forgotten."
            } catch (_: Exception) {
                message = "Could not forget the phone. Retry."
            }
        }
    }

    private fun operation(block: () -> Unit) {
        mutationRevision++
        viewModelScope.launch {
            try {
                items =
                    withContext(Dispatchers.IO) {
                        block()
                        store.list()
                    }
                mutationRevision++
            } catch (_: Exception) {
                message = "Local storage unavailable. Existing captures are kept."
            }
        }
    }

    private fun syncReceiver() {
        val generation = ++receiverGeneration
        session = null
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                receiverMutex.withLock {
                    receiver?.close()
                    receiver = null
                    if (
                        generation != receiverGeneration ||
                            !resumed ||
                            tvScreen != "add" ||
                            !lanAllowed
                    )
                        return@withLock
                    try {
                        val created = CaptureReceiver(app, store) { refresh() }
                        receiver = created
                        val opened = created.start()
                        withContext(Dispatchers.Main) {
                            if (generation == receiverGeneration && resumed && tvScreen == "add") {
                                session = opened
                                receiverDeadlineElapsedMs =
                                    android.os.SystemClock.elapsedRealtime() + 300_000L
                                remainingSeconds = 300
                            }
                        }
                    } catch (_: Exception) {
                        receiver?.close()
                        receiver = null
                        withContext(Dispatchers.Main) {
                            if (generation == receiverGeneration)
                                message = "Receiver unavailable. Check the local network and retry."
                        }
                    }
                }
            }
            while (
                generation == receiverGeneration && resumed && tvScreen == "add" && session != null
            ) {
                delay(1000)
                if (generation != receiverGeneration || !resumed || tvScreen != "add") break
                val active = receiver?.isReceiving == true
                remainingSeconds =
                    ((receiverDeadlineElapsedMs - android.os.SystemClock.elapsedRealtime() + 999L) /
                            1000L)
                        .toInt()
                        .coerceAtLeast(0)
                if (!active || remainingSeconds == 0) {
                    session = null
                    message = "Receive window closed. Select Start receiving to open a new window."
                    withContext(Dispatchers.IO) {
                        receiverMutex.withLock {
                            receiver?.close()
                            receiver = null
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        val closing = receiver
        Thread({ closing?.close() }, "Postbox-close").start()
        super.onCleared()
    }

    companion object {
        fun factory(application: Application, phone: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PostboxViewModel(application, phone) as T
            }
    }
}

internal fun captureType(capture: PostCapture): String =
    when {
        capture.text.isBlank() &&
            capture.imagesJpegBase64.isEmpty() &&
            capture.sourceUrl.isNotBlank() -> "Link only"
        capture.imagesJpegBase64.isNotEmpty() && capture.text.isNotBlank() -> "Text and image"
        capture.imagesJpegBase64.isNotEmpty() -> "Image"
        else -> "Saved text"
    }

internal fun captureSource(capture: PostCapture): String =
    when {
        capture.sourceUrl.contains("instagram.com") -> "Instagram"
        capture.sourceUrl.contains("x.com") -> "X"
        else -> "Phone capture"
    }

internal fun captureTitle(capture: PostCapture): String =
    capture.title.ifBlank {
        capture.text.lineSequence().firstOrNull { it.isNotBlank() }?.take(80)
            ?: if (capture.sourceUrl.isNotBlank()) "Post details" else "Captured image"
    }
