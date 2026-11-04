package com.udistrital.dominochaos.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.database.ValueEventListener
import com.udistrital.dominochaos.data.UserSession
import com.udistrital.dominochaos.domain.End
import com.udistrital.dominochaos.domain.Game
import com.udistrital.dominochaos.domain.GameAction
import com.udistrital.dominochaos.domain.GameEngine
import com.udistrital.dominochaos.domain.GameStatus
import com.udistrital.dominochaos.domain.Player
import com.udistrital.dominochaos.repositories.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

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

    private var joinAttempts = 0

    private val _game = MutableStateFlow<Game?>(null)

    private val gameListener: ValueEventListener = repo.observeGame(code) { game ->
        _game.value = game
        autoJoinIfNeeded(game)
    }

    val uiState: StateFlow<GameUiState> = combine(
        _game,
        selection,
        busy,
        errorMsg
    ) { game, sel, isBusy, error ->
        buildState(game, sel, isBusy, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameUiState(code = code))



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


    fun playAt(end: End) {
        val sel = selection.value
        val tileId = sel.tileId ?: return
        submit(myId, GameAction.Play(
            tileId = tileId,
            end = end,
            useComodin = sel.useComodin,
            reversed = sel.reversedFirst,
            outwardIsA = sel.outwardIsA
        ))
    }

    fun draw() = submit(myId, GameAction.Draw)

    fun pass() = submit(myId, GameAction.Pass)

    fun useSalto() = submit(myId, GameAction.UseSalto)

    fun useInversion() = submit(myId, GameAction.UseInversion)

    fun useIntercambio(targetPlayerId: String, targetTileId: String, myTileId: String) =
        submit(myId, GameAction.UseIntercambio(targetPlayerId, targetTileId, myTileId))

    fun startMatch() = submit(myId, GameAction.StartMatch)

    fun nextRound() = submit(myId, GameAction.StartNextRound)

    fun restart() = submit(myId, GameAction.RestartMatch)

    fun leave(onLeft: () -> Unit) {
        busy.value = true
        repo.leaveGame(code, myId) { error ->
            busy.value = false
            if (error != null) errorMsg.value = error.asMessage()
            onLeft()
        }
    }



    private fun autoJoinIfNeeded(game: Game?) {
        if (game == null || joinAttempts >= MAX_JOIN_ATTEMPTS) return
        if (game.player(myId) != null) return
        if (game.status != GameStatus.WAITING) return

        joinAttempts++
        val name = session.playerName ?: "Jugador"
        repo.joinGame(code, Player(id = myId, name = name)) { }
    }

    private fun submit(playerId: String, action: GameAction, silent: Boolean = false) {
        busy.value = true
        repo.submitAction(code, playerId, action) { error ->
            busy.value = false
            if (error != null && !silent) {
                errorMsg.value = error.asMessage()
            }
            if (error == null && action is GameAction.Play) {
                selection.value = Selection()
            }
        }
    }

    override fun onCleared() {
        repo.stopObserving(code, gameListener)
        super.onCleared()
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
        val me = game.player(myId)
        val options = GameEngine.turnOptions(game, myId)
        val selectedTile = me?.hand?.find { it.id == sel.tileId }
        val legalEnds = if (selectedTile != null && options.isYourTurn) {
            GameEngine.legalEndsFor(game, myId, selectedTile.id, sel.useComodin)
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
            activeSeatId = if (options.isYourTurn) myId else null,
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
        const val MAX_JOIN_ATTEMPTS = 3
    }
}

fun gameViewModelFactory(context: Context, code: String): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            GameViewModel(GameRepository(), UserSession(context.applicationContext), code)
        }
    }
