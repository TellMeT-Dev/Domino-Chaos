package com.udistrital.dominochaos.domain

import kotlin.random.Random

enum class GameError {
    GAME_NOT_FOUND,
    NOT_IN_GAME,
    NOT_YOUR_TURN,
    GAME_NOT_ACTIVE,
    GAME_FINISHED,
    ROOM_FULL,
    ALREADY_IN_GAME,
    NOT_HOST,
    NOT_ENOUGH_PLAYERS,
    TILE_NOT_IN_HAND,
    NO_LEGAL_END,
    CANNOT_DRAW,
    CANNOT_PASS,
    NO_POWERS_LEFT,
    INVALID_TARGET,
    ROUND_NOT_OVER,
    INVALID_ACTION
}

sealed class GameResult {
    abstract val game: Game

    data class Success(override val game: Game) : GameResult()

    data class Failure(
        override val game: Game,
        val error: GameError,
        val detail: String? = null
    ) : GameResult()

    val isSuccess: Boolean get() = this is Success

    fun errorOrNull(): GameError? = (this as? Failure)?.error
}

object GameEngine {

    // ---------------------------------------------------------------------
    // Creación y membresía
    // ---------------------------------------------------------------------

    fun createGame(
        code: String,
        host: Player,
        config: GameConfig = GameConfig(),
        mode: GameMode = GameMode.ONLINE,
        now: Long = 0L
    ): Game {
        val seated = host.seated(config)
        return Game(
            code = code,
            hostId = seated.id,
            status = GameStatus.WAITING,
            mode = mode,
            config = config,
            players = listOf(seated),
            createdAt = now
        )
    }

    fun join(game: Game, player: Player, rng: Random = Random.Default): GameResult {
        if (game.status != GameStatus.WAITING) return failure(game, GameError.GAME_NOT_ACTIVE)
        if (game.players.any { it.id == player.id }) return failure(game, GameError.ALREADY_IN_GAME)
        if (game.players.size >= game.config.totalPlayers) return failure(game, GameError.ROOM_FULL)
        val joined = game.copy(players = game.players + player.seated(game.config))
        return if (joined.players.size >= joined.config.totalPlayers) {
            Success(deal(joined, rng))
        } else {
            Success(joined)
        }
    }

    fun applyAction(
        game: Game,
        playerId: String,
        action: GameAction,
        rng: Random = Random.Default
    ): GameResult {
        if (game.player(playerId) == null) return failure(game, GameError.NOT_IN_GAME)
        if (game.status == GameStatus.FINISHED &&
            action !is GameAction.RestartMatch &&
            action !is GameAction.Abandon
        ) {
            return failure(game, GameError.GAME_FINISHED)
        }

        return when (action) {
            GameAction.StartMatch -> startMatch(game, playerId, rng)
            GameAction.StartNextRound -> startNextRound(game, rng)
            GameAction.RestartMatch -> restart(game, rng)
            GameAction.Abandon -> Success(abandon(game, playerId))

            is GameAction.Play, GameAction.Draw, GameAction.Pass, GameAction.UseSalto,
            is GameAction.UseIntercambio, GameAction.UseInversion -> {
                if (game.status != GameStatus.IN_PROGRESS) return failure(game, GameError.GAME_NOT_ACTIVE)
                if (!game.isTurn(playerId)) return failure(game, GameError.NOT_YOUR_TURN)
                val player = game.playerOrThrow(playerId)
                when (action) {
                    is GameAction.Play -> play(game, player, action)
                    GameAction.Draw -> draw(game, player)
                    GameAction.Pass -> pass(game, player)
                    GameAction.UseSalto -> salto(game, player)
                    is GameAction.UseIntercambio -> intercambio(game, player, action)
                    GameAction.UseInversion -> inversion(game, player)
                    else -> failure(game, GameError.INVALID_ACTION)
                }
            }

            else -> failure(game, GameError.INVALID_ACTION)
        }
    }

    // ---------------------------------------------------------------------
    // Acciones del anfitrión y de ronda
    // ---------------------------------------------------------------------

    private fun startMatch(game: Game, playerId: String, rng: Random): GameResult {
        if (game.status != GameStatus.WAITING) return failure(game, GameError.GAME_NOT_ACTIVE)
        if (game.hostId != playerId) return failure(game, GameError.NOT_HOST)
        if (game.players.size < 2) return failure(game, GameError.NOT_ENOUGH_PLAYERS)
        val reduced = game.copy(config = game.config.copy(totalPlayers = game.players.size))
        return Success(deal(reduced.copy(turnIndex = 0), rng))
    }

    private fun startNextRound(game: Game, rng: Random): GameResult {
        if (game.status != GameStatus.ROUND_OVER) return failure(game, GameError.ROUND_NOT_OVER)
        if (game.round >= game.config.maxRounds) return failure(game, GameError.GAME_FINISHED)
        val nextRound = game.round + 1
        val startTurn = (nextRound - 1) % game.players.size
        val next = game.copy(round = nextRound, turnIndex = startTurn, winnerIds = emptyList())
        return Success(deal(next, rng))
    }

    private fun restart(game: Game, rng: Random): GameResult {
        if (game.players.size < 2) return failure(game, GameError.NOT_ENOUGH_PLAYERS)
        val reset = game.copy(
            status = GameStatus.IN_PROGRESS,
            players = game.players.map { it.seated(game.config) },
            board = Board(),
            pozo = emptyList(),
            turnIndex = 0,
            round = 1,
            consecutivePasses = 0,
            turnsTaken = 0,
            roundResults = emptyList(),
            winnerIds = emptyList()
        )
        return Success(deal(reset, rng))
    }


    private fun abandon(game: Game, playerId: String): Game {
        if (game.status == GameStatus.FINISHED) return game
        val others = game.players.filterNot { it.id == playerId }
        return when {
            game.status == GameStatus.WAITING -> {
                if (others.isEmpty()) game.copy(players = emptyList())
                else {
                    val newHost = if (game.hostId == playerId) others.first().id else game.hostId
                    game.copy(players = others, hostId = newHost)
                }
            }

            others.size < 2 -> game.copy(status = GameStatus.FINISHED, winnerIds = others.map { it.id })

            else -> game.copy(
                status = GameStatus.FINISHED,
                winnerIds = ScoreCalculator.matchWinners(others)
            )
        }
    }

    private fun deal(game: Game, rng: Random): Game {
        val deck = DominoTile.newDeck().shuffled(rng)
        var cursor = 0
        val perPlayer = game.config.tilesPerPlayer
        val players = game.players.map { player ->
            val hand = deck.subList(cursor, cursor + perPlayer)
            cursor += perPlayer
            player.copy(hand = hand)
        }
        return game.copy(
            status = GameStatus.IN_PROGRESS,
            players = players,
            pozo = deck.drop(cursor),
            board = Board(),
            consecutivePasses = 0,
            turnsTaken = 0,
            winnerIds = emptyList()
        )
    }

    // ---------------------------------------------------------------------
    // Jugadas
    // ---------------------------------------------------------------------

    private fun play(
        game: Game,
        player: Player,
        action: GameAction.Play
    ): GameResult {
        val tile = player.hand.find { it.id == action.tileId }
            ?: return failure(game, GameError.TILE_NOT_IN_HAND)

        val natural = game.board.naturalEnds(tile)
        val needsWild = action.end !in natural

        if (needsWild && !action.useComodin) return failure(game, GameError.NO_LEGAL_END)
        if (needsWild && !PowerManager.canUse(player, Power.COMODIN)) {
            return failure(game, GameError.NO_POWERS_LEFT)
        }

        val orientation = when {
            game.board.isEmpty -> action.reversed
            needsWild -> if (action.end == End.RIGHT) action.outwardIsA else !action.outwardIsA
            else -> game.board.orientationFor(tile, action.end) ?: return failure(game, GameError.NO_LEGAL_END)
        }

        val players = game.players.map {
            if (it.id != player.id) it
            else {
                var updated = it.copy(hand = it.hand.filterNot { t -> t.id == tile.id })
                if (needsWild) {
                    updated = PowerManager.consume(updated, Power.COMODIN)
                }
                updated
            }
        }

        val next = game.copy(
            players = players,
            board = game.board.withTile(tile, action.end, orientation),
            consecutivePasses = 0
        )

        return if (players.first { it.id == player.id }.hand.isEmpty()) {
            // Se fue: fin de ronda por salida.
            Success(finishRound(next, player.id, RoundEndReason.WENT_OUT))
        } else {
            Success(endTurn(next))
        }
    }

    private fun draw(game: Game, player: Player): GameResult {
        if (game.pozo.isEmpty()) return failure(game, GameError.CANNOT_DRAW)
        if (hasNaturalMove(game, player)) return failure(game, GameError.CANNOT_DRAW)
        val tile = game.pozo.first()
        val players = game.players.map {
            if (it.id == player.id) it.copy(hand = it.hand + tile) else it
        }
        return Success(game.copy(players = players, pozo = game.pozo.drop(1)))
    }

    private fun pass(game: Game, player: Player): GameResult {
        if (game.pozo.isNotEmpty()) return failure(game, GameError.CANNOT_PASS)
        if (hasNaturalMove(game, player)) return failure(game, GameError.CANNOT_PASS)

        val passes = game.consecutivePasses + 1
        if (passes >= game.players.size) {
            val winnerId = ScoreCalculator.blockedRoundWinner(game)
            val reason = if (winnerId == null) RoundEndReason.VOID else RoundEndReason.BLOCKED
            return Success(finishRound(game, winnerId, reason))
        }
        return Success(endTurn(game.copy(consecutivePasses = passes)))
    }

    private fun salto(game: Game, player: Player): GameResult {
        if (!PowerManager.canUse(player, Power.SALTO)) return failure(game, GameError.NO_POWERS_LEFT)
        val players = game.players.map {
            if (it.id == player.id) PowerManager.consume(it, Power.SALTO) else it
        }
        return Success(endTurn(game.copy(players = players), skipExtra = 1))
    }

    private fun intercambio(
        game: Game,
        player: Player,
        action: GameAction.UseIntercambio
    ): GameResult {
        if (!PowerManager.canUse(player, Power.INTERCAMBIO)) {
            return failure(game, GameError.NO_POWERS_LEFT)
        }
        if (action.targetPlayerId == player.id) return failure(game, GameError.INVALID_TARGET)
        val target = game.player(action.targetPlayerId) ?: return failure(game, GameError.INVALID_TARGET)
        if (target.hand.isEmpty()) return failure(game, GameError.INVALID_TARGET)
        val mine = player.hand.find { it.id == action.myTileId } ?: return failure(game, GameError.TILE_NOT_IN_HAND)
        val theirs = target.hand.find { it.id == action.targetTileId } ?: return failure(game, GameError.TILE_NOT_IN_HAND)

        val players = game.players.map {
            when (it.id) {
                player.id -> PowerManager.consume(
                    it.copy(hand = it.hand.filterNot { t -> t.id == mine.id } + theirs),
                    Power.INTERCAMBIO
                )

                target.id -> it.copy(hand = it.hand.filterNot { t -> t.id == theirs.id } + mine)

                else -> it
            }
        }
        return Success(endTurn(game.copy(players = players, consecutivePasses = 0)))
    }

    private fun inversion(game: Game, player: Player): GameResult {
        if (!PowerManager.canUse(player, Power.INVERSION)) return failure(game, GameError.NO_POWERS_LEFT)
        if (game.board.isEmpty) return failure(game, GameError.INVALID_ACTION)
        val players = game.players.map {
            if (it.id == player.id) PowerManager.consume(it, Power.INVERSION) else it
        }
        return Success(
            game.copy(
                players = players,
                board = game.board.reversed()
            )
        )
    }

    // ---------------------------------------------------------------------
    // Turnos y fin de ronda
    // ---------------------------------------------------------------------

    private fun endTurn(game: Game, skipExtra: Int = 0): Game {
        val newTurn = (game.turnIndex + 1 + skipExtra) % game.players.size
        return game.copy(
            turnIndex = newTurn,
            turnsTaken = game.turnsTaken + 1
        )
    }

    private fun finishRound(
        game: Game,
        winnerId: String?,
        reason: RoundEndReason
    ): Game {
        val points = if (winnerId == null) 0 else ScoreCalculator.roundPoints(game, winnerId)
        val players = if (winnerId == null) game.players else game.players.map {
            if (it.id == winnerId) it.copy(score = it.score + points) else it
        }
        val results = game.roundResults + RoundResult(
            round = game.round,
            winnerId = winnerId,
            points = points,
            reason = reason
        )
        val matchOver = game.round >= game.config.maxRounds
        return game.copy(
            players = players,
            roundResults = results,
            status = if (matchOver) GameStatus.FINISHED else GameStatus.ROUND_OVER,
            winnerIds = if (matchOver) ScoreCalculator.matchWinners(players) else emptyList(),
            consecutivePasses = 0
        )
    }

    // ---------------------------------------------------------------------
    // Consultas para la interfaz (solo lectura, sin cambiar el estado)
    // ---------------------------------------------------------------------

    fun hasNaturalMove(game: Game, player: Player): Boolean =
        player.hand.any { tile -> game.board.naturalEnds(tile).isNotEmpty() }

    fun legalEndsFor(game: Game, playerId: String, tileId: String, useComodin: Boolean): List<End> {
        val player = game.player(playerId) ?: return emptyList()
        if (game.status != GameStatus.IN_PROGRESS || game.currentPlayer?.id != playerId) return emptyList()
        val tile = player.hand.find { it.id == tileId } ?: return emptyList()
        return when {
            useComodin && PowerManager.canUse(player, Power.COMODIN) -> listOf(End.LEFT, End.RIGHT)
            else -> game.board.naturalEnds(tile)
        }
    }

    fun turnOptions(game: Game, playerId: String): TurnOptions {
        val player = game.player(playerId)
        val yourTurn = game.status == GameStatus.IN_PROGRESS && player != null && game.isTurn(playerId)
        if (player == null || !yourTurn) {
            return TurnOptions(isYourTurn = false)
        }
        val canMove = hasNaturalMove(game, player)
        return TurnOptions(
            isYourTurn = true,
            canDraw = !canMove && game.pozo.isNotEmpty(),
            canPass = !canMove && game.pozo.isEmpty(),
            canSalto = PowerManager.canUse(player, Power.SALTO),
            canIntercambio = PowerManager.canUse(player, Power.INTERCAMBIO) &&
                    game.players.any { it.id != player.id && it.hand.isNotEmpty() },
            canInversion = PowerManager.canUse(player, Power.INVERSION) && !game.board.isEmpty,
            canUseComodin = PowerManager.canUse(player, Power.COMODIN),
            playableTileIds = player.hand
                .filter { tile -> game.board.naturalEnds(tile).isNotEmpty() }
                .map { it.id }
        )
    }
}

data class TurnOptions(
    val isYourTurn: Boolean = false,
    val canDraw: Boolean = false,
    val canPass: Boolean = false,
    val canSalto: Boolean = false,
    val canIntercambio: Boolean = false,
    val canInversion: Boolean = false,
    val canUseComodin: Boolean = false,
    val playableTileIds: List<String> = emptyList()
)

private fun failure(game: Game, error: GameError, detail: String? = null) =
    GameResult.Failure(game, error, detail)

private typealias Success = GameResult.Success
