package com.example.groupformer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

class SignInActivity : ComponentActivity() {
    companion object {
        private const val TAG = "SignInActivity"
    }

    private val activityScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SignInScreen()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }

    // Function to generate a hashed nonce (SHA-256)
    private fun generateHashedNonce(): String {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    // Handle sign-in failure
    private fun handleFailure(e: GetCredentialException) {
        Log.e(TAG, "Error getting credential", e)
        Toast.makeText(this, "Error getting credential", Toast.LENGTH_SHORT).show()
    }

    // Handle successful sign-in and navigate to MainActivity
    private fun handleSignIn(result: GetCredentialResponse) {
        val credential = result.credential

        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        Log.d(TAG, "Received Google ID token: ${googleIdTokenCredential.idToken.take(10)}")

                        // Save token to SharedPreferences
                        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putString("google_id_token", googleIdTokenCredential.idToken)
                        editor.apply()

                        // Navigate to MainActivity after successful sign-in
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra("USER_NAME", googleIdTokenCredential.displayName)
                        }
                        startActivity(intent)
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Received an invalid google id token response", e)
                    }
                } else {
                    Log.e(TAG, "Unexpected type of credential")
                }
            }

            else -> {
                Log.e(TAG, "Unexpected type of credential")
            }
        }
    }

    @Composable
    fun SignInScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "Welcome to Anti-Social Group Former!",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 20.dp),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Please Sign in below with your Google account",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            Button(
                onClick = {
                    val credentialManager = CredentialManager.create(this@SignInActivity)

                    val signInWithGoogleOption = GetSignInWithGoogleOption
                        .Builder(BuildConfig.WEB_CLIENT_ID)
                        .setNonce(generateHashedNonce())
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(signInWithGoogleOption)
                        .build()

                    // Perform sign-in asynchronously
                    activityScope.launch {
                        try {
                            val result = credentialManager.getCredential(
                                request = request,
                                context = this@SignInActivity
                            )
                            handleSignIn(result)
                        } catch (e: GetCredentialException) {
                            handleFailure(e)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign In with Google")
            }
        }
    }
}