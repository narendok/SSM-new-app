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
    @Test fun onlyDesignatedDepartmentCanDecidePendingReview() {
        val approval = row("""{"outcome":"pending","department_id":"hw","reviewer_department_id":"qa"}""")
        assertFalse(SharedAccess.canDecide(snapshot("department_lead", "EHW"), approval))
        assertTrue(SharedAccess.canDecide(snapshot("department_lead", "QA"), approval))
        assertTrue(SharedAccess.canDecide(snapshot("member", "QA"), approval))
        assertFalse(SharedAccess.canDecide(snapshot("member", "EHW"), approval))
        assertFalse(SharedAccess.canDecide(snapshot("viewer", "CLIENT"), approval))
        assertFalse(SharedAccess.canDecide(snapshot("admin", ""), row("""{"outcome":"approved"}""")))
    }

    @Test fun taskInReviewCannotBeCompletedWithoutResolvingLinkedReview() {
        val task = row("""{"status":"In review"}""")
        assertFalse(SharedAccess.canSetTaskStatus(task, "Complete"))
        assertTrue(SharedAccess.canSetTaskStatus(task, "Open"))
        assertTrue(SharedAccess.canSetTaskStatus(row("""{"status":"Open"}"""), "Complete"))
    }

    @Test fun authenticatedProjectCatalogUsesOnlyServerReturnedProjectsAndExplicitTaskLinks() {
        val data = row("""{
          "records":[
            {"id":"project-1","record_type":"Project","reference":"P-001","title":"Tracker PoC","status":"Draft","stage":"CM_01"},
            {"id":"record-1","record_type":"DVP&R","reference":"DVP-1","title":"Validation","fields":{"project_code":"P-001"}},
            {"id":"other-record","record_type":"DVP&R","reference":"DVP-2","title":"Unlinked","fields":{}}
          ]
        }""")

        val projects = SharedAccess.projects(data)
        assertEquals(listOf("ALL", "project-1"), projects.map { it.id })
        assertEquals("Tracker PoC", projects.last().title)
        assertEquals("project-1", SharedAccess.projectIdForTask(data, row("""{"record_id":"project-1"}""")))
        assertEquals("project-1", SharedAccess.projectIdForTask(data, row("""{"record_id":"record-1"}""")))
        assertEquals("", SharedAccess.projectIdForTask(data, row("""{"record_id":"other-record"}""")))
        assertEquals("", SharedAccess.projectIdForTask(data, row("""{"record_id":"missing-record"}""")))
    }

    @Test fun authenticatedDocumentsUseExplicitProjectLinksAndHonorsProjectFilter() {
        val data = row("""{
          "departments":[{"id":"dept-hw","code":"EHW"}],
          "records":[
            {"id":"project-1","record_type":"Project","reference":"P-001","title":"Tracker PoC"},
            {"id":"record-1","record_type":"PRS","reference":"PRS-01","fields":{"project_id":"project-1"}},
            {"id":"unlinked-record","record_type":"PRS","reference":"PRS-02","fields":{}}
          ],
          "documents":[
            {"id":"doc-direct","title":"Project charter","document_type":"Specification","department_id":"dept-hw","record_id":"project-1","created_at":"2026-10-31T10:00:00Z"},
            {"id":"doc-indirect","title":"Hardware PRS","document_type":"Requirement","department_id":"dept-hw","record_id":"record-1","created_at":"2026-10-31T11:00:00Z"},
            {"id":"doc-unlinked","title":"Unlinked note","document_type":"Note","department_id":"dept-hw","record_id":"unlinked-record","created_at":"2026-10-31T12:00:00Z"},
            {"id":"doc-missing-link","title":"Missing link","document_type":"Draft","department_id":"dept-hw","record_id":"nonexistent","created_at":"2026-10-31T13:00:00Z"}
          ]
        }""")

        assertEquals("project-1", SharedAccess.projectIdForDocument(data, row("""{"record_id":"project-1"}""")))
        assertEquals("project-1", SharedAccess.projectIdForDocument(data, row("""{"record_id":"record-1"}""")))
        assertEquals("", SharedAccess.projectIdForDocument(data, row("""{"record_id":"unlinked-record"}""")))
        assertEquals("", SharedAccess.projectIdForDocument(data, row("""{"record_id":"nonexistent"}""")))

        val allDocs = SharedAccess.sharedDocuments(data, "ALL")
        assertEquals(4, allDocs.size)
        assertEquals("EHW", allDocs[0].departmentCode)
        assertEquals("P-001", allDocs[0].linkedRecordReference)
        assertEquals("PRS-01", allDocs[1].linkedRecordReference)

        val projectDocs = SharedAccess.sharedDocuments(data, "project-1")
        assertEquals(listOf("doc-direct", "doc-indirect"), projectDocs.map { it.id })

        val otherProjectDocs = SharedAccess.sharedDocuments(data, "other-project")
        assertTrue(otherProjectDocs.isEmpty())

        assertTrue(SharedAccess.sharedDocuments(null, "ALL").isEmpty())
    }

    @Test fun sharedTaskStatusesMapToTheAgreedMobileBoardLanes() {
        assertEquals("Today", SharedAccess.taskLane("In Progress"))
        assertEquals("Review", SharedAccess.taskLane("In review"))
        assertEquals("To Do", SharedAccess.taskLane("Open"))
        assertEquals("Completed", SharedAccess.taskLane("Complete"))
    }

    @Test fun nonViewerMaySeeReviewRequestAffordanceButServerStillAuthorizes() {
        val record = row("""{"record_type":"DVP&R","department_id":"hw"}""")
        assertTrue(SharedAccess.canRequestReview(snapshot("admin", ""), record))
        assertTrue(SharedAccess.canRequestReview(snapshot("department_lead", "EHW"), record))
        assertTrue(SharedAccess.canRequestReview(snapshot("department_lead", "QA"), record))
        assertTrue(SharedAccess.canRequestReview(snapshot("member", "EHW"), record))
        assertFalse(SharedAccess.canRequestReview(snapshot("viewer", "EHW"), record))
        assertFalse(SharedAccess.canRequestReview(snapshot("member", "EHW"), row("""{"record_type":"DVP&R"}""")))
        assertFalse(SharedAccess.canRequestReview(snapshot("admin", ""), row("""{"record_type":"Project","department_id":"hw"}""")))
    }
}
