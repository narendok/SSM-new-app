package com.example.myapplication.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class ProjectCoordinationProjection(
    val records: List<ProjectCoordinationRecord> = emptyList(),
    val tasks: List<ProjectCoordinationTask> = emptyList(),
    val documents: List<ProjectCoordinationDocument> = emptyList()
)

@Serializable
data class ProjectCoordinationRecord(
    val id: String,
    val reference: String = "",
    val title: String = "",
    val type: String = "",
    val status: String = "",
    val fields: ProjectCoordinationRecordFields = ProjectCoordinationRecordFields()
)

@Serializable
data class ProjectCoordinationRecordFields(
    val projectId: String? = null,
    val projectCode: String? = null,
    val stage: String? = null,
    val lifecycleStage: String? = null,
    val currentStage: String? = null,
    val legacyStage: String? = null,
    val companyPhase: String? = null,
    val lifecycleBaseline: String? = null,
    val portfolioStatus: String? = null,
    val track: String? = null,
    val targetDate: String? = null,
    val dependsOn: String? = null,
    val requiredInputs: String? = null,
    val enquiryId: String? = null,
    val owner: String? = null,
    val scope: String? = null,
    val stageHistory: String? = null,
    val stageRemark: String? = null,
    val nextAction: String? = null,
    val fixtureRunId: String? = null,
    val outcome: String? = null,
    val terminalReason: String? = null,
    val totalPlannedUnits: String? = null,
    val lotQuantity: String? = null,
    val acceptedUnits: String? = null,
    val heldUnits: String? = null,
    val pendingInspectionUnits: String? = null,
    val remainingPlannedUnits: String? = null,
    val customer: String? = null
)

@Serializable
data class ProjectCoordinationTask(
    val id: String,
    val reference: String = "",
    val title: String = "",
    val type: String = "",
    val status: String = "",
    val area: String = "",
    val priority: String = "",
    val fields: ProjectCoordinationTaskFields = ProjectCoordinationTaskFields()
)

@Serializable
data class ProjectCoordinationTaskFields(
    val projectId: String? = null,
    val department: String? = null,
    val dueDate: String? = null,
    val assignee: String? = null
)

@Serializable
data class ProjectCoordinationDocument(
    val id: String,
    val title: String = "",
    val type: String = "",
    @Serializable(with = NullSafeReferenceListSerializer::class)
    val linkedRecordRefs: List<String> = emptyList()
)

/** Older TEST documents may serialize a missing linked record as JSON null. */
object NullSafeReferenceListSerializer : KSerializer<List<String>> {
    private val delegate = ListSerializer(String.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): List<String> {
        if (decoder !is JsonDecoder) return delegate.deserialize(decoder)
        val refs = decoder.decodeJsonElement() as? JsonArray ?: return emptyList()
        return refs.mapNotNull { value ->
            if (value == JsonNull) null else (value as? JsonPrimitive)?.takeIf { it.isString }?.content
        }
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        if (encoder is JsonEncoder) encoder.encodeJsonElement(JsonArray(value.map(::JsonPrimitive)))
        else delegate.serialize(encoder, value)
    }
}
