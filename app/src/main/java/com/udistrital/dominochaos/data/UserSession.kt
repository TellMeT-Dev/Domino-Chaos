package com.udistrital.dominochaos.data

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID

/**
 * Sesión local del jugador.
 *
 * [playerId] es el identificador usado dentro de la partida: se toma de
 * Firebase Auth para que coincida con el `uid` de la sala. Solo se usa un
 * UUID local como último recurso (sin sesión iniciada).
 */
class UserSession(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    val playerId: String
        get() = auth.currentUser?.uid
            ?: prefs.getString(KEY_ID, null)
            ?: UUID.randomUUID().toString().also { id ->
                prefs.edit().putString(KEY_ID, id).apply()
            }

    val playerName: String?
        get() = prefs.getString(KEY_NAME, null) ?: auth.currentUser?.email

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    fun login(name: String) {
        prefs.edit().putString(KEY_NAME, name.trim()).apply()
    }

    fun logout() {
        prefs.edit().remove(KEY_NAME).apply()
    }

    private companion object {
        const val PREFS_NAME = "dominocaos_session"
        const val KEY_ID = "player_id"
        const val KEY_NAME = "player_name"
    }
}
