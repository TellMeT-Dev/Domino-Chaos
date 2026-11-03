package com.udistrital.dominochaos.viewmodels

import com.udistrital.dominochaos.data.GameActionException
import com.udistrital.dominochaos.domain.DominoTile
import com.udistrital.dominochaos.domain.End
import com.udistrital.dominochaos.domain.GameAction
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

fun GameError.asMessage(): String = when (this) {
    GameError.GAME_NOT_FOUND -> "No se encontró la partida."
    GameError.NOT_IN_GAME -> "No estás en esta partida."
    GameError.NOT_YOUR_TURN -> "No es tu turno."
    GameError.GAME_NOT_ACTIVE -> "La partida todavía no está activa."
    GameError.GAME_FINISHED -> "La partida ya terminó."
    GameError.ROOM_FULL -> "La sala está llena."
    GameError.ALREADY_IN_GAME -> "Ya estás dentro de esa sala."
    GameError.NOT_HOST -> "Solo el anfitrión puede hacer eso."
    GameError.NOT_ENOUGH_PLAYERS -> "Se necesitan al menos 2 jugadores."
    GameError.TILE_NOT_IN_HAND -> "No tienes esa ficha."
    GameError.NO_LEGAL_END -> "Esa ficha no encaja en ese extremo."
    GameError.CANNOT_DRAW -> "No puedes robar ahora."
    GameError.CANNOT_PASS -> "No puedes pasar ahora."
    GameError.NO_POWERS_LEFT -> "No te quedan usos de ese poder."
    GameError.INVALID_TARGET -> "Jugador u objetivo no válido."
    GameError.ROUND_NOT_OVER -> "La ronda aún no ha terminado."
    GameError.INVALID_ACTION -> "Esa acción no está permitida ahora."
}

fun Throwable?.asMessage(): String = when (this) {
    null -> "Ocurrió un error inesperado."
    is GameActionException -> error.asMessage()
    else -> message ?: "Ocurrió un error inesperado."
}

