package com.amine.postbox

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun PhoneScreens(
    model: PostboxViewModel,
    onAddImage: () -> Unit,
    onPhotograph: () -> Unit,
    onPair: (String) -> Unit,
    onSend: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    BackHandler(model.phoneScreen != "library") {
        if (model.phoneScreen == "connect") model.closeConnect() else model.phoneLibrary()
    }
    val scroll = rememberScrollState()
    LaunchedEffect(model.phoneScreen) { scroll.scrollTo(0) }
    Column(
        Modifier.fillMaxSize()
            .background(WarmInk)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(scroll)
            .padding(horizontal = 24.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            BookmarkMark()
            Spacer(Modifier.width(12.dp))
            Text("Postbox", fontSize = 25.sp, fontWeight = FontWeight.SemiBold, color = Cream)
        }
        if (model.message.isNotBlank()) {
            Surface(color = Sage, shape = RoundedCornerShape(12.dp)) {
                Text(model.message, Modifier.padding(16.dp), color = Cream)
            }
        }
        if (model.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        when (model.phoneScreen) {
            "editor" -> {
                Text("Save a post", style = MaterialTheme.typography.headlineLarge)
                if (model.draft.linkOnly) {
                    Text(
                        "Link only · Only a link was shared",
                        color = Apricot,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text("Add text or image for TV reading. The post content has not been fetched.")
                }
                OutlinedTextField(
                    model.draft.title,
                    { model.updateDraft(title = it) },
                    enabled = !model.busy,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth().testTag("capture-title"),
                    supportingText = { Text("${model.draft.title.length}/160") },
                )
                OutlinedTextField(
                    model.draft.text,
                    { model.updateDraft(text = it) },
                    enabled = !model.busy,
                    label = { Text("Post text") },
                    placeholder = { Text("Paste the caption or post text") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("capture-text"),
                    supportingText = { Text("${model.draft.text.length}/20,000 · Plain text") },
                )
                OutlinedTextField(
                    model.draft.sourceUrl,
                    { model.updateDraft(source = it) },
                    enabled = !model.busy,
                    label = { Text("Source link (optional)") },
                    modifier = Modifier.fillMaxWidth().testTag("capture-source"),
                    supportingText = { Text("HTTPS links from X, Twitter, or Instagram") },
                )
                if (model.draft.images.isNotEmpty())
                    Text(
                        "${model.draft.images.size} ${if (model.draft.images.size == 1) "image" else "images"}"
                    )
                model.draft.images.forEachIndexed { index, encoded ->
                    val bitmap = captureBitmap(encoded)
                    bitmap?.let {
                        Image(
                            it,
                            "Captured image ${index + 1}",
                            Modifier.fillMaxWidth()
                                .heightIn(max = 260.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    }
                    OutlinedButton({ model.removeImage(index) }, enabled = !model.busy) {
                        Text("Remove image ${index + 1}")
                    }
                }
                OutlinedButton(
                    onAddImage,
                    Modifier.fillMaxWidth(),
                    enabled = !model.busy && model.draft.images.size < 4,
                ) {
                    Text("Add image")
                }
                Button(
                    model::savePhone,
                    Modifier.fillMaxWidth().testTag("save-phone"),
                    enabled = !model.busy,
                ) {
                    Text("Save on phone")
                }
                Text("Not sent to TV", color = Muted)
                TextButton(model::phoneLibrary) { Text("Back to library") }
            }
            "saved" -> {
                val item = model.selectedPhone
                Text(
                    if (item?.sent == true) "Received on TV" else "Saved on phone",
                    style = MaterialTheme.typography.headlineLarge,
                )
                if (item != null) {
                    PhoneCaptureCard(item.capture, item.sent)
                    if (captureType(item.capture) == "Link only")
                        Text("Add text or an image for TV reading.")
                    if (!item.sent) {
                        Button(onSend, Modifier.fillMaxWidth(), enabled = !model.busy) {
                            Text(if (model.pairedTv == null) "Connect TV" else "Send to TV")
                        }
                    }
                    OutlinedButton(
                        model::editPhone,
                        Modifier.fillMaxWidth(),
                        enabled = !model.busy,
                    ) {
                        Text("Edit capture")
                    }
                    if (model.pairedTv != null)
                        OutlinedButton(model::connect, Modifier.fillMaxWidth()) {
                            Text("Connect TV")
                        }
                    Text("Your phone copy is kept.", color = Muted)
                    OutlinedButton(
                        { confirmDelete = true },
                        Modifier.fillMaxWidth(),
                        enabled = !model.busy,
                    ) {
                        Text("Delete phone copy")
                    }
                }
                Button(model::newCapture, Modifier.fillMaxWidth()) { Text("Save another post") }
                TextButton(model::phoneLibrary) { Text("Back to library") }
            }
            "connect" -> {
                Text("Connect TV", style = MaterialTheme.typography.headlineLarge)
                Text("Open Add posts on your TV. Keep both devices on the same local network.")
                model.pairedTv?.let {
                    Text(
                        "Paired with ${it.name}. Pair again if its certificate or address changes.",
                        color = SoftSage,
                    )
                }
                Button(onPhotograph, Modifier.fillMaxWidth(), enabled = !model.busy) {
                    Text("Photograph TV code")
                }
                Text(
                    "Photograph the complete QR code. The photograph is deleted after reading.",
                    color = Muted,
                )
                OutlinedButton({ model.showPairingInput = true }, Modifier.fillMaxWidth()) {
                    Text("Paste pairing link")
                }
                if (model.showPairingInput) {
                    OutlinedTextField(
                        model.pairingInput,
                        { model.pairingInput = it },
                        label = { Text("Full pairing link") },
                        modifier = Modifier.fillMaxWidth().testTag("pairing-input"),
                        minLines = 3,
                    )
                    Button(
                        { onPair(model.pairingInput) },
                        Modifier.fillMaxWidth(),
                        enabled = !model.busy,
                    ) {
                        Text("Pair TV")
                    }
                }
                TextButton(model::closeConnect) { Text("Back") }
            }
            else -> {
                Text("Saved on phone", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Capture text, a source link, or screenshots. Save here, then choose Send to TV."
                )
                Button(model::newCapture, Modifier.fillMaxWidth(), enabled = !model.busy) {
                    Text("New capture")
                }
                OutlinedButton(model::connect, Modifier.fillMaxWidth()) { Text("Connect TV") }
                if (model.loaded && model.items.isEmpty())
                    Text(
                        "No captures yet. Share a post to Postbox or create a capture.",
                        color = Muted,
                    )
                model.items.forEach { item ->
                    Card(onClick = { model.openPhone(item.capture.id) }, Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                captureTitle(item.capture),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text("${captureSource(item.capture)} · ${captureType(item.capture)}")
                            Text(
                                if (item.sent) "Received on TV" else "Not sent to TV",
                                color = SoftSage,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    if (confirmDelete)
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete phone copy?") },
            text = { Text("The TV copy stays on the TV.") },
            confirmButton = {
                TextButton({
                    confirmDelete = false
                    model.deletePhone()
                }) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancel") } },
        )
}

@Composable
private fun PhoneCaptureCard(capture: com.amine.postbox.core.PostCapture, sent: Boolean) {
    Surface(color = Sage, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(captureTitle(capture), style = MaterialTheme.typography.headlineSmall)
            Text("${captureSource(capture)} · ${captureType(capture)}", color = Muted)
            if (capture.text.isNotBlank()) Text(capture.text, maxLines = 8)
            capture.imagesJpegBase64.firstOrNull()?.let { encoded ->
                captureBitmap(encoded)?.let {
                    Image(it, "Captured image", Modifier.fillMaxWidth().heightIn(max = 280.dp))
                }
            }
            if (capture.sourceUrl.isNotBlank()) Text(capture.sourceUrl, color = SoftSage)
            Text(
                if (sent) "Saved on Postbox TV" else "Your draft stays on this phone",
                color = Muted,
            )
        }
    }
}
