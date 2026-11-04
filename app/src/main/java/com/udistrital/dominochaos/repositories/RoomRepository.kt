package com.udistrital.dominochaos.repositories

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.udistrital.dominochaos.data.models.PlayerModel
import com.udistrital.dominochaos.data.models.RoomModel
import kotlinx.coroutines.tasks.await

class RoomRepository {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val roomsRef: DatabaseReference = database.getReference(ROOMS_NODE)

    data class RoomHandle(val key: String, val room: RoomModel)

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

    fun joinRoom(roomCode: String, guestPlayer: PlayerModel, onResult: (RoomHandle?) -> Unit) {
        val code = roomCode.trim().uppercase()
        if (code.isEmpty()) {
            onResult(null)
            return
        }

        roomsRef.orderByChild(ID_FIELD).equalTo(code)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        onResult(null)
                        return
                    }

                    var found = false
                    for (child in snapshot.children) {
                        val room = child.getValue(RoomModel::class.java)
                        if (room != null && room.player2 == null) {
                            found = true
                            val updates = mapOf(
                                PLAYER2_FIELD to guestPlayer,
                                TURN_FIELD to room.player1.uid
                            )
                            val key = child.key
                            if (key == null) {
                                onResult(null)
                            } else {
                                roomsRef.child(key).updateChildren(updates)
                                    .addOnSuccessListener {
                                        onResult(
                                            RoomHandle(
                                                key = key,
                                                room = room.copy(player2 = guestPlayer)
                                            )
                                        )
                                    }
                                    .addOnFailureListener { onResult(null) }
                            }
                            break
                        }
                    }
                    if (!found) {
                        onResult(null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    onResult(null)
                }
            })
    }


    fun observeRoom(key: String, listener: ValueEventListener) {
        if (key.isEmpty()) return
        roomsRef.child(key).addValueEventListener(listener)
    }

    fun stopObserving(key: String, listener: ValueEventListener) {
        if (key.isEmpty()) return
        roomsRef.child(key).removeEventListener(listener)
    }


    suspend fun deleteRoom(key: String): Result<Unit> = runCatching {
        if (key.isNotEmpty()) roomsRef.child(key).removeValue().await()
    }

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
