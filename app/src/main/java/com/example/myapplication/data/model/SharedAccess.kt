package com.example.myapplication.data.model

import kotlinx.serialization.json.*

/** Presentation filters only; the RPC/RLS response remains the access boundary. */
object SharedAccess {
    fun text(row: JsonObject, key: String) = (row[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
    fun rows(snapshot: JsonObject?, key: String) = (snapshot?.get(key) as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

    private val inactiveMasterStatuses = setOf("archived", "obsolete", "rejected", "inactive", "cancelled", "canceled", "retired", "disabled")

    private fun isSelectableMasterStatus(status: String) = status.trim().lowercase() !in inactiveMasterStatuses

    /** Canonical, authorized choices for Web catalogue master fields in Android record forms. */
    fun masterPickerOptions(snapshot: JsonObject?, master: String, projectCatalog: List<ProjectMetadata> = projects(snapshot).drop(1)): List<SharedPickerOption> {
        val optionKey = when (master) {
            "customers" -> "customerOptions"
            "suppliers" -> "supplierOptions"
            "parts" -> "partOptions"
            "products" -> "productOptions"
            else -> ""
        }
        if (master == "projects") {
            // Keep the server-authorized project catalogue as the source of truth,
            // but do not offer archived/inactive records for new links. The general
            // project browser can still retain archived rows for historical review.
            val selectableProjectIds = rows(snapshot, "records")
                .filter { text(it, "record_type").equals("Project", ignoreCase = true) }
                .filter { text(it, "archived_at").isBlank() && text(it, "archived") != "true" }
                .filter { isSelectableMasterStatus(text(it, "status")) }
                .map { text(it, "id") }
                .filter(String::isNotBlank)
                .toSet()
            return projectCatalog
            .filter { it.id != "ALL" && it.id in selectableProjectIds && isSelectableMasterStatus(it.status) }
            .map { SharedPickerOption(it.id, it.reference, it.title, it.status) }
            .distinctBy { it.id }
            .sortedBy { it.reference.lowercase() }
        }
        if (master == "people") {
            val departmentsById = rows(snapshot, "departments").associateBy { text(it, "id") }
            return rows(snapshot, "members")
                .mapNotNull { member ->
                    val id = text(member, "id")
                    if (id.isBlank()) null else {
                        val deptCode = departmentsById[text(member, "department_id")]?.let { text(it, "code") }.orEmpty()
                        val name = text(member, "display_name").ifBlank { "Unnamed member" }
                        SharedPickerOption(id, "", name, "Active", description = deptCode)
                    }
                }
                .distinctBy { it.id }
                .sortedBy { it.title.lowercase() }
        }
        if (master == "documentTypes") return rows(snapshot, "documents")
            .map { text(it, "document_type") }
            .filter(String::isNotBlank)
            .distinct()
            .sortedBy(String::lowercase)
            .map { SharedPickerOption(it, "", it) }

        val recordTypes = when (master) {
            "customers" -> setOf("customer")
            "suppliers" -> setOf("supplier")
            "parts" -> setOf("part")
            "products" -> setOf("product")
            "locations" -> setOf("location", "facility")
            "routes" -> setOf("production route", "route")
            else -> emptySet()
        }
        val byId = linkedMapOf<String, SharedPickerOption>()
        rows(snapshot, "records").forEach { record ->
            if (text(record, "record_type").trim().lowercase() !in recordTypes || !isSelectableMasterStatus(text(record, "status"))) return@forEach
            val id = text(record, "id")
            if (id.isBlank()) return@forEach
            val fields = record["fields"] as? JsonObject
            byId[id] = SharedPickerOption(
                id = id,
                reference = text(record, "reference"),
                title = text(record, "title"),
                status = text(record, "status"),
                mpn = fields?.get("mpn")?.jsonPrimitive?.contentOrNull.orEmpty()
            )
        }
        if (optionKey.isNotBlank()) rows(snapshot, optionKey).forEach { option ->
            val id = text(option, "id")
            if (id.isBlank()) return@forEach
            if (!isSelectableMasterStatus(text(option, "status"))) {
                byId.remove(id)
                return@forEach
            }
            val visible = byId[id]
            byId[id] = SharedPickerOption(
                id = id,
                reference = text(option, "reference").ifBlank { visible?.reference.orEmpty() },
                title = text(option, "title").ifBlank { visible?.title.orEmpty() },
                status = text(option, "status"),
                mpn = text(option, "mpn").ifBlank { visible?.mpn.orEmpty() }
            )
        }
        return byId.values.sortedBy { it.reference.lowercase() }
    }

    /** Project picker options from the server-authorized workspace projection. */
    fun projects(snapshot: JsonObject?): List<ProjectMetadata> {
        val permitted = rows(snapshot, "records")
            .filter { text(it, "record_type") == "Project" }
            .mapNotNull { row ->
                val id = text(row, "id")
                if (id.isBlank()) return@mapNotNull null
                ProjectMetadata(
                    id = id,
                    reference = text(row, "reference").ifBlank { id },
                    title = text(row, "title").ifBlank { "Untitled project" },
                    client = "", volume = "", totalValue = "",
                    stage = text(row, "stage"), status = text(row, "status"), deliverable = ""
                )
            }
            .distinctBy { it.id }
        return listOf(ProjectMetadata("ALL", "ALL", "All permitted projects", "", "", "", "", "", "")) + permitted
    }

    /** Resolve a task only through explicit project IDs or an explicit linked-record project field. */
    fun projectIdForTask(snapshot: JsonObject?, task: JsonObject): String {
        val projects = projects(snapshot).drop(1)
        val directLink = text(task, "project_id").ifBlank { text(task, "record_id") }
        if (projects.any { it.id == directLink }) return directLink
        if (directLink.isBlank()) return ""
        val linkedRecord = rows(snapshot, "records").firstOrNull { text(it, "id") == directLink } ?: return ""
        val fields = linkedRecord["fields"] as? JsonObject ?: return ""
        val linkedProject = fields["project_id"]?.jsonPrimitive?.contentOrNull
            ?: fields["projectId"]?.jsonPrimitive?.contentOrNull
            ?: fields["project_code"]?.jsonPrimitive?.contentOrNull
            ?: fields["projectCode"]?.jsonPrimitive?.contentOrNull
            ?: return ""
        return projects.firstOrNull { it.id == linkedProject || it.reference == linkedProject }?.id.orEmpty()
    }

    fun taskLane(status: String): String = when {
        status.equals("Complete", true) || status.equals("Completed", true) -> "Completed"
        status.equals("In review", true) || status.equals("Review", true) -> "Review"
        status.equals("In Progress", true) -> "Today"
        else -> "To Do"
    }
    fun departments(snapshot: JsonObject?): List<JsonObject> {
        val member = snapshot?.get("membership") as? JsonObject ?: return emptyList()
        val role = text(member, "role")
        if (role == "viewer") return emptyList()
        return rows(snapshot, "departments").filter {
            role == "admin" || text(it, "code") == text(member, "department_code")
        }
    }
    fun canUpdateTask(snapshot: JsonObject?, task: JsonObject): Boolean {
        val member = snapshot?.get("membership") as? JsonObject ?: return false
        return when(text(member, "role")) {
            "admin" -> true
            "member" -> text(task, "assignee_id") == text(member, "membership_id")
            "department_lead" -> departments(snapshot).any { text(it, "id") == text(task, "department_id") }
            else -> false
        }
    }
    fun canSetTaskStatus(task: JsonObject, status: String): Boolean =
        !(text(task, "status") == "In review" && status == "Complete")

    fun canRequestReview(snapshot: JsonObject?, record: JsonObject): Boolean {
        val member = snapshot?.get("membership") as? JsonObject ?: return false
        if (text(record, "record_type") == "Project") return false
        val role = text(member, "role")
        return role != "viewer" && text(record, "department_id").isNotBlank()
    }

    fun canDecide(snapshot: JsonObject?, approval: JsonObject): Boolean {
        val member = snapshot?.get("membership") as? JsonObject ?: return false
        if (text(approval, "outcome") != "pending") return false
        val role = text(member, "role")
        return role == "admin" || (role != "viewer" &&
            departments(snapshot).any { text(it, "id") == text(approval, "reviewer_department_id") })
    }

    /** Resolve a document only through an explicit project ID or an explicit linked-record project field. */
    fun projectIdForDocument(snapshot: JsonObject?, doc: JsonObject): String {
        val projects = projects(snapshot).drop(1)
        val directLink = text(doc, "record_id")
        if (directLink.isBlank()) return ""
        if (projects.any { it.id == directLink }) return directLink
        val linkedRecord = rows(snapshot, "records").firstOrNull { text(it, "id") == directLink } ?: return ""
        if (text(linkedRecord, "record_type") == "Project") return text(linkedRecord, "id")
        val fields = linkedRecord["fields"] as? JsonObject ?: return ""
        val linkedProject = fields["project_id"]?.jsonPrimitive?.contentOrNull
            ?: fields["projectId"]?.jsonPrimitive?.contentOrNull
            ?: fields["project_code"]?.jsonPrimitive?.contentOrNull
            ?: fields["projectCode"]?.jsonPrimitive?.contentOrNull
            ?: return ""
        return projects.firstOrNull { it.id == linkedProject || it.reference == linkedProject }?.id.orEmpty()
    }

    /**
     * Extracts document metadata from the shared TEST workspace snapshot.
     * Note: get_shared_test_workspace() authorizes and returns documents scoped strictly
     * by department membership; selectedProjectId provides an in-memory client-side view filter
     * rather than server-side project authorization.
     */
    fun sharedDocuments(snapshot: JsonObject?, selectedProjectId: String): List<SharedDocumentItem> {
        if (snapshot == null) return emptyList()
        val depts = rows(snapshot, "departments")
        val records = rows(snapshot, "records")
        return rows(snapshot, "documents").mapNotNull { doc ->
            val id = text(doc, "id")
            if (id.isBlank()) return@mapNotNull null
            val projectId = projectIdForDocument(snapshot, doc)
            val deptId = text(doc, "department_id")
            val deptCode = depts.firstOrNull { text(it, "id") == deptId }?.let { text(it, "code") }.orEmpty()
            val recordId = text(doc, "record_id")
            val recordRef = records.firstOrNull { text(it, "id") == recordId }?.let { text(it, "reference") }.orEmpty()
            SharedDocumentItem(
                id = id,
                title = text(doc, "title").ifBlank { "Untitled Document" },
                documentType = text(doc, "document_type").ifBlank { "Document" },
                departmentCode = deptCode,
                linkedRecordReference = recordRef,
                linkedRecordId = recordId,
                projectId = projectId,
                createdAt = text(doc, "created_at")
            )
        }.filter { item ->
            if (selectedProjectId == "ALL") true
            else item.projectId == selectedProjectId
        }
    }
}

data class SharedDocumentItem(
    val id: String,
    val title: String,
    val documentType: String,
    val departmentCode: String,
    val linkedRecordReference: String,
    val linkedRecordId: String = "",
    val projectId: String,
    val createdAt: String
)

data class SharedPickerOption(
    val id: String,
    val reference: String,
    val title: String,
    val status: String = "Active",
    val mpn: String = "",
    val description: String = ""
) {
    val label: String get() = listOf(reference, title, mpn, description).filter(String::isNotBlank).joinToString(" · ")
}
