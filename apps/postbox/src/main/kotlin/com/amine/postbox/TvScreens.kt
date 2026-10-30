package com.amine.postbox

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import com.amine.postbox.core.PostCapture
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.text.DateFormat
import java.util.Base64
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun captureBitmap(encoded: String): ImageBitmap? {
    val bitmap by
        produceState<ImageBitmap?>(null, encoded) {
            value =
                withContext(Dispatchers.IO) {
                    runCatching {
                            val bytes = Base64.getDecoder().decode(encoded)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        }
                        .getOrNull()
                }
        }
    return bitmap
}

@Composable
private fun qrBitmap(value: String): ImageBitmap? {
    val bitmap by
        produceState<ImageBitmap?>(null, value) {
            this.value =
                withContext(Dispatchers.IO) {
                    runCatching {
                            val matrix =
                                MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, 512, 512)
                            val pixels =
                                IntArray(512 * 512) { i ->
                                    if (matrix[i % 512, i / 512]) android.graphics.Color.BLACK
                                    else android.graphics.Color.WHITE
                                }
                            Bitmap.createBitmap(pixels, 512, 512, Bitmap.Config.ARGB_8888)
                                .asImageBitmap()
                        }
                        .getOrNull()
                }
        }
    return bitmap
}

@Composable
internal fun TvScreens(model: PostboxViewModel, onAddPosts: () -> Unit) {
    val savedListState = rememberLazyListState()
    val archiveListState = rememberLazyListState()
    BackHandler(model.tvScreen != "library") { model.library() }
    BoxWithConstraints(Modifier.fillMaxSize().background(WarmInk)) {
        val horizontal = maxWidth * .06f
        val vertical = maxHeight * .06f
        Column(
            Modifier.fillMaxSize()
                .padding(horizontal = horizontal, vertical = vertical)
                .testTag("tv-safe-frame"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (model.tvScreen) {
                "reader" -> TvReader(model)
                "add" -> ReceiveScreen(model, onAddPosts)
                else ->
                    TvLibrary(
                        model,
                        onAddPosts,
                        if (model.archived) archiveListState else savedListState,
                    )
            }
        }
    }
}

@Composable
private fun ColumnScope.TvLibrary(
    model: PostboxViewModel,
    onAddPosts: () -> Unit,
    listState: LazyListState,
) {
    val savedTab = remember { FocusRequester() }
    val archiveTab = remember { FocusRequester() }
    val add = remember { FocusRequester() }
    val emptyAdd = remember { FocusRequester() }
    val example = remember { FocusRequester() }
    val list = model.tvItems
    val requesters =
        remember(list.map { it.capture.id }) {
            list.associate { it.capture.id to FocusRequester() }
        }
    val landing = list.firstOrNull { it.capture.id == model.selectedTvId } ?: list.firstOrNull()
    val initial = landing?.let { requesters.getValue(it.capture.id) } ?: emptyAdd
    var pendingFocusId by remember { mutableStateOf<String?>(null) }
    val preview = list.firstOrNull { it.capture.id == model.selectedTvId } ?: list.firstOrNull()
    LaunchedEffect(
        model.loaded,
        list.map { it.capture.id },
        model.archived,
        model.resumeGeneration,
    ) {
        if (model.loaded) {
            val index = list.indexOfFirst { it.capture.id == model.selectedTvId }.coerceAtLeast(0)
            if (list.isEmpty()) {
                emptyAdd.requestFocus()
            } else {
                val id = list[index].capture.id
                pendingFocusId = id
                if (listState.layoutInfo.visibleItemsInfo.none { it.key == id }) {
                    listState.scrollToItem(index)
                }
            }
        }
    }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BookmarkMark()
        Text(
            "Postbox",
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = Cream,
            modifier = Modifier.weight(1f),
        )
        TvAction(
            "Saved",
            Modifier.testTag("tab-saved").focusRequester(savedTab).focusProperties {
                down = initial
            },
            selected = !model.archived,
        ) {
            model.setArchive(false)
        }
        TvAction(
            "Archive",
            Modifier.testTag("tab-archive").focusRequester(archiveTab).focusProperties {
                down = initial
            },
            selected = model.archived,
        ) {
            model.setArchive(true)
        }
        TvAction(
            "＋ Add posts",
            Modifier.focusRequester(add).testTag("header-add").focusProperties { down = initial },
            onClick = onAddPosts,
        )
    }
    if (model.message.isNotBlank())
        Text(model.message, color = Apricot, fontSize = 17.sp, maxLines = 2)
    Text(
        "${if (model.archived) "Archive" else "Saved"} · ${list.size}",
        color = Cream,
        fontSize = 22.sp,
    )
    if (list.isEmpty()) {
        Column(
            Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (model.archived) "No archived posts" else "Your saved posts, on the big screen",
                color = Cream,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (model.archived) "Archive a saved capture to keep it here."
                else "Share a post or screenshot from your Android phone.",
                color = Muted,
                fontSize = 21.sp,
            )
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                TvAction(
                    "＋ Add posts",
                    Modifier.testTag("empty-add").focusRequester(emptyAdd).focusProperties {
                        up = savedTab
                        right = example
                    },
                    onClick = onAddPosts,
                )
                TvAction(
                    "Try an example",
                    Modifier.testTag("example").focusRequester(example).focusProperties {
                        left = emptyAdd
                        up = archiveTab
                    },
                ) {
                    model.readExample()
                }
            }
        }
    } else {
        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            LazyColumn(
                Modifier.weight(.38f).fillMaxHeight(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(list, key = { it.capture.id }) { item ->
                    // This effect exists only after the lazy card is composed and its node
                    // attached.
                    LaunchedEffect(pendingFocusId) {
                        if (pendingFocusId == item.capture.id) {
                            requesters.getValue(item.capture.id).requestFocus()
                            pendingFocusId = null
                        }
                    }
                    TvAction(
                        "${captureTitle(item.capture)}\n${captureSource(item.capture)} · ${captureType(item.capture)}",
                        Modifier.fillMaxWidth()
                            .heightIn(min = 92.dp)
                            .testTag("capture-${item.capture.id}")
                            .focusRequester(requesters.getValue(item.capture.id))
                            .focusProperties {
                                up = if (item == list.first()) savedTab else FocusRequester.Default
                            }
                            .onFocusChanged { if (it.isFocused) model.selectTv(item.capture.id) },
                        selected = preview?.capture?.id == item.capture.id,
                    ) {
                        model.readTv(item.capture.id)
                    }
                }
            }
            preview?.capture?.let { capture ->
                Column(
                    Modifier.weight(.62f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "${captureSource(capture)} · ${captureType(capture)}",
                        color = Muted,
                        fontSize = 17.sp,
                    )
                    Text(
                        captureTitle(capture),
                        color = Cream,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                    )
                    if (captureType(capture) == "Link only") {
                        Text(
                            "Add text or a screenshot on your phone",
                            color = Cream,
                            fontSize = 22.sp,
                        )
                        Text(
                            "Only the link was shared. The post content is not saved yet.",
                            color = Muted,
                            fontSize = 18.sp,
                        )
                    } else if (capture.text.isNotBlank())
                        Text(
                            capture.text,
                            color = Cream,
                            fontSize = 22.sp,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                        )
                    else
                        capture.imagesJpegBase64.firstOrNull()?.let { encoded ->
                            captureBitmap(encoded)?.let {
                                Image(
                                    it,
                                    "Captured image",
                                    Modifier.fillMaxWidth().height(155.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }
                    Spacer(Modifier.weight(1f))
                    TvAction(
                        if (captureType(capture) == "Link only") "Post details" else "Read post",
                        Modifier.testTag("preview-read"),
                    ) {
                        model.readTv(capture.id)
                    }
                }
            }
        }
    }
    val okHint =
        when {
            list.isEmpty() -> "Select"
            preview?.capture?.let(::captureType) == "Link only" -> "Post details"
            else -> "Read"
        }
    Text(
        "↑ ↓ Choose   OK $okHint   Back Exit",
        color = Muted,
        fontSize = 15.sp,
        modifier = Modifier.widthIn(max = 540.dp),
    )
}

private val ExampleCapture =
    PostCapture(
        "00000000-0000-4000-8000-000000000000",
        "Small habits, lasting change",
        "A useful habit starts small. Choose one thing you can repeat, make room for it, and return to it tomorrow.\n\nWhen progress feels slow, look for a simpler next step. Give it your full attention. Keep the part that helps and leave the rest.",
        "",
        emptyList(),
        0,
    )

@Composable
private fun ColumnScope.TvReader(model: PostboxViewModel) {
    val capture = if (model.example) ExampleCapture else model.selectedTv?.capture
    if (capture == null) {
        LaunchedEffect(Unit) { model.library() }
        return
    }
    val body = remember(capture.id) { FocusRequester() }
    val archive = remember(capture.id) { FocusRequester() }
    val image = remember(capture.id) { FocusRequester() }
    val original = remember(capture.id) { FocusRequester() }
    val delete = remember(capture.id) { FocusRequester() }
    val back = remember(capture.id) { FocusRequester() }
    var action by remember(capture.id) { mutableStateOf(if (model.example) back else archive) }
    var enlarged by rememberSaveable(capture.id) { mutableStateOf(false) }
    var sourceQr by rememberSaveable(capture.id) { mutableStateOf(false) }
    var deleting by rememberSaveable(capture.id) { mutableStateOf(false) }
    var returnToImage by remember(capture.id) { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(enlarged, model.resumeGeneration) {
        if (!enlarged) (if (returnToImage) image else action).requestFocus()
    }
    BackHandler {
        if (enlarged) {
            enlarged = false
            returnToImage = true
        } else model.library()
    }
    if (enlarged) {
        ImageReader(capture) {
            enlarged = false
            returnToImage = true
        }
        return
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BookmarkMark()
        Spacer(Modifier.width(12.dp))
        Text("Postbox", color = Cream, fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(28.dp))
        Text(
            if (model.example) "Example · Not in your library"
            else "Back to ${if (model.archived) "Archive" else "Saved"}",
            color = Muted,
            fontSize = 18.sp,
        )
    }
    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
        var bodyFocused by remember { mutableStateOf(false) }
        Column(
            Modifier.weight(1f)
                .fillMaxHeight()
                .testTag("reader-body")
                .focusRequester(body)
                .onFocusChanged { bodyFocused = it.isFocused }
                .border(
                    2.dp,
                    if (bodyFocused) Apricot else Color.Transparent,
                    RoundedCornerShape(10.dp),
                )
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) false
                    else
                        when (event.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                action.requestFocus()
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_UP -> {
                                scope.launch {
                                    scroll.animateScrollTo((scroll.value - 150).coerceAtLeast(0))
                                }
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                scope.launch {
                                    scroll.animateScrollTo(
                                        (scroll.value + 150).coerceAtMost(scroll.maxValue)
                                    )
                                }
                                true
                            }
                            else -> false
                        }
                }
                .focusable()
                .verticalScroll(scroll)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                "${captureSource(capture)} · ${captureType(capture)}",
                color = Muted,
                fontSize = 17.sp,
            )
            Text(
                if (captureType(capture) == "Link only") "Post details" else captureTitle(capture),
                color = Cream,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (!model.example)
                Text(
                    "Captured ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(capture.capturedAtEpochMs))}",
                    color = Muted,
                    fontSize = 16.sp,
                )
            if (captureType(capture) == "Link only") {
                if (capture.title.isNotBlank()) Text(capture.title, color = Cream, fontSize = 23.sp)
                Text(
                    "Only the link was shared. Add text or an image on your phone for TV reading.",
                    color = Cream,
                    fontSize = 22.sp,
                )
            } else if (capture.text.isNotBlank())
                Text(capture.text, color = Cream, fontSize = 22.sp, lineHeight = 32.sp)
            capture.imagesJpegBase64.firstOrNull()?.let { encoded ->
                captureBitmap(encoded)?.let {
                    Image(
                        it,
                        "Captured image",
                        Modifier.fillMaxWidth().height(210.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Text("Choose View image to enlarge.", color = Muted, fontSize = 17.sp)
            }
            Spacer(Modifier.height(16.dp))
        }
        Column(
            Modifier.width(220.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            fun actionModifier(tag: String, requester: FocusRequester) =
                Modifier.fillMaxWidth()
                    .testTag(tag)
                    .focusRequester(requester)
                    .focusProperties { left = body }
                    .onFocusChanged { if (it.isFocused) action = requester }
            if (!model.example) {
                TvAction(
                    if (model.selectedTv?.archived == true) "Restore" else "Archive",
                    actionModifier("reader-archive", archive),
                ) {
                    model.archiveTv()
                }
                if (capture.imagesJpegBase64.isNotEmpty())
                    TvAction("View image", actionModifier("reader-image", image)) {
                        returnToImage = true
                        enlarged = true
                    }
                if (capture.sourceUrl.isNotBlank())
                    TvAction("Original post", actionModifier("reader-original", original)) {
                        sourceQr = true
                    }
                TvAction("Delete", actionModifier("reader-delete", delete)) { deleting = true }
            }
            TvAction(
                "Back to ${if (model.archived) "Archive" else "Saved"}",
                actionModifier("reader-back", back),
            ) {
                model.library()
            }
            Spacer(Modifier.weight(1f))
        }
    }
    Text(
        "← Read   ↑ ↓ Scroll   → Actions   Back Library",
        color = Muted,
        fontSize = 15.sp,
        modifier = Modifier.widthIn(max = 590.dp),
    )
    if (deleting)
        DeleteDialog(
            onCancel = {
                deleting = false
                delete.requestFocus()
            },
            onDelete = {
                deleting = false
                model.deleteTv()
            },
        )
    if (sourceQr)
        QrDialog("Open original on your phone", capture.sourceUrl) {
            sourceQr = false
            original.requestFocus()
        }
}

@Composable
private fun ColumnScope.ImageReader(capture: PostCapture, onBack: () -> Unit) {
    var index by rememberSaveable(capture.id) { mutableIntStateOf(0) }
    val body = remember { FocusRequester() }
    val action = remember { FocusRequester() }
    var lastAction by remember { mutableStateOf(action) }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var focused by remember { mutableStateOf(false) }
    val bitmap = captureBitmap(capture.imagesJpegBase64[index])
    LaunchedEffect(Unit) { action.requestFocus() }
    LaunchedEffect(index) { scroll.scrollTo(0) }
    BackHandler(onBack = onBack)
    Row(verticalAlignment = Alignment.CenterVertically) {
        BookmarkMark()
        Spacer(Modifier.width(12.dp))
        Text(
            "Postbox    Image ${index + 1} of ${capture.imagesJpegBase64.size}",
            color = Cream,
            fontSize = 25.sp,
        )
    }
    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
        Box(
            Modifier.weight(1f)
                .fillMaxHeight()
                .testTag("image-body")
                .focusRequester(body)
                .onFocusChanged { focused = it.isFocused }
                .border(2.dp, if (focused) Apricot else Sage, RoundedCornerShape(10.dp))
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) false
                    else
                        when (event.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                lastAction.requestFocus()
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                scope.launch {
                                    scroll.animateScrollTo(
                                        (scroll.value + 160).coerceAtMost(scroll.maxValue)
                                    )
                                }
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_UP -> {
                                scope.launch {
                                    scroll.animateScrollTo((scroll.value - 160).coerceAtLeast(0))
                                }
                                true
                            }
                            else -> false
                        }
                }
                .focusable()
                .verticalScroll(scroll)
                .padding(10.dp)
        ) {
            bitmap?.let {
                Image(
                    it,
                    "Captured image ${index + 1}",
                    Modifier.fillMaxWidth()
                        .aspectRatio(it.width.toFloat() / it.height)
                        .testTag("enlarged-bitmap"),
                    contentScale = ContentScale.FillWidth,
                )
            }
        }
        Column(Modifier.width(220.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Image view", color = Cream, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            TvAction(
                "Scroll image",
                Modifier.fillMaxWidth()
                    .focusRequester(action)
                    .focusProperties { left = body }
                    .onFocusChanged { if (it.isFocused) lastAction = action },
            ) {
                body.requestFocus()
            }
            if (capture.imagesJpegBase64.size > 1)
                TvAction(
                    "Next image",
                    Modifier.fillMaxWidth().testTag("next-image").focusProperties { left = body },
                ) {
                    index = (index + 1) % capture.imagesJpegBase64.size
                }
            TvAction(
                "Back to post",
                Modifier.fillMaxWidth().focusProperties { left = body },
                onClick = onBack,
            )
            Text(
                "Left enters the image. Up and Down scroll. Right returns to actions.",
                color = Muted,
                fontSize = 18.sp,
            )
        }
    }
    Text("↑ ↓ Scroll image   → Actions   Back Post", color = Muted, fontSize = 15.sp)
}

@Composable
private fun ColumnScope.ReceiveScreen(model: PostboxViewModel, retry: () -> Unit) {
    val stop = remember { FocusRequester() }
    var showLink by remember { mutableStateOf(false) }
    LaunchedEffect(model.session, model.resumeGeneration) { stop.requestFocus() }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BookmarkMark()
        Spacer(Modifier.width(12.dp))
        Text("Postbox", color = Cream, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(28.dp))
        Text("Add posts", color = Muted, fontSize = 20.sp)
    }
    Row(
        Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.width(264.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val session = model.session
            if (session != null)
                qrBitmap(session.pairingLink)?.let {
                    Image(
                        it,
                        "Pairing QR code",
                        Modifier.size(250.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit,
                    )
                }
            else
                Box(
                    Modifier.size(250.dp).background(Sage, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Receiver closed", color = Cream, fontSize = 23.sp)
                }
            Text("Connect Postbox on your phone", color = Muted, fontSize = 17.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Send from your phone",
                color = Cream,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "1  Use the same local network.\n2  Open Postbox and choose Connect TV.\n3  Choose Photograph TV code.",
                color = Cream,
                fontSize = 20.sp,
                lineHeight = 30.sp,
            )
            Text(
                if (model.session != null)
                    "Ready to receive · ${model.remainingSeconds / 60}:${(model.remainingSeconds % 60).toString().padStart(2, '0')} remaining"
                else model.message.ifBlank { "Opening receiver…" },
                color = if (model.session != null) SoftSage else Apricot,
                fontSize = 18.sp,
                maxLines = 3,
            )
            TvAction(
                if (model.session != null) "Stop receiving" else "Start receiving",
                Modifier.fillMaxWidth().focusRequester(stop).testTag("receive-control"),
            ) {
                if (model.session != null) model.library() else retry()
            }
            if (model.session != null)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvAction("Show pairing link", Modifier.weight(1f)) { showLink = true }
                    TvAction("Forget phone", Modifier.weight(1f)) { model.forgetPhone() }
                }
        }
    }
    Text(
        "Keep this screen open while sending. Back closes the receiver.",
        color = Muted,
        fontSize = 15.sp,
        modifier = Modifier.widthIn(max = 600.dp),
    )
    if (showLink && model.session != null)
        PairingLinkDialog(model.session!!.pairingLink) { showLink = false }
}

@Composable
private fun DeleteDialog(onCancel: () -> Unit, onDelete: () -> Unit) {
    val cancel = remember { FocusRequester() }
    LaunchedEffect(Unit) { cancel.requestFocus() }
    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier.width(480.dp).background(Sage, RoundedCornerShape(18.dp)).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text("Delete this capture?", color = Cream, fontSize = 28.sp)
            Text(
                "This removes the TV copy. Your phone copy stays on the phone.",
                color = Cream,
                fontSize = 21.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                TvAction(
                    "Cancel",
                    Modifier.testTag("delete-cancel").focusRequester(cancel),
                    onClick = onCancel,
                )
                TvAction("Delete", Modifier.testTag("delete-confirm"), onClick = onDelete)
            }
        }
    }
}

@Composable
private fun QrDialog(title: String, value: String, onClose: () -> Unit) {
    val close = remember { FocusRequester() }
    LaunchedEffect(Unit) { close.requestFocus() }
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.width(450.dp).background(Sage, RoundedCornerShape(18.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, color = Cream, fontSize = 24.sp)
            qrBitmap(value)?.let { Image(it, "Original post QR code", Modifier.size(240.dp)) }
            Text(value, color = Cream, fontSize = 17.sp, maxLines = 3)
            TvAction("Back to post", Modifier.focusRequester(close), onClick = onClose)
        }
    }
}

@Composable
private fun PairingLinkDialog(value: String, onClose: () -> Unit) {
    val close = remember { FocusRequester() }
    LaunchedEffect(Unit) { close.requestFocus() }
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.width(590.dp).background(Sage, RoundedCornerShape(18.dp)).padding(26.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("Full pairing link", color = Cream, fontSize = 27.sp)
            Text(
                "On your phone, choose Paste pairing link. This link expires with this receive window.",
                color = Muted,
                fontSize = 19.sp,
            )
            Text(value, color = Cream, fontSize = 18.sp)
            TvAction("Close", Modifier.focusRequester(close), onClick = onClose)
        }
    }
}
