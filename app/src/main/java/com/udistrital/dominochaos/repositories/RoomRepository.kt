package com.udistrital.dominochaos.repositories

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.udistrital.dominochaos.data.await
import com.udistrital.dominochaos.data.models.PlayerModel
import com.udistrital.dominochaos.data.models.RoomModel

/**
 * Salas de espera en Realtime Database (`rooms/{pushKey}`).
 * El código de sala es el campo `id` de [RoomModel].
 */
class RoomRepository {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val roomsRef: DatabaseReference = database.getReference(ROOMS_NODE)

    /** Referencia conocida de una sala: clave interna + datos. */
    data class RoomHandle(val key: String, val room: RoomModel)

    // ---------------------------------------------------------------------
    // Crear / unirse
    // ---------------------------------------------------------------------

    suspend fun createRoom(hostPlayer: PlayerModel): Result<RoomHandle> = runCatching {
        val newRoomRef = roomsRef.push()
        val room = RoomModel(
            id = randomCode(),
            player1 = hostPlayer,
            player2 = null,
            startAt = System.currentTimeMillis().toString(),
            winnerPlayerUid = null,
            turnPlayerUid = hostPlayer.uid,
            finished = false,
            active = true,
        )
        newRoomRef.setValue(room).await()
        RoomHandle(key = newRoomRef.key.orEmpty(), room = room)
    }

    suspend fun joinRoom(roomCode: String, guestPlayer: PlayerModel): Result<RoomHandle> =
        runCatching {
            val code = roomCode.trim().uppercase()
            require(code.isNotEmpty()) { "Código vacío" }

            val snapshot = roomsRef.orderByChild(ID_FIELD).equalTo(code).get().await()
            val child = snapshot.children.firstOrNull()
                ?: throw NoSuchElementException("Sala no encontrada")

            val room = child.getValue(RoomModel::class.java)
                ?: throw IllegalStateException("Sala ilegible")

            if (room.player2 != null) throw IllegalStateException("Sala llena")

            val key = child.key.orEmpty()
            val updates = mapOf(
                PLAYER2_FIELD to guestPlayer,
                TURN_FIELD to room.player1.uid
            )
            roomsRef.child(key).updateChildren(updates).await()

            RoomHandle(key = key, room = room.copy(player2 = guestPlayer))
        }

    // ---------------------------------------------------------------------
    // Observar cambios (para que el anfitrión vea al invitado)
    // ---------------------------------------------------------------------

    fun observeRoom(key: String, listener: ValueEventListener) {
        if (key.isEmpty()) return
        roomsRef.child(key).addValueEventListener(listener)
    }

    fun stopObserving(key: String, listener: ValueEventListener) {
        if (key.isEmpty()) return
        roomsRef.child(key).removeEventListener(listener)
    }

    // ---------------------------------------------------------------------
    // Salir / limpiar
    // ---------------------------------------------------------------------

    /** El anfitrión cancela: se borra la sala entera. */
    suspend fun deleteRoom(key: String): Result<Unit> = runCatching {
        if (key.isNotEmpty()) roomsRef.child(key).removeValue().await()
    }

    /** Un invitado se va: se libera la plaza de la sala. */
    suspend fun freeSpot(key: String): Result<Unit> = runCatching {
        if (key.isNotEmpty()) {
            roomsRef.child(key).child(PLAYER2_FIELD).setValue(null).await()
        }
    }

    private fun randomCode(): String {
        val chars = CODE_ALPHABET
        return (1..CODE_LENGTH).map { chars.random() }.joinToString("")
    }

    private companion object {
        const val ROOMS_NODE = "rooms"
        const val ID_FIELD = "id"
        const val PLAYER2_FIELD = "player2"
        const val TURN_FIELD = "turnPlayerUid"
        const val CODE_LENGTH = 6
        const val CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    }
}
