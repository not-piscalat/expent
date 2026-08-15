package com.expent.app.data.auth

/**
 * Google Sign-In configuration.
 *
 * GOOGLE_WEB_CLIENT_ID is the OAuth "Web client (auto created by Google Service)"
 * ID for this Firebase project. It is created when you enable the Google provider:
 * Firebase console -> Authentication -> Sign-in method -> Google.
 *
 * It is NOT a secret (it ships in google-services.json and is sent to Google's
 * identity endpoint), so committing it here is safe.
 */
object AuthConfig {
    // TODO: paste the web client ID from Firebase console -> Authentication ->
    //  Sign-in method -> Google -> "Web client ID" once the provider is enabled.
    const val GOOGLE_WEB_CLIENT_ID: String = "913568544699-4t96i35eg88v0hunrpccjdjk9d546a45.apps.googleusercontent.com"
}
