package com.linusv.englishcoach.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.linusv.englishcoach.BuildConfig
import com.linusv.englishcoach.EnglishCoachApplication
import kotlinx.coroutines.tasks.await

data class SignedInUser(val uid: String, val email: String, val displayName: String?)

class AuthRepository(
    private val application: EnglishCoachApplication,
) {
    private val allowedEmail = BuildConfig.ALLOWED_EMAIL.trim().lowercase()

    fun currentUser(): SignedInUser? {
        val user = if (application.firebaseConfigured) FirebaseAuth.getInstance().currentUser else null
        return user?.email?.let { SignedInUser(user.uid, it, user.displayName) }
    }

    suspend fun signIn(activity: Activity): Result<SignedInUser> {
        if (!application.firebaseConfigured) {
            return Result.success(SignedInUser("demo-user", "demo@local", "Demo learner"))
        }
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) return Result.failure(IllegalStateException("Thiếu firebase.webClientId trong local.properties"))
        return runCatching {
            val credentialManager = CredentialManager.create(activity)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                "Credential không phải Google ID token"
            }
            val token = try {
                GoogleIdTokenCredential.createFrom(credential.data)
            } catch (e: GoogleIdTokenParsingException) {
                error("Không đọc được Google ID token: ${e.message}")
            }
            val firebaseCredential = GoogleAuthProvider.getCredential(token.idToken, null)
            val authResult = FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
            val user = requireNotNull(authResult.user)
            val email = requireNotNull(user.email)
            require(isAllowed(email)) { "Tài khoản này chưa được cấp quyền cho app cá nhân." }
            SignedInUser(user.uid, email, user.displayName)
        }
    }

    fun isAllowed(email: String): Boolean = allowedEmail.isBlank() || email.trim().lowercase() == allowedEmail

    fun signOut() {
        if (application.firebaseConfigured) FirebaseAuth.getInstance().signOut()
    }
}
