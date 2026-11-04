package com.udistrital.dominochaos.repositories

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.udistrital.dominochaos.domain.Game
import com.udistrital.dominochaos.domain.GameAction
import com.udistrital.dominochaos.domain.GameConfig
import com.udistrital.dominochaos.domain.GameEngine
import com.udistrital.dominochaos.domain.GameError
import com.udistrital.dominochaos.domain.GameMode
import com.udistrital.dominochaos.domain.GameResult
import com.udistrital.dominochaos.domain.Player
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json

class GameRepository {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val gamesRef: DatabaseReference = database.getReference(GAMES_NODE)

    private fun gameRef(code: String): DatabaseReference = gamesRef.child(code)

    private fun encode(game: Game): String =
        json.encodeToString(Game.serializer(), game)

    private fun decode(raw: String): Game? =
        runCatching { json.decodeFromString(Game.serializer(), raw) }.getOrNull()

    private fun decode(snapshot: DataSnapshot): Game? =
        snapshot.getValue(String::class.java)?.let { decode(it) }


    suspend fun createGame(
        code: String,
        host: Player,
        config: GameConfig = GameConfig(),
        mode: GameMode = GameMode.ONLINE
    ): Result<Unit> = runCatching {
        val game = GameEngine.createGame(
            code = code,
            host = host,
            config = config,
            mode = mode,
            now = System.currentTimeMillis()
        )
        gameRef(code).setValue(encode(game)).await()
    }

    fun joinGame(code: String, player: Player, onResult: (Throwable?) -> Unit) {
        val ref = gameRef(code)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val game = decode(snapshot)
                if (game == null) {
                    onResult(GameActionException(GameError.GAME_NOT_FOUND))
                    return
                }
                when (val result = GameEngine.join(game, player)) {
                    is GameResult.Success -> write(ref, result.game, onResult)
                    is GameResult.Failure -> onResult(result.asException())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(error.toException())
            }
        })
    }


    fun submitAction(
        code: String,
        playerId: String,
        action: GameAction,
        onResult: (Throwable?) -> Unit
    ) {
        val ref = gameRef(code)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val game = decode(snapshot)
                if (game == null) {
                    onResult(GameActionException(GameError.GAME_NOT_FOUND))
                    return
                }
                when (val result = GameEngine.applyAction(game, playerId, action)) {
                    is GameResult.Success -> write(ref, result.game, onResult)
                    is GameResult.Failure -> onResult(result.asException())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(error.toException())
            }
        })
    }

    fun leaveGame(code: String, playerId: String, onResult: (Throwable?) -> Unit) {
        if (code.isEmpty()) {
            onResult(null)
            return
        }
        val ref = gameRef(code)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val game = decode(snapshot)
                if (game == null || game.player(playerId) == null) {
                    onResult(null)
                    return
                }
                when (val result =
                    GameEngine.applyAction(game, playerId, GameAction.Abandon)
                ) {
                    is GameResult.Success -> {
                        val updated = result.game
                        val value = if (updated.players.isEmpty()) null else encode(updated)
                        ref.setValue(value)
                            .addOnSuccessListener { onResult(null) }
                            .addOnFailureListener { onResult(it) }
                    }

                    is GameResult.Failure -> onResult(result.asException())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(error.toException())
            }
        })
    }


    fun observeGame(code: String, onGame: (Game?) -> Unit): ValueEventListener {
        val ref = gameRef(code)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onGame(decode(snapshot))
            }

            override fun onCancelled(error: DatabaseError) {
                onGame(null)
            }
        }
        ref.addValueEventListener(listener)
        return listener
    }

    fun stopObserving(code: String, listener: ValueEventListener) {
        if (code.isEmpty()) return
        gameRef(code).removeEventListener(listener)
    }


    private fun write(ref: DatabaseReference, game: Game, onResult: (Throwable?) -> Unit) {
        ref.setValue(encode(game))
            .addOnSuccessListener { onResult(null) }
            .addOnFailureListener { onResult(it) }
    }

    private fun GameResult.Failure.asException(): GameActionException =
        GameActionException(error, detail ?: error.name)

    private companion object {
        const val GAMES_NODE = "games"
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}

class GameActionException(
    val error: GameError,
    detail: String = error.name
) : Exception(detail)
