package com.udistrital.dominochaos.data.models

/** Jugador dentro de una sala (solo identificadores, no la partida). */
data class PlayerModel(
    var uid: String = "",
    var email: String = ""
)
