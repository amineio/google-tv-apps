package com.amine.memoryparade

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

internal val Gold = Color(0xFFFFD378)
internal val Cream = Color(0xFFFFF1D1)
internal val Cyan = Color(0xFFABFFFF)
internal val Deep = Color(0xFF102C28)
private val PanelShape = RoundedCornerShape(14.dp)

@Composable fun MemoryApp(model: GameViewModel, exit: () -> Unit) {
    val focus = remember { FocusRequester() }
    val context = LocalContext.current
    val audio = remember { GameAudio(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(audio, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) audio.stop()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); audio.release() }
    }
    LaunchedEffect(model.event) { model.consumeAudioEvent()?.let(audio::play) }
    LaunchedEffect(model.active, model.sound, model.screen) {
        if (!model.active || !model.sound || model.screen == Screen.PAUSE) audio.stop()
    }
    BackHandler { model.back(exit) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    MaterialTheme {
        BoxWithConstraints(Modifier.fillMaxSize().background(Deep).testTag("game_root")
            .focusRequester(focus).onPreviewKeyEvent { event ->
                val direction = when (event.key) { Key.DirectionUp -> Direction.UP; Key.DirectionDown -> Direction.DOWN; Key.DirectionLeft -> Direction.LEFT; Key.DirectionRight -> Direction.RIGHT; else -> null }
                when {
                    direction != null -> { if (event.type == KeyEventType.KeyDown) model.direction(direction); true }
                    event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter -> {
                        if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0) model.select(exit)
                        true
                    }
                    else -> false
                }
            }.focusable(), contentAlignment = Alignment.Center) {
            val scale = minOf(maxWidth.value / 960f, maxHeight.value / 540f)
            Box(Modifier.requiredSize(960.dp, 540.dp).graphicsLayer { scaleX = scale; scaleY = scale }) {
                Image(painterResource(R.drawable.forest), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x79062E28), Color.Transparent, Color(0x4D042824)))))
                if (model.screen == Screen.WELCOME) Welcome(model) else {
                    Playfield(model)
                    if (model.screen == Screen.PAUSE) Modal("Paused", null,
                        listOf("Resume", "Restart", if (model.sound) "Sound On" else "Sound Off", "Choose board", "Exit"), model.menuIndex)
                    if (model.screen == Screen.RESULTS) Modal("Parade complete", "${model.game.moves} moves  ·  Best ${model.best}",
                        listOf("Play Again", "Choose board", "Exit"), model.menuIndex)
                }
            }
        }
    }
}

@Composable private fun Welcome(model: GameViewModel) {
    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0x80072A24), Color.Transparent))))
    Box(Modifier.offset(48.dp, 45.dp).background(Deep.copy(alpha = .94f), PanelShape).padding(horizontal = 20.dp, vertical = 12.dp)) { Title(42) }
    Image(painterResource(R.drawable.fox), null, Modifier.offset(639.dp, 32.dp).size(107.dp, 125.dp))
    Image(painterResource(R.drawable.owl), null, Modifier.offset(760.dp, 40.dp).size(98.dp, 110.dp))
    Row(Modifier.offset(48.dp, 180.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        BoardSize.entries.forEachIndexed { index, size ->
            val selected = model.menuIndex == index
            Column(Modifier.size(272.dp, 265.dp).shadow(if (selected) 18.dp else 6.dp, PanelShape)
                .background(if (selected) Color(0xFF184E40) else Deep.copy(alpha = .96f), PanelShape)
                .border(if (selected) 4.dp else 1.dp, if (selected) Cyan else Gold.copy(alpha = .7f), PanelShape)
                .padding(20.dp).testTag("board_${size.key}").semantics { this.selected = selected; role = Role.Button; contentDescription = "${size.cards} cards" },
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${size.cards} cards", color = Cream, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                MiniBoard(size)
                val record = model.bestFor(size)
                if (record > 0) Text("Best $record", color = Cream, fontSize = 20.sp, modifier = Modifier.padding(top = 18.dp))
            }
        }
    }
    Hint("OK  Play  ·  Back  Exit")
}

@Composable private fun MiniBoard(size: BoardSize) {
    val cell = if (size == BoardSize.EASY) 38.dp else 27.dp
    Column(Modifier.height(124.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        repeat(size.rows) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(size.columns) { Box(Modifier.size(cell).background(Color(0xFF2B8172), RoundedCornerShape(5.dp)).border(1.dp, Gold, RoundedCornerShape(5.dp))) }
            }
            if (row < size.rows - 1) Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable private fun Playfield(model: GameViewModel) {
    Box(Modifier.offset(48.dp, 27.dp).background(Deep.copy(alpha = .96f), PanelShape).padding(horizontal = 16.dp, vertical = 8.dp)) { Title(32) }
    Row(Modifier.offset(653.dp, 27.dp).width(259.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Counter("Moves", "${model.game.moves}", Modifier.weight(1f))
        Counter("Pairs", "${model.game.pairs}/${model.game.size.pairs}", Modifier.weight(1f))
    }
    val layout = BoardLayout(model.game.size)
    model.game.cards.forEachIndexed { index, animal ->
        AnimalCard(animal, index in model.game.visible, index in model.game.matched,
            model.game.isMatch && index in listOf(model.game.first, model.game.second), model.selected == index,
            Modifier.offset(layout.cardX(index).dp, layout.cardY(index).dp).size(layout.cardWidth.dp, layout.cardHeight.dp)
                .testTag("card_$index").semantics {
                    selected = model.selected == index
                    contentDescription = if (index in model.game.visible) animal.label else "Hidden card ${index + 1}"
                })
    }
    Parade(model.game, model.active && model.screen == Screen.GAME)
    if (model.game.isMatch && model.active && model.screen == Screen.GAME) MatchSparkles(model.game)
    Hint("Back  Pause")
}

@Composable private fun AnimalCard(animal: Animal, faceUp: Boolean, matched: Boolean, celebrating: Boolean, focused: Boolean, modifier: Modifier) {
    val rotation by animateFloatAsState(if (faceUp) 180f else 0f, tween(320, easing = FastOutSlowInEasing), label = "card flip")
    val pulse by animateFloatAsState(if (celebrating) 1.045f else 1f, spring(dampingRatio = .5f), label = "match pulse")
    val shape = RoundedCornerShape(13.dp)
    Box(modifier.graphicsLayer { scaleX = pulse; scaleY = pulse }
        .shadow(if (focused) 15.dp else 7.dp, shape, ambientColor = if (focused) Cyan else Color.Black, spotColor = if (focused) Cyan else Color.Black)
        .then(if (focused) Modifier.border(3.dp, Cyan, shape) else Modifier), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().padding(5.dp).graphicsLayer { rotationY = rotation; cameraDistance = 14 * density }
            .clip(shape).background(Brush.verticalGradient(if (rotation > 90f) listOf(Color(0xFF38694D), Color(0xFF123F36)) else listOf(Color(0xFF227A70), Color(0xFF063E3B))))
            .border(if (matched || celebrating) 3.dp else 2.dp, if (matched || celebrating) Gold else Color(0xFFC99B45), shape), contentAlignment = Alignment.Center) {
            if (rotation > 90f) Image(painterResource(animal.drawable), animal.label,
                Modifier.fillMaxSize().padding(1.dp).graphicsLayer { rotationY = 180f }, contentScale = ContentScale.Fit)
            else Image(painterResource(R.drawable.card_back), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        }
    }
}

@Composable internal fun Title(size: Int) {
    Row {
        Text("Memory ", color = Gold, fontSize = size.sp, fontWeight = FontWeight.SemiBold)
        Text("Parade", color = Cyan, fontSize = size.sp, fontWeight = FontWeight.SemiBold)
    }
}
@Composable internal fun Hint(text: String) {
    Text(text, Modifier.offset(48.dp, 479.dp).background(Deep, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 5.dp),
        color = Cream, fontSize = 20.sp, fontWeight = FontWeight.Medium)
}
@Composable private fun Counter(label: String, value: String, modifier: Modifier) {
    Column(modifier.shadow(5.dp, PanelShape).background(Color(0xFFF7D99F), PanelShape)
        .border(2.dp, Color(0xFF94622F), PanelShape).padding(horizontal = 8.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF302014), fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Text(value, color = Color(0xFF302014), fontSize = 34.sp, fontWeight = FontWeight.Bold)
    }
}
@Composable private fun Modal(title: String, subtitle: String?, actions: List<String>, selected: Int) {
    Box(Modifier.fillMaxSize().background(Color(0xC4051817)), contentAlignment = Alignment.Center) {
        Column(Modifier.width(480.dp).shadow(24.dp, PanelShape).background(Deep, PanelShape)
            .border(2.dp, Gold, PanelShape).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Cream, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (subtitle != null) Text(subtitle, color = Cream, fontSize = 23.sp, modifier = Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(20.dp))
            actions.forEachIndexed { index, action ->
                ActionPanel(action, selected == index, Modifier.fillMaxWidth().height(48.dp).testTag("menu_$index").semantics { this.selected = selected == index })
                if (index < actions.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }
    }
}
@Composable private fun ActionPanel(label: String, selected: Boolean, modifier: Modifier) {
    Box(modifier.background(if (selected) Color(0xFF286A49) else Color(0xFF223F34), PanelShape)
        .border(if (selected) 3.dp else 1.dp, if (selected) Cyan else Gold.copy(alpha = .6f), PanelShape), contentAlignment = Alignment.Center) {
        Text(label, color = Cream, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    }
}
internal val Animal.drawable get() = when (this) {
    Animal.FOX -> R.drawable.fox; Animal.PANDA -> R.drawable.panda; Animal.RABBIT -> R.drawable.rabbit; Animal.OWL -> R.drawable.owl
    Animal.DEER -> R.drawable.deer; Animal.BEAR -> R.drawable.bear; Animal.SQUIRREL -> R.drawable.squirrel; Animal.RACCOON -> R.drawable.raccoon
}
