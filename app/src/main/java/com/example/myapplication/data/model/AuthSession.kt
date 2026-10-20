package com.example.myapplication.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthUser(
    val id: String = "",
    val email: String? = null,
    @SerialName("user_metadata") val userMetadata: Map<String, String>? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class AuthSession(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long = 3600,
    @SerialName("expires_at") val expiresAt: Long = 0,
    val user: AuthUser? = null,
    val testMembership: TestMembership? = null
)

sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticating(val message: String) : AuthState()
    data class Authenticated(val session: AuthSession) : AuthState()
    data class Error(val message: String) : AuthState()
}

@Serializable
data class TestMembership(
    @SerialName("organization_name") val organizationName: String,
    val role: String,
    @SerialName("department_code") val departmentCode: String? = null,
    @SerialName("display_name") val displayName: String
) {
    fun workspaceRole(): DepartmentRole? = when (role) {
        "admin" -> DepartmentRole.ADMIN
        "viewer" -> DepartmentRole.CLIENT
        "department_lead", "member" -> DepartmentRole.entries.firstOrNull {
            !it.isLegacy && !it.isClient && it != DepartmentRole.ADMIN &&
                (it.code == departmentCode || (it == DepartmentRole.HROPS && departmentCode == "HR"))
        }
        else -> null
    }
}
