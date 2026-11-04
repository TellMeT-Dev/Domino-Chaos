package com.udistrital.dominochaos.domain

import kotlinx.serialization.Serializable
@Serializable
data class DominoTile(
    val id: String,
    val a: Int,
    val b: Int
) {
    val isDouble: Boolean get() = a == b
    val pips: Int get() = a + b

    fun matches(value: Int): Boolean = a == value || b == value

    fun reversed(): DominoTile = copy(a = b, b = a)

    companion object {
        fun newDeck(): List<DominoTile> =
            (0..6).flatMap { x -> (x..6).map { y -> DominoTile(id = "$x-$y", a = x, b = y) } }
    }
}
