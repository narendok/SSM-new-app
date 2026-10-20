package com.example.myapplication
import com.example.myapplication.data.model.*
import org.junit.Assert.*
import org.junit.Test
class TestAccountsTest {
    @Test fun catalogHasAllExistingCanonicalAccounts() {
        assertEquals(26, TestAccounts.all.size)
        assertEquals(26, TestAccounts.all.map { it.email }.distinct().size)
        assertEquals(12, TestAccounts.all.count { it.label.endsWith("Employee") })
        assertTrue(TestAccounts.all.any { it.email == "employee.hr@example.com" && it.password == "Demo@HREMP2026" })
        assertFalse(TestAccounts.all.any { it.email == "employee.hrops@example.com" })
        assertFalse(TestAccounts.all.any { it.label.startsWith("ESW") || it.label.startsWith("TST") })
    }
    @Test fun workspaceUsesServerRoleRatherThanEmailOrDefaultAdmin() {
        fun member(role: String, dept: String?) = TestMembership("TEST ONLY \u00b7 SSM-new1 synthetic workspace", role, dept, "Test")
        assertEquals(DepartmentRole.HROPS, member("member", "HR").workspaceRole())
        assertEquals(DepartmentRole.QA, member("department_lead", "QA").workspaceRole())
        assertEquals(DepartmentRole.CLIENT, member("viewer", null).workspaceRole())
        assertNull(member("member", "UNKNOWN").workspaceRole())
        assertNull(member("member", null).workspaceRole())
        assertNull(member("unrecognized", "QA").workspaceRole())
    }
}
