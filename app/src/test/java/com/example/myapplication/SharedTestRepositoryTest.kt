package com.example.myapplication

import com.example.myapplication.data.repository.SharedTestRepository
import com.example.myapplication.data.model.ProjectCoordinationDocument
import com.example.myapplication.data.model.ProjectCoordinationProjection
import com.example.myapplication.data.model.ProjectCoordinationRecord
import com.example.myapplication.data.model.ProjectCoordinationRecordFields
import com.example.myapplication.data.model.ProjectCoordinationTask
import com.example.myapplication.data.model.ProjectCoordinationTaskFields
import com.example.myapplication.data.model.reviewForProject
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class SharedTestRepositoryTest {
    @Test fun lifecycleWorkspaceLoadsFromAuthenticatedSharedRpc() = runBlocking {
        val repository = SharedTestRepository { method, body, token ->
            assertEquals("get_lifecycle_workspace", method)
            assertEquals("{}", body)
            assertEquals("test-session", token)
            Result.success("""{"revision":{"id":"rev-1"},"files":[],"assets":[{"id":"asset-rev-1"}],"uses":[]}""")
        }
        assertEquals("asset-rev-1", repository.loadLifecycleWorkspace("test-session")["assets"]!!.jsonArray
            .first().jsonObject["id"]!!.jsonPrimitive.content)
    }

    @Test fun reusableAssetLinkUsesExactRevisionStableRetryAndValidatesRequiredFields() = runBlocking {
        val requestId = "20000000-0000-4000-8000-000000000088"
        val bodies = mutableListOf<String>()
        val payload = buildJsonObject {
            put("projectId", "consumer-project")
            put("assetRevisionId", "asset-revision-01")
            put("assessment", "Check connector fit and enclosure clearance before reuse")
        }
        val repository = SharedTestRepository { method, body, token ->
            assertEquals("mutate_lifecycle_workspace", method)
            assertEquals("test-session", token)
            bodies += body
            Result.success("""{"id":"use-1","action":"link_asset"}""")
        }
        repeat(2) { assertEquals("use-1", repository.linkReusableAsset("test-session", payload, requestId)["id"]!!.jsonPrimitive.content) }
        assertEquals(bodies[0], bodies[1])
        val sent = Json.parseToJsonElement(bodies[0]).jsonObject
        assertEquals("link_asset", sent["p_action"]!!.jsonPrimitive.content)
        assertEquals(requestId, sent["p_request_id"]!!.jsonPrimitive.content)
        assertEquals(payload, sent["p_payload"])
        try {
            repository.linkReusableAsset("test-session", buildJsonObject { put("projectId", "consumer-project") }, requestId)
            fail("An exact revision and compatibility assessment are required")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun reusableAssetLinkRejectsReceiptForDifferentAction() = runBlocking {
        val repository = SharedTestRepository { method, _, token ->
            assertEquals("mutate_lifecycle_workspace", method)
            assertEquals("test-session", token)
            Result.success("""{"id":"use-1","action":"approve_gate"}""")
        }
        val payload = buildJsonObject {
            put("projectId", "consumer-project")
            put("assetRevisionId", "asset-revision-01")
            put("assessment", "Verify fit before adoption")
        }
        try {
            repository.linkReusableAsset("test-session", payload, "20000000-0000-4000-8000-000000000089")
            fail("A receipt for another action must not count as a successful asset link")
        } catch (error: IllegalArgumentException) {
            assertEquals("Unexpected asset-use receipt action", error.message)
        }
    }

    @Test fun documentRegistrationRetainsRetryIdentityAndReturnsServerReceipt() = runBlocking {
        val requests = mutableListOf<String>()
        val repository = SharedTestRepository { method, body, token ->
            assertEquals("register_controlled_document", method)
            assertEquals("session", token)
            requests += body
            Result.success("""{"id":"saved-document","status":"Draft"}""")
        }
        val payload = buildJsonObject { put("scope", "internal"); put("projectId", "project-id"); put("title", "Schematic"); put("documentType", "Schematic") }
        val requestId = "20000000-0000-4000-8000-000000000099"
        repeat(2) {
            val receipt = repository.registerDocument("session", payload, requestId)
            assertEquals("saved-document", receipt["id"]!!.jsonPrimitive.content)
            assertEquals("Draft", receipt["status"]!!.jsonPrimitive.content)
        }
        assertEquals(requests[0], requests[1])
        val request = Json.parseToJsonElement(requests.first()).jsonObject
        assertEquals(requestId, request["p_request_id"]!!.jsonPrimitive.content)
        assertEquals(payload, request["p_payload"])
        assertFalse(request.containsKey("organization_id"))
    }
    @Test fun originalAccessUsesCanonicalIdAndCurrentSessionAndFailsClosed() = runBlocking {
        val fileId = "20000000-0000-4000-8000-000000000001"
        var calls = 0
        val repository = SharedTestRepository { method, body, token ->
            calls++
            assertEquals("get_controlled_file_access", method)
            assertEquals("current-session", token)
            assertEquals(fileId, Json.parseToJsonElement(body).jsonObject["p_file_id"]!!.jsonPrimitive.content)
            Result.success("[]")
        }
        try { repository.controlledFileAccess("current-session", fileId); fail("Missing access must fail") }
        catch (_: SecurityException) { }
        try { repository.controlledFileAccess("", fileId); fail("Missing session must fail before transport") }
        catch (_: IllegalArgumentException) { }
        try { repository.controlledFileAccess("current-session", "Drawing.pdf"); fail("Filename is not an identity") }
        catch (_: IllegalArgumentException) { }
        assertEquals(1, calls)
    }
    @Test fun originalRegistrationUsesServerDerivedTestScopeAndStableRetryId() = runBlocking {
        val requestId = "20000000-0000-4000-8000-000000000099"
        val payload = buildJsonObject {
            put("documentId", "20000000-0000-4000-8000-000000000001")
            put("revision", "01")
            put("originalName", "schematic.pdf")
            put("contentType", "application/pdf")
            put("sizeBytes", 4)
            put("checksum", "a".repeat(64))
        }
        val bodies = mutableListOf<String>()
        val repository = SharedTestRepository { method, body, token ->
            assertEquals("register_test_controlled_original", method)
            assertEquals("employee-session", token)
            bodies += body
            Result.success("""{"id":"20000000-0000-4000-8000-000000000002","status":"Registered"}""")
        }
        repeat(2) {
            val receipt = repository.registerOriginal("employee-session", payload, requestId)
            assertEquals("20000000-0000-4000-8000-000000000002", receipt["id"]!!.jsonPrimitive.content)
        }
        assertEquals(bodies[0], bodies[1])
        val sent = Json.parseToJsonElement(bodies.first()).jsonObject
        assertEquals(requestId, sent["p_request_id"]!!.jsonPrimitive.content)
        assertEquals(payload, sent["p_payload"])
        assertFalse(sent["p_payload"]!!.jsonObject.containsKey("organization_id"))
        assertFalse(sent["p_payload"]!!.jsonObject.containsKey("department_id"))
    }
    @Test fun readsUseCurrentSessionAndNoLocalFallback() = runBlocking {
        val calls = mutableListOf<String>()
        val repository = SharedTestRepository { method, payload, token ->
            calls.add("$method|$payload|$token")
            Result.success("{\"records\":[],\"tasks\":[]}")
        }
        assertEquals(0, repository.load("account-a")["records"]!!.jsonArray.size)
        repository.load("account-b")
        assertEquals(listOf("get_shared_test_workspace|{}|account-a", "get_shared_test_workspace|{}|account-b"), calls)
        val denied = SharedTestRepository { _, _, _ -> Result.failure(SecurityException("Denied")) }
        try { denied.load("expired"); fail("Must fail closed") } catch (e: SecurityException) { assertEquals("Denied", e.message) }
    }
    @Test fun mutationPreservesRetryIdentityAndStaleVersionContract() = runBlocking {
        val bodies = mutableListOf<JsonObject>()
        val repository = SharedTestRepository { method, body, token ->
            assertEquals("mutate_shared_test_workspace", method)
            assertEquals("employee-session", token)
            bodies.add(Json.parseToJsonElement(body).jsonObject)
            Result.success("{}")
        }
        val payload = buildJsonObject { put("id", "task-id"); put("status", "Complete"); put("expected_updated_at", "server-version") }
        repeat(2) { repository.mutate("employee-session", "update_task", payload, "same-request-id") }
        assertEquals(bodies[0], bodies[1])
        assertEquals(payload, bodies[0]["p_payload"])
        assertEquals("same-request-id", bodies[0]["p_request_id"]!!.jsonPrimitive.content)
        assertFalse(bodies[0].containsKey("organization_id"))
        assertFalse(bodies[0].containsKey("role"))
    }

    @Test fun approvalDecisionSendsServerConcurrencyTokenAndReviewerRationale() = runBlocking {
        var sentAction = ""
        var sentPayload: JsonObject? = null
        val repository = SharedTestRepository { method, body, _ ->
            assertEquals("mutate_shared_test_workspace", method)
            val request = Json.parseToJsonElement(body).jsonObject
            sentAction = request["p_action"]!!.jsonPrimitive.content
            sentPayload = request["p_payload"]!!.jsonObject
            Result.success("{}")
        }
        val payload = buildJsonObject {
            put("id", "approval-id")
            put("outcome", "returned")
            put("expected_updated_at", "server-version")
            put("notes", "Attach the missing verification evidence.")
        }

        repository.mutate("reviewer-session", "decide_approval", payload, "decision-request-id")

        assertEquals("decide_approval", sentAction)
        assertEquals(payload, sentPayload)
    }

    @Test fun reviewRequestSendsRecordAndSelectedReviewerDepartmentToServer() = runBlocking {
        var sentAction = ""
        var sentPayload: JsonObject? = null
        val repository = SharedTestRepository { method, body, _ ->
            assertEquals("mutate_shared_test_workspace", method)
            val request = Json.parseToJsonElement(body).jsonObject
            sentAction = request["p_action"]!!.jsonPrimitive.content
            sentPayload = request["p_payload"]!!.jsonObject
            Result.success("{}")
        }
        val payload = buildJsonObject {
            put("record_id", "record-id")
            put("reviewer_department_id", "quality-department-id")
        }

        repository.mutate("requester-session", "request_approval", payload, "review-request-id")

        assertEquals("request_approval", sentAction)
        assertEquals(payload, sentPayload)
    }

    @Test fun projectReviewUsesTheAuthorizedCoordinationProjection() = runBlocking {
        var call = ""
        val repository = SharedTestRepository { method, payload, token ->
            call = "$method|$payload|$token"
            Result.success("""{"records":[{"id":"project-1","reference":"PRJ-1","type":"Project","status":"in_progress","fields":{"stage":"Gate 2","unknownFutureField":"ignored"}},{"id":"record-1","type":"DVP&R","fields":{"projectId":"project-1","stage":"Stage 3","stageRemark":"Review pending"}}],"tasks":[{"id":"task-1","title":"Review","type":"Task","status":"Open","fields":{"projectId":"record-1","department":"EHW","assignee":"Engineer"}}],"documents":[{"id":"doc-1","title":"Plan","type":"DVP&R","linkedRecordRefs":[null,"record-1"]}]}""")
        }

        val result = repository.loadProjectCoordination("session-token")

        assertEquals("get_project_coordination|{}|session-token", call)
        assertEquals("project-1", result.records.first().id)
        assertEquals("in_progress", result.records.first().status)
        assertEquals("Stage 3", result.records[1].fields.stage)
        assertEquals("Review pending", result.records[1].fields.stageRemark)
        assertEquals("Engineer", result.tasks.first().fields.assignee)
        assertEquals("EHW", result.tasks.first().fields.department)
        assertEquals(listOf("record-1"), result.documents.first().linkedRecordRefs)
    }

    @Test fun projectReviewDoesNotFallBackWhenTheServerDeniesAccess() = runBlocking {
        val repository = SharedTestRepository { _, _, _ ->
            Result.failure(SecurityException("COORDINATION_ACCESS_DENIED"))
        }

        try {
            repository.loadProjectCoordination("viewer-session")
            fail("Denied project review must not be replaced with local examples")
        } catch (e: SecurityException) {
            assertEquals("COORDINATION_ACCESS_DENIED", e.message)
        }
    }

    @Test fun malformedProjectReviewResponseFailsExplicitly() = runBlocking {
        val repository = SharedTestRepository { _, _, _ -> Result.success("""{"records":[{"type":"Project"}]}""") }

        try {
            repository.loadProjectCoordination("session-token")
            fail("Malformed coordination records must not be presented as an empty result")
        } catch (e: kotlinx.serialization.SerializationException) {
            assertNotNull(e.message)
        }
    }

    @Test fun projectReviewGroupsOnlyPersistedStagesAndExplicitTaskLinks() {
        val project = ProjectCoordinationRecord(
            id = "project-1",
            reference = "PRJ-1",
            type = "Project"
        )
        val projection = ProjectCoordinationProjection(
            records = listOf(
                project,
                ProjectCoordinationRecord(
                    id = "record-1",
                    type = "DVP&R",
                    title = "Title contains Stage 9 but does not set stage",
                    fields = ProjectCoordinationRecordFields(projectId = project.id, stage = "Stage 2")
                ),
                ProjectCoordinationRecord(
                    id = "record-2",
                    type = "ECN",
                    fields = ProjectCoordinationRecordFields(projectCode = project.reference, lifecycleStage = "Stage 2")
                ),
                ProjectCoordinationRecord(
                    id = "record-3",
                    type = "PRS",
                    fields = ProjectCoordinationRecordFields(projectId = project.id)
                ),
                ProjectCoordinationRecord(
                    id = "unrelated",
                    type = "DFMEA",
                    fields = ProjectCoordinationRecordFields(projectId = "other-project", projectCode = "")
                ),
                ProjectCoordinationRecord(
                    id = "blank-code-match",
                    type = "KPA",
                    fields = ProjectCoordinationRecordFields(projectCode = "")
                )
            ),
            tasks = listOf(
                ProjectCoordinationTask(
                    id = "record-task",
                    title = "Review",
                    status = "Open",
                    fields = ProjectCoordinationTaskFields(projectId = "record-1", department = "EHW")
                ),
                ProjectCoordinationTask(
                    id = "project-task",
                    title = "Project task",
                    fields = ProjectCoordinationTaskFields(projectId = project.id, department = "SAL")
                ),
                ProjectCoordinationTask(
                    id = "unrelated-task",
                    title = "Unrelated",
                    fields = ProjectCoordinationTaskFields(projectId = "other-record")
                )
            ),
            documents = listOf(
                ProjectCoordinationDocument(id = "doc-1", linkedRecordRefs = listOf("record-2")),
                ProjectCoordinationDocument(id = "unrelated-doc", linkedRecordRefs = listOf("unrelated"))
            )
        )

        val review = projection.reviewForProject(project)

        assertEquals(listOf("Stage 2", "Stage not specified"), review.stages.map { it.stage })
        assertEquals(listOf("record-1", "record-2"), review.stages.first().records.map { it.id })
        assertEquals(listOf("record-task"), review.stages.first().tasksByRecordId["record-1"]!!.map { it.id })
        assertEquals(listOf("project-task"), review.projectTasks.map { it.id })
        assertEquals(listOf("doc-1"), review.documents.map { it.id })
    }
}
