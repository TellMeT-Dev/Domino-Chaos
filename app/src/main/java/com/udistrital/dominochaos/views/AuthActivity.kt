package com.udistrital.dominochaos.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.udistrital.dominochaos.R
import com.udistrital.dominochaos.data.UserSession
import com.udistrital.dominochaos.views.screens.LoginScreen

class AuthActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var session: UserSession

    private val TAG = "signUD"

    public override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            goToGame()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        auth = Firebase.auth
        session = UserSession(this)

        super.onCreate(savedInstanceState)
        setContent {
            LoginScreen(
                onLoginClick = { email, password -> signIn(email, password) }
            )
        }
    }

    private fun signIn(email: String, password: String) {
        if (!credentialsOk(email, password)) return

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    session.login(email)
                    goToGame()
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    toast(R.string.error_auth)
                }
            }
    }

    private fun register(email: String, password: String) {
        if (!credentialsOk(email, password)) return

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    session.login(email)
                    goToGame()
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    toast(R.string.error_register)
                }
            }
    }

    private fun credentialsOk(email: String, password: String): Boolean {
        if (email.isBlank() || password.isBlank()) {
            toast(R.string.error_empty_credentials)
            return false
        }
        if (password.length < 6) {
            toast(R.string.error_short_password)
            return false
        }
        return true
    }

    private fun goToGame() {
        startActivity(Intent(this, GameActivity::class.java))
        finish()
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(baseContext, getString(messageRes), Toast.LENGTH_SHORT).show()
    }
}
