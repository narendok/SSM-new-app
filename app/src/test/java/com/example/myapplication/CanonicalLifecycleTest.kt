package com.example.myapplication

import com.example.myapplication.data.model.CanonicalLifecycleEngine
import com.example.myapplication.data.model.CanonicalProjectTrack
import com.example.myapplication.data.model.LifecycleScenarios
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalLifecycleTest {

    @Test
    fun testSevenCanonicalTracksAndNormalization() {
        assertEquals(7, CanonicalProjectTrack.entries.size)
        assertEquals(CanonicalProjectTrack.READY_MADE, CanonicalProjectTrack.normalize("ready_made"))
        assertEquals(CanonicalProjectTrack.READY_MADE, CanonicalProjectTrack.normalize("readymade"))
        assertEquals(CanonicalProjectTrack.CUSTOM_HARDWARE, CanonicalProjectTrack.normalize("custom_hardware"))
        assertEquals(CanonicalProjectTrack.CUSTOM_HARDWARE, CanonicalProjectTrack.normalize("hw_only"))
        assertEquals(CanonicalProjectTrack.FIRMWARE_ONLY, CanonicalProjectTrack.normalize("firmware_only"))
        assertEquals(CanonicalProjectTrack.SOFTWARE_ONLY, CanonicalProjectTrack.normalize("software_only"))
        assertEquals(CanonicalProjectTrack.HARDWARE_FIRMWARE, CanonicalProjectTrack.normalize("hardware_firmware"))
        assertEquals(CanonicalProjectTrack.HARDWARE_FIRMWARE, CanonicalProjectTrack.normalize("hw+fw"))
        assertEquals(CanonicalProjectTrack.FIRMWARE_SOFTWARE, CanonicalProjectTrack.normalize("firmware_software"))
        assertEquals(CanonicalProjectTrack.FULL_SYSTEM, CanonicalProjectTrack.normalize("full_system"))
        assertEquals(CanonicalProjectTrack.FULL_SYSTEM, CanonicalProjectTrack.normalize(null))
    }

    @Test
    fun testFreshProjectInvariantOnlyCM01Ready() {
        val completed = emptySet<String>()
        // On a fresh project with 0 completed stages, only CM_01 is ready
        val cm01State = CanonicalLifecycleEngine.computeStageState("CM_01", CanonicalProjectTrack.FULL_SYSTEM, completed)
        assertEquals("ready", cm01State)

        // All subsequent stages must be planned, not ready
        val cm02State = CanonicalLifecycleEngine.computeStageState("CM_02", CanonicalProjectTrack.FULL_SYSTEM, completed)
        assertEquals("planned", cm02State)

        val int01State = CanonicalLifecycleEngine.computeStageState("INT_01", CanonicalProjectTrack.FULL_SYSTEM, completed)
        assertEquals("planned", int01State)

        val cl01State = CanonicalLifecycleEngine.computeStageState("CL_01", CanonicalProjectTrack.FULL_SYSTEM, completed)
        assertEquals("planned", cl01State)

        // Bypassing prerequisite via in_progress input must be strictly rejected
        val bypassedState = CanonicalLifecycleEngine.computeStageState(
            stageCode = "CL_01",
            track = CanonicalProjectTrack.FULL_SYSTEM,
            completedStages = completed,
            inProgressStages = setOf("CL_01")
        )
        assertEquals("planned", bypassedState)
    }

    @Test
    fun testPreProjectFeasibilityRequiresOnlyCM01() {
        // HW_01, FW_01, SW_01 require only CM_01 (enquiry intake), NOT CM_03/CM_04 kickoff
        val hw01Prereqs = CanonicalLifecycleEngine.resolvePrerequisites("HW_01", CanonicalProjectTrack.CUSTOM_HARDWARE)
        assertEquals(listOf("CM_01"), hw01Prereqs)

        val fw01Prereqs = CanonicalLifecycleEngine.resolvePrerequisites("FW_01", CanonicalProjectTrack.FIRMWARE_ONLY)
        assertEquals(listOf("CM_01"), fw01Prereqs)

        val sw01Prereqs = CanonicalLifecycleEngine.resolvePrerequisites("SW_01", CanonicalProjectTrack.SOFTWARE_ONLY)
        assertEquals(listOf("CM_01"), sw01Prereqs)

        // Once CM_01 is completed, feasibility stages become ready before project kickoff
        val completed = setOf("CM_01")
        assertEquals("ready", CanonicalLifecycleEngine.computeStageState("HW_01", CanonicalProjectTrack.CUSTOM_HARDWARE, completed))
        assertEquals("ready", CanonicalLifecycleEngine.computeStageState("FW_01", CanonicalProjectTrack.FIRMWARE_ONLY, completed))
        assertEquals("ready", CanonicalLifecycleEngine.computeStageState("SW_01", CanonicalProjectTrack.SOFTWARE_ONLY, completed))
    }

    @Test
    fun testNonCircularReleaseReadiness() {
        // CL_01 release readiness depends on domain verification evidence, never deployment/closure
        val rmRelease = CanonicalLifecycleEngine.resolvePrerequisites("CL_01", CanonicalProjectTrack.READY_MADE)
        assertEquals(listOf("RM_02"), rmRelease)

        val swRelease = CanonicalLifecycleEngine.resolvePrerequisites("CL_01", CanonicalProjectTrack.SOFTWARE_ONLY)
        assertEquals(listOf("SW_09"), swRelease)

        val fwRelease = CanonicalLifecycleEngine.resolvePrerequisites("CL_01", CanonicalProjectTrack.FIRMWARE_ONLY)
        assertEquals(listOf("FW_11"), fwRelease)

        val hwRelease = CanonicalLifecycleEngine.resolvePrerequisites("CL_01", CanonicalProjectTrack.CUSTOM_HARDWARE)
        assertEquals(listOf("HW_32"), hwRelease)

        val coupledRelease = CanonicalLifecycleEngine.resolvePrerequisites("CL_01", CanonicalProjectTrack.FULL_SYSTEM)
        assertEquals(listOf("INT_02"), coupledRelease)
    }

    @Test
    fun testCanonicalSampleProjectsCatalogue() {
        val samples = CanonicalLifecycleEngine.SAMPLES
        assertEquals(9, samples.size)

        // Exactly 8 projects + 1 declined enquiry
        val projects = samples.filter { !it.isDeclinedEnquiry }
        assertEquals(8, projects.size)

        val declinedEnquiry = samples.find { it.isDeclinedEnquiry }!!
        assertEquals("LC2-E02", declinedEnquiry.sampleId)
        assertNull(declinedEnquiry.projectCode)
        assertEquals("Nordic Fleet Logistics AB", declinedEnquiry.clientName)
    }

    @Test
    fun testScenarioS02DeclinedEnquiryAndS06ProductionReconciliation() {
        val s02 = LifecycleScenarios.get("S02")!!
        assertNull(s02.projectCode)
        assertEquals("ENQ-2026-EXT-01", s02.enquiryCode)

        val s06 = LifecycleScenarios.get("S06")!!
        val recon = s06.productionReconciliation!!
        // 150 accepted + 5 held + 45 pending = 200 lot units
        assertEquals(200, recon.lotQuantity)
        assertEquals(200, recon.acceptedUnits + recon.heldUnits + recon.pendingInspectionUnits)
        // 500 total - 200 lot = 300 remaining planned
        assertEquals(500, recon.totalPlannedUnits)
        assertEquals(300, recon.remainingPlannedUnits)
        assertEquals(recon.totalPlannedUnits - recon.lotQuantity, recon.remainingPlannedUnits)
    }
}
