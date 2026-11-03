package com.udistrital.dominochaos.domain

import kotlinx.serialization.Serializable

enum class End { LEFT, RIGHT }


@Serializable
data class PlacedTile(val tile: DominoTile, val reversed: Boolean = false) {

    val leftValue: Int get() = if (reversed) tile.b else tile.a


    val rightValue: Int get() = if (reversed) tile.a else tile.b
}
@Serializable
data class Board(val tiles: List<PlacedTile> = emptyList()) {

    val isEmpty: Boolean get() = tiles.isEmpty()

    val leftEnd: Int? get() = tiles.firstOrNull()?.leftValue

    val rightEnd: Int? get() = tiles.lastOrNull()?.rightValue

    fun reversed(): Board = Board(tiles.asReversed().map { it.copy(reversed = !it.reversed) })

    fun withTile(tile: DominoTile, end: End, reversed: Boolean): Board = when {
        isEmpty -> Board(listOf(PlacedTile(tile, reversed)))
        end == End.RIGHT -> Board(tiles + PlacedTile(tile, reversed))
        else -> Board(listOf(PlacedTile(tile, reversed)) + tiles)
    }

    fun orientationFor(tile: DominoTile, end: End): Boolean? {
        if (isEmpty) return null
        return when (end) {
            End.RIGHT -> {
                val target = rightEnd ?: return null
                when {
                    tile.a == target -> false
                    tile.b == target -> true
                    else -> null
                }
            }
            End.LEFT -> {
                val target = leftEnd ?: return null
                when {
                    tile.b == target -> false
                    tile.a == target -> true
                    else -> null
                }
            }
        }
    }

    fun naturalEnds(tile: DominoTile): List<End> = when {
        isEmpty -> listOf(End.LEFT, End.RIGHT)
        else -> listOf(End.LEFT, End.RIGHT).filter { orientationFor(tile, it) != null }
    }
}
