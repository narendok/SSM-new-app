package com.example.myapplication

import com.example.myapplication.data.model.BudgetDisplayState
import com.example.myapplication.data.model.BudgetScopePolicy
import com.example.myapplication.data.model.DepartmentAllocationItem
import com.example.myapplication.data.model.DepartmentRole
import com.example.myapplication.data.model.SharedAccess
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DepartmentContextAndBudgetPolicyTest {

    private val sampleAllocations = listOf(
        DepartmentAllocationItem(
            allocationId = "alloc-ehw-01",
            departmentCode = "EHW",
            departmentName = "Embedded Hardware",
            period = "Q4 2026",
            category = "Lab & Prototyping",
            allocatedAmount = 450000.0,
            committedAmount = 185000.0,
            spentAmount = 142000.0,
            approvedBy = "Finance Lead"
        ),
        DepartmentAllocationItem(
            allocationId = "alloc-pur-01",
            departmentCode = "PUR",
            departmentName = "Purchasing",
            period = "Q4 2026",
            category = "Component Procurement",
            allocatedAmount = 3200000.0,
            committedAmount = 1400000.0,
            spentAmount = 950000.0,
            approvedBy = "Supply Chain Lead"
        )
    )

    private fun createSharedSnapshot(
        memberRole: String = "admin",
        memberId: String = "mem-admin-001",
        departmentsList: List<Pair<String, String>> = listOf("dept-ehw-id" to "EHW", "dept-pur-id" to "PUR")
    ) = buildJsonObject {
        put("membership", buildJsonObject {
            put("id", memberId)
            put("role", memberRole)
            put("organization_id", "org-test-001")
        })
        put("departments", buildJsonArray {
            departmentsList.forEach { (id, code) ->
                add(buildJsonObject {
                    put("id", id)
                    put("code", code)
                    put("name", "$code Department")
                })
            }
        })
        put("tasks", buildJsonArray {
            add(buildJsonObject {
                put("id", "task-1")
                put("title", "Hardware Schematic Review")
                put("department_id", "dept-ehw-id")
                put("department_code", "EHW")
                put("status", "Open")
            })
            add(buildJsonObject {
                put("id", "task-2")
                put("title", "Issue Purchase Order for Passives")
                put("department_id", "dept-pur-id")
                put("department_code", "PUR")
                put("status", "Open")
            })
        })
        put("records", buildJsonArray { })
    }

    @Test
    fun adminDepartmentSelectionChangesViewContextWithoutMutatingIdentityOrPermissions() {
        val snapshot = createSharedSnapshot()
        val membership = snapshot["membership"] as kotlinx.serialization.json.JsonObject
        val initialRoleId = membership["role"]?.jsonPrimitive?.content
        val initialMemberId = membership["id"]?.jsonPrimitive?.content

        val approval = buildJsonObject {
            put("outcome", "pending")
            put("department_id", "dept-pur-id")
        }

        // Selecting a specific department (e.g. PUR)
        val selectedDeptId = "dept-pur-id"
        val viewDepartment: String? = selectedDeptId

        // 1. Authenticated identity and membership role remain immutable
        assertEquals("admin", initialRoleId)
        assertEquals("mem-admin-001", initialMemberId)
        assertTrue(SharedAccess.canDecide(snapshot, approval))

        // 2. View/filter context filters tasks to the selected department
        val tasks = SharedAccess.rows(snapshot, "tasks")
        val filteredTasks = tasks.filter { viewDepartment == null || SharedAccess.text(it, "department_id") == viewDepartment }
        assertEquals(1, filteredTasks.size)
        assertEquals("Issue Purchase Order for Passives", SharedAccess.text(filteredTasks.first(), "title"))

        // 3. Admin permissions to update tasks remain true across any department context
        assertTrue(SharedAccess.canUpdateTask(snapshot, filteredTasks.first()))

        // 4. Switching context to All (null) changes view context only
        val allDeptContext: String? = null
        val allFilteredTasks = tasks.filter { allDeptContext == null || SharedAccess.text(it, "department_id") == allDeptContext }
        assertEquals(2, allFilteredTasks.size)
        assertEquals("admin", membership["role"]?.jsonPrimitive?.content)
    }

    @Test
    fun unknownDepartmentDoesNotSilentlyMapToAllOrAnotherDepartment() {
        val snapshot = createSharedSnapshot()
        val unknownDeptId = "dept-unknown-999"

        // 1. Task filter returns empty, not all tasks or another department
        val tasks = SharedAccess.rows(snapshot, "tasks")
        val filteredTasks = tasks.filter { SharedAccess.text(it, "department_id") == unknownDeptId }
        assertTrue(filteredTasks.isEmpty())

        // 2. Budget policy produces AuthenticatedUnavailable with explicit Unknown Department label
        val budgetState = BudgetScopePolicy.resolveBudgetState(
            isAuthenticated = true,
            sharedSnapshot = snapshot,
            viewDepartment = unknownDeptId,
            currentRole = DepartmentRole.ADMIN,
            localAllocations = sampleAllocations
        )

        assertTrue(budgetState is BudgetDisplayState.AuthenticatedUnavailable)
        val unavailable = budgetState as BudgetDisplayState.AuthenticatedUnavailable
        assertTrue(unavailable.departmentTitle.contains("Unknown Department"))
        assertTrue(unavailable.reason.contains("not found in configured workspace departments"))
    }

    @Test
    fun loadingDepartmentsDoesNotSilentlyMapToAllOrAnotherDepartment() {
        // Snapshot with empty departments (syncing / loading state)
        val loadingSnapshot = createSharedSnapshot(departmentsList = emptyList())
        val deptId = "dept-any"

        val budgetState = BudgetScopePolicy.resolveBudgetState(
            isAuthenticated = true,
            sharedSnapshot = loadingSnapshot,
            viewDepartment = deptId,
            currentRole = DepartmentRole.ADMIN,
            localAllocations = sampleAllocations
        )

        assertTrue(budgetState is BudgetDisplayState.AuthenticatedUnavailable)
        val unavailable = budgetState as BudgetDisplayState.AuthenticatedUnavailable
        assertEquals("Loading...", unavailable.departmentTitle)
        assertTrue(unavailable.reason.contains("Department configurations are loading"))
    }

    @Test
    fun authenticatedModeNeverPresentsLocalSampleBudgetsAsSharedData() {
        val snapshot = createSharedSnapshot()

        // Even with local sample allocations present, authenticated mode must return AuthenticatedUnavailable
        val budgetState = BudgetScopePolicy.resolveBudgetState(
            isAuthenticated = true,
            sharedSnapshot = snapshot,
            viewDepartment = "dept-ehw-id",
            currentRole = DepartmentRole.EHW,
            localAllocations = sampleAllocations
        )

        assertTrue("Authenticated mode must return AuthenticatedUnavailable when no server budget table exists",
            budgetState is BudgetDisplayState.AuthenticatedUnavailable)
        assertFalse("Must never return LocalSample in authenticated mode",
            budgetState is BudgetDisplayState.LocalSample)

        val unavailable = budgetState as BudgetDisplayState.AuthenticatedUnavailable
        assertEquals("EHW Department", unavailable.departmentTitle)
        assertTrue(unavailable.reason.contains("Server-backed shared budget source is not configured or unavailable"))
    }

    @Test
    fun authenticatedAdminAllModeShowsConsolidatedUnavailableWithoutAmounts() {
        val snapshot = createSharedSnapshot()

        val budgetState = BudgetScopePolicy.resolveBudgetState(
            isAuthenticated = true,
            sharedSnapshot = snapshot,
            viewDepartment = null,
            currentRole = DepartmentRole.ADMIN,
            localAllocations = sampleAllocations
        )

        assertTrue(budgetState is BudgetDisplayState.AuthenticatedUnavailable)
        val unavailable = budgetState as BudgetDisplayState.AuthenticatedUnavailable
        assertEquals("All Departments", unavailable.departmentTitle)
        assertTrue(unavailable.reason.contains("Consolidated budget unavailable"))
    }

    @Test
    fun authenticatedLeadBudgetDefaultsToItsMembershipDepartment() {
        val base = createSharedSnapshot(
            memberRole = "department_lead",
            departmentsList = listOf("dept-fw-id" to "FW")
        )
        val snapshot = kotlinx.serialization.json.JsonObject(base + mapOf(
            "membership" to buildJsonObject {
                put("id", "mem-fw-lead")
                put("role", "department_lead")
                put("department_id", "dept-fw-id")
                put("department_code", "FW")
            }
        ))

        val budgetState = BudgetScopePolicy.resolveBudgetState(
            isAuthenticated = true,
            sharedSnapshot = snapshot,
            viewDepartment = null,
            currentRole = DepartmentRole.FW,
            localAllocations = sampleAllocations
        )

        assertTrue(budgetState is BudgetDisplayState.AuthenticatedUnavailable)
        val unavailable = budgetState as BudgetDisplayState.AuthenticatedUnavailable
        assertEquals("FW Department", unavailable.departmentTitle)
        assertFalse(unavailable.departmentTitle.contains("All Departments"))
        assertTrue(unavailable.reason.contains("for FW Department"))
    }

    @Test
    fun localTestAdminAllModeResolvesToLocalAllDeptsUnmodeled() {
        // In Local TEST mode, Admin All mode must never fall back to EHW or any single department's amount
        val budgetState = BudgetScopePolicy.resolveBudgetState(
            isAuthenticated = false,
            sharedSnapshot = null,
            viewDepartment = null,
            currentRole = DepartmentRole.ADMIN,
            localAllocations = sampleAllocations
        )

        assertTrue("Admin All mode in local TEST must resolve to LocalAllDeptsUnmodeled",
            budgetState is BudgetDisplayState.LocalAllDeptsUnmodeled)
        val unmodeled = budgetState as BudgetDisplayState.LocalAllDeptsUnmodeled
        assertTrue(unmodeled.message.contains("Consolidated budget figures are not modeled"))
    }

    @Test
    fun localTestSpecificDepartmentResolvesToClearlyLabeledLocalSample() {
        // Specific department in Local TEST mode
        val budgetState = BudgetScopePolicy.resolveBudgetState(
            isAuthenticated = false,
            sharedSnapshot = null,
            viewDepartment = "PUR",
            currentRole = DepartmentRole.ADMIN,
            localAllocations = sampleAllocations
        )

        assertTrue(budgetState is BudgetDisplayState.LocalSample)
        val sample = budgetState as BudgetDisplayState.LocalSample
        assertEquals("PUR", sample.departmentCode)
        assertEquals(3200000.0, sample.allocation.allocatedAmount, 0.01)
        assertEquals("Component Procurement", sample.allocation.category)
    }

    @Test
    fun localTestUnknownDepartmentDoesNotFallBackToAnotherDepartment() {
        val budgetState = BudgetScopePolicy.resolveBudgetState(
            isAuthenticated = false,
            sharedSnapshot = null,
            viewDepartment = "UNKNOWN_CODE",
            currentRole = DepartmentRole.ADMIN,
            localAllocations = sampleAllocations
        )

        assertTrue(budgetState is BudgetDisplayState.LocalAllDeptsUnmodeled)
        val unmodeled = budgetState as BudgetDisplayState.LocalAllDeptsUnmodeled
        assertTrue(unmodeled.message.contains("No sample budget configured for department 'UNKNOWN_CODE'"))
    }
}
