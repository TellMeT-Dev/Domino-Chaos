package com.udistrital.dominochaos.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class UserSession(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dominocaos_session", Context.MODE_PRIVATE)

    val playerId: String
        get() = prefs.getString(KEY_ID, null) ?: UUID.randomUUID().toString().also { id ->
            prefs.edit().putString(KEY_ID, id).apply()
        }

    val playerName: String?
        get() = prefs.getString(KEY_NAME, null)

    val isLoggedIn: Boolean
        get() = !playerName.isNullOrBlank()

    fun login(name: String) {
        prefs.edit().putString(KEY_NAME, name.trim()).apply()
    }

    fun logout() {
        prefs.edit().remove(KEY_NAME).apply()
    }

    private companion object {
        const val KEY_ID = "player_id"
        const val KEY_NAME = "player_name"
    }
}
