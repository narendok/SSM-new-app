package com.example.myapplication.data.model

import kotlinx.serialization.json.*

/** Presentation filters only; the RPC/RLS response remains the access boundary. */
object SharedAccess {
    fun text(row: JsonObject, key: String) = (row[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
    fun rows(snapshot: JsonObject?, key: String) = (snapshot?.get(key) as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

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
