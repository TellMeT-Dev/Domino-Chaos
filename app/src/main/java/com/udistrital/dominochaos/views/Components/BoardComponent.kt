package com.udistrital.dominochaos.views.Components

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.udistrital.dominochaos.domain.DominoTile
import com.udistrital.dominochaos.domain.End
import com.udistrital.dominochaos.domain.PlacedTile


@Composable
fun BoardComponent(
    chain: List<PlacedTile>,
    leftEnd: Int?,
    rightEnd: Int?,
    selectedTile: DominoTile?,
    legalEnds: List<End>,
    onPlayAt: (End) -> Unit,
    modifier: Modifier = Modifier
) {
    val awaitingPlacement = selectedTile != null

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        if (chain.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (awaitingPlacement) "Coloca la primera ficha" else "Tablero vacío",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                EndZone(
                    label = "Aquí",
                    value = null,
                    enabled = awaitingPlacement,
                    playable = legalEnds.isNotEmpty(),
                    onClick = { onPlayAt(End.RIGHT) }
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EndZone(
                    label = "◀",
                    value = leftEnd,
                    enabled = awaitingPlacement,
                    playable = End.LEFT in legalEnds,
                    onClick = { onPlayAt(End.LEFT) }
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    chain.forEach { placed ->
                        DominoTileComponent(placed = placed)
                    }
                }
                EndZone(
                    label = "▶",
                    value = rightEnd,
                    enabled = awaitingPlacement,
                    playable = End.RIGHT in legalEnds,
                    onClick = { onPlayAt(End.RIGHT) }
                )
            }
        }
    }
}

@Composable
private fun EndZone(
    label: String,
    value: Int?,
    enabled: Boolean,
    playable: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val borderColor = when {
        playable -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    Surface(
        modifier = Modifier
            .width(56.dp)
            .height(78.dp)
            .border(
                width = if (playable) 2.dp else 1.dp,
                color = borderColor,
                shape = shape
            ),
        shape = shape,
        color = when {
            playable -> MaterialTheme.colorScheme.primaryContainer
            enabled -> MaterialTheme.colorScheme.surfaceContainerHighest
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
        onClick = { if (playable) onClick() },
        enabled = enabled && playable
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = value?.toString() ?: label,
                    style = if (value != null) MaterialTheme.typography.headlineSmall
                    else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (playable) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (value != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
