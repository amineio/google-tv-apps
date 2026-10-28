package com.amine.familyhome

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import kotlin.math.min
import kotlin.math.roundToInt

data class LauncherApp(val pkg: String, val label: String, val component: ComponentName,
    val icon: Bitmap, val banner: Bitmap? = null) {
    val backdropColor: Int = icon.darkBackdrop()
}

internal fun loadLauncherCatalog(manager: PackageManager, ownPackage: String): List<LauncherApp> {
    val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
    return manager.queryIntentActivities(query, PackageManager.ResolveInfoFlags.of(0)).mapNotNull { info ->
        val pkg = info.activityInfo.packageName
        if (pkg == ownPackage) return@mapNotNull null
        try {
            val label = DefaultApps.labels[pkg] ?: info.loadLabel(manager).toString().filter { !it.isISOControl() }.take(64)
            val banner = info.activityInfo.loadBanner(manager)
                ?: runCatching { manager.getApplicationBanner(pkg) }.getOrNull()
            LauncherApp(pkg, label, ComponentName(pkg, info.activityInfo.name),
                info.loadIcon(manager).render(128, 128), banner?.render(384, 216))
        } catch (_: RuntimeException) { null }
    }.distinctBy { it.pkg }.sortedBy { it.label.lowercase() }
}

private fun Drawable.render(width: Int, height: Int): Bitmap {
    val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val sourceWidth = intrinsicWidth.takeIf { it > 0 } ?: width
    val sourceHeight = intrinsicHeight.takeIf { it > 0 } ?: height
    val scale = min(width.toFloat() / sourceWidth, height.toFloat() / sourceHeight)
    val fittedWidth = (sourceWidth * scale).roundToInt().coerceAtLeast(1)
    val fittedHeight = (sourceHeight * scale).roundToInt().coerceAtLeast(1)
    val left = (width - fittedWidth) / 2
    val top = (height - fittedHeight) / 2
    setBounds(left, top, left + fittedWidth, top + fittedHeight)
    draw(Canvas(image))
    return image
}

private fun Bitmap.darkBackdrop(): Int {
    var red = 0L
    var green = 0L
    var blue = 0L
    var count = 0
    for (y in 0 until height step (height / 8).coerceAtLeast(1)) {
        for (x in 0 until width step (width / 8).coerceAtLeast(1)) {
            val pixel = getPixel(x, y)
            if (Color.alpha(pixel) < 128) continue
            red += Color.red(pixel); green += Color.green(pixel); blue += Color.blue(pixel)
            count++
        }
    }
    if (count == 0) return Color.rgb(24, 32, 42)
    fun dark(channel: Long) = (channel.toDouble() / count * .22 + 10).roundToInt().coerceIn(0, 255)
    return Color.rgb(dark(red), dark(green), dark(blue))
}

internal fun orderedApps(apps: List<LauncherApp>, viewer: Viewer, allowed: Set<String>): List<LauncherApp> {
    val order = if (viewer.isChild) DefaultApps.labels.keys.toList()
        else listOf("com.google.android.youtube.tv") + DefaultApps.labels.keys
    return apps.filter { !viewer.isChild || it.pkg in allowed }.distinctBy { it.pkg }
        .sortedBy { order.indexOf(it.pkg).let { index -> if (index < 0) Int.MAX_VALUE else index } }
}
