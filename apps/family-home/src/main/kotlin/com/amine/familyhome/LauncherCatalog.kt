package com.amine.familyhome

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable

data class LauncherApp(val pkg: String, val label: String, val component: ComponentName,
    val icon: Bitmap, val banner: Bitmap? = null)

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
    setBounds(0, 0, width, height)
    draw(Canvas(image))
    return image
}

internal fun orderedApps(apps: List<LauncherApp>, viewer: Viewer, allowed: Set<String>): List<LauncherApp> {
    val order = if (viewer.isChild) DefaultApps.labels.keys.toList()
        else listOf("com.google.android.youtube.tv") + DefaultApps.labels.keys
    return apps.filter { !viewer.isChild || it.pkg in allowed }.distinctBy { it.pkg }
        .sortedBy { order.indexOf(it.pkg).let { index -> if (index < 0) Int.MAX_VALUE else index } }
}
