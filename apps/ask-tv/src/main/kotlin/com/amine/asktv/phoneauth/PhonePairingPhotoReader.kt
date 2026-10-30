package com.amine.asktv.phoneauth

import android.graphics.BitmapFactory
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import java.io.File

internal object PhonePairingPhotoReader {
    /** A camera photograph is temporary. Every result removes the private source file. */
    fun read(file: File): PhonePairingLink {
        try {
            require(file.length() in 1..25_000_000)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0)
            var sample = 1
            while ((maxOf(bounds.outWidth, bounds.outHeight).toLong() + sample - 1) / sample > 2048) sample *= 2
            val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: throw IllegalArgumentException("Could not read the photograph.")
            try {
                require(bitmap.width <= 2048 && bitmap.height <= 2048)
                var width = bitmap.width
                var height = bitmap.height
                var pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                val reader = MultiFormatReader().apply {
                    setHints(mapOf(DecodeHintType.TRY_HARDER to true, DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
                }
                for (orientation in 0..3) {
                    val image = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width, height, pixels)))
                    val decoded = try { reader.decodeWithState(image).text }
                    catch (_: ReaderException) { null }
                    // A decoded foreign/malformed link is a final failure, never a reason to retry.
                    if (decoded != null) return PhonePairingLink.parse(decoded)
                    if (orientation < 3) {
                        val rotated = IntArray(pixels.size)
                        for (y in 0 until height) for (x in 0 until width) {
                            rotated[x * height + height - 1 - y] = pixels[y * width + x]
                        }
                        pixels = rotated
                        val previousWidth = width
                        width = height
                        height = previousWidth
                    }
                }
                throw IllegalArgumentException("Could not read the TV code.")
            } finally { bitmap.recycle() }
        } finally { file.delete() }
    }
}
