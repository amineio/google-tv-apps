package com.amine.battleship.ui

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

internal interface WaterPainter {
    fun draw(scope: DrawScope, geometry: BoardGeometry, seconds: Float)
}

@Composable
internal fun WaterSurface(compact: Boolean, seconds: State<Float>) {
    val renderer = LocalBoardPresentation.current.renderer
    val painter = remember(renderer) { ResilientWaterPainter(renderer) }
    Canvas(Modifier.fillMaxSize().graphicsLayer()) {
        val geometry = BoardGeometry.measure(size.width, compact)
        clipRect(geometry.originX, geometry.originY, geometry.right, geometry.bottom) {
            painter.draw(this, geometry, seconds.value)
        }
    }
}

/** Shader construction and uniform failures both fall back without changing board state. */
private class ResilientWaterPainter(renderer: WaterRenderer) : WaterPainter {
    private var shader: WaterPainter? = if (renderer == WaterRenderer.AUTOMATIC && Build.VERSION.SDK_INT >= 33) {
        runCatching { ShaderWaterPainter() }.getOrNull()
    } else null
    private val fallback by lazy { CanvasWaterPainter() }
    override fun draw(scope: DrawScope, geometry: BoardGeometry, seconds: Float) {
        val current = shader
        if (current != null) {
            try { current.draw(scope, geometry, seconds); return } catch (_: RuntimeException) { shader = null }
        }
        fallback.draw(scope, geometry, seconds)
    }
}

/** One small, seamless normal-lit texture is cached for all boards; frames change only matrices. */
private object RippleTexture {
    private const val SEED = 0x51EA1234

    /** Periodic quintic value noise: the value and slope meet at each tile boundary. */
    private fun noise(u: Double, v: Double, period: Int, seed: Int): Double {
        val x = u * period
        val y = v * period
        val ix = floor(x).toInt()
        val iy = floor(y).toInt()
        val fx = x - ix
        val fy = y - iy
        val sx = fx * fx * fx * (fx * (fx * 6.0 - 15.0) + 10.0)
        val sy = fy * fy * fy * (fy * (fy * 6.0 - 15.0) + 10.0)
        val a = lattice(ix, iy, period, seed)
        val b = lattice(ix + 1, iy, period, seed)
        val c = lattice(ix, iy + 1, period, seed)
        val d = lattice(ix + 1, iy + 1, period, seed)
        return a + (b - a) * sx + (c - a) * sy + (a - b - c + d) * sx * sy
    }

    private fun lattice(x: Int, y: Int, period: Int, seed: Int): Double {
        var hash = Math.floorMod(x, period) * 374761393 + Math.floorMod(y, period) * 668265263 + seed
        hash = (hash xor (hash ushr 13)) * 1274126177
        return ((hash xor (hash ushr 16)) ushr 8) / 16777215.0
    }

    private fun smooth(low: Double, high: Double, value: Double): Double {
        val t = ((value - low) / (high - low)).coerceIn(0.0, 1.0)
        return t * t * (3.0 - 2.0 * t)
    }

    val bitmap: Bitmap by lazy {
        val edge = 256
        val heights = FloatArray(edge * edge)
        // Both warps are periodic. Their bent sampling domains therefore remain seamless too.
        for (y in 0 until edge) for (x in 0 until edge) {
            val u = x.toDouble() / edge
            val v = y.toDouble() / edge
            val qx = u + (noise(u, v, 3, SEED) - .5) * .22
            val qy = v + (noise(u, v, 4, SEED xor 0x2A713) - .5) * .20
            val swell = noise(qx, qy, 7, SEED xor 0x71A3)
            val ripple = noise(qx, qy, 23, SEED xor 0x31D9)
            val detail = noise(qx, qy, 53, SEED xor 0x19B7)
            heights[y * edge + x] = (swell * .68 + ripple * .24 + detail * .08).toFloat()
        }
        val pixels = IntArray(edge * edge)
        for (y in 0 until edge) for (x in 0 until edge) {
            // Wrapped central differences light the cached heightmap once, never during a frame.
            val left = heights[y * edge + (x + edge - 1) % edge]
            val right = heights[y * edge + (x + 1) % edge]
            val up = heights[((y + edge - 1) % edge) * edge + x]
            val down = heights[((y + 1) % edge) * edge + x]
            val slopeX = -(right - left) * edge * .0275
            val slopeY = -(down - up) * edge * .0275
            val length = sqrt(slopeX * slopeX + slopeY * slopeY + 1.0)
            val nx = slopeX / length
            val ny = slopeY / length
            val nz = 1.0 / length
            val diffuse = ((-.35 * nx - .55 * ny + nz) / 1.193734).coerceIn(0.0, 1.0)
            val facing = ((-.22 * nx - .39 * ny + 1.4 * nz) / 1.469864).coerceIn(0.0, 1.0)
            val height = heights[y * edge + x].toDouble()
            val sky = smooth(-.30, .60, ny * .8 + nx * .4)
            val glint = facing.pow(65.0) * (.10 + .90 * smooth(.46, .80, height))
            val depth = (.75 + diffuse * .25) * (.88 + .12 * height)
            // Match the automatic renderer's dark blue base and sparse, soft reflections.
            val r = ((.025 * depth + .025 * sky + .14 * glint) * 255).toInt().coerceIn(0, 255)
            val g = ((.080 * depth + .050 * sky + .18 * glint) * 255).toInt().coerceIn(0, 255)
            val b = ((.125 * depth + .065 * sky + .19 * glint) * 255).toInt().coerceIn(0, 255)
            pixels[y * edge + x] = (255 shl 24) or (r shl 16) or (g shl 8) or b
        }
        Bitmap.createBitmap(pixels, edge, edge, Bitmap.Config.ARGB_8888)
    }
}

private class CanvasWaterPainter : WaterPainter {
    private val bitmap = RippleTexture.bitmap
    private val texture = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    private val secondary = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { shader = texture }
    private val reflection = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        shader = secondary; alpha = 42
    }
    private val matrix = Matrix()
    override fun draw(scope: DrawScope, geometry: BoardGeometry, seconds: Float) {
        val scale = geometry.side / bitmap.width * .72f
        matrix.setScale(scale, scale * .79f)
        matrix.postTranslate(geometry.originX + seconds * geometry.pitch * .045f,
            geometry.originY + seconds * geometry.pitch * .022f)
        texture.setLocalMatrix(matrix)
        val canvas = scope.drawContext.canvas.nativeCanvas
        canvas.drawRect(geometry.originX, geometry.originY, geometry.right, geometry.bottom, paint)
        matrix.setScale(scale * 1.6f, scale * 1.16f)
        matrix.postRotate(19f, geometry.originX, geometry.originY)
        matrix.postTranslate(-seconds * geometry.pitch * .026f, seconds * geometry.pitch * .031f)
        secondary.setLocalMatrix(matrix)
        canvas.drawRect(geometry.originX, geometry.originY, geometry.right, geometry.bottom, reflection)
    }
}
