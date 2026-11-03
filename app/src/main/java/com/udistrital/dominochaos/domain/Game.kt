package com.udistrital.dominochaos.domain

import kotlinx.serialization.Serializable


enum class GameStatus { WAITING, IN_PROGRESS, ROUND_OVER, FINISHED }


enum class GameMode { ONLINE }


enum class RoundEndReason { WENT_OUT, BLOCKED, VOID }

@Serializable
data class RoundResult(
    val round: Int,
    val winnerId: String? = null,
    val points: Int = 0,
    val reason: RoundEndReason
)

@Serializable
data class GameConfig(
    val totalPlayers: Int = 2,
    val tilesPerPlayer: Int = 7,
    val maxRounds: Int = 5,
    val eventProbability: Double = 0.25,
    val powerUses: Int = 2,
    val extraEventPoints: Int = 5
) {
    init {
        require(totalPlayers in 2..4) { "Debe haber de 2 a 4 jugadores" }
        require(tilesPerPlayer in 1..7) { "Máximo 7 fichas por jugador" }
        require(eventProbability in 0.0..1.0) { "Probabilidad fuera de rango" }
    }
}


@Serializable
data class Game(
    val code: String = "",
    val hostId: String = "",
    val status: GameStatus = GameStatus.WAITING,
    val mode: GameMode = GameMode.ONLINE,
    val config: GameConfig = GameConfig(),
    val players: List<Player> = emptyList(),
    val board: Board = Board(),
    val pozo: List<DominoTile> = emptyList(),
    val turnIndex: Int = 0,
    val round: Int = 1,
    val consecutivePasses: Int = 0,
    val turnsTaken: Int = 0,
    val roundResults: List<RoundResult> = emptyList(),
    val winnerIds: List<String> = emptyList(),
    val createdAt: Long = 0L
) {
    val currentPlayer: Player? get() = players.getOrNull(turnIndex)

    val isOver: Boolean get() = status == GameStatus.FINISHED

    val openSeats: Int get() = (config.totalPlayers - players.size).coerceAtLeast(0)

    fun player(id: String): Player? = players.find { it.id == id }

    fun isTurn(playerId: String): Boolean =
        status == GameStatus.IN_PROGRESS && currentPlayer?.id == playerId

    fun playerOrThrow(id: String): Player =
        player(id) ?: throw IllegalStateException("Jugador $id no está en la partida $code")
}
