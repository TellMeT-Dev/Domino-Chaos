package com.udistrital.dominochaos.views

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.udistrital.dominochaos.R
import com.udistrital.dominochaos.data.enums.TypeScreen
import com.udistrital.dominochaos.viewmodels.GameViewModel
import com.udistrital.dominochaos.viewmodels.RoomViewModel
import com.udistrital.dominochaos.viewmodels.gameViewModelFactory
import com.udistrital.dominochaos.viewmodels.roomViewModelFactory
import com.udistrital.dominochaos.views.screens.GameScreen
import com.udistrital.dominochaos.views.screens.HomeScreen
import com.udistrital.dominochaos.views.screens.LobbyScreen
import com.udistrital.dominochaos.views.screens.RoomSelectionScreen
import com.udistrital.dominochaos.views.screens.SearchScreen
import com.udistrital.dominochaos.views.theme.DominoChaosTheme

class GameActivity : ComponentActivity() {

    private val roomViewModel: RoomViewModel by viewModels { roomViewModelFactory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DominoChaosTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var currentScreen by rememberSaveable { mutableStateOf(TypeScreen.HOME) }
                    var gameCode by rememberSaveable { mutableStateOf<String?>(null) }
                    val room by roomViewModel.model.collectAsStateWithLifecycle()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            TypeScreen.HOME -> HomeScreen(
                                onClickMultiplayer = {
                                    currentScreen = TypeScreen.ROOM_SELECTION
                                },
                                onLogout = { logout() }
                            )

                            TypeScreen.ROOM_SELECTION -> RoomSelectionScreen(
                                onCreateRoomClick = {
                                    roomViewModel.createRoom { created ->
                                        if (created) {
                                            gameCode = roomViewModel.model.value?.id
                                            currentScreen = TypeScreen.LOBBY
                                        } else {
                                            toast(R.string.error_create_room)
                                        }
                                    }
                                },
                                onJoinOpenRoomClick = {
                                    currentScreen = TypeScreen.JOIN
                                }
                            )

                            TypeScreen.LOBBY -> LobbyScreen(
                                roomCode = room?.id,
                                hostName = room?.player1?.email ?: "",
                                guestName = room?.player2?.email,
                                onCancelClick = {
                                    roomViewModel.cancelRoom()
                                    gameCode = null
                                    currentScreen = TypeScreen.ROOM_SELECTION
                                },
                                onStartGameClick = {
                                    currentScreen = TypeScreen.GAME
                                }
                            )

                            TypeScreen.GAME -> {
                                val code = gameCode
                                if (code.isNullOrEmpty()) {
                                    NoGameScreen(onBack = {
                                        currentScreen = TypeScreen.HOME
                                    })
                                } else {
                                    val factory = remember(code) {
                                        gameViewModelFactory(this@GameActivity, code)
                                    }
                                    val gameViewModel: GameViewModel =
                                        viewModel(key = "game_$code", factory = factory)

                                    GameScreen(
                                        vm = gameViewModel,
                                        onExit = {
                                            roomViewModel.cancelRoom()
                                            gameCode = null
                                            currentScreen = TypeScreen.HOME
                                        }
                                    )
                                }
                            }

                            TypeScreen.JOIN -> SearchScreen(
                                onCancelClick = {
                                    currentScreen = TypeScreen.ROOM_SELECTION
                                },
                                onJoinGameClick = { code ->
                                    roomViewModel.joinRoom(code) { valid ->
                                        if (valid) {
                                            gameCode = code.trim().uppercase()
                                            currentScreen = TypeScreen.GAME
                                        } else {
                                            toast(R.string.error_join_room)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(baseContext, getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }
}

@Composable
private fun NoGameScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(R.string.no_active_game))
            TextButton(onClick = onBack) { Text(text = stringResource(R.string.back_to_home)) }
        }
    }
}
