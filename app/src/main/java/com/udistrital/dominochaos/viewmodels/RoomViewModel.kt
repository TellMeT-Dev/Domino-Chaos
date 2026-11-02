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

    fun joinOpenRoom(roomCode: String) {
        val currentUser = auth.currentUser ?: return
        val guest = PlayerModel(
            uid = currentUser.uid,
            email = currentUser.email.orEmpty()
        )
        repository.joinOpenRoom(guest, roomCode) { room ->
            _model.value = room
        }
    }
}