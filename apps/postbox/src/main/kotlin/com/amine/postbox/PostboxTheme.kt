package com.amine.postbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme

internal val WarmInk = Color(0xFF131817)
internal val Cream = Color(0xFFF4EFDF)
internal val Sage = Color(0xFF303B32)
internal val SoftSage = Color(0xFFAFBBA1)
internal val Apricot = Color(0xFFF6B77E)
internal val Muted = Color(0xFFB9C0B6)

@Composable
internal fun PostboxTheme(phone: Boolean = false, content: @Composable () -> Unit) {
    if (phone) {
        androidx.compose.material3.MaterialTheme(
            colorScheme =
                androidx.compose.material3.darkColorScheme(
                    primary = Apricot,
                    onPrimary = WarmInk,
                    secondary = SoftSage,
                    background = WarmInk,
                    onBackground = Cream,
                    surface = Sage,
                    onSurface = Cream,
                    surfaceVariant = Sage,
                    onSurfaceVariant = Muted,
                ),
            content = content,
        )
    } else {
        MaterialTheme(
            colorScheme =
                darkColorScheme(
                    primary = Apricot,
                    background = WarmInk,
                    onBackground = Cream,
                    surface = Sage,
                    onSurface = Cream,
                ),
            content = content,
        )
    }
}

@Composable
internal fun TvAction(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = ButtonDefaults.shape(shape),
        scale = ButtonDefaults.scale(focusedScale = 1f),
        colors =
            ButtonDefaults.colors(
                containerColor = if (selected) Sage else Color(0xFF242C27),
                contentColor = Cream,
                focusedContainerColor = Sage,
                focusedContentColor = Cream,
            ),
        border =
            ButtonDefaults.border(
                border = Border(BorderStroke(2.dp, Color.Transparent), shape = shape),
                focusedBorder = Border(BorderStroke(2.dp, Apricot), shape = shape),
            ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            label,
            fontSize = 19.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun BookmarkMark() {
    androidx.compose.foundation.Canvas(Modifier.size(22.dp, 28.dp)) {
        val path =
            androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width * .1f, 0f)
                lineTo(size.width * .9f, 0f)
                lineTo(size.width * .9f, size.height)
                lineTo(size.width * .5f, size.height * .74f)
                lineTo(size.width * .1f, size.height)
                close()
            }
        drawPath(path, Cream)
    }
}
