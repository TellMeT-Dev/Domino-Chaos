package com.udistrital.dominochaos.views.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udistrital.dominochaos.domain.Power
import com.udistrital.dominochaos.domain.PowerUses
import com.udistrital.dominochaos.domain.RoundEndReason
import com.udistrital.dominochaos.domain.TurnOptions
import com.udistrital.dominochaos.viewmodels.GameUiState
import com.udistrital.dominochaos.viewmodels.GameViewModel
import com.udistrital.dominochaos.viewmodels.PlayerUi
import com.udistrital.dominochaos.viewmodels.asMessage
import com.udistrital.dominochaos.views.Components.BoardComponent
import com.udistrital.dominochaos.views.Components.DominoTileComponent
import com.udistrital.dominochaos.views.Components.PlayerComponent
import com.udistrital.dominochaos.views.Components.PowerControls
import com.udistrital.dominochaos.views.Components.ScoreBoard

/**
 * Pantalla de juego: pinta el [GameUiState] y envía las acciones al
 * [GameViewModel]. Aquí NO se validan jugadas: los extremos legales, la mano
 * jugable y los poderes disponibles llegan ya calculados desde el motor.
 */
@Composable
fun GameScreen(
    vm: GameViewModel,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    when {
        state.loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }

        state.notFound -> Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No se encontró la partida \"${state.code}\".",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Button(onClick = onExit, modifier = Modifier.padding(top = 16.dp)) {
                Text("Volver al lobby")
            }
        }

        else -> GameBody(state = state, vm = vm, onExit = onExit, modifier = modifier)
    }
}

@Composable
private fun GameBody(
    state: GameUiState,
    vm: GameViewModel,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLeaveConfirm by rememberSaveable { mutableStateOf(false) }
    var showExchange by remember { mutableStateOf(false) }
    val currentName = state.players.firstOrNull { it.isCurrentTurn }?.name

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Marcador + salir ------------------------------------------------
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScoreBoard(players = state.players, modifier = Modifier.weight(1f))
            TextButton(onClick = { showLeaveConfirm = true }) { Text("✕ Salir") }
        }

        if (state.busy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Información general ---------------------------------------------
        Text(
            text = buildString {
                append("Ronda ${state.round}/${state.maxRounds}")
                append("  ·  Pozo: ${state.pozoCount}")
                if (currentName != null && state.inProgress) append("  ·  Turno: $currentName")
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )


        // Rivales ----------------------------------------------------------
        if (state.opponents.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.opponents, key = { it.id }) { opponent ->
                    PlayerComponent(player = opponent)
                }
            }
        }

        // Tablero ----------------------------------------------------------
        BoardComponent(
            chain = state.chain,
            leftEnd = state.leftEnd,
            rightEnd = state.rightEnd,
            selectedTile = state.selectedTile,
            legalEnds = state.legalEnds,
            onPlayAt = { end -> vm.playAt(end) },
            modifier = Modifier.weight(1f)
        )

        // Opciones de jugada ------------------------------------------------
        if (state.myTurn) {
            SelectionOptions(state = state, vm = vm)
            PowerControls(
                remaining = state.me?.powers ?: PowerUses(),
                options = state.options,
                comodinActive = state.useComodin,
                onToggleComodin = { vm.setUseComodin(it) },
                onUse = { power ->
                    when (power) {
                        Power.SALTO -> vm.useSalto()
                        Power.INTERCAMBIO -> showExchange = true
                        Power.INVERSION -> vm.useInversion()
                        Power.COMODIN -> vm.setUseComodin(!state.useComodin)
                    }
                }
            )
            ActionRow(
                options = state.options,
                onDraw = vm::draw,
                onPass = vm::pass
            )
        } else if (state.botThinking) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = "La máquina está pensando…",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (state.inProgress) {
            Text(
                text = "Esperando el turno de $currentName…",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Mi mano ------------------------------------------------------------
        HandRow(state = state, vm = vm)

        // Sala de espera ------------------------------------------------------
        if (state.isWaiting) {
            WaitingPanel(state = state, vm = vm)
        }
    }

    // ---------------------------------------------------------------------
    // Diálogos
    // ---------------------------------------------------------------------

    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = { vm.clearError() },
            title = { Text("Atención") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { vm.clearError() }) { Text("Aceptar") }
            }
        )
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("¿Abandonar la partida?") },
            text = { Text("Perderás tu turno y la partida terminará para ti.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveConfirm = false
                        vm.leave(onLeft = onExit)
                    }
                ) { Text("Abandonar") }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    if (showExchange && state.activeSeatId != null) {
        ExchangeDialog(
            state = state,
            onConfirm = { targetId, targetTileId, myTileId ->
                showExchange = false
                vm.useIntercambio(targetId, targetTileId, myTileId)
            },
            onDismiss = { showExchange = false }
        )
    }

    if (state.isRoundOver && state.roundResult != null) {
        RoundResultDialog(state = state, onNextRound = vm::nextRound)
    }

    if (state.isFinished) {
        MatchResultDialog(
            state = state,
            onRestart = vm::restart,
            onExit = onExit
        )
    }
}

// ---------------------------------------------------------------------
// Secciones
// ---------------------------------------------------------------------

@Composable
private fun SelectionOptions(state: GameUiState, vm: GameViewModel) {
    val tile = state.selectedTile ?: return
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.chain.isEmpty()) {
            Text("Orientación:", style = MaterialTheme.typography.labelMedium)
            FilterChip(
                selected = !state.reversedFirst,
                onClick = { vm.setReversedFirst(false) },
                label = { Text("${tile.a} | ${tile.b}") }
            )
            if (tile.a != tile.b) {
                FilterChip(
                    selected = state.reversedFirst,
                    onClick = { vm.setReversedFirst(true) },
                    label = { Text("${tile.b} | ${tile.a}") }
                )
            }
        } else if (state.useComodin) {
            Text("Nuevo extremo:", style = MaterialTheme.typography.labelMedium)
            FilterChip(
                selected = state.outwardIsA,
                onClick = { vm.setOutwardIsA(true) },
                label = { Text(tile.a.toString()) }
            )
            if (tile.a != tile.b) {
                FilterChip(
                    selected = !state.outwardIsA,
                    onClick = { vm.setOutwardIsA(false) },
                    label = { Text(tile.b.toString()) }
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    options: TurnOptions,
    onDraw: () -> Unit,
    onPass: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (options.canDraw) {
            OutlinedButton(onClick = onDraw) { Text("🁫 Robar del pozo") }
        }
        if (options.canPass) {
            OutlinedButton(onClick = onPass) { Text("Pasar turno") }
        }
        if (!options.canDraw && !options.canPass) {
            Text(
                text = "Selecciona una ficha y un extremo para jugar.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun HandRow(state: GameUiState, vm: GameViewModel) {
    val hand = state.me?.hand ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        hand.forEach { tile ->
            val playable = tile.id in state.options.playableTileIds
            DominoTileComponent(
                tile = tile,
                selected = state.selectedTileId == tile.id,
                dimmed = state.myTurn && !playable && !state.useComodin,
                onClick = if (state.myTurn) ({ vm.selectTile(tile.id) }) else null
            )
        }
    }
}

@Composable
private fun WaitingPanel(state: GameUiState, vm: GameViewModel) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Código de la sala", style = MaterialTheme.typography.labelMedium)
            Text(
                text = state.code,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Jugadores: ${state.players.size}/${state.totalPlayers}" +
                    " · ${state.players.joinToString { it.name }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            when {
                state.isHost && state.players.size >= 2 -> Button(onClick = vm::startMatch) {
                    Text("Empezar partida")
                }

                state.isHost -> Text(
                    text = "Esperando a otro jugador…",
                    style = MaterialTheme.typography.bodySmall
                )

                else -> Text(
                    text = "El anfitrión empezará la partida.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// Diálogos de resultados e intercambio
// ---------------------------------------------------------------------

@Composable
private fun RoundResultDialog(state: GameUiState, onNextRound: () -> Unit) {
    val result = state.roundResult ?: return
    val winnerName = state.players.firstOrNull { it.id == result.winnerId }?.name
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Ronda ${result.round} terminada") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = when {
                        result.winnerId == null -> "Ronda anulada por empate de pips."
                        else -> "Gana $winnerName: +${result.points} puntos"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = when (result.reason) {
                        RoundEndReason.WENT_OUT -> "Se vació la mano de un jugador."
                        RoundEndReason.BLOCKED -> "Nadie pudo jugar: tablero bloqueado."
                        RoundEndReason.VOID -> "Empate entre los jugadores."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onNextRound) { Text("Siguiente ronda") }
        }
    )
}

@Composable
private fun MatchResultDialog(
    state: GameUiState,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("¡Partida terminada!") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = when {
                        state.winnerNames.size > 1 -> "Empate entre ${state.winnerNames.joinToString()}"
                        state.winnerNames.size == 1 -> "🏆 Ganador: ${state.winnerNames.first()}"
                        else -> "Sin ganador."
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                state.players
                    .sortedByDescending { it.score }
                    .forEachIndexed { index, player ->
                        Text(
                            text = "${index + 1}. ${player.name} — ${player.score} pts" +
                                " (${player.powersUsed} poderes usados)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
            }
        },
        confirmButton = {
            Button(onClick = onRestart) { Text("Reiniciar") }
        },
        dismissButton = {
            TextButton(onClick = onExit) { Text("Salir al lobby") }
        }
    )
}

@Composable
private fun ExchangeDialog(
    state: GameUiState,
    onConfirm: (targetPlayerId: String, targetTileId: String, myTileId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var targetId by remember { mutableStateOf<String?>(null) }
    var targetTileId by remember { mutableStateOf<String?>(null) }
    var myTileId by remember { mutableStateOf<String?>(null) }
    val seatId = state.activeSeatId
    val target = state.players.firstOrNull { it.id == targetId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⇄ Intercambio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    targetId == null -> {
                        Text("Elige al rival:", style = MaterialTheme.typography.bodyMedium)
                        state.players
                            .filter { it.id != seatId && it.tileCount > 0 }
                            .forEach { player ->
                                TextButton(
                                    onClick = { targetId = player.id },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("${player.name} (${player.tileCount} fichas)")
                                }
                            }
                    }

                    targetTileId == null -> {
                        Text(
                            text = "Ficha de ${target?.name} que quieres:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TilePicker(
                            tiles = state.handsById[targetId.orEmpty()].orEmpty(),
                            selectedId = targetTileId,
                            onSelect = { targetTileId = it }
                        )
                        TextButton(onClick = { targetId = null }) { Text("Cambiar de rival") }
                    }

                    myTileId == null -> {
                        Text(
                            text = "Tu ficha que entregas:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TilePicker(
                            tiles = state.handsById[seatId.orEmpty()].orEmpty(),
                            selectedId = myTileId,
                            onSelect = { myTileId = it }
                        )
                        TextButton(onClick = { targetTileId = null }) { Text("Atrás") }
                    }

                    else -> {
                        Text(
                            text = "Vas a cambiar una ficha con ${target?.name}.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { myTileId = null }) { Text("Atrás") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = targetId != null && targetTileId != null && myTileId != null,
                onClick = {
                    val t = targetId
                    val tt = targetTileId
                    val m = myTileId
                    if (t != null && tt != null && m != null) onConfirm(t, tt, m)
                }
            ) { Text("Intercambiar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun TilePicker(
    tiles: List<com.udistrital.dominochaos.domain.DominoTile>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tiles.forEach { tile ->
            DominoTileComponent(
                tile = tile,
                selected = tile.id == selectedId,
                onClick = { onSelect(tile.id) }
            )
        }
    }
}
