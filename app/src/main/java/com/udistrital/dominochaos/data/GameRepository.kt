package com.udistrital.dominochaos.data

import com.udistrital.dominochaos.domain.Game
import com.udistrital.dominochaos.domain.GameAction
import com.udistrital.dominochaos.domain.GameConfig
import com.udistrital.dominochaos.domain.GameError
import com.udistrital.dominochaos.domain.GameMode
import com.udistrital.dominochaos.domain.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class GameSummary(
    val code: String = "",
    val hostName: String = "",
    val playerCount: Int = 0,
    val totalPlayers: Int = 2,
    val mode: GameMode = GameMode.ONLINE,
    val createdAt: Long = 0L
)

fun Game.toSummary(): GameSummary = GameSummary(
    code = code,
    hostName = players.firstOrNull { it.id == hostId }?.name ?: players.firstOrNull()?.name ?: "",
    playerCount = players.size,
    totalPlayers = config.totalPlayers,
    mode = mode,
    createdAt = createdAt
)

class GameActionException(
    val error: GameError,
    detail: String = error.name
) : Exception(detail)


interface GameRepository {

    val isOnline: Boolean

    fun observeOpenGames(): Flow<List<GameSummary>>

    fun observeGame(code: String): Flow<Game?>

    suspend fun createGame(
        host: Player,
        config: GameConfig,
        mode: GameMode,
        otherPlayers: List<Player> = emptyList()
    ): Result<String>

    suspend fun joinGame(code: String, player: Player): Result<Unit>

    suspend fun submitAction(code: String, playerId: String, action: GameAction): Result<Unit>

    suspend fun leaveGame(code: String, playerId: String): Result<Unit>
}
