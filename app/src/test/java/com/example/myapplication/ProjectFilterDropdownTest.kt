package com.example.myapplication

import com.example.myapplication.data.model.AutomotiveProjects
import com.example.myapplication.data.model.ProjectMetadata
import com.example.myapplication.ui.components.resolveProjectSelection
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectFilterDropdownTest {
    @Test
    fun keepsSelectionWhenProjectIsAuthorized() {
        val selectedProject = AutomotiveProjects.all.first().id

        assertEquals(
            selectedProject,
            resolveProjectSelection(selectedProject, AutomotiveProjects.all)
        )
    }

    @Test
    fun staleOrUnauthorizedSelectionFallsBackToAllPermittedProjects() {
        val authorizedProjects = AutomotiveProjects.all.drop(1)

        assertEquals(
            "ALL",
            resolveProjectSelection(AutomotiveProjects.all.first().id, authorizedProjects)
        )
    }

    @Test
    fun emptyProjectCatalogDoesNotInventAProjectSelection() {
        assertEquals("ALL", resolveProjectSelection("restricted-project", emptyList()))
    }

    @Test
    fun explicitAllSelectionIsPreserved() {
        val project = ProjectMetadata("project-1", "P-1", "Project", "", "", "", "", "Active", "")

        assertEquals("ALL", resolveProjectSelection("ALL", listOf(project)))
    }
}
