package com.example.myapplication.ui.viewmodel

import android.app.Application
import com.example.myapplication.BuildConfig
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.cache.CacheManager
import com.example.myapplication.data.model.AuthState
import com.example.myapplication.data.model.DepartmentRole
import com.example.myapplication.data.model.PartComponent
import com.example.myapplication.data.model.ProjectCoordinationProjection
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
    private val controlledOriginalRepository = com.example.myapplication.data.repository.ControlledOriginalRepository(supabaseClient)
    suspend fun controlledOriginalRevisions(documentId: String): JsonArray {
        val token = authRepository.validAccessToken()
        return Json.parseToJsonElement(supabaseClient.listControlledOriginals(documentId, token).getOrThrow()).jsonArray
    }
    suspend fun verifyControlledOriginal(fileId: String) {
        supabaseClient.verifyControlledOriginal(fileId, authRepository.validAccessToken()).getOrThrow()
    }
    suspend fun employeeLetterDownloadUrl(itemId: String): String =
        supabaseClient.employeeLetterDownloadUrl(itemId, authRepository.validAccessToken()).getOrThrow()
    suspend fun verifiedHrLetterCatalogue(departmentIds: List<String>): List<JsonObject> {
        val token = authRepository.validAccessToken()
        val docs = Json.parseToJsonElement(supabaseClient.listHrLetterDocuments(departmentIds, token).getOrThrow()).jsonArray
        val result = mutableListOf<JsonObject>()
        for (doc in docs.mapNotNull { it as? JsonObject }) {
            val revisions = Json.parseToJsonElement(supabaseClient.listControlledOriginals(doc["id"]?.jsonPrimitive?.content.orEmpty(), token).getOrThrow()).jsonArray
            for (file in revisions.mapNotNull { it as? JsonObject }) {
                val size = file["size_bytes"]?.jsonPrimitive?.longOrNull ?: 0L
                val checksum = file["checksum"]?.jsonPrimitive?.contentOrNull.orEmpty()
                if (file["superseded_at"]?.jsonPrimitive?.contentOrNull?.isNotBlank() == true || size <= 0L || !checksum.matches(Regex("^[a-fA-F0-9]{64}$"))) continue
                if (runCatching { supabaseClient.verifyControlledOriginal(file["id"]?.jsonPrimitive?.content.orEmpty(), token).getOrThrow() }.isSuccess) {
                    result += buildJsonObject {
                        put("documentId", doc["id"]?.jsonPrimitive?.content.orEmpty())
                        put("fileId", file["id"]?.jsonPrimitive?.content.orEmpty())
                        put("title", doc["title"]?.jsonPrimitive?.contentOrNull.orEmpty())
                        put("revision", file["revision"]?.jsonPrimitive?.contentOrNull.orEmpty())
                        put("originalName", file["original_name"]?.jsonPrimitive?.contentOrNull.orEmpty())
                    }
                }
            }
        }
        return result
    }
    suspend fun controlledOriginalDownload(fileId: String): String =
        controlledOriginalRepository.downloadUrl(authRepository.validAccessToken(), fileId)
    suspend fun createSharedDocumentWithOriginal(
        projectId: String, departmentId: String, title: String, documentType: String,
        originalName: String, contentType: String, bytes: ByteArray
    ): String {
        require(BuildConfig.SCMS_BACKEND == "staging") { "Shared document creation is restricted to TEST staging" }
        require(projectId.isNotBlank() && projectId != "ALL") { "Select a project first" }
        require(title.isNotBlank() && documentType.isNotBlank()) { "Document title and type are required" }
        val userId = (authState.value as? AuthState.Authenticated)?.session?.user?.id
            ?: throw SecurityException("Sign in to the TEST workspace first")
        val token = authRepository.validAccessToken()
        val checksum = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val signature = "document-original:$projectId:$departmentId:$title:$documentType:$originalName:$checksum"
        val documentRequestId = secureStorage.sharedRequestId(userId, signature)
        val fileRequestId = secureStorage.sharedRequestId(userId, "$signature:original")
        val receipt = sharedRepository.registerDocument(token, buildJsonObject {
            put("scope", "internal")
            put("projectId", projectId)
            put("departmentId", departmentId)
            put("title", title.trim())
            put("documentType", documentType)
        }, documentRequestId)
        val documentId = receipt["id"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("Document registration returned no ID")
        val fileId = supabaseClient.registerControlledOriginal(
            documentId = documentId,
            revision = "01",
            originalName = originalName,
            contentType = contentType,
            bytes = bytes,
            accessToken = token,
            requestId = fileRequestId
        ).getOrThrow()
        secureStorage.completeSharedRequest(userId, signature)
        secureStorage.completeSharedRequest(userId, "$signature:original")
        _sharedSnapshot.value = sharedRepository.load(authRepository.validAccessToken())
        return fileId
    }
    /** Creates a shared record, then stores its source file as a private controlled original linked to that record. */
    suspend fun createSharedRecordWithOriginal(
        recordPayload: JsonObject,
        departmentId: String,
        projectId: String?,
        recordTitle: String,
        recordType: String,
        originalName: String,
        contentType: String,
        bytes: ByteArray
    ): JsonObject {
        if (_sharedBusy.value) throw IllegalStateException("Another shared save is in progress. Wait and retry.")
        _sharedBusy.value = true
        _sharedError.value = null
        try {
        require(BuildConfig.SCMS_BACKEND == "staging") { "Record-linked original upload is not enabled for this workspace." }
        require(departmentId.isNotBlank() && recordTitle.isNotBlank() && recordType.isNotBlank()) { "Record context is incomplete." }
        require(originalName.isNotBlank() && contentType.isNotBlank()) { "Choose a readable source file." }
        require(bytes.isNotEmpty() && bytes.size <= 25 * 1024 * 1024) { "File must be between 1 byte and 25 MB." }
        val userId = (authState.value as? AuthState.Authenticated)?.session?.user?.id
            ?: throw SecurityException("Sign in to the shared workspace first.")
        val token = authRepository.validAccessToken()
        val checksum = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val recordSignature = "smart-record:${recordPayload}"
        val recordRequestId = secureStorage.sharedRequestId(userId, recordSignature)
        val recordReceipt = sharedRepository.mutateWithReceipt(token, "create_record", recordPayload, recordRequestId)
        val recordId = recordReceipt["id"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalStateException("The shared record was not returned.")

        val documentPayload = buildJsonObject {
            put("title", "$recordTitle · Source")
            put("documentType", "Record source attachment")
            put("scope", if (projectId.isNullOrBlank()) "department" else "internal")
            put("departmentId", departmentId)
            if (!projectId.isNullOrBlank()) put("projectId", projectId)
            put("recordId", recordId)
        }
        val documentSignature = "smart-record-document:$recordId:${documentPayload}"
        val documentRequestId = secureStorage.sharedRequestId(userId, documentSignature)
        val documentReceipt = sharedRepository.registerDocument(token, documentPayload, documentRequestId)
        val documentId = documentReceipt["id"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalStateException("The source reference was not returned.")

        val originalSignature = "smart-record-original:$recordId:$documentId:$checksum"
        val originalRequestId = secureStorage.sharedRequestId(userId, originalSignature)
        val fileId = supabaseClient.registerControlledOriginal(
            documentId = documentId,
            revision = "01",
            originalName = originalName,
            contentType = contentType,
            bytes = bytes,
            accessToken = token,
            requestId = originalRequestId
        ).getOrThrow()
        secureStorage.completeSharedRequest(userId, recordSignature)
        secureStorage.completeSharedRequest(userId, documentSignature)
        secureStorage.completeSharedRequest(userId, originalSignature)
        _sharedSnapshot.value = sharedRepository.load(authRepository.validAccessToken())
        _sharedSaved.value++
        return buildJsonObject {
            recordReceipt.forEach { (key, value) -> put(key, value) }
            put("source_file_id", JsonPrimitive(fileId))
        }
        } catch (e: Exception) {
            _sharedError.value = e.message ?: "Record or source upload failed. Retry with the same file."
            throw e
        } finally {
            _sharedBusy.value = false
        }
    }
    private val _sharedSnapshot = MutableStateFlow<JsonObject?>(null)
    val sharedSnapshot: StateFlow<JsonObject?> = _sharedSnapshot.asStateFlow()
    private val _orgConfig = MutableStateFlow<JsonObject?>(null)
    val orgConfig: StateFlow<JsonObject?> = _orgConfig.asStateFlow()
    private val _customerChanges = MutableStateFlow<JsonObject?>(null)
    val customerChanges: StateFlow<JsonObject?> = _customerChanges.asStateFlow()
    private val _lifecycleWorkspace = MutableStateFlow<JsonObject?>(null)
    val lifecycleWorkspace: StateFlow<JsonObject?> = _lifecycleWorkspace.asStateFlow()
    private val _lifecycleWorkspaceError = MutableStateFlow<String?>(null)
    val lifecycleWorkspaceError: StateFlow<String?> = _lifecycleWorkspaceError.asStateFlow()
    private val _lifecycleWorkspaceBusy = MutableStateFlow(false)
    val lifecycleWorkspaceBusy: StateFlow<Boolean> = _lifecycleWorkspaceBusy.asStateFlow()
    fun refreshLifecycleWorkspace() {
        if (BuildConfig.SCMS_BACKEND != "staging" || authState.value !is AuthState.Authenticated || _lifecycleWorkspaceBusy.value) return
        val epoch = sharedEpoch
        _lifecycleWorkspaceBusy.value = true
        _lifecycleWorkspaceError.value = null
        viewModelScope.launch {
            try {
                val result = sharedRepository.loadLifecycleWorkspace(authRepository.validAccessToken())
                if (epoch == sharedEpoch) _lifecycleWorkspace.value = result
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (epoch == sharedEpoch) { _lifecycleWorkspace.value = null; _lifecycleWorkspaceError.value = e.message ?: "Reusable revisions could not be loaded" }
            } finally { if (epoch == sharedEpoch) _lifecycleWorkspaceBusy.value = false }
        }
    }
    fun linkReusableAsset(projectId: String, assetRevisionId: String, assessment: String) {
        if (BuildConfig.SCMS_BACKEND != "staging") { _lifecycleWorkspaceError.value = "Reusable asset linking is restricted to TEST"; return }
        val userId = (authState.value as? AuthState.Authenticated)?.session?.user?.id ?: run {
            _lifecycleWorkspaceError.value = "Sign in to the TEST workspace first"; return
        }
        if (_lifecycleWorkspaceBusy.value) return
        val payload = buildJsonObject {
            put("projectId", projectId)
            put("assetRevisionId", assetRevisionId)
            put("assessment", assessment.trim())
        }
        val signature = "lifecycle:link_asset:${payload}"
        val requestId = try { secureStorage.sharedRequestId(userId, signature) } catch (e: Exception) {
            _lifecycleWorkspaceError.value = "Cannot preserve retry identity. Nothing was sent."; return
        }
        val epoch = sharedEpoch
        _lifecycleWorkspaceBusy.value = true
        _lifecycleWorkspaceError.value = null
        viewModelScope.launch {
            try {
                sharedRepository.linkReusableAsset(authRepository.validAccessToken(), payload, requestId)
                if (epoch == sharedEpoch) {
                    secureStorage.completeSharedRequest(userId, signature)
                    _lifecycleWorkspace.value = sharedRepository.loadLifecycleWorkspace(authRepository.validAccessToken())
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (epoch == sharedEpoch) _lifecycleWorkspaceError.value = e.message ?: "Link failed; retry the same selection"
            } finally { if (epoch == sharedEpoch) _lifecycleWorkspaceBusy.value = false }
        }
    }
    private val _sharedError = MutableStateFlow<String?>(null)
    val sharedError: StateFlow<String?> = _sharedError.asStateFlow()
    private val _sharedBusy = MutableStateFlow(false)
    val sharedBusy: StateFlow<Boolean> = _sharedBusy.asStateFlow()
    private val _sharedSaved = MutableStateFlow(0)
    val sharedSaved: StateFlow<Int> = _sharedSaved.asStateFlow()
    private val _projectCoordination = MutableStateFlow<ProjectCoordinationProjection?>(null)
    val projectCoordination: StateFlow<ProjectCoordinationProjection?> = _projectCoordination.asStateFlow()
    private val _projectCoordinationError = MutableStateFlow<String?>(null)
    val projectCoordinationError: StateFlow<String?> = _projectCoordinationError.asStateFlow()
    private val _projectCoordinationBusy = MutableStateFlow(false)
    val projectCoordinationBusy: StateFlow<Boolean> = _projectCoordinationBusy.asStateFlow()
    private val _viewDepartment = MutableStateFlow<String?>(null)
    val viewDepartment: StateFlow<String?> = _viewDepartment.asStateFlow()
    fun selectDepartment(id: String?) {
        val snapshot = _sharedSnapshot.value
        if (snapshot == null) {
            _viewDepartment.value = id
            return
        }
        val member = snapshot["membership"] as? JsonObject ?: return
        if (id == null && member["role"]?.jsonPrimitive?.content == "admin") {
            _viewDepartment.value = null
        } else if (com.example.myapplication.data.model.SharedAccess.departments(snapshot).any { it["id"]?.jsonPrimitive?.content == id }) {
            _viewDepartment.value = id
        }
    }
    private val businessRepository = com.example.myapplication.data.repository.SharedBusinessRepository(supabaseClient)
    private val sharedInventoryRepository = com.example.myapplication.data.repository.SharedInventoryRepository(supabaseClient)
    private val _sharedInventorySnapshot = MutableStateFlow<JsonObject?>(null)
    val sharedInventorySnapshot: StateFlow<JsonObject?> = _sharedInventorySnapshot.asStateFlow()
    private val _sharedInventoryBusy = MutableStateFlow(false)
    val sharedInventoryBusy: StateFlow<Boolean> = _sharedInventoryBusy.asStateFlow()
    private val _sharedInventoryError = MutableStateFlow<String?>(null)
    val sharedInventoryError: StateFlow<String?> = _sharedInventoryError.asStateFlow()

    fun refreshSharedInventory() {
        if (BuildConfig.SCMS_BACKEND != "staging" || authState.value !is AuthState.Authenticated || _sharedInventoryBusy.value) return
        val epoch = sharedEpoch
        _sharedInventoryBusy.value = true
        viewModelScope.launch {
            try {
                val result = sharedInventoryRepository.load(authRepository.validAccessToken())
                if (epoch == sharedEpoch) { _sharedInventorySnapshot.value = result; _sharedInventoryError.value = null }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (epoch == sharedEpoch) { _sharedInventorySnapshot.value = null; _sharedInventoryError.value = e.message ?: "Shared TEST inventory could not be loaded" }
            } finally {
                if (epoch == sharedEpoch) _sharedInventoryBusy.value = false
            }
        }
    }

    fun recordSharedInventoryEvent(action: String, payload: JsonObject) {
        if (BuildConfig.SCMS_BACKEND != "staging") { _sharedInventoryError.value = "Shared inventory actions are restricted to TEST."; return }
        val userId = (authState.value as? AuthState.Authenticated)?.session?.user?.id ?: return
        if (_sharedInventoryBusy.value) return
        val signature = "shared_inventory:" + action + payload.toString()
        val requestId = try { secureStorage.sharedRequestId(userId, signature) } catch (e: Exception) {
            _sharedInventoryError.value = "Could not preserve a safe retry ID. Nothing was sent."; return
        }
        val eventPayload = kotlinx.serialization.json.buildJsonObject { payload.forEach { (key, value) -> put(key, value) }; put("requestId", requestId) }
        val epoch = sharedEpoch
        _sharedInventoryBusy.value = true
        _sharedInventoryError.value = null
        viewModelScope.launch {
            try {
                val receipt = sharedInventoryRepository.recordEvent(action, eventPayload, authRepository.validAccessToken())
                require(receipt["eventId"]?.jsonPrimitive?.contentOrNull?.isNotBlank() == true) { "Shared TEST did not confirm the inventory event." }
                if (epoch == sharedEpoch) {
                    secureStorage.completeSharedRequest(userId, signature)
                    _sharedInventorySnapshot.value = sharedInventoryRepository.load(authRepository.validAccessToken())
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (epoch == sharedEpoch) _sharedInventoryError.value = e.message ?: "Shared inventory save failed; retry is safe."
            } finally {
                if (epoch == sharedEpoch) _sharedInventoryBusy.value = false
            }
        }
    }

    fun issueSharedInventoryBomKit(payload: JsonObject) {
        if (BuildConfig.SCMS_BACKEND != "staging") { _sharedInventoryError.value = "BOM kit issues are restricted to TEST."; return }
        val userId = (authState.value as? AuthState.Authenticated)?.session?.user?.id ?: return
        if (_sharedInventoryBusy.value) return
        val signature = "shared_inventory_kit_issue:" + payload.toString()
        val requestId = try { secureStorage.sharedRequestId(userId, signature) } catch (e: Exception) {
            _sharedInventoryError.value = "Could not preserve a safe retry ID. Nothing was sent."; return
        }
        val requestPayload = buildJsonObject { payload.forEach { (key, value) -> put(key, value) }; put("requestId", requestId) }
        val epoch = sharedEpoch
        _sharedInventoryBusy.value = true
        _sharedInventoryError.value = null
        viewModelScope.launch {
            try {
                val receipt = sharedInventoryRepository.issueBomKit(requestPayload, authRepository.validAccessToken())
                require(receipt["kitIssueId"]?.jsonPrimitive?.contentOrNull?.isNotBlank() == true) {
                    "Shared TEST did not confirm the BOM kit issue."
                }
                if (epoch == sharedEpoch) {
                    secureStorage.completeSharedRequest(userId, signature)
                    _sharedInventorySnapshot.value = sharedInventoryRepository.load(authRepository.validAccessToken())
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (epoch == sharedEpoch) _sharedInventoryError.value = e.message ?: "BOM kit issue failed; retry is safe."
            } finally {
                if (epoch == sharedEpoch) _sharedInventoryBusy.value = false
            }
        }
    }

    private val _expenseCategories = MutableStateFlow<List<String>>(emptyList())
    val expenseCategories: StateFlow<List<String>> = _expenseCategories.asStateFlow()
    private val _expenseServices = MutableStateFlow<List<JsonObject>>(emptyList())
    val expenseServices: StateFlow<List<JsonObject>> = _expenseServices.asStateFlow()
    private val _expenseSuppliers = MutableStateFlow<List<JsonObject>>(emptyList())
    val expenseSuppliers: StateFlow<List<JsonObject>> = _expenseSuppliers.asStateFlow()
    private val _expenseLocations = MutableStateFlow<List<JsonObject>>(emptyList())
    val expenseLocations: StateFlow<List<JsonObject>> = _expenseLocations.asStateFlow()
    private val _expenseCatalogueBusy = MutableStateFlow(false)
    val expenseCatalogueBusy: StateFlow<Boolean> = _expenseCatalogueBusy.asStateFlow()
    private val _expenseCatalogueLoaded = MutableStateFlow(false)
    val expenseCatalogueLoaded: StateFlow<Boolean> = _expenseCatalogueLoaded.asStateFlow()
    private val _expenseCatalogueError = MutableStateFlow<String?>(null)
    val expenseCatalogueError: StateFlow<String?> = _expenseCatalogueError.asStateFlow()
    private val _travelPolicy = MutableStateFlow<JsonObject?>(null)
    val travelPolicy: StateFlow<JsonObject?> = _travelPolicy.asStateFlow()
    private val _travelPolicyError = MutableStateFlow<String?>(null)
    val travelPolicyError: StateFlow<String?> = _travelPolicyError.asStateFlow()
    private val _travelPolicySaving = MutableStateFlow(false)
    val travelPolicySaving: StateFlow<Boolean> = _travelPolicySaving.asStateFlow()
    private val _travelPolicySaveMessage = MutableStateFlow<String?>(null)
    val travelPolicySaveMessage: StateFlow<String?> = _travelPolicySaveMessage.asStateFlow()
    private var expenseOptionsScope: String? = null
    fun refreshExpenseCategories(departmentId: String?, organizationId: String?, force: Boolean = false) {
        val scope = departmentId?.takeIf { it.isNotBlank() }
        val requestScope = "${organizationId.orEmpty()}|${scope ?: "company"}"
        if (!force && _expenseCatalogueLoaded.value && expenseOptionsScope == requestScope) return
        if (authState.value !is AuthState.Authenticated) return
        _expenseCategories.value = emptyList()
        _expenseServices.value = emptyList()
        _expenseSuppliers.value = emptyList()
        _expenseLocations.value = emptyList()
        expenseOptionsScope = requestScope
        _expenseCatalogueBusy.value = true
        _expenseCatalogueLoaded.value = false
        _expenseCatalogueError.value = null
        viewModelScope.launch {
            try {
                val result = businessRepository.loadExpenseCatalogue(authRepository.validAccessToken(), scope)
                val categories = (result["categories"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
                val services = (result["services"] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
                val suppliers = (result["suppliers"] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
                val locations = (result["locations"] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
                if (expenseOptionsScope == requestScope) {
                    _expenseCategories.value = categories
                    _expenseServices.value = services
                    _expenseSuppliers.value = suppliers
                    _expenseLocations.value = locations
                    _expenseCatalogueLoaded.value = true
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (expenseOptionsScope == requestScope) {
                    _expenseCatalogueError.value = e.message ?: "Expense categories could not be loaded"
                    _expenseCatalogueLoaded.value = true
                }
            } finally {
                if (expenseOptionsScope == requestScope) _expenseCatalogueBusy.value = false
            }
        }
    }
    private val _expenseLocationRequestBusy = MutableStateFlow(false)
    val expenseLocationRequestBusy: StateFlow<Boolean> = _expenseLocationRequestBusy.asStateFlow()
    private val _expenseLocationRequestSaved = MutableStateFlow(0)
    val expenseLocationRequestSaved: StateFlow<Int> = _expenseLocationRequestSaved.asStateFlow()
    private val _expenseLocationRequestError = MutableStateFlow<String?>(null)
    val expenseLocationRequestError: StateFlow<String?> = _expenseLocationRequestError.asStateFlow()
    fun requestExpenseLocation(name:String,city:String,reason:String,departmentId:String?,organizationId:String?) {
        val user=(authState.value as? AuthState.Authenticated)?.session?.user?.id ?: return
        if(_expenseLocationRequestBusy.value) return
        val signature="expense-location-request:$name|$city|$reason"
        val requestId=try{secureStorage.sharedRequestId(user,signature)}catch(e:Exception){_expenseLocationRequestError.value="Cannot preserve retry identity. Nothing was sent.";return}
        val epoch=sharedEpoch
        _expenseLocationRequestBusy.value=true;_expenseLocationRequestError.value=null
        viewModelScope.launch {
            try {
                businessRepository.requestExpenseLocation(authRepository.validAccessToken(),name.trim(),city.trim(),reason.trim(),requestId)
                if(epoch==sharedEpoch){secureStorage.completeSharedRequest(user,signature);_expenseLocationRequestSaved.value++;refreshExpenseCategories(departmentId,organizationId,true)}
            } catch(e:Exception){if(e is kotlinx.coroutines.CancellationException) throw e;if(epoch==sharedEpoch)_expenseLocationRequestError.value=e.message?:"Location request failed; retry safely."}
            finally {if(epoch==sharedEpoch)_expenseLocationRequestBusy.value=false}
        }
    }
    fun refreshTravelPolicy(departmentId:String?,force:Boolean=false) {
        if (authState.value !is AuthState.Authenticated) return
        if (!force && _travelPolicy.value != null) return
        val epoch=sharedEpoch
        viewModelScope.launch {
            try { val policy=businessRepository.loadTravelPolicy(authRepository.validAccessToken(),departmentId);if(epoch==sharedEpoch){_travelPolicy.value=policy;_travelPolicyError.value=null} }
            catch(e:Exception){if(e is kotlinx.coroutines.CancellationException) throw e;if(epoch==sharedEpoch){_travelPolicy.value=null;_travelPolicyError.value=e.message ?: "Travel policy could not be loaded"}}
        }
    }
    fun saveTravelPolicy(departmentId:String?,value:JsonObject,version:Int,publish:Boolean) {
        if (authState.value !is AuthState.Authenticated || _travelPolicySaving.value) return
        val epoch=sharedEpoch
        _travelPolicySaving.value=true
        _travelPolicySaveMessage.value=null
        viewModelScope.launch {
            try {
                val saved=businessRepository.saveTravelPolicy(authRepository.validAccessToken(),departmentId,value,version,publish)
                if(epoch==sharedEpoch){_travelPolicy.value=saved;_travelPolicyError.value=null;_travelPolicySaveMessage.value=if(publish)"Shared travel policy published." else "Shared travel policy saved."}
            } catch(e:Exception) {
                if(e is kotlinx.coroutines.CancellationException) throw e
                if(epoch==sharedEpoch)_travelPolicySaveMessage.value=e.message ?: "Travel policy save failed; reload before retrying."
            } finally { if(epoch==sharedEpoch)_travelPolicySaving.value=false }
        }
    }
    private val _businessSnapshot = MutableStateFlow<JsonObject?>(null)
    val businessSnapshot: StateFlow<JsonObject?> = _businessSnapshot.asStateFlow()
    private val _businessError = MutableStateFlow<String?>(null)
    val businessError: StateFlow<String?> = _businessError.asStateFlow()
    private val _businessBusy = MutableStateFlow(false)
    val businessBusy: StateFlow<Boolean> = _businessBusy.asStateFlow()
    private val _businessSaved = MutableStateFlow(0)
    val businessSaved: StateFlow<Int> = _businessSaved.asStateFlow()
    fun clearBusinessError() { _businessError.value = null }
    fun refreshBusinessWorkspace() {
        if (authState.value !is AuthState.Authenticated || _businessBusy.value) return
        val epoch = sharedEpoch
        _businessBusy.value = true
        viewModelScope.launch {
            try { val snapshot = businessRepository.load(authRepository.validAccessToken()); if(epoch == sharedEpoch) { _businessSnapshot.value = snapshot; _businessError.value = null } }
            catch(e:Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; if(epoch == sharedEpoch) { _businessSnapshot.value = null; _businessError.value = e.message ?: "Shared read failed" } }
            finally { if(epoch == sharedEpoch) _businessBusy.value = false }
        }
    }
    fun mutateBusinessWorkspace(action:String, payload:JsonObject) {
        val user = (authState.value as? AuthState.Authenticated)?.session?.user?.id ?: return
        if(_businessBusy.value) return
        val signature = "business:" + action + payload.toString()
        val requestId = try { secureStorage.sharedRequestId(user, signature) } catch (e: Exception) {
            _businessError.value = "Cannot preserve retry identity. Nothing was sent."; return
        }
        val epoch = sharedEpoch
        _businessBusy.value = true; _businessError.value = null
        viewModelScope.launch {
            try {
                businessRepository.mutate(authRepository.validAccessToken(),action,payload,requestId)
                if(epoch == sharedEpoch) {
                    secureStorage.completeSharedRequest(user, signature)
                    _businessSaved.value++
                    _businessSnapshot.value = businessRepository.load(authRepository.validAccessToken())
                }
            } catch(e:Exception) { if(e is kotlinx.coroutines.CancellationException) throw e; if(epoch == sharedEpoch) _businessError.value = e.message ?: "Save failed; retry without changing the form" }
            finally { if(epoch == sharedEpoch) _businessBusy.value = false }
        }
    }

    private var sharedEpoch = 0

    private fun loadOptionalSharedFeeds(epoch: Int) {
        if (authState.value !is AuthState.Authenticated) return
        viewModelScope.launch {
            runCatching { sharedRepository.loadOrganizationConfiguration(authRepository.validAccessToken()) }
                .onSuccess { if (epoch == sharedEpoch) _orgConfig.value = it }
        }
        viewModelScope.launch {
            runCatching { sharedRepository.loadCustomerChangeRequests(authRepository.validAccessToken()) }
                .onSuccess { if (epoch == sharedEpoch) _customerChanges.value = it }
        }
    }

    fun resetSamplePack(runId: String = "LC2-2026-10-28") {
        val userId = (authState.value as? AuthState.Authenticated)?.session?.user?.id ?: return
        if (_sharedBusy.value) return
        val signature = "reset_sample_pack:$runId"
        val requestId = try { secureStorage.sharedRequestId(userId, signature) } catch (e: Exception) {
            _sharedError.value = "Cannot preserve retry identity. Reset was not sent."; return
        }
        val epoch = sharedEpoch
        _sharedBusy.value = true
        viewModelScope.launch {
            try {
                sharedRepository.resetSamplePack(authRepository.validAccessToken(), runId, requestId)
                if (epoch == sharedEpoch) {
                    secureStorage.completeSharedRequest(userId, signature)
                    _sharedSaved.value++
                    _sharedError.value = null
                    _sharedSnapshot.value = sharedRepository.load(authRepository.validAccessToken())
                    refreshLifecycleWorkspace()
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (epoch == sharedEpoch) _sharedError.value = e.message ?: "Reset TEST pack failed"
            } finally { if (epoch == sharedEpoch) _sharedBusy.value = false }
        }
    }

    fun refreshSharedWorkspace() {
        val token = (authState.value as? AuthState.Authenticated)?.session?.accessToken ?: return
        if (_sharedBusy.value) return
        val epoch = sharedEpoch
        _sharedBusy.value = true
        viewModelScope.launch {
            try {
                val snapshot = sharedRepository.load(authRepository.validAccessToken())
                if (epoch == sharedEpoch) {
                    _sharedSnapshot.value = snapshot
                    _sharedError.value = null
                    loadOptionalSharedFeeds(epoch)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (epoch == sharedEpoch) _sharedError.value = e.message ?: "Shared read failed"
            } finally { if (epoch == sharedEpoch) _sharedBusy.value = false }
        }
    }

    fun refreshProjectCoordination() {
        if (authState.value !is AuthState.Authenticated || _projectCoordinationBusy.value) return
        val epoch = sharedEpoch
        _projectCoordinationBusy.value = true
        _projectCoordinationError.value = null
        viewModelScope.launch {
            try {
                val data = sharedRepository.loadProjectCoordination(authRepository.validAccessToken())
                if (epoch == sharedEpoch) {
                    _projectCoordination.value = data
                    _projectCoordinationError.value = null
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (epoch == sharedEpoch) {
                    _projectCoordination.value = null
                    _projectCoordinationError.value = e.message ?: "Project coordination read failed"
                }
            } finally {
                if (epoch == sharedEpoch) _projectCoordinationBusy.value = false
            }
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
                _businessSnapshot.value = null; _businessError.value = null; _businessBusy.value = false; _businessSaved.value = 0
                _sharedInventorySnapshot.value = null; _sharedInventoryError.value = null; _sharedInventoryBusy.value = false
                _expenseCategories.value = emptyList(); _expenseServices.value = emptyList(); _expenseSuppliers.value = emptyList(); expenseOptionsScope = null
                _expenseCatalogueBusy.value = false; _expenseCatalogueLoaded.value = false; _expenseCatalogueError.value = null
                _travelPolicy.value = null; _travelPolicyError.value = null; _travelPolicySaving.value = false; _travelPolicySaveMessage.value = null
                _viewDepartment.value = null
                _selectedTab.value = 0
                _sharedSnapshot.value = null; _sharedError.value = null; _sharedBusy.value = false
                _orgConfig.value = null; _customerChanges.value = null
                _lifecycleWorkspace.value = null; _lifecycleWorkspaceError.value = null; _lifecycleWorkspaceBusy.value = false
                _projectCoordination.value = null; _projectCoordinationError.value = null; _projectCoordinationBusy.value = false
                if (state is AuthState.Authenticated) {
                    state.session.testMembership?.workspaceRole()?.let { scmsRepository.switchRole(it) }
                    refreshSharedWorkspace()
                    refreshLifecycleWorkspace()
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
