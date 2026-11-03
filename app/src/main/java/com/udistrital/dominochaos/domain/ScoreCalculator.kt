package com.udistrital.dominochaos.domain


object ScoreCalculator {

    fun roundPoints(game: Game, winnerId: String): Int =
        game.players.filter { it.id != winnerId }.sumOf { it.pipsInHand }

    fun blockedRoundWinner(game: Game): String? {
        if (game.players.isEmpty()) return null
        val minPips = game.players.minOf { it.pipsInHand }
        val tied = game.players.filter { it.pipsInHand == minPips }
        return tied.singleOrNull()?.id
    }

    fun matchWinners(players: List<Player>): List<String> {
        if (players.isEmpty()) return emptyList()
        val maxScore = players.maxOf { it.score }
        val top = players.filter { it.score == maxScore }
        val sole = top.singleOrNull() ?: return run {
            val minPowers = top.minOf { it.powersUsed }
            top.filter { it.powersUsed == minPowers }.map { it.id }
        }
        return listOf(sole.id)
    }

    fun matchWinners(game: Game): List<String> = matchWinners(game.players)
}
