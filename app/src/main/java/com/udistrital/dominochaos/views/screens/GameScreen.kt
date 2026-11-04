package com.udistrital.dominochaos.views.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udistrital.dominochaos.R
import com.udistrital.dominochaos.domain.Power
import com.udistrital.dominochaos.domain.PowerUses
import com.udistrital.dominochaos.domain.RoundEndReason
import com.udistrital.dominochaos.domain.TurnOptions
import com.udistrital.dominochaos.viewmodels.GameUiState
import com.udistrital.dominochaos.viewmodels.GameViewModel
import com.udistrital.dominochaos.views.Components.BoardComponent
import com.udistrital.dominochaos.views.Components.DominoTileComponent
import com.udistrital.dominochaos.views.Components.PlayerComponent
import com.udistrital.dominochaos.views.Components.PowerControls
import com.udistrital.dominochaos.views.Components.ScoreBoard

@Composable
fun GameScreen(
    vm: GameViewModel,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val config = LocalConfiguration.current

    val gameBitmap = remember {
        BitmapFactory.decodeResource(
            context.resources,
            R.drawable.game
        ).asImageBitmap()
    }

    Canvas(
        modifier = Modifier
    ){
        drawImage(
            image = gameBitmap,
            dstSize = IntSize(
                config.screenWidthDp * 4,
                config.screenHeightDp * 4
            )
        )
    }

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
                text = stringResource(R.string.game_not_found, state.code),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Button(onClick = onExit, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.back_to_lobby))
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScoreBoard(players = state.players, modifier = Modifier.weight(1f))
            TextButton(onClick = { showLeaveConfirm = true }) {
                Text(stringResource(R.string.leave_short))
            }
        }

        if (state.busy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        val roundStatus = stringResource(
            R.string.game_round_status, state.round, state.maxRounds, state.pozoCount
        )
        val turnInfo = if (currentName != null && state.inProgress) {
            stringResource(R.string.game_round_turn, currentName)
        } else ""
        Text(
            text = roundStatus + turnInfo,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )


        if (state.opponents.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.opponents, key = { it.id }) { opponent ->
                    PlayerComponent(player = opponent)
                }
            }
        }

        BoardComponent(
            chain = state.chain,
            leftEnd = state.leftEnd,
            rightEnd = state.rightEnd,
            selectedTile = state.selectedTile,
            legalEnds = state.legalEnds,
            onPlayAt = { end -> vm.playAt(end) },
            modifier = Modifier.weight(1f)
        )

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
                text = stringResource(R.string.bot_thinking),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (state.inProgress) {
            Text(
                text = stringResource(R.string.waiting_turn, currentName ?: ""),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HandRow(state = state, vm = vm)

        if (state.isWaiting) {
            WaitingPanel(state = state, vm = vm)
        }
    }


    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = { vm.clearError() },
            title = { Text(stringResource(R.string.attention)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { vm.clearError() }) {
                    Text(stringResource(R.string.accept))
                }
            }
        )
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text(stringResource(R.string.leave_confirm_title)) },
            text = { Text(stringResource(R.string.leave_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveConfirm = false
                        vm.leave(onLeft = onExit)
                    }
                ) { Text(stringResource(R.string.leave)) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
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


@Composable
private fun SelectionOptions(state: GameUiState, vm: GameViewModel) {
    val tile = state.selectedTile ?: return
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.chain.isEmpty()) {
            Text(
                stringResource(R.string.orientation_label),
                style = MaterialTheme.typography.labelMedium
            )
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
            Text(
                stringResource(R.string.new_end_label),
                style = MaterialTheme.typography.labelMedium
            )
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
            OutlinedButton(onClick = onDraw) { Text(stringResource(R.string.draw)) }
        }
        if (options.canPass) {
            OutlinedButton(onClick = onPass) { Text(stringResource(R.string.pass)) }
        }
        if (!options.canDraw && !options.canPass) {
            Text(
                text = stringResource(R.string.select_hint),
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
            Text(
                stringResource(R.string.room_code_label),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = state.code,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(
                    R.string.players_line,
                    state.players.size,
                    state.totalPlayers,
                    state.players.joinToString { it.name }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            when {
                state.isHost && state.players.size >= 2 -> Button(onClick = vm::startMatch) {
                    Text(stringResource(R.string.start_match))
                }

                state.isHost -> Text(
                    text = stringResource(R.string.waiting_other),
                    style = MaterialTheme.typography.bodySmall
                )

                else -> Text(
                    text = stringResource(R.string.host_will_start),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


@Composable
private fun RoundResultDialog(state: GameUiState, onNextRound: () -> Unit) {
    val result = state.roundResult ?: return
    val winnerName = state.players.firstOrNull { it.id == result.winnerId }?.name
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.round_done, result.round)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = when {
                        result.winnerId == null -> stringResource(R.string.round_tie)
                        else -> stringResource(
                            R.string.round_winner, winnerName ?: "", result.points
                        )
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = when (result.reason) {
                        RoundEndReason.WENT_OUT -> stringResource(R.string.round_went_out)
                        RoundEndReason.BLOCKED -> stringResource(R.string.round_blocked)
                        RoundEndReason.VOID -> stringResource(R.string.round_void)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onNextRound) { Text(stringResource(R.string.next_round)) }
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
        title = { Text(stringResource(R.string.match_finished)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = when {
                        state.winnerNames.size > 1 -> stringResource(
                            R.string.match_draw, state.winnerNames.joinToString()
                        )
                        state.winnerNames.size == 1 -> stringResource(
                            R.string.match_winner, state.winnerNames.first()
                        )
                        else -> stringResource(R.string.no_winner)
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                state.players
                    .sortedByDescending { it.score }
                    .forEachIndexed { index, player ->
                        Text(
                            text = stringResource(
                                R.string.ranking_line,
                                index + 1,
                                player.name,
                                player.score,
                                player.powersUsed
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
            }
        },
        confirmButton = {
            Button(onClick = onRestart) { Text(stringResource(R.string.restart)) }
        },
        dismissButton = {
            TextButton(onClick = onExit) { Text(stringResource(R.string.exit_lobby)) }
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
        title = { Text(stringResource(R.string.exchange_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    targetId == null -> {
                        Text(
                            stringResource(R.string.choose_rival),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        state.players
                            .filter { it.id != seatId && it.tileCount > 0 }
                            .forEach { player ->
                                TextButton(
                                    onClick = { targetId = player.id },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        stringResource(
                                            R.string.rival_tiles, player.name, player.tileCount
                                        )
                                    )
                                }
                            }
                    }

                    targetTileId == null -> {
                        Text(
                            text = stringResource(R.string.pick_tile_of, target?.name ?: ""),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TilePicker(
                            tiles = state.handsById[targetId.orEmpty()].orEmpty(),
                            selectedId = targetTileId,
                            onSelect = { targetTileId = it }
                        )
                        TextButton(onClick = { targetId = null }) {
                            Text(stringResource(R.string.change_rival))
                        }
                    }

                    myTileId == null -> {
                        Text(
                            text = stringResource(R.string.your_tile),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TilePicker(
                            tiles = state.handsById[seatId.orEmpty()].orEmpty(),
                            selectedId = myTileId,
                            onSelect = { myTileId = it }
                        )
                        TextButton(onClick = { targetTileId = null }) {
                            Text(stringResource(R.string.back))
                        }
                    }

                    else -> {
                        Text(
                            text = stringResource(
                                R.string.exchange_summary, target?.name ?: ""
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { myTileId = null }) {
                            Text(stringResource(R.string.back))
                        }
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
            ) { Text(stringResource(R.string.exchange_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
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
