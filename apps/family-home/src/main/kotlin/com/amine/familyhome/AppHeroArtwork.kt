package com.amine.familyhome

import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.min

/** An optional drawable owned by the selected application's installed package. */
data class AppHeroArtworkReference(val packageName: String, val resourceId: Int)

internal fun readAppHeroArtwork(manager: PackageManager, packageName: String): AppHeroArtworkReference? = try {
    val info = manager.getApplicationInfo(packageName,
        PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
    val resourceId = info.metaData?.getInt("com.amine.familyhome.HERO_IMAGE", 0) ?: 0
    resourceId.takeIf { it > 0 }?.let { AppHeroArtworkReference(packageName, it) }
} catch (_: PackageManager.NameNotFoundException) {
    null
} catch (_: RuntimeException) {
    null
}

internal suspend fun decodeAppHeroArtwork(manager: PackageManager, reference: AppHeroArtworkReference): Bitmap? =
    withContext(Dispatchers.IO) {
        currentCoroutineContext().ensureActive()
        val image = try {
            val resources = manager.getResourcesForApplication(reference.packageName)
            if (resources.getResourceTypeName(reference.resourceId) != "drawable") return@withContext null
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resources, reference.resourceId)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val width = info.size.width
                val height = info.size.height
                val scale = min(1.0, min(1920.0 / width, 1080.0 / height))
                if (scale < 1.0) decoder.setTargetSize(
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1))
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: Resources.NotFoundException) {
            null
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: SecurityException) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
        currentCoroutineContext().ensureActive()
        image
    }

@Composable
internal fun selectedAppHeroArtwork(packageName: String?, reference: AppHeroArtworkReference?, resume: Int): Bitmap? {
    val manager = LocalContext.current.packageManager
    // A new selection, reference, or resume starts with no bitmap in the same composition.
    // An obsolete decode can update only its previous state holder, never the new selection.
    var bitmap by remember(packageName, reference, resume) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(packageName, reference, resume) {
        if (reference != null) bitmap = decodeAppHeroArtwork(manager, reference)
    }
    return bitmap
}
