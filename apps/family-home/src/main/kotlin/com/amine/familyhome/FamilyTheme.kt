package com.amine.familyhome

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import kotlinx.coroutines.delay
import java.util.Date

internal val Gold = Color(0xFFF6CD83)
internal val Background = Color(0xFF0D0E12)
internal val Panel = Color(0xFF25252A)
internal val Muted = Color(0xFFB4B0AC)
internal val Ink = Color(0xFFF8F5F0)
internal val Outfit = FontFamily(Font(R.font.outfit))

@Composable
internal fun FamilyTheme(content: @Composable () -> Unit) {
    val base = Typography()
    val typography = remember {
        Typography(
            displayLarge = base.displayLarge.copy(fontFamily = Outfit),
            displayMedium = base.displayMedium.copy(fontFamily = Outfit),
            displaySmall = base.displaySmall.copy(fontFamily = Outfit),
            headlineLarge = base.headlineLarge.copy(fontFamily = Outfit),
            headlineMedium = base.headlineMedium.copy(fontFamily = Outfit),
            headlineSmall = base.headlineSmall.copy(fontFamily = Outfit),
            titleLarge = base.titleLarge.copy(fontFamily = Outfit),
            titleMedium = base.titleMedium.copy(fontFamily = Outfit),
            titleSmall = base.titleSmall.copy(fontFamily = Outfit),
            bodyLarge = base.bodyLarge.copy(fontFamily = Outfit),
            bodyMedium = base.bodyMedium.copy(fontFamily = Outfit),
            bodySmall = base.bodySmall.copy(fontFamily = Outfit),
            labelLarge = base.labelLarge.copy(fontFamily = Outfit),
            labelMedium = base.labelMedium.copy(fontFamily = Outfit),
            labelSmall = base.labelSmall.copy(fontFamily = Outfit),
        )
    }
    MaterialTheme(
        colorScheme = darkColorScheme(primary = Gold, surface = Panel, onSurface = Ink,
            background = Background, onBackground = Ink),
        typography = typography,
    ) {
        CompositionLocalProvider(LocalContentColor provides Ink, LocalTextStyle provides typography.bodyLarge, content = content)
    }
}

@Composable
internal fun FamilyBackdrop(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().drawWithCache {
        val warm = Brush.radialGradient(listOf(Color(0xFF5C4027).copy(alpha = .4f), Color.Transparent),
            center = Offset(size.width * .03f, size.height * .04f), radius = size.width * .54f)
        val lower = Brush.radialGradient(listOf(Color(0xFF725033).copy(alpha = .22f), Color.Transparent),
            center = Offset(size.width * .96f, size.height * .93f), radius = size.width * .58f)
        onDrawBehind { drawRect(Background); drawRect(warm); drawRect(lower) }
    }, content = content)
}

@Composable
internal fun FamilyHeader(modifier: Modifier = Modifier, trailing: (@Composable RowScope.() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().height(38.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("Family ") }
            withStyle(SpanStyle(color = Gold)) { append("Home") }
        }, fontSize = 22.sp)
        Spacer(Modifier.weight(1f))
        if (trailing != null) trailing() else FamilyClock()
    }
}

@Composable
private fun FamilyClock() {
    val context = LocalContext.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) { delay(30_000); value = System.currentTimeMillis() }
    }
    Text(DateFormat.getTimeFormat(context).format(Date(now)), color = Muted, fontSize = 16.sp)
}

@Composable
internal fun ProfilePortrait(viewer: Viewer, size: Dp, modifier: Modifier = Modifier) {
    val resource = when (viewer) {
        Viewer.AMINE -> R.drawable.avatar_amine
        Viewer.ELIAS -> R.drawable.avatar_elias
        Viewer.SOFIA -> R.drawable.avatar_sofia
    }
    Box(modifier.size(size).clip(CircleShape)) {
        Image(painterResource(resource), "${viewer.label} profile portrait",
            Modifier.fillMaxSize().graphicsLayer { scaleX = 1.09f; scaleY = 1.09f }, contentScale = ContentScale.Crop)
    }
}

@Composable
internal fun TvAction(text: String, modifier: Modifier = Modifier, primary: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Button(onClick, modifier.height(42.dp),
        shape = ButtonDefaults.shape(shape),
        scale = ButtonDefaults.scale(focusedScale = 1.035f),
        colors = ButtonDefaults.colors(
            containerColor = if (primary) Gold else Color.White.copy(alpha = .065f),
            contentColor = if (primary) Background else Ink,
            focusedContainerColor = if (primary) Ink else Gold,
            focusedContentColor = Background),
        border = ButtonDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = .12f)), shape = shape),
            focusedBorder = Border(BorderStroke(2.dp, Gold), shape = shape)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
    ) { Text(text, fontSize = 17.sp, maxLines = 1) }
}
