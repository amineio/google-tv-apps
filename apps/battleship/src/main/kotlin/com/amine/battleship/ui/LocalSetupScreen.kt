package com.amine.battleship.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import com.amine.battleship.R
import com.amine.battleship.game.model.*
import com.amine.battleship.localization.gameText
import com.amine.battleship.viewmodel.*

internal fun portraitResource(id: String): Int = when (id) {
    "owl" -> R.drawable.profile_owl; "bear" -> R.drawable.profile_bear; "cat" -> R.drawable.profile_cat
    "storm" -> R.drawable.profile_storm; "reed" -> R.drawable.profile_reed; "kai" -> R.drawable.profile_kai
    "mira" -> R.drawable.profile_mira; else -> R.drawable.profile_fox
}

@Composable
internal fun profileName(profile: PlayerProfile): String = profile.customName ?: gameText(when (profile.portraitId) {
    "owl" -> R.string.profile_owl; "bear" -> R.string.profile_bear; "cat" -> R.string.profile_cat
    "storm" -> R.string.profile_storm; "reed" -> R.string.profile_reed; "kai" -> R.string.profile_kai
    "mira" -> R.string.profile_mira; else -> R.string.profile_fox
})

@Composable
internal fun CaptainPortrait(profile: PlayerProfile, modifier: Modifier = Modifier) {
    Image(painterResource(portraitResource(profile.portraitId)), profileName(profile),
        modifier.clip(CircleShape), contentScale = ContentScale.Crop)
}

@Composable
internal fun ModeScreen(onAction: (GameAction) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { }; first.requestFocus() }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(gameText(R.string.choose_opponent), fontSize = 34.sp)
        Spacer(Modifier.height(24.dp))
        NavalButton(gameText(R.string.computer_mode), Modifier.width(300.dp).focusRequester(first), primary = true) { onAction(GameAction.Play) }
        Spacer(Modifier.height(12.dp))
        NavalButton(gameText(R.string.local_mode), Modifier.width(300.dp).testTag("local_mode")) { onAction(GameAction.LocalSetup) }
        Spacer(Modifier.height(20.dp))
        NavalButton(gameText(R.string.back)) { onAction(GameAction.Back) }
    }
}

@Composable
internal fun LocalSetupScreen(state: SessionState, onAction: (GameAction) -> Unit) {
    val draft = state.setup ?: return
    val profile = if (draft.slot == 0) draft.left else draft.right
    val other = if (draft.slot == 0) draft.right else draft.left
    val first = remember(draft.slot) { FocusRequester() }
    val nameFocus = remember { FocusRequester() }
    var editing by remember { mutableStateOf(false) }
    var nameText by remember { mutableStateOf("") }
    var restoreNameFocus by remember { mutableStateOf(false) }
    LaunchedEffect(draft.slot) { withFrameNanos { }; first.requestFocus() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(end = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(gameText(if (draft.slot == 0) R.string.choose_left else if (draft.slot == 1) R.string.choose_right else R.string.review_captains),
            fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        if (draft.slot < 2) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CaptainPortrait(profile, Modifier.size(45.dp)); Spacer(Modifier.width(12.dp))
                Text(profileName(profile), fontSize = 23.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(350.dp))
                Text(gameText(if (draft.slot == 0) R.string.left_owner else R.string.right_owner), color = Muted, fontSize = 15.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                PlayerProfile.portraitIds.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { id ->
                            val captain = PlayerProfile(id)
                            var focused by remember { mutableStateOf(false) }
                            val selected = profile.portraitId == id
                            val unavailable = draft.slot == 1 && other.portraitId == id
                            val label = profileName(captain)
                            val unavailableLabel = gameText(R.string.portrait_unavailable)
                            val firstAvailable = PlayerProfile.portraitIds.first { draft.slot == 0 || it != other.portraitId }
                            Column(Modifier.width(140.dp).height(111.dp)
                                .then(if (id == firstAvailable) Modifier.focusRequester(first) else Modifier)
                                .focusProperties {
                                    if (id in listOf("storm", "reed", "kai", "mira")) down = nameFocus
                                }
                                .onFocusChanged { focused = it.isFocused }
                                .background(if (focused) Cyan.copy(alpha = .24f) else DeepWater, RoundedCornerShape(8.dp))
                                .border(if (focused) 3.dp else 1.dp, if (focused) Ink else if (selected) Cyan else Steel, RoundedCornerShape(8.dp))
                                .semantics { contentDescription = label; this.selected = selected; if (unavailable) stateDescription = unavailableLabel }
                                .testTag("portrait_$id")
                                .clickable(enabled = !unavailable) { onAction(GameAction.SelectPortrait(id)) }
                                .padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                CaptainPortrait(captain, Modifier.size(64.dp))
                                Text(label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (selected) "✓ ${gameText(R.string.portrait_assigned)}" else if (unavailable) gameText(R.string.portrait_unavailable) else "",
                                    fontSize = 10.sp, color = Cyan, maxLines = 1)
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                NavalButton(gameText(R.string.edit_name), Modifier.focusRequester(nameFocus).testTag("edit_name")) {
                    nameText = profile.customName.orEmpty(); editing = true
                }
                NavalButton(gameText(if (draft.slot == 0) R.string.next_captain else R.string.review_captains), primary = true) { onAction(GameAction.NextParticipant) }
                NavalButton(gameText(R.string.local_cancel)) { onAction(GameAction.Cancel) }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                for (captain in listOf(draft.left, draft.right)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CaptainPortrait(captain, Modifier.size(100.dp))
                        Text(profileName(captain), fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(280.dp))
                    }
                }
            }
            Text(gameText(R.string.memory_rule), color = Muted, fontSize = 19.sp, modifier = Modifier.widthIn(max = 720.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                NavalButton(gameText(R.string.start_local), Modifier.focusRequester(first), primary = true) { onAction(GameAction.StartLocal) }
                NavalButton(gameText(R.string.local_cancel)) { onAction(GameAction.Cancel) }
            }
        }
    }
    if (editing) {
        fun close() { editing = false; restoreNameFocus = true }
        Dialog(onDismissRequest = { close() }) {
            Column(Modifier.width(570.dp).background(DeepWater, RoundedCornerShape(12.dp)).padding(25.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Text(gameText(R.string.edit_name_title, profileName(profile)), fontSize = 23.sp)
                val editorFocus = remember { FocusRequester() }
                LaunchedEffect(Unit) { withFrameNanos { }; editorFocus.requestFocus() }
                BasicTextField(value = nameText, onValueChange = { nameText = it }, singleLine = true,
                    textStyle = TextStyle(color = Ink, fontSize = 22.sp), cursorBrush = SolidColor(Cyan),
                    modifier = Modifier.fillMaxWidth().background(Ocean).padding(12.dp).focusRequester(editorFocus).testTag("name_editor"))
                val valid = runCatching { PlayerProfile.normalizeName(nameText) }.isSuccess
                Text(gameText(if (valid) R.string.name_limit else R.string.name_invalid), color = if (valid) Muted else Impact, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NavalButton(gameText(R.string.name_save)) { if (valid) { onAction(GameAction.SaveName(nameText)); close() } }
                    NavalButton(gameText(R.string.local_cancel)) { close() }
                }
            }
        }
    }
    LaunchedEffect(editing) { if (!editing && restoreNameFocus) { withFrameNanos { }; if (draft.slot < 2) nameFocus.requestFocus(); restoreNameFocus = false } }
}

/** Key-down owns Ready. The enclosing screen consumes release before enabling targeting. */
@Composable
internal fun LocalReadyScreen(state: SessionState, onAction: (GameAction) -> Unit) {
    val seat = state.readySeat
    val match = state.game
    val profile = if (seat == Player.COMPUTER) state.readyRightProfile ?: match?.rightProfile else state.readyLeftProfile ?: match?.leftProfile
    val first = remember(seat, state.saveWarning) { FocusRequester() }
    LaunchedEffect(seat, state.saveWarning) { if (seat != null || state.saveWarning != null) { withFrameNanos { }; first.requestFocus() } }
    Column(Modifier.fillMaxSize().testTag("local_ready_screen"), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        if (profile != null && seat != null) {
            CaptainPortrait(profile, Modifier.size(105.dp))
            Spacer(Modifier.height(13.dp))
            Text(gameText(R.string.captain_ready, profileName(profile)), fontSize = 30.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(gameText(R.string.pass_remote, profileName(profile)), color = Muted, fontSize = 18.sp)
        } else Text(gameText(R.string.local_saving), fontSize = 28.sp)
        if (state.previewInterrupted) Text(gameText(R.string.preview_interrupted), fontSize = 17.sp, color = Muted)
        Spacer(Modifier.height(20.dp))
        if (state.saveWarning != null) {
            Text(gameText(R.string.save_failed), color = Impact, fontSize = 17.sp)
            NavalButton(gameText(R.string.retry_save), Modifier.focusRequester(first), primary = true) { onAction(GameAction.RetrySave) }
            if (state.localStarting) NavalButton(gameText(R.string.local_cancel)) { onAction(GameAction.Cancel) }
        } else if (seat != null) NavalButton(gameText(R.string.local_ready), Modifier.focusRequester(first).testTag("captain_ready")
            .onPreviewKeyEvent { event ->
                if (event.key in listOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)) {
                    if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0) onAction(GameAction.Ready)
                    if (event.type == KeyEventType.KeyUp) onAction(GameAction.ReleaseConfirm)
                    true
                } else false
            }, primary = true) { onAction(GameAction.Ready); onAction(GameAction.ReleaseConfirm) }
    }
}
