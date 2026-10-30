package com.amine.postbox

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.util.Base64
import kotlin.math.roundToInt

/** Call on an IO worker while the incoming URI grant is still available. */
internal object ImageNormalizer {
    fun read(resolver: ContentResolver, uri: Uri): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Image unavailable." }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 4096) sample *= 2
        val decoded =
            resolver.openInputStream(uri).use {
                BitmapFactory.decodeStream(
                    it,
                    null,
                    BitmapFactory.Options().apply { inSampleSize = sample },
                )
            } ?: throw IllegalArgumentException("Image unavailable.")
        var bitmap = decoded
        try {
            val orientation =
                try {
                    resolver.openInputStream(uri).use { stream ->
                        if (stream == null) 1
                        else ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
                    }
                } catch (_: Exception) {
                    1
                }
            val matrix =
                Matrix().apply {
                    when (orientation) {
                        2 -> setScale(-1f, 1f)
                        3 -> setRotate(180f)
                        4 -> setScale(1f, -1f)
                        5 -> {
                            setRotate(90f)
                            postScale(-1f, 1f)
                        }
                        6 -> setRotate(90f)
                        7 -> {
                            setRotate(270f)
                            postScale(-1f, 1f)
                        }
                        8 -> setRotate(270f)
                    }
                }
            if (!matrix.isIdentity) {
                bitmap =
                    Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                if (bitmap !== decoded) decoded.recycle()
            }
            val longest = maxOf(bitmap.width, bitmap.height)
            if (longest > 2048) {
                val ratio = 2048f / longest
                val scaled =
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * ratio).roundToInt().coerceAtLeast(1),
                        (bitmap.height * ratio).roundToInt().coerceAtLeast(1),
                        true,
                    )
                if (scaled !== bitmap) bitmap.recycle()
                bitmap = scaled
            }
            val output = ByteArrayOutputStream()
            require(bitmap.compress(Bitmap.CompressFormat.JPEG, 82, output))
            return Base64.getEncoder().encodeToString(output.toByteArray())
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }
}
