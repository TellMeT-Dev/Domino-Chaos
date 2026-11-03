package com.udistrital.dominochaos.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udistrital.dominochaos.data.GameRepository
import com.udistrital.dominochaos.data.UserSession
import com.udistrital.dominochaos.domain.End
import com.udistrital.dominochaos.domain.Game
import com.udistrital.dominochaos.domain.GameAction
import com.udistrital.dominochaos.domain.GameEngine
import com.udistrital.dominochaos.domain.GameMode
import com.udistrital.dominochaos.domain.GameStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class Selection(
    val tileId: String? = null,
    val useComodin: Boolean = false,
    val outwardIsA: Boolean = false,
    val reversedFirst: Boolean = false
)


class GameViewModel(
    private val repo: GameRepository,
    private val session: UserSession,
    private val code: String
) : ViewModel() {

    private val myId: String get() = session.playerId

    private val selection = MutableStateFlow(Selection())
    private val busy = MutableStateFlow(false)
    private val errorMsg = MutableStateFlow<String?>(null)

    private val gameFlow: StateFlow<Game?> = repo.observeGame(code)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<GameUiState> = combine(
        gameFlow,
        selection,
        busy,
        errorMsg
    ) { game, sel, isBusy, error ->
        buildState(game, sel, isBusy, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameUiState(code = code))


    // ---------------------------------------------------------------------
    // Selección de ficha / opciones
    // ---------------------------------------------------------------------

    fun selectTile(tileId: String?) = selection.update { current ->
        when {
            tileId == null -> Selection()
            current.tileId == tileId -> Selection()
            else -> Selection(tileId = tileId)
        }
    }

    fun setUseComodin(on: Boolean) = selection.update { it.copy(useComodin = on) }

    fun setOutwardIsA(value: Boolean) = selection.update { it.copy(outwardIsA = value) }

    fun setReversedFirst(value: Boolean) = selection.update { it.copy(reversedFirst = value) }

    fun clearError() {
        errorMsg.value = null
    }

    // ---------------------------------------------------------------------
    // Acciones de juego
    // ---------------------------------------------------------------------

    fun playAt(end: End) {
        val sel = selection.value
        val tileId = sel.tileId ?: return
        submit(seatId(), GameAction.Play(
            tileId = tileId,
            end = end,
            useComodin = sel.useComodin,
            reversed = sel.reversedFirst,
            outwardIsA = sel.outwardIsA
        ))
    }

    fun draw() = submit(seatId(), GameAction.Draw)

    fun pass() = submit(seatId(), GameAction.Pass)

    fun useSalto() = submit(seatId(), GameAction.UseSalto)

    fun useInversion() = submit(seatId(), GameAction.UseInversion)

    fun useIntercambio(targetPlayerId: String, targetTileId: String, myTileId: String) =
        submit(seatId(), GameAction.UseIntercambio(targetPlayerId, targetTileId, myTileId))

    fun startMatch() = submit(myId, GameAction.StartMatch)

    fun nextRound() = submit(myId, GameAction.StartNextRound)

    fun restart() = submit(myId, GameAction.RestartMatch)

    fun leave(onLeft: () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            val result = repo.leaveGame(code, myId)
            busy.value = false
            if (result.isFailure) errorMsg.value = result.exceptionOrNull().asMessage()
            onLeft()
        }
    }

    // ---------------------------------------------------------------------
    // Internos
    // ---------------------------------------------------------------------


    private fun seatId(): String {
        val current = gameFlow.value?.currentPlayer
        return when {
            current == null -> myId
            !current.isBot && current.id != myId &&
                gameFlow.value?.mode != GameMode.ONLINE -> current.id
            else -> myId
        }
    }

    private fun submit(playerId: String, action: GameAction, silent: Boolean = false) {
        viewModelScope.launch {
            busy.value = true
            val result = repo.submitAction(code, playerId, action)
            busy.value = false
            if (result.isFailure && !silent) {
                errorMsg.value = result.exceptionOrNull().asMessage()
            }
            if (result.isSuccess && action is GameAction.Play) {
                selection.value = Selection()
            }
        }
    }



    private fun buildState(game: Game?, sel: Selection, isBusy: Boolean, error: String?): GameUiState {
        if (game == null) {
            return GameUiState(
                loading = false,
                code = code,
                notFound = true,
                error = error
            )
        }

        val current = game.currentPlayer
        val seatId = when {
            current == null -> myId
            !current.isBot && current.id != myId && game.mode != GameMode.ONLINE -> current.id
            else -> myId
        }
        val me = game.player(seatId) ?: game.player(myId)
        val options = GameEngine.turnOptions(game, seatId)
        val selectedTile = me?.hand?.find { it.id == sel.tileId }
        val legalEnds = if (selectedTile != null && options.isYourTurn) {
            GameEngine.legalEndsFor(game, seatId, selectedTile.id, sel.useComodin)
        } else {
            emptyList()
        }

        return GameUiState(
            loading = false,
            code = game.code,
            status = game.status,
            mode = game.mode,
            isHost = game.hostId == myId,
            me = me,
            activeSeatId = if (options.isYourTurn) seatId else null,
            players = game.players.map { player ->
                PlayerUi(
                    id = player.id,
                    name = player.name,
                    score = player.score,
                    tileCount = player.hand.size,
                    isBot = player.isBot,
                    isCurrentTurn = current?.id == player.id && game.status == GameStatus.IN_PROGRESS,
                    isSessionPlayer = player.id == myId,
                    powersUsed = player.powersUsed
                )
            },
            handsById = game.players.associate { it.id to it.hand },
            chain = game.board.tiles,
            leftEnd = game.board.leftEnd,
            rightEnd = game.board.rightEnd,
            pozoCount = game.pozo.size,
            round = game.round,
            maxRounds = game.config.maxRounds,
            totalPlayers = game.config.totalPlayers,
            myTurn = options.isYourTurn,
            options = options,
            selectedTileId = if (selectedTile != null) sel.tileId else null,
            selectedTile = selectedTile,
            legalEnds = legalEnds,
            useComodin = sel.useComodin,
            outwardIsA = sel.outwardIsA,
            reversedFirst = sel.reversedFirst,
            roundResult = if (game.status == GameStatus.ROUND_OVER) game.roundResults.lastOrNull() else null,
            winnerNames = game.winnerIds.mapNotNull { id -> game.player(id)?.name },
            busy = isBusy,
            error = error,
            botThinking = current?.isBot == true && game.status == GameStatus.IN_PROGRESS
        )
    }

    private companion object {
        const val BOT_DELAY_MS = 700L
    }
}
