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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
    LaunchedEffect(model.event) { if (model.active && model.sound) audio.play(model.eventKind) }
    LaunchedEffect(model.active, model.sound) { if (!model.active || !model.sound) audio.stop() }
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
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xAA062E28), Color.Transparent, Color(0x59042824)))))
                if (model.screen == Screen.WELCOME) Welcome(model)
                else {
                    Playfield(model)
                    if (model.screen == Screen.PAUSE) Modal("Take a little break", "Your parade will wait.", listOf("Resume", "Restart", if (model.sound) "Sound On" else "Sound Off", "Exit"), model.menuIndex)
                    if (model.screen == Screen.RESULTS) Modal("A wonderful parade!", "${model.game.moves} moves  ·  Best ${model.best}", listOf("Play Again", "Exit"), model.menuIndex)
                }
            }
        }
    }
}

@Composable private fun Welcome(model: GameViewModel) {
    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xBB082C25), Color.Transparent))))
    Column(Modifier.offset(70.dp, 66.dp).width(450.dp)) {
        Title(54)
        Spacer(Modifier.height(18.dp))
        Label("A little memory. A lot of magic.", 23)
        Spacer(Modifier.height(16.dp))
        Label("Find six animal pairs.\nWatch your forest friends join the parade.", 21)
        Spacer(Modifier.height(30.dp))
        ActionPanel("Play", true, Modifier.width(260.dp).height(62.dp).testTag("menu_0"))
        Spacer(Modifier.height(18.dp))
        Label("D-pad to move  ·  OK to turn a card", 17, Cream.copy(alpha = .9f))
        Label("Back to pause  ·  Sound ${if (model.sound) "on" else "off"}", 17, Cream.copy(alpha = .9f))
    }
    Image(painterResource(R.drawable.fox), null, Modifier.offset(550.dp, 158.dp).size(210.dp, 270.dp))
    Image(painterResource(R.drawable.owl), null, Modifier.offset(737.dp, 220.dp).size(140.dp, 190.dp))
}

@Composable private fun Playfield(model: GameViewModel) {
    Column(Modifier.offset(48.dp, 26.dp)) {
        Title(38)
        Label("Find the pairs. Build your parade.", 18)
    }
    Row(Modifier.offset(653.dp, 43.dp).width(260.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Counter("Moves", "${model.game.moves}", Modifier.weight(1f))
        Counter("Pairs found", "${model.game.pairs} / 6", Modifier.weight(1f))
    }
    Column(Modifier.offset(48.dp, 120.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) { row -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(4) { column ->
                val index = row * 4 + column
                AnimalCard(model.game.cards[index], index in model.game.visible, index in model.game.matched,
                    model.game.isMatch && index in listOf(model.game.first, model.game.second), model.selected == index,
                    Modifier.size(134.dp, 115.dp).testTag("card_$index").semantics {
                        selected = model.selected == index
                        contentDescription = if (index in model.game.visible) model.game.cards[index].label else "Hidden card ${index + 1}"
                    })
            }
        } }
    }
    Parade(model.game, model.active && model.screen == Screen.GAME)
    if (model.game.isMatch && model.active && model.screen == Screen.GAME) MatchSparkles(model.game.second ?: 0)
    Row(Modifier.offset(48.dp, 497.dp).width(710.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Label(if (model.game.comparing) { if (model.game.isMatch) "A pair! A new friend joins." else "Remember these friends…" }
            else if (model.game.first != null) "Find this animal's friend." else "D-pad to move  ·  OK to turn a card", 17)
        Label("Back to pause", 17)
    }
}

@Composable private fun AnimalCard(animal: Animal, faceUp: Boolean, matched: Boolean, celebrating: Boolean, focused: Boolean, modifier: Modifier) {
    val rotation by animateFloatAsState(if (faceUp) 180f else 0f, tween(320, easing = FastOutSlowInEasing), label = "card flip")
    val pulse by animateFloatAsState(if (celebrating) 1.055f else 1f, spring(dampingRatio = .5f), label = "match pulse")
    val shape = RoundedCornerShape(13.dp)
    Box(modifier.graphicsLayer { scaleX = pulse; scaleY = pulse }
        .shadow(if (focused) 15.dp else 7.dp, shape, ambientColor = if (focused) Cyan else Color.Black, spotColor = if (focused) Cyan else Color.Black)
        .then(if (focused) Modifier.border(3.dp, Cyan, shape) else Modifier), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().padding(if (focused) 5.dp else 2.dp).graphicsLayer { rotationY = rotation; cameraDistance = 14 * density }
            .clip(shape).background(Brush.verticalGradient(if (rotation > 90f) listOf(Color(0xFF38694D), Color(0xFF123F36)) else listOf(Color(0xFF227A70), Color(0xFF063E3B))))
            .border(if (matched || celebrating) 3.dp else 2.dp, if (matched || celebrating) Gold else Color(0xFFC99B45), shape), contentAlignment = Alignment.Center) {
            if (rotation > 90f) {
                Column(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }.padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(animal.drawable), animal.label, Modifier.weight(1f).fillMaxWidth(), contentScale = ContentScale.Fit)
                    Label(animal.label, 13, Cream)
                }
            } else Image(painterResource(R.drawable.card_back), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        }
    }
}

@Composable internal fun Title(size: Int) {
    Row {
        Text("Memory ", color = Gold, fontSize = size.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif,
            style = TextStyle(shadow = Shadow(Color(0xFF3F230E), androidx.compose.ui.geometry.Offset(2f, 4f), 3f)))
        Text("Parade", color = Color(0xFF76DDF3), fontSize = size.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif,
            style = TextStyle(shadow = Shadow(Color(0xFF092B3D), androidx.compose.ui.geometry.Offset(2f, 4f), 3f)))
    }
}
@Composable internal fun Label(text: String, size: Int, color: Color = Cream, modifier: Modifier = Modifier) {
    Text(text, modifier, color = color, fontSize = size.sp, fontWeight = FontWeight.SemiBold,
        style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = .65f), androidx.compose.ui.geometry.Offset(0f, 2f), 3f)))
}
@Composable private fun Counter(label: String, value: String, modifier: Modifier) {
    Column(modifier.shadow(5.dp, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFFFBE0AA), Color(0xFFD9AF71))))
        .border(2.dp, Color(0xFF94622F), RoundedCornerShape(10.dp)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF3C2819), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color(0xFF402516), fontSize = 27.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black)
    }
}
@Composable private fun Modal(title: String, subtitle: String, actions: List<String>, selected: Int) {
    Box(Modifier.fillMaxSize().background(Color(0xC4051817)), contentAlignment = Alignment.Center) {
        Column(Modifier.width(480.dp).shadow(24.dp, RoundedCornerShape(24.dp)).background(Brush.verticalGradient(listOf(Color(0xFF285246), Deep)), RoundedCornerShape(24.dp))
            .border(2.dp, Gold, RoundedCornerShape(24.dp)).padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Gold, fontSize = 30.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp)); Label(subtitle, 20)
            Spacer(Modifier.height(20.dp))
            actions.forEachIndexed { index, action ->
                ActionPanel(action, selected == index, Modifier.fillMaxWidth().height(50.dp).testTag("menu_$index").semantics { this.selected = selected == index })
                if (index < actions.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }
    }
}
@Composable private fun ActionPanel(label: String, selected: Boolean, modifier: Modifier) {
    Box(modifier.shadow(if (selected) 12.dp else 3.dp, RoundedCornerShape(12.dp)).background(
        Brush.verticalGradient(if (selected) listOf(Color(0xFF3E8757), Color(0xFF165238)) else listOf(Color(0xFF344B40), Color(0xFF203C32))), RoundedCornerShape(12.dp))
        .border(if (selected) 3.dp else 1.dp, if (selected) Cyan else Color(0xFFB39862), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Label(label, 23, if (selected) Cream else Cream.copy(alpha = .75f))
    }
}
internal val Animal.drawable get() = when (this) { Animal.FOX -> R.drawable.fox; Animal.PANDA -> R.drawable.panda; Animal.RABBIT -> R.drawable.rabbit; Animal.OWL -> R.drawable.owl; Animal.DEER -> R.drawable.deer; Animal.BEAR -> R.drawable.bear }
