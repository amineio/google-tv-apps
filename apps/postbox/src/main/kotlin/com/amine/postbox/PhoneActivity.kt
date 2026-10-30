package com.amine.postbox

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhoneActivity : ComponentActivity() {
    private lateinit var model: PostboxViewModel
    private var pendingPhoto: String? = null
    private var pendingNetworkAction: (() -> Unit)? = null
    private val networkPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            model.permissionChanged(granted)
            val action = pendingNetworkAction
            pendingNetworkAction = null
            if (granted) action?.invoke()
        }
    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(4)) { uris ->
            model.addImages(uris)
        }
    private val photograph =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val path = pendingPhoto
            pendingPhoto = null
            lifecycleScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        val file = path?.let(::File)
                        try {
                            if (success && file != null) PairingPhotoReader.read(file) else null
                        } catch (_: Exception) {
                            ""
                        } finally {
                            file?.delete()
                        }
                    }
                when (result) {
                    null -> Unit // Cancellation leaves the phone capture unchanged.
                    "" ->
                        model.showMessage(
                            "Could not read the TV code. Photograph the complete code again."
                        )
                    else -> withLocalNetwork { model.pair(result) }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingPhoto = savedInstanceState?.getString("pairing-photo")
        model =
            ViewModelProvider(this, PostboxViewModel.factory(application, true))[
                PostboxViewModel::class.java]
        if (savedInstanceState == null) model.receiveIntent(intent)
        setContent {
            PostboxTheme(phone = true) {
                PhoneScreens(
                    model,
                    onAddImage = {
                        imagePicker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    onPhotograph = ::takePhotograph,
                    onPair = { value -> withLocalNetwork { model.pair(value) } },
                    onSend = { withLocalNetwork { model.sendPhone() } },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        model.receiveIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingPhoto?.let { outState.putString("pairing-photo", it) }
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        model.setResumed(true, hasLocalNetworkPermission())
    }

    override fun onPause() {
        model.setResumed(false, hasLocalNetworkPermission())
        super.onPause()
    }

    private fun withLocalNetwork(action: () -> Unit) {
        if (hasLocalNetworkPermission()) action()
        else {
            pendingNetworkAction = action
            networkPermission.launch(LOCAL_NETWORK_PERMISSION)
        }
    }

    private fun takePhotograph() {
        lifecycleScope.launch {
            var file: File? = null
            try {
                val created =
                    withContext(Dispatchers.IO) {
                        val directory = File(cacheDir, "pairing-photos")
                        check(directory.isDirectory || directory.mkdirs())
                        File.createTempFile("tv-code-", ".jpg", directory)
                    }
                file = created
                pendingPhoto = created.path
                val uri =
                    FileProvider.getUriForFile(
                        this@PhoneActivity,
                        "$packageName.pairing-files",
                        created,
                    )
                photograph.launch(uri)
            } catch (_: Exception) {
                pendingPhoto = null
                withContext(Dispatchers.IO) { file?.delete() }
                model.showMessage("Camera unavailable. Use Paste pairing link instead.")
            }
        }
    }
}
