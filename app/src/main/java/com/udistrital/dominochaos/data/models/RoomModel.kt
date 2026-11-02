package com.udistrital.dominochaos.data.models

data class RoomModel(
    var id: String? = "",
    val player1: PlayerModel,
    val player2: PlayerModel? = null,
    val startAt: String,
    val winnerPlayerUid: String? = "",
    val turnPlayerUid: String = "",
    val finished: Boolean = false,
    val active: Boolean = true
)