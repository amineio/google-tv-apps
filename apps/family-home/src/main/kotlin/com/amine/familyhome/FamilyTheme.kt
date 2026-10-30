package com.amine.familyhome

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
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
@OptIn(ExperimentalTextApi::class)
internal val Outfit = FontFamily(
    *listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(R.font.outfit, weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
    }.toTypedArray()
)

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
internal fun FamilyHeader(modifier: Modifier = Modifier, height: Dp = 38.dp, trailing: (@Composable RowScope.() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().height(height), verticalAlignment = Alignment.CenterVertically) {
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
internal fun ProfilePortrait(viewer: Viewer, size: Dp, modifier: Modifier = Modifier,
    displayName: String = viewer.label, portrait: Portrait = viewer.defaultPortrait) {
    val resource = when (portrait) {
        Portrait.AMINE -> R.drawable.avatar_amine
        Portrait.ELIAS -> R.drawable.avatar_elias
        Portrait.SOFIA -> R.drawable.avatar_sofia
        Portrait.HASNAA -> R.drawable.avatar_hasnaa
        Portrait.CHILD -> null
    }
    Box(modifier.size(size).clip(CircleShape), contentAlignment = Alignment.Center) {
        if (resource != null) Image(painterResource(resource), "$displayName profile portrait",
            Modifier.fillMaxSize().graphicsLayer { scaleX = 1.09f; scaleY = 1.09f }, contentScale = ContentScale.Crop)
        else {
            val initials = displayName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") {
                it.codePoints().findFirst().orElse(0).let { point -> String(Character.toChars(point)) }
            }.uppercase(java.util.Locale.ROOT).ifEmpty { "?" }
            Box(Modifier.fillMaxSize().background(Panel).semantics {
                contentDescription = "$displayName profile portrait"
            }, contentAlignment = Alignment.Center) {
                Text(initials, color = Gold, fontSize = (size.value * .34f).sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
internal fun TvAction(text: String, modifier: Modifier = Modifier, primary: Boolean = false,
    enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Button(onClick, modifier.height(42.dp), enabled = enabled,
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
    ) { Text(text, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}
