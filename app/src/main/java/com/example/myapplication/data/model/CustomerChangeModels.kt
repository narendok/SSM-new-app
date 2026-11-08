package com.example.myapplication.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class CustomerChangeSnapshot(
    val requests: List<CustomerChangeRequest> = emptyList()
)

@Serializable
data class CustomerChangeRequest(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("project_record_id") val projectRecordId: String,
    @SerialName("input_sheet_id") val inputSheetId: String,
    @SerialName("requested_by") val requestedBy: String,
    val title: String,
    @SerialName("customer_reference") val customerReference: String,
    val description: String,
    val rationale: String,
    val status: String,
    @SerialName("baseline_revision") val baselineRevision: Int,
    @SerialName("baseline_version") val baselineVersion: Int,
    @SerialName("before_values") val beforeValues: JsonObject,
    @SerialName("proposed_values") val proposedValues: JsonObject,
    val version: Int,
    @SerialName("decision_notes") val decisionNotes: String,
    @SerialName("approved_revision") val approvedRevision: Int? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val impacts: List<CustomerChangeImpact> = emptyList(),
    val events: List<CustomerChangeEvent> = emptyList()
)

@Serializable
data class CustomerChangeImpact(
    val id: String,
    @SerialName("department_id") val departmentId: String,
    @SerialName("department_code") val departmentCode: String,
    @SerialName("impact_scope") val impactScope: CustomerChangeImpactScope,
    val status: String,
    val assessment: String,
    val outcome: String,
    @SerialName("assessed_by") val assessedBy: String? = null,
    @SerialName("assessed_at") val assessedAt: String? = null,
    val version: Int,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class CustomerChangeImpactScope(
    val tracks: List<String>,
    val stages: List<String>,
    val documents: List<String>,
    val bom: List<String>,
    @SerialName("reusableAssets") val reusableAssets: List<String>
)

@Serializable
data class CustomerChangeEvent(
    val id: String,
    @SerialName("actor_id") val actorId: String,
    @SerialName("actor_name") val actorName: String? = null,
    val action: String,
    @SerialName("request_version") val requestVersion: Int,
    val details: JsonObject,
    @SerialName("created_at") val createdAt: String
)
