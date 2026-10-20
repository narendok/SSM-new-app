package com.example.myapplication.data.model

import kotlinx.serialization.json.*

/** Presentation filters only; the RPC/RLS response remains the access boundary. */
object SharedAccess {
    fun text(row: JsonObject, key: String) = (row[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
    fun rows(snapshot: JsonObject?, key: String) = (snapshot?.get(key) as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
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
    fun canDecide(snapshot: JsonObject?, approval: JsonObject): Boolean {
        val member = snapshot?.get("membership") as? JsonObject ?: return false
        if (text(approval, "outcome") != "pending") return false
        return text(member, "role") == "admin" || (text(member, "role") == "department_lead" &&
            departments(snapshot).any { text(it, "id") == text(approval, "reviewer_department_id") })
    }
}
