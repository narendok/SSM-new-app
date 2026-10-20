package com.example.myapplication.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.cache.CacheManager
import com.example.myapplication.data.model.AuthState
import com.example.myapplication.data.model.DepartmentRole
import com.example.myapplication.data.model.PartComponent
import com.example.myapplication.data.remote.SupabaseClient
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.AuthRepositoryImpl
import com.example.myapplication.data.repository.PrivateStorageRepository
import com.example.myapplication.data.repository.PrivateStorageRepositoryImpl
import com.example.myapplication.data.repository.SafeViewsRepository
import com.example.myapplication.data.repository.SafeViewsRepositoryImpl
import com.example.myapplication.data.repository.ScmsRepository
import com.example.myapplication.data.repository.ScmsRepositoryImpl
import com.example.myapplication.data.repository.StockMutationRepository
import com.example.myapplication.data.repository.StockMutationRepositoryImpl
import com.example.myapplication.data.security.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import com.example.myapplication.data.repository.SharedTestRepository

sealed class ViewUiState {
    object Idle : ViewUiState()
    object Loading : ViewUiState()
    data class Error(val message: String) : ViewUiState()
    data class Success(val message: String) : ViewUiState()
    data class SchemaPending(val viewName: String, val details: String) : ViewUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val secureStorage = SecureStorage(application)
    private val supabaseClient = SupabaseClient()
    private val cacheManager = CacheManager(application)

    val authRepository: AuthRepository = AuthRepositoryImpl(supabaseClient, secureStorage)
    val safeViewsRepository: SafeViewsRepository = SafeViewsRepositoryImpl()
    val privateStorageRepository: PrivateStorageRepository = PrivateStorageRepositoryImpl(supabaseClient, secureStorage)
    val stockMutationRepository: StockMutationRepository = StockMutationRepositoryImpl(secureStorage)
    val scmsRepository: ScmsRepository = ScmsRepositoryImpl(secureStorage, supabaseClient)

    val authState: StateFlow<AuthState> = authRepository.authState

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _viewState = MutableStateFlow<ViewUiState>(ViewUiState.Idle)
    val viewState: StateFlow<ViewUiState> = _viewState.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val sharedRepository = SharedTestRepository(supabaseClient)
    private val _sharedSnapshot = MutableStateFlow<JsonObject?>(null)
    val sharedSnapshot: StateFlow<JsonObject?> = _sharedSnapshot.asStateFlow()
    private val _sharedError = MutableStateFlow<String?>(null)
    val sharedError: StateFlow<String?> = _sharedError.asStateFlow()
    private val _sharedBusy = MutableStateFlow(false)
    val sharedBusy: StateFlow<Boolean> = _sharedBusy.asStateFlow()
    private val _sharedSaved = MutableStateFlow(0)
    val sharedSaved: StateFlow<Int> = _sharedSaved.asStateFlow()
    private val _viewDepartment = MutableStateFlow<String?>(null)
    val viewDepartment: StateFlow<String?> = _viewDepartment.asStateFlow()
    fun selectDepartment(id: String?) {
        val snapshot = _sharedSnapshot.value ?: return
        val member = snapshot["membership"] as? JsonObject ?: return
        if (id == null && member["role"]?.jsonPrimitive?.content == "admin") _viewDepartment.value = null
        else if (com.example.myapplication.data.model.SharedAccess.departments(snapshot).any { it["id"]?.jsonPrimitive?.content == id }) _viewDepartment.value = id
    }
    private var sharedEpoch = 0

    fun refreshSharedWorkspace() {
        val token = (authState.value as? AuthState.Authenticated)?.session?.accessToken ?: return
        if (_sharedBusy.value) return
        val epoch = sharedEpoch
        _sharedBusy.value = true
        viewModelScope.launch {
            try {
                val snapshot = sharedRepository.load(authRepository.validAccessToken())
                if (epoch == sharedEpoch) { _sharedSnapshot.value = snapshot; _sharedError.value = null }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (epoch == sharedEpoch) _sharedError.value = e.message ?: "Shared read failed"
            } finally { if (epoch == sharedEpoch) _sharedBusy.value = false }
        }
    }

    fun mutateSharedWorkspace(action: String, payload: JsonObject) {
        val token = (authState.value as? AuthState.Authenticated)?.session?.accessToken ?: return
        if (_sharedBusy.value) return
        val signature = action + payload.toString()
        val userId = (authState.value as? AuthState.Authenticated)?.session?.user?.id ?: return
        val requestId = try { secureStorage.sharedRequestId(userId, signature) } catch (e: Exception) {
            _sharedError.value = "Cannot save retry identity. Action was not sent."; return
        }
        val epoch = sharedEpoch
        _sharedBusy.value = true
        viewModelScope.launch {
            try {
                sharedRepository.mutate(authRepository.validAccessToken(), action, payload, requestId)
                if (epoch == sharedEpoch) {
                    secureStorage.completeSharedRequest(userId, signature)
                    _sharedSaved.value++
                    _sharedError.value = null
                    _sharedSnapshot.value = sharedRepository.load(authRepository.validAccessToken())
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (epoch == sharedEpoch) _sharedError.value = e.message ?: "Shared action failed; retry the same action"
            } finally { if (epoch == sharedEpoch) _sharedBusy.value = false }
        }
    }

    init {
        viewModelScope.launch {
            var priorIdentity: String? = null
            authState.collect { state ->
                val identity = (state as? AuthState.Authenticated)?.session?.user?.id
                if (identity != null && identity == priorIdentity) return@collect
                priorIdentity = identity
                sharedEpoch++
                _viewDepartment.value = null
                _selectedTab.value = 0
                _sharedSnapshot.value = null; _sharedError.value = null; _sharedBusy.value = false
                if (state is AuthState.Authenticated) {
                    state.session.testMembership?.workspaceRole()?.let { scmsRepository.switchRole(it) }
                    refreshSharedWorkspace()
                }
            }
        }
    }

    private var signingIn = false

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun handleAuthCallbackCode(code: String) {
        viewModelScope.launch {
            authRepository.handleAuthCallbackCode(code)
        }
    }

    fun signInWithPassword(email: String, password: String) {
        if (signingIn || authState.value is AuthState.Authenticating) return
        signingIn = true
        viewModelScope.launch {
            try {
                authRepository.signInWithPassword(email, password)
            } finally {
                signingIn = false
            }
        }
    }

    fun switchRole(role: DepartmentRole) {
        if (authState.value !is AuthState.Unauthenticated) return
        scmsRepository.switchRole(role)
    }

    fun refreshActiveTab() {
        if (authState.value is AuthState.Authenticated) { refreshSharedWorkspace(); return }
        _viewState.value = ViewUiState.Loading
        _lastSyncTimestamp.value = System.currentTimeMillis()
        viewModelScope.launch {
            _viewState.value = ViewUiState.Success("Refreshed SCMS state successfully")
        }
    }

    fun getFormattedLastSync(): String {
        return cacheManager.getFormattedLastSync(_lastSyncTimestamp.value)
    }
}
