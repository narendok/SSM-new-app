package com.example.myapplication
import com.example.myapplication.data.model.SharedAccess
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
class SharedAccessTest {
    private fun snapshot(role: String, code: String) = Json.parseToJsonElement("""{
      "membership":{"role":"$role","department_code":"$code","membership_id":"me"},
      "departments":[{"id":"hw","code":"EHW"},{"id":"qa","code":"QA"}]
    }""").jsonObject
    private fun row(value: String) = Json.parseToJsonElement(value).jsonObject
    @Test fun departmentNavigationNeverTurnsLeadIntoAdminOrAnotherDepartment() {
        assertEquals(1, SharedAccess.departments(snapshot("department_lead", "EHW")).size)
        assertEquals("hw", SharedAccess.text(SharedAccess.departments(snapshot("department_lead", "EHW")).single(), "id"))
        assertEquals(2, SharedAccess.departments(snapshot("admin", "")).size)
        assertTrue(SharedAccess.departments(snapshot("viewer", "CLIENT")).isEmpty())
        assertTrue(SharedAccess.departments(snapshot("member", "UNKNOWN")).isEmpty())
        assertTrue(SharedAccess.departments(null).isEmpty())
    }
    @Test fun employeeCannotEditColleagueTaskAndLeadCannotEditOtherDepartmentTask() {
        val task = row("""{"assignee_id":"someone-else","department_id":"hw"}""")
        assertFalse(SharedAccess.canUpdateTask(snapshot("member", "EHW"), task))
        assertTrue(SharedAccess.canUpdateTask(snapshot("department_lead", "EHW"), task))
        assertFalse(SharedAccess.canUpdateTask(snapshot("department_lead", "QA"), task))
        assertFalse(SharedAccess.canUpdateTask(snapshot("viewer", "CLIENT"), task))
        assertTrue(SharedAccess.canUpdateTask(snapshot("member", "EHW"), row("""{"assignee_id":"me","department_id":"hw"}""")))
    }
    @Test fun onlyDesignatedReviewerCanDecidePendingReview() {
        val approval = row("""{"outcome":"pending","department_id":"hw","reviewer_department_id":"qa"}""")
        assertFalse(SharedAccess.canDecide(snapshot("department_lead", "EHW"), approval))
        assertTrue(SharedAccess.canDecide(snapshot("department_lead", "QA"), approval))
        assertFalse(SharedAccess.canDecide(snapshot("member", "QA"), approval))
        assertFalse(SharedAccess.canDecide(snapshot("viewer", "CLIENT"), approval))
        assertFalse(SharedAccess.canDecide(snapshot("admin", ""), row("""{"outcome":"approved"}""")))
    }
}
