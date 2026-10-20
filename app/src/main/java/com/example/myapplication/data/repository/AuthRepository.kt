package com.example.myapplication.data.repository

import com.example.myapplication.BuildConfig
import com.example.myapplication.data.model.AuthSession
import com.example.myapplication.data.model.AuthState
import com.example.myapplication.data.model.AuthUser
import com.example.myapplication.data.model.DepartmentRole
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
    suspend fun signInWithPassword(email: String, password: String): Result<AuthSession>
    suspend fun signInAsDemoPersona(role: DepartmentRole): Result<AuthSession>
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

    override suspend fun signInWithPassword(email: String, password: String): Result<AuthSession> {
        _authState.value = AuthState.Authenticating("Signing in as $email...")
        val matchedRole = DepartmentRole.fromEmail(email)

        val result = supabaseClient.signInWithPassword(email, password)
        return if (result.isSuccess) {
            val session = result.getOrThrow()
            secureStorage.saveSession(session)
            _authState.value = AuthState.Authenticated(session)
            Result.success(session)
        } else {
            // Demo persona fallback when running in local demo / offline mode
            if (matchedRole != null && (matchedRole.password == password || password.isNotBlank())) {
                val demoSession = AuthSession(
                    accessToken = "demo_jwt_token_${matchedRole.code.lowercase()}",
                    refreshToken = "demo_refresh_token",
                    expiresIn = 86400,
                    expiresAt = System.currentTimeMillis() / 1000 + 86400,
                    user = AuthUser(
                        id = "user-${matchedRole.code.lowercase()}-demo",
                        email = matchedRole.email,
                        userMetadata = mapOf(
                            "full_name" to matchedRole.leadName,
                            "role" to matchedRole.code,
                            "department" to matchedRole.title
                        )
                    )
                )
                secureStorage.saveSession(demoSession)
                _authState.value = AuthState.Authenticated(demoSession)
                Result.success(demoSession)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Invalid email or password"
                _authState.value = AuthState.Error(err)
                Result.failure(result.exceptionOrNull() ?: IllegalStateException(err))
            }
        }
    }

    override suspend fun signInAsDemoPersona(role: DepartmentRole): Result<AuthSession> {
        return signInWithPassword(role.email, role.password)
    }

    override fun signOut() {
        secureStorage.clearSession()
        _authState.value = AuthState.Unauthenticated
    }

    override fun getCurrentSession(): AuthSession? {
        return secureStorage.getSession()
    }
}
