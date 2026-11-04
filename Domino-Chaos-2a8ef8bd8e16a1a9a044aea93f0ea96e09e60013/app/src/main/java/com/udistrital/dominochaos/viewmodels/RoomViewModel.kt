package com.udistrital.dominochaos.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.udistrital.dominochaos.data.models.PlayerModel
import com.udistrital.dominochaos.data.models.RoomModel
import com.udistrital.dominochaos.repositories.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RoomViewModel : ViewModel() {
    private val repository = RoomRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _model = MutableStateFlow<RoomModel?>(null)
    val model: StateFlow<RoomModel?> = _model.asStateFlow()

    fun createRoom() {
        val currentUser = auth.currentUser ?: return
        val host = PlayerModel(
            uid = currentUser.uid,
            email = currentUser.email.orEmpty()
        )
        val room = repository.createRoom(host)
        _model.value = room
    }

    fun joinRoom(roomCode: String, onResult: (Boolean) -> Unit){
        val currentUser = auth.currentUser ?: return
        val guestPlayer = PlayerModel(
            uid = currentUser.uid,
            email = currentUser.email.orEmpty()
        )
        repository.joinRoom(roomCode, guestPlayer, onResult)
    }
}