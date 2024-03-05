package org.supla.android.features.details.blindsdetail.ui
/*
Copyright (C) AC SOFTWARE SP. Z O.O.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.supla.android.R
import org.supla.android.core.ui.theme.Distance
import org.supla.android.core.ui.theme.SuplaTheme
import kotlin.math.ceil

private val WINDOW_DIMENS = object {
  val width = 288f
  val height = 336f
  val ratio = width / height

  val topLineHeight = 16f
  val slatHeight = 24f
  val slatDistance = 5f

  val windowHorizontalMargin = 16f
  val glassMiddleMargin = 20f
  val glassHorizontalMargin = 18f
  val glassVerticalMargin = 24f
  val slatHorizontalMargin = 8f

  val markerHeight = 8f
  val markerWidth = 28f
}
val WINDOW_VIEW_RATIO = WINDOW_DIMENS.ratio

private val windowShadowRadius = 4.dp
private val windowPaint = Paint().asFrameworkPaint().apply {
  style = android.graphics.Paint.Style.FILL
  strokeCap = android.graphics.Paint.Cap.SQUARE
}
val slatShadowRadius = 1.dp
private val slatPaint = Paint().asFrameworkPaint().apply {
  style = android.graphics.Paint.Style.FILL
  strokeCap = android.graphics.Paint.Cap.SQUARE
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WindowView(
  rollerState: BlindRollerState,
  modifier: Modifier = Modifier,
  colors: WindowColors = WindowColors.standard(),
  onPositionChanging: ((Float) -> Unit)? = null,
  onPositionChanged: ((Float) -> Unit)? = null
) {
  val (windowDimens, updateDimens) = remember { mutableStateOf<WindowViewState?>(null) }
  val moveState = remember { mutableStateOf(MoveState()) }

  Canvas(
    modifier = modifier
      .onSizeChanged { updateDimens(WindowViewState.make(viewSize = it)) }
      .pointerInteropFilter { event ->
        if (windowDimens != null) {
          when (event.action) {
            MotionEvent.ACTION_DOWN ->
              moveState.value = moveState.value.copy(
                initialPoint = Offset(event.x, event.y),
                initialPercentage = rollerState.position
              )

            MotionEvent.ACTION_MOVE -> {
              moveState.value = moveState.value.copy(lastPoint = Offset(event.x, event.y))
              onPositionChanging?.let { it(windowDimens.getPositionFromState(moveState.value)) }
            }

            MotionEvent.ACTION_UP ->
              onPositionChanged?.let { it(rollerState.position) }
          }
        }
        true
      }
  ) {
    if (windowDimens == null) {
      return@Canvas // Skip drawing when view size is not set yet
    }

    drawWindow(windowViewState = windowDimens, colors = colors, rollerState = rollerState)
  }
}

context(DrawScope)
private fun drawWindow(windowViewState: WindowViewState, colors: WindowColors, rollerState: BlindRollerState) {
  windowPaint.apply {
    color = colors.window.toArgb()
    setShadowLayer(windowShadowRadius.toPx(), 0f, 2.dp.toPx(), colors.shadow.toArgb())
  }
  slatPaint.apply {
    color = colors.slatBackground.toArgb()
    setShadowLayer(slatShadowRadius.toPx(), 0f, 1.5.dp.toPx(), colors.shadow.toArgb())
  }

  val windowFrameRadius = 4.dp.toPx().times(windowViewState.scale)

  drawContext.canvas.nativeCanvas.drawRoundRect(
    windowViewState.windowRect.left,
    windowViewState.windowRect.top,
    windowViewState.windowRect.right,
    windowViewState.windowRect.bottom,
    windowFrameRadius,
    windowFrameRadius,
    windowPaint
  )

  drawGlasses(windowViewState, colors)
  drawSlats(rollerState, windowViewState, colors)

  drawContext.canvas.nativeCanvas.drawRect(
    windowViewState.topLineRect.left,
    windowViewState.topLineRect.top,
    windowViewState.topLineRect.right,
    windowViewState.topLineRect.bottom,
    windowPaint
  )

  rollerState.markers.forEach { position ->
    val topPosition = windowViewState.topLineRect.bottom
      .plus(windowViewState.blindRollerHeight.minus(windowViewState.slatDistance).times(position).div(100f))

    drawMarker(Offset(0f, topPosition), windowViewState, colors)
  }
}

context (DrawScope)
private fun drawSlats(rollerState: BlindRollerState, windowViewState: WindowViewState, colors: WindowColors) {
  // 0 ... 1 -> 0 ... 100%
  val positionCorrectedByBottomPosition = rollerState.position
    .div(rollerState.bottomPosition)
    .let { if (it > 1) 1f else it }

  val topCorrection = positionCorrectedByBottomPosition
    .times(windowViewState.blindRollerHeight)
    .minus(windowViewState.slatsDistances)
    .plus(windowViewState.slatDistance.times(1.5f)) // Needed to align slats bottom with window bottom

  // When the blinds position is bigger then bottom position we need to start "closing slats".
  // Here the available space for "opened" slats is calculated
  val availableSpaceForSlatDistances = if (rollerState.position > rollerState.bottomPosition) {
    windowViewState.slatsDistances
      .times(100f.minus(rollerState.position))
      .div(100f.minus(rollerState.bottomPosition))
  } else {
    null
  }
  var slatsCorrection = availableSpaceForSlatDistances?.let { windowViewState.slatsDistances.minus(it) } ?: 0f

  windowViewState.slats.forEachIndexed { idx, slat ->
    if (availableSpaceForSlatDistances != null) {
      val summarizedDistance = idx.times(windowViewState.slatDistance)
      // When the summarized distance used between slats is bigger then available,
      // add additional slat correction, to make slats displayed next to each other (without distance)
      if (summarizedDistance > availableSpaceForSlatDistances) {
        slatsCorrection -= summarizedDistance.minus(availableSpaceForSlatDistances).let {
          // Remove max one slat distance for each slat
          if (it > windowViewState.slatDistance) windowViewState.slatDistance else it
        }
      }
    }
    drawSlat(topCorrection - windowViewState.blindRollerHeight + slatsCorrection, slat, windowViewState, colors)
  }
}

context(DrawScope)
private fun drawSlat(topCorrection: Float, rect: Rect, windowViewState: WindowViewState, colors: WindowColors) {
  val bottom = rect.bottom + topCorrection
  if (bottom < windowViewState.topLineRect.bottom) {
    // skip slats over screen
    return
  }
  val top = rect.top.plus(topCorrection).let {
    if (it < windowViewState.topLineRect.bottom) windowViewState.topLineRect.bottom else it
  }
  drawContext.canvas.nativeCanvas.drawRect(
    rect.left,
    top,
    rect.right,
    bottom,
    slatPaint
  )
  drawRect(
    color = colors.slatBorder,
    topLeft = Offset(rect.left, top),
    size = Size(rect.width, bottom - top),
    style = Stroke(width = 1.dp.toPx())
  )
}

context(DrawScope)
private fun drawGlasses(dimens: WindowViewState, colors: WindowColors) {
  val glassHorizontalMargin = WINDOW_DIMENS.glassHorizontalMargin.times(dimens.scale)
  val glassVerticalMargin = WINDOW_DIMENS.glassVerticalMargin.times(dimens.scale)
  val glassMiddleMargin = WINDOW_DIMENS.glassMiddleMargin.times(dimens.scale)
  val singleGlassWidth = dimens.windowRect.width
    .minus(glassHorizontalMargin.times(2))
    .minus(glassMiddleMargin)
    .div(2f)

  val brush = Brush.verticalGradient(listOf(colors.glassTop, colors.glassBottom))
  val left = dimens.windowRect.left.plus(glassHorizontalMargin)
  val height = dimens.canvasRect.height.minus(glassVerticalMargin.times(2))

  drawRect(
    brush = brush,
    topLeft = Offset(left, glassVerticalMargin),
    size = Size(singleGlassWidth, height)
  )
  drawRect(
    brush = brush,
    topLeft = Offset(
      left.plus(singleGlassWidth).plus(glassMiddleMargin),
      glassVerticalMargin
    ),
    size = Size(singleGlassWidth, height)
  )
}

context (DrawScope)
private fun drawMarker(offset: Offset, windowViewState: WindowViewState, windowColors: WindowColors) {
  windowViewState.markerPath.translate(offset)
  drawPath(
    path = windowViewState.markerPath,
    color = windowColors.markerBackground,
    style = Fill
  )
  drawPath(
    path = windowViewState.markerPath,
    color = windowColors.markerBorder,
    style = Stroke(width = 1.dp.toPx())
  )
  windowViewState.markerPath.translate(offset.times(-1f))
}

private data class MoveState(
  val initialPoint: Offset = Offset(0f, 0f),
  val initialPercentage: Float = 0f,
  val lastPoint: Offset = Offset(0f, 0f)
)

data class BlindRollerState(
  /**
   * The whole blind roller position in percentage
   * 0 - open
   * 100 - closed
   */
  val position: Float,

  /**
   * Position as percentage of [position] when blind roller is touching the parapet but the are still gaps between slats
   */
  val bottomPosition: Float = 100f,

  /**
   * Used for groups - shows positions of single blinds
   */
  val markers: List<Float> = emptyList()
)

private data class WindowViewState(
  val canvasRect: Rect,
  val topLineRect: Rect,
  val windowRect: Rect,
  val slats: List<Rect>,
  val scale: Float,
  val slatDistance: Float,
  val slatsDistances: Float,
  val markerPath: Path
) {

  val blindRollerHeight: Float
    get() = canvasRect.height - topLineRect.height

  fun getPositionFromState(state: MoveState): Float {
    val positionDiffAsPercentage =
      state.lastPoint.y.minus(state.initialPoint.y)
        .div(blindRollerHeight)
        .times(100f)

    return state.initialPercentage.plus(positionDiffAsPercentage).let {
      // Trim to 0% - 100%
      if (it < 0f) {
        0f
      } else if (it > 100f) {
        100f
      } else {
        it
      }
    }
  }

  companion object {

    fun make(viewSize: IntSize): WindowViewState {
      val canvasRect = canvasRect(viewSize = viewSize)
      val scale = canvasRect.width / WINDOW_DIMENS.width
      val topLineRect = Rect(
        Offset(canvasRect.left, canvasRect.top),
        Size(canvasRect.width, WINDOW_DIMENS.topLineHeight.times(scale))
      )
      val windowRect = windowRect(scale, canvasRect, topLineRect)
      val slats = slats(scale, canvasRect, topLineRect)
      val slatDistance = WINDOW_DIMENS.slatDistance.times(scale)

      return WindowViewState(
        canvasRect = canvasRect,
        topLineRect = topLineRect,
        windowRect = windowRect,
        slats = slats,
        scale = scale,
        slatDistance = slatDistance,
        slatsDistances = (slats.size - 1) * slatDistance,
        markerPath = getMarkerPath(scale)
      )
    }

    private fun canvasRect(viewSize: IntSize): Rect {
      val size = getSize(viewSize = viewSize)
      return Rect(
        Offset(viewSize.width.minus(size.width).div(2), 0f),
        size
      )
    }

    private fun windowRect(scale: Float, canvasRect: Rect, topLineRect: Rect): Rect {
      val windowHorizontalMargin = WINDOW_DIMENS.windowHorizontalMargin.times(scale)
      val windowTop = topLineRect.height.div(2)
      val windowSize = Size(
        canvasRect.width.minus(windowHorizontalMargin.times(2)),
        canvasRect.height.minus(windowTop)
      )
      val windowOffset = Offset(canvasRect.left.plus(windowHorizontalMargin), windowTop)

      return Rect(windowOffset, windowSize)
    }

    private fun slats(scale: Float, canvasRect: Rect, topLineRect: Rect): List<Rect> {
      val slatHorizontalMargin = WINDOW_DIMENS.slatHorizontalMargin.times(scale)
      val slatSize = Size(
        canvasRect.width.minus(slatHorizontalMargin.times(2)),
        WINDOW_DIMENS.slatHeight.times(scale)
      )

      val slatDistance = WINDOW_DIMENS.slatDistance.times(scale)
      val slatSpace = slatSize.height.plus(slatDistance)
      val slatsCount = ceil(canvasRect.height.minus(topLineRect.height).div(slatSize.height)).toInt()
      val top = topLineRect.bottom - slatSize.height

      return mutableListOf<Rect>().also {
        for (i in 0 until slatsCount) {
          it.add(Rect(Offset(canvasRect.left.plus(slatHorizontalMargin), top.plus(slatSpace.times(i))), slatSize))
        }
      }
    }

    private fun getSize(viewSize: IntSize): Size {
      val canvasRatio = viewSize.width / viewSize.height.toFloat()
      return if (canvasRatio > WINDOW_DIMENS.ratio) {
        Size(viewSize.height.times(WINDOW_DIMENS.ratio), viewSize.height.toFloat())
      } else {
        Size(viewSize.width.toFloat(), viewSize.width.div(WINDOW_DIMENS.ratio))
      }
    }

    private fun getMarkerPath(scale: Float): Path {
      val startsAt = WINDOW_DIMENS.windowHorizontalMargin.times(scale)

      val height = WINDOW_DIMENS.markerHeight.times(scale)
      val halfHeight = height.div(2f)
      val width = WINDOW_DIMENS.markerWidth.times(scale)
      val path = Path()
      path.moveTo(startsAt, 0f)
      path.lineTo(startsAt + halfHeight, -halfHeight) // (top) -> /
      path.lineTo(startsAt + width, -halfHeight) // (top) -> /‾‾‾
      path.lineTo(startsAt + width, halfHeight) // (top) -> /‾‾‾|
      path.lineTo(startsAt + halfHeight, halfHeight) // (bottom) -> ___|
      path.close()

      return path
    }
  }
}

@Preview
@Composable
private fun Preview_Normal() {
  SuplaTheme {
    Column(verticalArrangement = Arrangement.spacedBy(Distance.tiny)) {
      Box(
        modifier = Modifier
          .width(200.dp)
          .height(200.dp)
          .background(color = colorResource(id = R.color.background))
      ) {
        WindowView(
          rollerState = BlindRollerState(75f),
          modifier = Modifier
            .fillMaxSize()
            .padding(all = Distance.small)
        )
      }
      Box(
        modifier = Modifier
          .width(200.dp)
          .height(350.dp)
          .background(color = colorResource(id = R.color.background))
      ) {
        WindowView(
          rollerState = BlindRollerState(50f, markers = listOf(0f, 10f, 50f, 100f)),
          modifier = Modifier
            .fillMaxSize()
            .padding(all = Distance.small)
        )
      }
      Box(
        modifier = Modifier
          .width(200.dp)
          .height(100.dp)
          .background(color = colorResource(id = R.color.background))
      ) {
        WindowView(
          rollerState = BlindRollerState(25f),
          modifier = Modifier
            .fillMaxSize()
            .padding(all = Distance.small)
        )
      }
    }
  }
}
