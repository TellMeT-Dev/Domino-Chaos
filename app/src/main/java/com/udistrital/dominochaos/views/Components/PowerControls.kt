package com.udistrital.dominochaos.views.Components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.udistrital.dominochaos.domain.Power
import com.udistrital.dominochaos.domain.PowerUses
import com.udistrital.dominochaos.domain.TurnOptions


@Composable
fun PowerControls(
    remaining: PowerUses,
    options: TurnOptions,
    comodinActive: Boolean,
    onToggleComodin: (Boolean) -> Unit,
    onUse: (Power) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val comodinUses = remaining.remaining(Power.COMODIN)
        FilterChip(
            selected = comodinActive,
            enabled = options.canUseComodin,
            onClick = { onToggleComodin(!comodinActive) },
            label = { Text("★ Comodín ×$comodinUses") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        PowerChip(
            label = "⚡ Salto ×${remaining.remaining(Power.SALTO)}",
            enabled = options.canSalto,
            onClick = { onUse(Power.SALTO) }
        )
        PowerChip(
            label = "⇄ Intercambio ×${remaining.remaining(Power.INTERCAMBIO)}",
            enabled = options.canIntercambio,
            onClick = { onUse(Power.INTERCAMBIO) }
        )
        PowerChip(
            label = "↺ Inversión ×${remaining.remaining(Power.INVERSION)}",
            enabled = options.canInversion,
            onClick = { onUse(Power.INVERSION) }
        )
    }
}

@Composable
private fun PowerChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        enabled = enabled,
        onClick = onClick,
        label = { Text(label) }
    )
}
