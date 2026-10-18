package com.example.myapplication.data.repository

import com.example.myapplication.BuildConfig
import com.example.myapplication.data.model.AuthSession
import com.example.myapplication.data.model.AuthState
import com.example.myapplication.data.remote.PkceUtil
import com.example.myapplication.data.remote.SupabaseClient
import com.example.myapplication.data.security.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>
    fun buildGoogleWorkspaceAuthUrl(): String
    suspend fun handleAuthCallbackCode(code: String): Result<AuthSession>
    fun signOut()
    fun getCurrentSession(): AuthSession?
}

class AuthRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val secureStorage: SecureStorage
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var activeCodeVerifier: String? = null

    init {
        val existingSession = secureStorage.getSession()
        if (existingSession != null) {
            _authState.value = AuthState.Authenticated(existingSession)
        }
    }

    override fun buildGoogleWorkspaceAuthUrl(): String {
        val verifier = PkceUtil.generateCodeVerifier()
        activeCodeVerifier = verifier
        val challenge = PkceUtil.generateCodeChallenge(verifier)
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val redirectUri = "com.example.myapplication://auth-callback"

        return "$baseUrl/auth/v1/authorize?provider=google&response_type=code&code_challenge=$challenge&code_challenge_method=S256&redirect_to=$redirectUri"
    }

    override suspend fun handleAuthCallbackCode(code: String): Result<AuthSession> {
        val verifier = activeCodeVerifier ?: return Result.failure(
            IllegalStateException("No active PKCE code_verifier found. Please initiate Google Workspace Sign-In again.")
        )

        _authState.value = AuthState.Authenticating("Exchanging PKCE authorization code...")

        val result = supabaseClient.exchangePkceCode(code, verifier)
        return result.onSuccess { session ->
            secureStorage.saveSession(session)
            _authState.value = AuthState.Authenticated(session)
            activeCodeVerifier = null
        }.onFailure { error ->
            _authState.value = AuthState.Error(error.message ?: "Authentication failed")
        }
    }

    override fun signOut() {
        secureStorage.clearSession()
        _authState.value = AuthState.Unauthenticated
    }

    override fun getCurrentSession(): AuthSession? {
        return secureStorage.getSession()
    }
}
