package com.udistrital.dominochaos.views.Components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.udistrital.dominochaos.domain.DominoTile
import com.udistrital.dominochaos.domain.PlacedTile

@Composable
fun DominoTileComponent(
    leftValue: Int,
    rightValue: Int,
    modifier: Modifier = Modifier,
    size: TileSize = TileSize.HAND,
    selected: Boolean = false,
    dimmed: Boolean = false,
    showFace: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(size.radius)
    val borderColor = when {
        selected -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val background = if (showFace) MaterialTheme.colorScheme.surfaceContainerHigh
    else MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = modifier
            .height(size.height)
            .width(size.width)
            .clip(shape)
            .background(background)
            .border(width = if (selected) 2.dp else 1.dp, color = borderColor, shape = shape)
            .alpha(if (dimmed) 0.35f else 1f)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        if (showFace) {
            ValueHalf(leftValue, size)
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.Gray.copy(alpha = 0.5f))
            )
            ValueHalf(rightValue, size)
        } else {
            Box(
                modifier = Modifier
                    .width(size.width)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "◆",
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    fontSize = size.fontSize.sp
                )
            }
        }
    }
}

@Composable
private fun RowScope.ValueHalf(value: Int, size: TileSize) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.toString(),
            fontSize = size.fontSize.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

enum class TileSize(val width: Dp, val height: Dp, val radius: Int, val fontSize: Int) {
    HAND(width = 58.dp, height = 34.dp, radius = 8, fontSize = 16),
    CHAIN(width = 44.dp, height = 26.dp, radius = 6, fontSize = 13),
    MINI(width = 30.dp, height = 18.dp, radius = 4, fontSize = 10)
}

@Composable
fun DominoTileComponent(
    placed: PlacedTile,
    modifier: Modifier = Modifier,
    size: TileSize = TileSize.CHAIN,
    onClick: (() -> Unit)? = null
) {
    DominoTileComponent(
        leftValue = placed.leftValue,
        rightValue = placed.rightValue,
        modifier = modifier,
        size = size,
        onClick = onClick
    )
}

@Composable
fun DominoTileComponent(
    tile: DominoTile,
    modifier: Modifier = Modifier,
    size: TileSize = TileSize.HAND,
    selected: Boolean = false,
    dimmed: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    DominoTileComponent(
        leftValue = tile.a,
        rightValue = tile.b,
        modifier = modifier,
        size = size,
        selected = selected,
        dimmed = dimmed,
        onClick = onClick
    )
}
