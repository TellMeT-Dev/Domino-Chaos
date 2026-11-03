package com.udistrital.dominochaos.repositories

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.udistrital.dominochaos.data.models.PlayerModel
import com.udistrital.dominochaos.data.models.RoomModel

class RoomRepository {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val roomsRef: DatabaseReference = database.getReference("rooms")

    fun createRoom(hostPlayer: PlayerModel): RoomModel? {
        val newRoomRef = roomsRef.push()
        val caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val idRoom = (1..6).map { caracteres.random() }.joinToString("")

        val room = RoomModel(
            id = idRoom,
            player1 = hostPlayer,
            player2 = null,
            startAt = System.currentTimeMillis().toString(),
            winnerPlayerUid = null,
            turnPlayerUid = hostPlayer.uid,
            finished = false,
            active = true,
        )

        newRoomRef.setValue(room)
        return room
    }

    fun joinRoom(roomCode: String, guestPlayer: PlayerModel, onResult: (Boolean) -> Unit) {
        val code = roomCode.trim()
        if (code.isEmpty()) {
            onResult(false)
            return
        }

        roomsRef.orderByChild("id").equalTo(code)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        onResult(false)
                        return
                    }

                    var found = false
                    for (child in snapshot.children) {
                        val room = child.getValue(RoomModel::class.java)
                        if (room != null && room.player2 == null) {
                            found = true
                            val updates = mapOf(
                                "player2" to guestPlayer,
                                "turnPlayerUid" to room.player1.uid
                            )
                            child.key?.let { key ->
                                roomsRef.child(key).updateChildren(updates)
                                    .addOnSuccessListener { onResult(true) }
                                    .addOnFailureListener { onResult(false) }
                            } ?: onResult(false)
                            break
                        }
                    }
                    if (!found) {
                        onResult(false)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    onResult(false)
                }
            })
    }

//    fun joinRoom(roomCode:String, guestPlayer: PlayerModel, onResult: (Boolean) -> Unit){
//        roomsRef.orderByChild("id").equalTo(roomCode.trim())
//            .addListenerForSingleValueEvent(object : ValueEventListener{
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    onResult(snapshot.exists())
//                    for(child in snapshot.children){
//                        val room = child.getValue(RoomModel::class.java)
//                        if(room != null && room.player2 == null){
//                            val updates = mapOf(
//                                "player2" to guestPlayer,
//                                "turnPlayerUid" to room.player1.uid
//                            )
//                            roomsRef.child(child.key!!).updateChildren(updates)
//                                .addOnSuccessListener{onResult(true)}.addOnFailureListener { onResult(false) }
//                        }else{
//                            onResult(false)
//                        }
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    onResult(false)
//                }
//            })
//    }


}