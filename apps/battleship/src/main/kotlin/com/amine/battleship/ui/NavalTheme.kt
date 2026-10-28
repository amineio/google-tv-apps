package com.amine.battleship.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import com.amine.battleship.R

internal val Ocean = Color(0xFF071421)
internal val DeepWater = Color(0xFF102A3A)
internal val Cyan = Color(0xFF72E6F5)
internal val Ink = Color(0xFFF2F7FA)
internal val Muted = Color(0xFFB0C5D0)
internal val Impact = Color(0xFFFF7569)
internal val Steel = Color(0xFF91A8B6)

@OptIn(ExperimentalTextApi::class)
internal val Outfit = FontFamily(
    *listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(R.font.outfit, weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
    }.toTypedArray()
)

@Composable
internal fun NavalTheme(content: @Composable () -> Unit) {
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
        colorScheme = darkColorScheme(primary = Cyan, surface = DeepWater, onSurface = Ink,
            background = Ocean, onBackground = Ink),
        typography = typography,
    ) {
        CompositionLocalProvider(LocalContentColor provides Ink, LocalTextStyle provides typography.bodyLarge,
            content = content)
    }
}

@Composable
internal fun NavalButton(text: String, modifier: Modifier = Modifier, primary: Boolean = false,
    onClick: () -> Unit) {
    val shape = RoundedCornerShape(7.dp)
    Button(onClick = onClick, modifier = modifier.height(40.dp),
        shape = ButtonDefaults.shape(shape),
        scale = ButtonDefaults.scale(focusedScale = 1.035f),
        colors = ButtonDefaults.colors(
            containerColor = if (primary) Cyan.copy(alpha = .13f) else Color.White.copy(alpha = .055f),
            contentColor = Ink,
            focusedContainerColor = Cyan,
            focusedContentColor = Ocean),
        border = ButtonDefaults.border(
            border = Border(BorderStroke(1.dp, if (primary) Cyan.copy(alpha = .55f) else Color.White.copy(alpha = .14f)), shape = shape),
            focusedBorder = Border(BorderStroke(2.dp, Ink), shape = shape)),
        contentPadding = PaddingValues(horizontal = 19.dp, vertical = 7.dp),
    ) { Text(text, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}
