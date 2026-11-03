package com.udistrital.dominochaos.domain

import kotlinx.serialization.Serializable

enum class Power { COMODIN, SALTO, INTERCAMBIO, INVERSION }

@Serializable
data class PowerUses(
    val comodin: Int = DEFAULT_USES,
    val salto: Int = DEFAULT_USES,
    val intercambio: Int = DEFAULT_USES,
    val inversion: Int = DEFAULT_USES
) {
    fun remaining(power: Power): Int = when (power) {
        Power.COMODIN -> comodin
        Power.SALTO -> salto
        Power.INTERCAMBIO -> intercambio
        Power.INVERSION -> inversion
    }

    fun consume(power: Power): PowerUses = when (power) {
        Power.COMODIN -> copy(comodin = (comodin - 1).coerceAtLeast(0))
        Power.SALTO -> copy(salto = (salto - 1).coerceAtLeast(0))
        Power.INTERCAMBIO -> copy(intercambio = (intercambio - 1).coerceAtLeast(0))
        Power.INVERSION -> copy(inversion = (inversion - 1).coerceAtLeast(0))
    }

    companion object {
        const val DEFAULT_USES = 2

        fun initial(uses: Int = DEFAULT_USES): PowerUses =
            PowerUses(comodin = uses, salto = uses, intercambio = uses, inversion = uses)
    }
}

object PowerManager {

    fun canUse(player: Player, power: Power): Boolean = player.powers.remaining(power) > 0

    fun consume(player: Player, power: Power): Player = player.copy(
        powers = player.powers.consume(power),
        powersUsed = player.powersUsed + 1
    )
}
