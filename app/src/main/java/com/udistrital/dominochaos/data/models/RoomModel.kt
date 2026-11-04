package com.udistrital.dominochaos.data.models

data class RoomModel(
    var id: String? = "",
    var player1: PlayerModel = PlayerModel(),
    var player2: PlayerModel? = null,
    var startAt: String = "",
    var winnerPlayerUid: String? = "",
    var turnPlayerUid: String = "",
    var finished: Boolean = false,
    var active: Boolean = true
)
