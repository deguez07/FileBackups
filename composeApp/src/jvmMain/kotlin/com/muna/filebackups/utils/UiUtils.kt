package com.muna.filebackups.utils

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

/**
 * Positions a popup/tooltip centered above its anchor, clamped so it stays
 * at least [windowMargin] px from every window edge. Falls back to below the
 * anchor when there isn't enough space above.
 *
 * @param windowMargin minimum distance in pixels between the popup and the window bounds.
 */
class WindowAwareTooltipPositionProvider(
    private val windowMargin: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val preferredX = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        val preferredY = anchorBounds.top - popupContentSize.height - 8

        val clampedX = preferredX.coerceIn(
            windowMargin,
            (windowSize.width - popupContentSize.width - windowMargin).coerceAtLeast(windowMargin),
        )
        val clampedY = if (preferredY >= windowMargin) {
            preferredY
        } else {
            anchorBounds.bottom + 8
        }.coerceIn(
            windowMargin,
            (windowSize.height - popupContentSize.height - windowMargin).coerceAtLeast(windowMargin),
        )

        return IntOffset(clampedX, clampedY)
    }
}
