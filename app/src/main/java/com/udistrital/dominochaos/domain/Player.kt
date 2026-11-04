package com.udistrital.dominochaos.domain

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String,
    val name: String,
    val hand: List<DominoTile> = emptyList(),
    val score: Int = 0,
    val powers: PowerUses = PowerUses(),
    val powersUsed: Int = 0
) {
    val pipsInHand: Int get() = hand.sumOf { it.pips }

    fun seated(config: GameConfig): Player = copy(
        hand = emptyList(),
        score = 0,
        powers = PowerUses.initial(config.powerUses),
        powersUsed = 0
    )
}
