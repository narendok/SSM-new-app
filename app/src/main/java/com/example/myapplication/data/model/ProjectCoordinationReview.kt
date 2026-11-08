package com.example.myapplication.data.model

data class ProjectStageReview(
    val stage: String,
    val records: List<ProjectCoordinationRecord>,
    val tasksByRecordId: Map<String, List<ProjectCoordinationTask>>
)

data class ProjectCoordinationReview(
    val stages: List<ProjectStageReview>,
    val projectTasks: List<ProjectCoordinationTask>,
    val documents: List<ProjectCoordinationDocument>
)

fun ProjectCoordinationProjection.reviewForProject(
    project: ProjectCoordinationRecord
): ProjectCoordinationReview {
    val relatedRecords = records.filter { record ->
        record.type != "Project" && (
            record.fields.projectId == project.id ||
                (project.reference.isNotBlank() && record.fields.projectCode == project.reference)
            )
    }
    val recordIds = relatedRecords.mapTo(mutableSetOf()) { it.id }
    val groupedRecords = linkedMapOf<String, MutableList<ProjectCoordinationRecord>>()
    relatedRecords.forEach { record ->
        val stage = record.fields.lifecycleStage
            ?.takeIf(String::isNotBlank)
            ?: record.fields.stage?.takeIf(String::isNotBlank)
            ?: "Stage not specified"
        groupedRecords.getOrPut(stage, ::mutableListOf).add(record)
    }

    return ProjectCoordinationReview(
        stages = groupedRecords.map { (stage, stageRecords) ->
            ProjectStageReview(
                stage = stage,
                records = stageRecords,
                tasksByRecordId = stageRecords.associate { record ->
                    record.id to tasks.filter { it.fields.projectId == record.id }
                }
            )
        },
        projectTasks = tasks.filter { it.fields.projectId == project.id },
        documents = documents.filter { document ->
            document.linkedRecordRefs.any { it == project.id || it in recordIds }
        }
    )
}
