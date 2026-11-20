package com.example.myapplication.data.model

import kotlinx.serialization.json.JsonObject

/**
 * Budget display state representing the resolved budget context.
 * Strict invariant: Never present local sample amounts as authenticated shared data,
 * and never fall back to a single department's amount in Admin All mode.
 */
sealed class BudgetDisplayState {
    data class AuthenticatedServerBudget(
        val departmentTitle: String,
        val allocation: DepartmentAllocationItem
    ) : BudgetDisplayState()

    data class AuthenticatedUnavailable(
        val departmentTitle: String,
        val reason: String
    ) : BudgetDisplayState()

    data class LocalSample(
        val departmentCode: String,
        val allocation: DepartmentAllocationItem
    ) : BudgetDisplayState()

    data class LocalAllDeptsUnmodeled(
        val message: String
    ) : BudgetDisplayState()
}

/**
 * Enforces strict budget data labeling and scoping:
 * 1. In authenticated mode: Shows amounts ONLY if a verified server-backed shared budget source exists;
 *    otherwise returns AuthenticatedUnavailable without amounts or "Within Budget".
 * 2. In Admin All mode: Never falls back to one department's amount or labels it consolidated.
 * 3. In Local TEST mode: Labels sample clearly as LocalSample, never confusing it with live data.
 * 4. Unknown/loading departments: Never silently map to All or another department.
 */
object BudgetScopePolicy {
    fun resolveBudgetState(
        isAuthenticated: Boolean,
        sharedSnapshot: JsonObject?,
        viewDepartment: String?,
        currentRole: DepartmentRole,
        localAllocations: List<DepartmentAllocationItem>
    ): BudgetDisplayState {
        if (isAuthenticated) {
            val configuredDepts = SharedAccess.departments(sharedSnapshot)
            val membership = sharedSnapshot?.get("membership") as? JsonObject
            val isAdmin = membership?.let { SharedAccess.text(it, "role") } == "admin"
            val memberDepartmentCode = membership?.let { SharedAccess.text(it, "department_code") }.orEmpty().uppercase().let { code ->
                when (code) {
                    "EFW" -> "FW"
                    "CSW" -> "SW"
                    "HR", "OPS", "HR/OPS" -> "HROPS"
                    else -> code
                }
            }
            // A department lead's budget context comes from the signed-in server
            // membership. The optional selector is only an Admin presentation filter.
            val targetDepartmentId = if (isAdmin) viewDepartment else {
                membership?.let { SharedAccess.text(it, "department_id") }.orEmpty().ifBlank {
                    configuredDepts.firstOrNull {
                        SharedAccess.text(it, "code").equals(memberDepartmentCode, true)
                    }?.let { SharedAccess.text(it, "id") }.orEmpty()
                }
            }

            val (deptTitle, reason) = when {
                isAdmin && targetDepartmentId == null -> {
                    Pair(
                        "All Departments",
                        "Consolidated budget unavailable. Server-backed shared budget source is not configured in this workspace."
                    )
                }
                configuredDepts.isEmpty() -> {
                    Pair(
                        "Loading...",
                        "Department configurations are loading. Shared budget source is unavailable."
                    )
                }
                else -> {
                    val dept = configuredDepts.firstOrNull { SharedAccess.text(it, "id") == targetDepartmentId }
                    if (dept != null) {
                        val name = SharedAccess.text(dept, "name").ifBlank { SharedAccess.text(dept, "code") }
                        Pair(
                            name,
                            "Server-backed shared budget source is not configured or unavailable for $name. No shared financial figures to display."
                        )
                    } else {
                        val unknownDepartment = targetDepartmentId.orEmpty()
                        Pair(
                            when {
                                isAdmin && unknownDepartment.isNotBlank() -> "Unknown Department ($unknownDepartment)"
                                memberDepartmentCode.isNotBlank() -> memberDepartmentCode
                                else -> "Unknown Department"
                            },
                            when {
                                isAdmin && unknownDepartment.isNotBlank() -> "Department '$unknownDepartment' is not found in configured workspace departments. No shared financial figures to display."
                                memberDepartmentCode.isNotBlank() -> "Department configuration is loading or unavailable for $memberDepartmentCode. No shared financial figures to display."
                                else -> "The authenticated membership has no mapped department. No shared financial figures to display."
                            }
                        )
                    }
                }
            }

            // Check for verified server-backed shared budget source in sharedSnapshot records
            val serverBudgetRows = SharedAccess.rows(sharedSnapshot, "records")
                .filter { SharedAccess.text(it, "record_type").equals("Budget", ignoreCase = true) }

            return if (serverBudgetRows.isNotEmpty()) {
                // Future server budget mapping when verified table/RPC is deployed
                BudgetDisplayState.AuthenticatedUnavailable(deptTitle, "Server-backed shared budget source is not verified.")
            } else {
                BudgetDisplayState.AuthenticatedUnavailable(deptTitle, reason)
            }
        } else {
            // Local TEST mode
            val isAllDepts = currentRole == DepartmentRole.ADMIN && viewDepartment == null
            if (isAllDepts) {
                return BudgetDisplayState.LocalAllDeptsUnmodeled(
                    "Consolidated budget figures are not modeled in local sample data. Select a specific department to view department-specific sample allocations."
                )
            }

            val targetCode = if (viewDepartment != null) {
                val match = localAllocations.firstOrNull { it.departmentCode.equals(viewDepartment, ignoreCase = true) }
                if (match == null) {
                    return BudgetDisplayState.LocalAllDeptsUnmodeled(
                        "No sample budget configured for department '$viewDepartment'. Unknown departments do not map to other departments."
                    )
                }
                match.departmentCode
            } else {
                currentRole.code
            }

            val alloc = localAllocations.firstOrNull { it.departmentCode.equals(targetCode, ignoreCase = true) }
            return if (alloc != null) {
                BudgetDisplayState.LocalSample(alloc.departmentCode, alloc)
            } else {
                BudgetDisplayState.LocalAllDeptsUnmodeled("No sample budget configured for ${currentRole.code}.")
            }
        }
    }
}
