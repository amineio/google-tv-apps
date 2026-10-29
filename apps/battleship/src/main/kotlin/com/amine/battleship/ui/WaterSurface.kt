package com.amine.battleship.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.amine.battleship.R
import kotlin.math.roundToInt

/** One still bitmap. Neither texture sampling nor brightness changes between frames. */
@Composable
internal fun WaterSurface() {
    val ocean = ImageBitmap.imageResource(R.drawable.ocean_still)
    Canvas(Modifier.fillMaxSize()) {
        val geometry = BoardGeometry.measure(size.width, compact = false)
        val edge = minOf(ocean.width, ocean.height)
        drawImage(ocean, srcOffset = IntOffset((ocean.width - edge) / 2, (ocean.height - edge) / 2),
            srcSize = IntSize(edge, edge), dstOffset = IntOffset(geometry.originX.roundToInt(), geometry.originY.roundToInt()),
            dstSize = IntSize(geometry.side.roundToInt(), geometry.side.roundToInt()))
        drawRect(Ocean.copy(alpha = .28f), Offset(geometry.originX, geometry.originY), Size(geometry.side, geometry.side))
    }
}
