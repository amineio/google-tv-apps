package com.amine.postbox

import android.graphics.BitmapFactory
import com.amine.postbox.core.PairingLink
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.io.File

internal object PairingPhotoReader {
    /** The delegated camera writes a full-resolution private file, never a thumbnail. */
    fun read(file: File): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        val bitmap =
            BitmapFactory.decodeFile(
                file.path,
                BitmapFactory.Options().apply { inSampleSize = sample },
            ) ?: throw IllegalArgumentException()
        try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
            val reader =
                MultiFormatReader().apply {
                    setHints(
                        mapOf(
                            DecodeHintType.TRY_HARDER to true,
                            DecodeHintType.POSSIBLE_FORMATS to
                                listOf(com.google.zxing.BarcodeFormat.QR_CODE),
                        )
                    )
                }
            val result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
            PairingLink.parse(result)
            return result
        } finally {
            bitmap.recycle()
        }
    }
}
