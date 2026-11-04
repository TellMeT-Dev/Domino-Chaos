package com.udistrital.dominochaos.viewmodels

import android.content.Context
import com.udistrital.dominochaos.R
import com.udistrital.dominochaos.repositories.GameActionException
import com.udistrital.dominochaos.domain.DominoTile
import com.udistrital.dominochaos.domain.End
import com.udistrital.dominochaos.domain.GameError
import com.udistrital.dominochaos.domain.GameMode
import com.udistrital.dominochaos.domain.GameStatus
import com.udistrital.dominochaos.domain.PlacedTile
import com.udistrital.dominochaos.domain.Player
import com.udistrital.dominochaos.domain.RoundResult
import com.udistrital.dominochaos.domain.TurnOptions

data class PlayerUi(
    val id: String,
    val name: String,
    val score: Int,
    val tileCount: Int,
    val isBot: Boolean,
    val isCurrentTurn: Boolean,
    val isSessionPlayer: Boolean,
    val powersUsed: Int = 0
)


data class GameUiState(
    val loading: Boolean = true,
    val code: String = "",
    val notFound: Boolean = false,
    val status: GameStatus? = null,
    val mode: GameMode? = null,
    val isHost: Boolean = false,
    val me: Player? = null,
    val activeSeatId: String? = null,
    val players: List<PlayerUi> = emptyList(),
    val handsById: Map<String, List<DominoTile>> = emptyMap(),
    val chain: List<PlacedTile> = emptyList(),
    val leftEnd: Int? = null,
    val rightEnd: Int? = null,
    val pozoCount: Int = 0,
    val round: Int = 1,
    val maxRounds: Int = 5,
    val totalPlayers: Int = 2,
    val myTurn: Boolean = false,
    val options: TurnOptions = TurnOptions(),
    val selectedTileId: String? = null,
    val selectedTile: DominoTile? = null,
    val legalEnds: List<End> = emptyList(),
    val useComodin: Boolean = false,
    val outwardIsA: Boolean = false,
    val reversedFirst: Boolean = false,
    val roundResult: RoundResult? = null,
    val winnerNames: List<String> = emptyList(),
    val busy: Boolean = false,
    val error: String? = null,
    val botThinking: Boolean = false
) {
    val isWaiting: Boolean get() = status == GameStatus.WAITING
    val isRoundOver: Boolean get() = status == GameStatus.ROUND_OVER
    val isFinished: Boolean get() = status == GameStatus.FINISHED
    val inProgress: Boolean get() = status == GameStatus.IN_PROGRESS

    val opponents: List<PlayerUi> get() = players.filter { !it.isSessionPlayer }
}

fun GameError.asMessage(context: Context): String = when (this) {
    GameError.GAME_NOT_FOUND -> context.getString(R.string.error_game_not_found)
    GameError.NOT_IN_GAME -> context.getString(R.string.error_not_in_game)
    GameError.NOT_YOUR_TURN -> context.getString(R.string.error_not_your_turn)
    GameError.GAME_NOT_ACTIVE -> context.getString(R.string.error_game_not_active)
    GameError.GAME_FINISHED -> context.getString(R.string.error_game_finished)
    GameError.ROOM_FULL -> context.getString(R.string.error_room_full)
    GameError.ALREADY_IN_GAME -> context.getString(R.string.error_already_in_game)
    GameError.NOT_HOST -> context.getString(R.string.error_not_host)
    GameError.NOT_ENOUGH_PLAYERS -> context.getString(R.string.error_not_enough_players)
    GameError.TILE_NOT_IN_HAND -> context.getString(R.string.error_tile_not_in_hand)
    GameError.NO_LEGAL_END -> context.getString(R.string.error_no_legal_end)
    GameError.CANNOT_DRAW -> context.getString(R.string.error_cannot_draw)
    GameError.CANNOT_PASS -> context.getString(R.string.error_cannot_pass)
    GameError.NO_POWERS_LEFT -> context.getString(R.string.error_no_powers_left)
    GameError.INVALID_TARGET -> context.getString(R.string.error_invalid_target)
    GameError.ROUND_NOT_OVER -> context.getString(R.string.error_round_not_over)
    GameError.INVALID_ACTION -> context.getString(R.string.error_invalid_action)
}

fun Throwable?.asMessage(context: Context): String = when (this) {
    null -> context.getString(R.string.error_unexpected)
    is GameActionException -> error.asMessage(context)
    else -> message ?: context.getString(R.string.error_unexpected)
}

