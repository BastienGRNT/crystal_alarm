package com.crystal.alarm.presentation.components

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crystal.alarm.presentation.theme.CrystalColors
import com.crystal.alarm.presentation.theme.CrystalType
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlin.math.abs

private const val VISIBLE_ITEMS = 5
private const val LOOPS = 1_000

/**
 * Infinite, snapping vertical wheel (iOS style). Items fade and shrink away from the center.
 */
@Composable
fun WheelPicker(
    count: Int,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 64.dp,
    width: Dp = 112.dp,
    label: (Int) -> String,
) {
    val haptics = LocalHapticFeedback.current
    val startIndex = remember { count * (LOOPS / 2) + selected }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val currentOnSelected by rememberUpdatedState(onSelected)

    // The item in the middle of the viewport, thanks to the padding equal to 2 items.
    val centerIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - viewportCenter) }?.index
                ?: listState.firstVisibleItemIndex
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { centerIndex }
            .distinctUntilChanged()
            .drop(1)
            .collect {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                currentOnSelected(it % count)
            }
    }

    Box(
        modifier = modifier
            .width(width)
            .height(itemHeight * VISIBLE_ITEMS)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.35f to Color.Black,
                        0.65f to Color.Black,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            contentPadding = PaddingValues(vertical = itemHeight * (VISIBLE_ITEMS / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(count * LOOPS) { index ->
                val distance = abs(index - centerIndex)
                val isCenter = distance == 0
                Text(
                    text = label(index % count),
                    style = CrystalType.Display,
                    color = if (isCenter) CrystalColors.Text else CrystalColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .graphicsLayer {
                            val scale = if (isCenter) 1f else 0.78f
                            scaleX = scale
                            scaleY = scale
                            alpha = if (distance > 1) 0.5f else 1f
                        }
                        .wrapContentHeight(Alignment.CenterVertically),
                )
            }
        }
    }
}
