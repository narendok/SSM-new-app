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

    init {
        viewModelScope.launch {
            authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    state.session.testMembership?.workspaceRole()?.let { scmsRepository.switchRole(it) }
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
