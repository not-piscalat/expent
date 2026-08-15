package com.expent.app.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.credentials.CustomCredential

/**
 * Launches the Google account picker via Android's Credential Manager and
 * returns the ID token Firebase Auth can consume.
 *
 * Returns null when the user dismisses the picker (that's not an error);
 * throws on real failures.
 */
object GoogleSignIn {

    suspend fun getIdToken(context: Context): String? {
        val webClientId = AuthConfig.GOOGLE_WEB_CLIENT_ID
        if (webClientId.isBlank()) {
            throw IllegalStateException(
                "Google sign-in is not configured yet — enable the Google provider in " +
                    "Firebase console and paste the web client ID into AuthConfig"
            )
        }

        val credentialManager = CredentialManager.create(context)
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setServerClientId(webClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .build()
            )
            .build()

        val result = try {
            credentialManager.getCredential(context, request)
        } catch (e: GetCredentialCancellationException) {
            return null
        }

        val credential = result.credential
        require(credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            "Unexpected credential type: ${credential.type}"
        }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
}
