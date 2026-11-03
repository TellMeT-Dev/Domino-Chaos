package com.udistrital.dominochaos.views

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.browser.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.udistrital.dominochaos.data.enums.TypeScreen
import com.udistrital.dominochaos.data.models.PlayerModel
import com.udistrital.dominochaos.viewmodels.RoomViewModel
import com.udistrital.dominochaos.views.screens.GameScreen
import com.udistrital.dominochaos.views.screens.LobbyScreen
import com.udistrital.dominochaos.views.screens.RoomSelectionScreen
import com.udistrital.dominochaos.views.screens.SearchScreen
import com.udistrital.dominochaos.views.theme.DominoChaosTheme
import com.udistrital.hockeygame.views.screens.HomeScreen
import kotlin.getValue

class GameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DominoChaosTheme() {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var currentScreen by remember { mutableStateOf(TypeScreen.HOME) }

                    val roomViewModel: RoomViewModel by viewModels()
                    val model by roomViewModel.model.collectAsState()

                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when (currentScreen) {
                            TypeScreen.HOME -> {
                                HomeScreen(
                                    onClickGame = {
                                        currentScreen = TypeScreen.GAME
                                    },
                                    onClickMultiplayer = {
                                        currentScreen = TypeScreen.ROOM_SELECTION
                                    }
                                )
                            }

                            TypeScreen.ROOM_SELECTION -> {
                                RoomSelectionScreen(
                                    onCreateRoomClick = {
                                        roomViewModel.createRoom()
                                        currentScreen = TypeScreen.LOBBY
                                    },
                                    onJoinOpenRoomClick = {
                                        currentScreen = TypeScreen.JOIN
                                    }

                                )
                            }

                            TypeScreen.LOBBY -> {
                                LobbyScreen(
                                    roomCode = model?.id,
                                    hostName = model?.player1?.email ?: "",
                                    guestName = model?.player2?.email,
                                    onCancelClick = {
                                        currentScreen = TypeScreen.ROOM_SELECTION
                                    },
                                    onStartGameClick = {
                                        currentScreen = TypeScreen.GAME
                                    }
                                )
                            }

                            TypeScreen.GAME -> {
                                GameScreen {
                                    currentScreen = TypeScreen.HOME
                                }
                            }

                            TypeScreen.JOIN -> {
                                SearchScreen (
                                    onCancelClick = {
                                        currentScreen = TypeScreen.ROOM_SELECTION
                                    },
                                    onJoinGameClick = { code ->
                                        roomViewModel.joinRoom(code) {
                                            valid ->

                                            if(valid){
                                                currentScreen = TypeScreen.GAME
                                            } else {
                                                Toast.makeText(
                                                    baseContext,
                                                    "Invalid Session",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
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
    }
}