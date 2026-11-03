package com.udistrital.dominochaos.data

import com.udistrital.dominochaos.domain.Game
import com.udistrital.dominochaos.domain.GameAction
import com.udistrital.dominochaos.domain.GameConfig
import com.udistrital.dominochaos.domain.GameError
import com.udistrital.dominochaos.domain.GameMode
import com.udistrital.dominochaos.domain.Player
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de la partida en línea. La implementación real es
 * [FirebaseGameRepository]; la partida entera se guarda como JSON en
 * `games/{codigo}` dentro de Realtime Database.
 */
interface GameRepository {

    fun observeGame(code: String): Flow<Game?>

    suspend fun createGame(
        code: String,
        host: Player,
        config: GameConfig = GameConfig(),
        mode: GameMode = GameMode.ONLINE
    ): Result<Unit>

    suspend fun joinGame(code: String, player: Player): Result<Unit>

    suspend fun submitAction(code: String, playerId: String, action: GameAction): Result<Unit>

    suspend fun leaveGame(code: String, playerId: String): Result<Unit>
}

class GameActionException(
    val error: GameError,
    detail: String = error.name
) : Exception(detail)
