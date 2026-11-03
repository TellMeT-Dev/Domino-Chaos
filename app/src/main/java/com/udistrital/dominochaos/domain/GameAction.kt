package com.udistrital.dominochaos.domain

import kotlinx.serialization.Serializable
@Serializable
sealed class GameAction {

    @Serializable
    data class Play(
        val tileId: String,
        val end: End,
        val useComodin: Boolean = false,
        val reversed: Boolean = false,
        val outwardIsA: Boolean = false
    ) : GameAction()

    @Serializable
    data object Draw : GameAction()

    @Serializable
    data object Pass : GameAction()

    @Serializable
    data object UseSalto : GameAction()

    @Serializable
    data class UseIntercambio(
        val targetPlayerId: String,
        val targetTileId: String,
        val myTileId: String
    ) : GameAction()

    @Serializable
    data object UseInversion : GameAction()

    @Serializable
    data object StartMatch : GameAction()

    @Serializable
    data object StartNextRound : GameAction()

    @Serializable
    data object RestartMatch : GameAction()

    @Serializable
    data object Abandon : GameAction()
}
