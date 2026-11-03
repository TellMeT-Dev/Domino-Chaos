package com.udistrital.dominochaos.data.models

/**
 * Modelo de sala en Realtime Database.
 *
 * Todos los campos son `var` y con valor por defecto: Firebase rellena los
 * objetos con reflexión y solo sabe escribir propiedades con setter
 * (los `val` quedarían siempre con su valor por defecto al leer).
 */
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
