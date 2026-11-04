package com.udistrital.dominochaos.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.udistrital.dominochaos.data.UserSession
import com.udistrital.dominochaos.data.models.PlayerModel
import com.udistrital.dominochaos.data.models.RoomModel
import com.udistrital.dominochaos.domain.GameConfig
import com.udistrital.dominochaos.domain.GameMode
import com.udistrital.dominochaos.domain.Player
import com.udistrital.dominochaos.repositories.GameRepository
import com.udistrital.dominochaos.repositories.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RoomViewModel(
    private val repository: RoomRepository,
    private val gameRepository: GameRepository,
    private val session: UserSession
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _model = MutableStateFlow<RoomModel?>(null)
    val model: StateFlow<RoomModel?> = _model.asStateFlow()

    private var handle: RoomRepository.RoomHandle? = null
    private var roomListener: ValueEventListener? = null
    private var observedKey: String = ""


    fun createRoom(onResult: (Boolean) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            val host = PlayerModel(uid = user.uid, email = user.email.orEmpty())
            val created = repository.createRoom(host)
            val roomHandle = created.getOrNull()
            if (roomHandle == null) {
                onResult(false)
                return@launch
            }

            handle = roomHandle
            _model.value = roomHandle.room
            observe(roomHandle.key)

            val code = roomHandle.room.id.orEmpty()
            val gameCreated = gameRepository.createGame(
                code = code,
                host = Player(id = session.playerId, name = roomHandle.room.player1.email),
                config = GameConfig(),
                mode = GameMode.ONLINE
            )

            if (gameCreated.isFailure) {
                stopObserving()
                repository.deleteRoom(roomHandle.key)
                handle = null
                _model.value = null
            }

            onResult(gameCreated.isSuccess)
        }
    }


    fun joinRoom(roomCode: String, onResult: (Boolean) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false)
            return
        }

        val guest = PlayerModel(uid = user.uid, email = user.email.orEmpty())
        repository.joinRoom(roomCode, guest) { joined ->
            if (joined == null) {
                onResult(false)
                return@joinRoom
            }

            handle = joined
            _model.value = joined.room
            observe(joined.key)
            onResult(true)
        }
    }


    fun cancelRoom() {
        val current = handle ?: return
        val code = current.room.id.orEmpty()
        val isHost = auth.currentUser?.uid == current.room.player1.uid

        viewModelScope.launch {
            if (isHost) {
                repository.deleteRoom(current.key)
            } else {
                repository.freeSpot(current.key)
            }
        }
        gameRepository.leaveGame(code, session.playerId) { }

        stopObserving()
        handle = null
        _model.value = null
    }


    private fun observe(key: String) {
        stopObserving()
        observedKey = key
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val room = snapshot.getValue(RoomModel::class.java)
                _model.value = room ?: _model.value
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }
        roomListener = listener
        repository.observeRoom(key, listener)
    }

    private fun stopObserving() {
        roomListener?.let { repository.stopObserving(observedKey, it) }
        roomListener = null
        observedKey = ""
    }

    override fun onCleared() {
        stopObserving()
        super.onCleared()
    }
}

fun roomViewModelFactory(context: Context): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            RoomViewModel(RoomRepository(), GameRepository(), UserSession(context.applicationContext))
        }
    }
