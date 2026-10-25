package com.example.myapplication.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Warning
import com.example.myapplication.data.model.DailyTaskItem
import com.example.myapplication.data.model.TeamMemberItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.ApqpStage
import com.example.myapplication.data.model.ClientDeliverable
import com.example.myapplication.data.model.DepartmentRole
import com.example.myapplication.data.model.FeasibilityReview
import com.example.myapplication.data.repository.ScmsRepository

@Composable
fun CommandCenterScreen(
    scmsRepository: ScmsRepository,
    onOpenRoleSwitcher: () -> Unit,
    authenticatedName: String? = null,
    sharedTasks: List<com.example.myapplication.data.model.DailyTaskItem>? = null,
    onSharedTaskToggle: ((String) -> Unit)? = null
) {
    val currentRole by scmsRepository.currentRole.collectAsState()
    val stages by scmsRepository.apqpStages.collectAsState()
    val selectedProject by scmsRepository.selectedProject.collectAsState()
    val demoTasks by scmsRepository.tasks.collectAsState()
    val allTasks = if (!sharedTasks.isNullOrEmpty()) sharedTasks else demoTasks
    val tasks = allTasks.filter { selectedProject == "ALL" || it.projectId == selectedProject }
    val raciMatrix by scmsRepository.raciMatrix.collectAsState()
    val feasibilityReviews by scmsRepository.feasibilityReviews.collectAsState()
    val hardwarePath by scmsRepository.hardwarePath.collectAsState()
    val engineeringSquad by scmsRepository.engineeringSquad.collectAsState()
    val clientDeliverables by scmsRepository.clientDeliverables.collectAsState()
    val approvals by scmsRepository.approvals.collectAsState()
    val pendingApprovalsCount = approvals.count { it.outcome == "Pending" }
    val teamMembers by scmsRepository.teamMembers.collectAsState()

    var selectedStageForDetail by remember { mutableStateOf<ApqpStage?>(null) }
    var showRaciMatrix by remember { mutableStateOf(false) }
    var showFeasibilityModal by remember { mutableStateOf(false) }
    var showHardwarePathModal by remember { mutableStateOf(false) }
    var showClientTicketModal by remember { mutableStateOf(false) }
    var showTravelerModal by remember { mutableStateOf(false) }
    var showApprovalsScreen by remember { mutableStateOf(false) }
    var showCrossDeptSyncModal by remember { mutableStateOf(false) }
    var ticketTitle by remember { mutableStateOf("") }
    var ticketDesc by remember { mutableStateOf("") }
    var ticketConfirmationMsg by remember { mutableStateOf<String?>(null) }

    // 3-Lane Task Management State
    var selectedTaskLane by remember { mutableStateOf("Today") }
    var showCompletedTasks by remember { mutableStateOf(false) }
    var showCreateTaskModal by remember { mutableStateOf(false) }
    var taskToReassign by remember { mutableStateOf<DailyTaskItem?>(null) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskPriority by remember { mutableStateOf("High") }
    var newTaskLane by remember { mutableStateOf("Today") }
    var newTaskAssignee by remember { mutableStateOf("") }
    var reassignNotes by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Dynamic Project Overview Intelligence Banner
        item {
            com.example.myapplication.ui.components.ProjectOverviewBanner(
                projectId = selectedProject
            )
        }

        // 1b. Human Approvals & Governance Decision Center Banner (Master Plan §26.3)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showApprovalsScreen = true },
                color = if (pendingApprovalsCount > 0) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (pendingApprovalsCount > 0) Color(0xFFFFB74D) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (pendingApprovalsCount > 0) Color(0xFFFFE0B2) else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Gavel,
                                    contentDescription = null,
                                    tint = if (pendingApprovalsCount > 0) Color(0xFFE65100) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Approvals & Governance Queue",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pendingApprovalsCount > 0) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface
                                )
                                if (pendingApprovalsCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFC62828)
                                    ) {
                                        Text(
                                            text = "$pendingApprovalsCount PENDING",
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Expenses, Travel, POs, Deviations & Office Assets",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                    FilledTonalButton(
                        onClick = { showApprovalsScreen = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Review", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Executive Cross-Dept Sync & RACI Order Hub Action Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Cross-Dept Sync & RACI Hub",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = Color(0xFFE8F5E9)
                                ) {
                                    Text(
                                        text = "Verified",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Order signals, APQP handoffs & 9-stage RACI grid",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Button(
                        onClick = { showCrossDeptSyncModal = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open Hub", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2b. Specialized Client OEM Customer Portal (Master Plan §3)
        if (currentRole == DepartmentRole.CLIENT) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tata Motors OEM Portal (§3)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Button(
                                onClick = { showClientTicketModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("+ Support Ticket", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Commercial Vehicle Telematics Division · 50,000 units · ₹20.33 Cr Locked",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Client Deliverables & Verification Dossier:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        clientDeliverables.forEach { deliv ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = deliv.code, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text(text = deliv.title, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Surface(
                                        color = if (deliv.status.contains("Signed") || deliv.status.contains("Executed") || deliv.status.contains("Ready")) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = deliv.status,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (deliv.status.contains("Signed") || deliv.status.contains("Executed") || deliv.status.contains("Ready")) Color(0xFF2E7D32) else Color(0xFFE65100)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Daily Tasks & Action Items (3-Lane Operational Checklist)
        item {
            val todayTasks = tasks.filter { !it.isCompleted && it.lane.equals("Today", ignoreCase = true) }
            val tomorrowTasks = tasks.filter { !it.isCompleted && it.lane.equals("Tomorrow", ignoreCase = true) }
            val reviewTasks = tasks.filter { !it.isCompleted && it.lane.equals("Review", ignoreCase = true) }
            val completedTasks = tasks.filter { it.isCompleted || it.lane.equals("Completed", ignoreCase = true) }
            val overdueTasksCount = todayTasks.count { it.isOverdue }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header Row with Title and + Add Task button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Daily Action Checklist",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (overdueTasksCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFFFEBEE),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF9A9A))
                                    ) {
                                        Text(
                                            text = "$overdueTasksCount OVERDUE",
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFC62828),
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "3-lane operational pipeline: Today, Planned, and Review.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                newTaskTitle = ""
                                newTaskAssignee = currentRole.leadName
                                newTaskLane = selectedTaskLane
                                newTaskPriority = "High"
                                showCreateTaskModal = true
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("New Task", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3 Segmented Lane Selector Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            Triple("Today", "Today", todayTasks.size),
                            Triple("Tomorrow", "Tomorrow", tomorrowTasks.size),
                            Triple("Review", "Review", reviewTasks.size)
                        ).forEach { (laneKey, laneLabel, count) ->
                            val isSelected = selectedTaskLane == laneKey
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTaskLane = laneKey },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (isSelected) 1.dp else 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = laneLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                                    ) {
                                        Text(
                                            text = "$count",
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Active Lane Task List
                    val activeLaneTasks = when (selectedTaskLane) {
                        "Tomorrow" -> tomorrowTasks
                        "Review" -> reviewTasks
                        else -> todayTasks
                    }

                    if (activeLaneTasks.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("No tasks in $selectedTaskLane lane", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            activeLaneTasks.forEach { task ->
                                OperationalTaskCard(
                                    task = task,
                                    currentLane = selectedTaskLane,
                                    onToggleComplete = {
                                        if (sharedTasks != null) onSharedTaskToggle?.invoke(task.id) else scmsRepository.toggleTask(task.id)
                                    },
                                    onMoveLane = { targetLane ->
                                        scmsRepository.moveTaskLane(task.id, targetLane)
                                    },
                                    onDelegate = {
                                        taskToReassign = task
                                        reassignNotes = ""
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Collapsed Completed Banner
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCompletedTasks = !showCompletedTasks },
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${completedTasks.size} Tasks Completed",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = if (showCompletedTasks) "Hide ▲" else "View ▼",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    AnimatedVisibility(visible = showCompletedTasks) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (completedTasks.isEmpty()) {
                                Text(
                                    text = "No completed tasks yet today.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            } else {
                                completedTasks.forEach { task ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = task.title,
                                                    style = MaterialTheme.typography.bodySmall.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "${task.dept} | ${task.assignee} | Completed",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    fontSize = 10.sp
                                                )
                                            }
                                            TextButton(
                                                onClick = { scmsRepository.moveTaskLane(task.id, "Today") },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                modifier = Modifier.height(24.dp)
                                            ) {
                                                Text("Restore", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. Six Lifecycle Scenarios (§14.14)
        item {
            var showScenarios by remember { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showScenarios = !showScenarios },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Six Lifecycle Scenarios (§14.14)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (showScenarios) "Hide ▲" else "View ▼",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(visible = showScenarios) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            com.example.myapplication.data.model.LifecycleScenarios.all.forEach { scenario ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${scenario.id} · ${scenario.fixtureKey}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = scenario.projectCode ?: "No Project Created (§14.14)",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (scenario.projectCode == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = scenario.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                        Text(
                                            text = scenario.currentPosition,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                        Text(
                                            text = "Gate Remark: ${scenario.stageRemark}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                        if (scenario.productionReconciliation != null) {
                                            val pr = scenario.productionReconciliation
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 6.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text(
                                                        text = "Production Reconciliation: ${pr.acceptedUnits} Accepted + ${pr.heldUnits} Held + ${pr.pendingInspectionUnits} Pending Insp = ${pr.lotQuantity} Lot 1 (${pr.remainingPlannedUnits} Balance Remaining)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Operational KPI Highlights
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    KpiCard(
                        title = "Actions Due",
                        value = "2",
                        sub = "Scheduled Today",
                        color = Color(0xFF1976D2)
                    )
                }
                item {
                    KpiCard(
                        title = "Priority Gates",
                        value = "2",
                        sub = "Critical APQP",
                        color = Color(0xFFD32F2F)
                    )
                }
                item {
                    KpiCard(
                        title = "Doc Reviews",
                        value = "1",
                        sub = "Pending Sign-off",
                        color = Color(0xFFED6C02)
                    )
                }
                item {
                    KpiCard(
                        title = "HOD Approvals",
                        value = "3",
                        sub = "Feasibility Locked",
                        color = Color(0xFF2E7D32)
                    )
                }
                item {
                    KpiCard(
                        title = "Progress",
                        value = "68%",
                        sub = "Milestones Complete",
                        color = Color(0xFF0288D1)
                    )
                }
            }
        }

        // 4. 9-Stage APQP Stepper with Prerequisite Interlock
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "9-Stage Automotive APQP Stepper",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Stage Gates & Lifecycle",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(stages) { stage ->
                    ApqpStageCard(
                        stage = stage,
                        onClick = { selectedStageForDetail = stage }
                    )
                }
            }
        }

        // 5. Stage Dependency Warning Banner
        item {
            Surface(
                color = Color(0xFFFFF8E1),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFFECB3),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Stage Gate Interlock Active (§6.1)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBF360C)
                        )
                        Text(
                            text = "Stage 5 (SMT Pilot Build) & Stage 7 (PPAP Level 3) remain LOCKED until Stage 4 Hardware Schematics is signed off by EHW Lead.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5D4037),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 5b. Electronic Production Routing Traveler (Stage 6)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PrecisionManufacturing,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Production Routing Traveler",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Stage 6 Electronic Traveler",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = { showTravelerModal = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Open Traveler", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Active Work Order: WO-2026-AI4T-001 • Station 2: 10-Zone Reflow Oven • 100 units Pilot Batch",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 5a. Feasibility & Contract Review (Master Plan §4 & §5, Stage 2 Gate)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Feasibility Review",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFE8F5E9)
                                ) {
                                    Text(
                                        text = "HLPL/F/MKT/01",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Text(
                                text = "Stage 2 Sign-off · 6/6 HODs Completed",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        FilledTonalButton(
                            onClick = { showFeasibilityModal = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Inspect 6 HODs", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Multi-departmental sign-off verified: EHW, ESW, MECH, PUR, QA, FIN. Unit price locked at ₹4,066 ($48.80) with ₹45L NRE milestone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("EHW", "ESW", "MECH", "PUR", "QA", "FIN").forEach { hod ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE8F5E9),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC8E6C9))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = hod,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5b. Hardware Kick-off Path Selection & Engineering Squad (§4)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Hardware Kick-off Path & Squad",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (hardwarePath == "pilot") "Direct-to-Pilot Path (12 Weeks)" else "Proof-of-Concept First (20 Weeks)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        OutlinedButton(
                            onClick = { showHardwarePathModal = true },
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Text("Change Path", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Dedicated Engineering Squad:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(engineeringSquad) { member ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = member,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. RACI Responsibility Governance
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRaciMatrix = !showRaciMatrix },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RACI Responsibility Matrix (§7.1)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (showRaciMatrix) "Hide ▲" else "View ▼",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(visible = showRaciMatrix) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            raciMatrix.forEach { row ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "Stage ${row.stageNo}: ${row.stageName}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "R: ${row.responsible} | A: ${row.accountable}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "C: ${row.consulted} | I: ${row.informed}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Stage Detail Dialog
    selectedStageForDetail?.let { stage ->
        AlertDialog(
            onDismissRequest = { selectedStageForDetail = null },
            title = {
                Text(
                    text = "Stage ${stage.stageNumber}: ${stage.name}",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column {
                    Text(
                        text = stage.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Status: ${stage.status}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = when (stage.status) {
                            "Approved" -> Color(0xFF2E7D32)
                            "In Progress" -> Color(0xFF1976D2)
                            "Locked" -> Color(0xFFD32F2F)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    if (stage.isBlockedByPrerequisite && stage.prerequisiteDoc != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "⚠ Blocked by Prerequisite: ${stage.prerequisiteDoc} must be signed off first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC62828)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedStageForDetail = null }) {
                    Text("OK")
                }
            }
        )
    }

    // Feasibility Review 6 HOD Modal (Master Plan §5.3)
    if (showFeasibilityModal) {
        AlertDialog(
            onDismissRequest = { showFeasibilityModal = false },
            title = {
                Text(
                    text = "Feasibility & Contract Review HLPL/F/MKT/01",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "Stage 2 Sign-off · 6 Multi-Departmental HOD Verdicts:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(feasibilityReviews) { review ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${review.deptCode} · ${review.deptName}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        color = if (review.verdict == "FEASIBLE") Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = review.verdict,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (review.verdict == "FEASIBLE") Color(0xFF2E7D32) else Color(0xFFE65100)
                                        )
                                    }
                                }
                                Text(
                                    text = "Lead: ${review.leadName} | Signed: ${review.signedAt}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = review.scopeSummary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Client Highlights: ${review.clientHighlights}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF1565C0)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Internal SCM/Eng Remarks: ${review.internalRemarks}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF424242)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Sig: ${review.signatureHash.take(24)}...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFeasibilityModal = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Hardware Path Selection Modal
    if (showHardwarePathModal) {
        AlertDialog(
            onDismissRequest = { showHardwarePathModal = false },
            title = {
                Text(
                    text = "Select Hardware Kick-off Path (§4)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose the hardware execution strategy for Tata Ace EV Telematics Gateway:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scmsRepository.selectHardwarePath("pilot", "Direct-to-Pilot execution selected per customer delivery commitment.")
                                showHardwarePathModal = false
                            },
                        color = if (hardwarePath == "pilot") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Direct-to-Pilot Path (12 Weeks)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Recommended for locked architectures. Directly builds SMT pilot batch on Line 2 with zero prototype spin overhead.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scmsRepository.selectHardwarePath("poc", "Proof-of-Concept selected for preliminary RF antenna matching.")
                                showHardwarePathModal = false
                            },
                        color = if (hardwarePath == "poc") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Proof-of-Concept First (20 Weeks)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Builds hand-soldered prototype bench boards first before opening steel injection tooling.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHardwarePathModal = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    // Client Support Ticket Modal
    if (showClientTicketModal) {
        AlertDialog(
            onDismissRequest = {
                showClientTicketModal = false
                ticketConfirmationMsg = null
            },
            title = {
                Text(
                    text = "Submit Client Engineering Ticket",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (ticketConfirmationMsg != null) {
                        Text(
                            text = ticketConfirmationMsg!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Submit technical request to XYZ Pvt Ltd Engineering Squad for HLPL-AI4T-01:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = ticketTitle,
                            onValueChange = { ticketTitle = it },
                            label = { Text("Ticket Title / Subject") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ticketDesc,
                            onValueChange = { ticketDesc = it },
                            label = { Text("Technical Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }
            },
            confirmButton = {
                if (ticketConfirmationMsg != null) {
                    TextButton(onClick = {
                        showClientTicketModal = false
                        ticketConfirmationMsg = null
                    }) {
                        Text("Done")
                    }
                } else {
                    Button(
                        onClick = {
                            if (ticketTitle.isNotBlank()) {
                                val result = scmsRepository.submitClientTicket(ticketTitle, ticketDesc)
                                if (result.isSuccess) {
                                    val tid = result.getOrNull()
                                    ticketConfirmationMsg = "Ticket Logged: $tid\nAssigned to Avery Vale (Lead HW Architect)."
                                    ticketTitle = ""
                                    ticketDesc = ""
                                }
                            }
                        },
                        enabled = ticketTitle.isNotBlank()
                    ) {
                        Text("Submit Ticket")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showClientTicketModal = false
                    ticketConfirmationMsg = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTravelerModal) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showTravelerModal = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                ShopfloorScreen(
                    scmsRepository = scmsRepository,
                    onBack = { showTravelerModal = false }
                )
            }
        }
    }

    if (showApprovalsScreen) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showApprovalsScreen = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ApprovalsScreen(
                scmsRepository = scmsRepository,
                onBack = { showApprovalsScreen = false }
            )
        }
    }

    // Modal: Quick Task Creation
    if (showCreateTaskModal) {
        AlertDialog(
            onDismissRequest = { showCreateTaskModal = false },
            title = {
                Text(
                    text = "Add Operational Task",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("Task Description / Deliverable") },
                        placeholder = { Text("e.g. Bring-up CAN J1939 telemetry") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )

                    // Lane Selector
                    Column {
                        Text("Operational Lane:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Today", "Tomorrow", "Review").forEach { lane ->
                                val isSel = newTaskLane == lane
                                Surface(
                                    modifier = Modifier.clickable { newTaskLane = lane },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                ) {
                                    Text(
                                        text = lane,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Priority Selector
                    Column {
                        Text("Priority Level:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Critical", "High", "Normal", "Low").forEach { prio ->
                                val isSel = newTaskPriority == prio
                                Surface(
                                    modifier = Modifier.clickable { newTaskPriority = prio },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSel) when (prio) {
                                        "Critical" -> Color(0xFFFFEBEE)
                                        "High" -> Color(0xFFFFF3E0)
                                        else -> Color(0xFFE8F5E9)
                                    } else MaterialTheme.colorScheme.surfaceContainer,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSel) when (prio) {
                                            "Critical" -> Color(0xFFC62828)
                                            "High" -> Color(0xFFE65100)
                                            else -> Color(0xFF2E7D32)
                                        } else Color.Transparent
                                    )
                                ) {
                                    Text(
                                        text = prio,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) when (prio) {
                                            "Critical" -> Color(0xFFC62828)
                                            "High" -> Color(0xFFE65100)
                                            else -> Color(0xFF2E7D32)
                                        } else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newTaskAssignee,
                        onValueChange = { newTaskAssignee = it },
                        label = { Text("Assignee Name") },
                        placeholder = { Text(currentRole.leadName) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Linked to active project: HLPL-AI4T-01 (${currentRole.code} Dept)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            val task = DailyTaskItem(
                                id = "",
                                title = newTaskTitle.trim(),
                                dept = currentRole.code,
                                priority = newTaskPriority,
                                assignee = newTaskAssignee.ifBlank { currentRole.leadName },
                                dueDate = if (newTaskLane == "Today") "Today" else if (newTaskLane == "Tomorrow") "Tomorrow" else "Next 48h",
                                isCompleted = false,
                                lane = newTaskLane,
                                projectId = "HLPL-AI4T-01"
                            )
                            scmsRepository.createDailyTask(task)
                            showCreateTaskModal = false
                        }
                    },
                    enabled = newTaskTitle.isNotBlank()
                ) {
                    Text("Create Task")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTaskModal = false }) {
                    Text("Cancel")
                }

            }
        )
    }

    // Modal: Task Delegation / Reassignment
    taskToReassign?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToReassign = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delegate Task", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(task.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("Current Assignee: ${task.assignee} (${task.dept})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Text("Select Team Member to Delegate to:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                    val candidateMembers = teamMembers.filter {
                        currentRole == DepartmentRole.ADMIN || it.deptCode.equals(task.dept, ignoreCase = true) || it.deptCode.equals(currentRole.code, ignoreCase = true)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        candidateMembers.take(4).forEach { member ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scmsRepository.reassignTask(task.id, member.name, reassignNotes)
                                        taskToReassign = null
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(member.avatarInitials.ifBlank { member.name.take(2).uppercase() }, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(member.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            Text(member.roleTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when (member.capacityStatus) {
                                            "Available" -> Color(0xFFE8F5E9)
                                            "At Capacity" -> Color(0xFFFFEBEE)
                                            else -> Color(0xFFE3F2FD)
                                        }
                                    ) {
                                        Text(
                                            text = "${member.openTasksCount} tasks (${member.capacityStatus})",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when (member.capacityStatus) {
                                                "Available" -> Color(0xFF2E7D32)
                                                "At Capacity" -> Color(0xFFC62828)
                                                else -> Color(0xFF1565C0)
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = reassignNotes,
                        onValueChange = { reassignNotes = it },
                        label = { Text("Handover / Rework Instructions") },
                        placeholder = { Text("e.g. Please run thermal test before tomorrow 2 PM") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { taskToReassign = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCrossDeptSyncModal) {
        com.example.myapplication.ui.components.CrossDeptSyncRaciModal(
            scmsRepository = scmsRepository,
            onDismiss = { showCrossDeptSyncModal = false }
        )
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(130.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ApqpStageCard(
    stage: ApqpStage,
    onClick: () -> Unit
) {
    val (statusColor, statusBg) = when (stage.status) {
        "Approved" -> Color(0xFF2E7D32) to Color(0xFFE8F5E9)
        "In Progress" -> Color(0xFF1565C0) to Color(0xFFE3F2FD)
        "Locked" -> Color(0xFFC62828) to Color(0xFFFFEBEE)
        else -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceContainer
    }

    Card(
        modifier = Modifier
            .width(125.dp)
            .height(86.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = statusBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Stage ${stage.stageNumber}",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        fontSize = 10.sp
                    )
                }
                if (stage.isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = statusColor,
                        modifier = Modifier.size(13.dp)
                    )
                } else if (stage.status == "Approved") {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Approved",
                        tint = statusColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Text(
                text = stage.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = stage.status,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun OperationalTaskCard(
    task: com.example.myapplication.data.model.DailyTaskItem,
    currentLane: String,
    onToggleComplete: () -> Unit,
    onMoveLane: (String) -> Unit,
    onDelegate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isOverdue && !task.isCompleted) Color(0xFFFFF8F8) else MaterialTheme.colorScheme.surfaceContainer
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (task.isOverdue && !task.isCompleted) Color(0xFFFFCDD2) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = task.dept,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Text(
                            text = "Assignee: ${task.assignee}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.5.sp
                        )

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Due: ${task.dueDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (task.isOverdue) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (task.isOverdue) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.5.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = when (task.priority) {
                            "Critical" -> Color(0xFFFFEBEE)
                            "High" -> Color(0xFFFFF3E0)
                            else -> Color(0xFFE8F5E9)
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = task.priority,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = when (task.priority) {
                                "Critical" -> Color(0xFFC62828)
                                "High" -> Color(0xFFE65100)
                                else -> Color(0xFF2E7D32)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }

                    if (task.isOverdue && !task.isCompleted) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Color(0xFFD32F2F)
                        ) {
                            Text(
                                text = "OVERDUE",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }

            // Quick Action Buttons Row
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (currentLane == "Today") {
                        FilledTonalButton(
                            onClick = { onMoveLane("Tomorrow") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("→ Tomorrow", fontSize = 10.sp)
                        }
                        FilledTonalButton(
                            onClick = { onMoveLane("Review") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("→ Review", fontSize = 10.sp)
                        }
                    } else if (currentLane == "Tomorrow") {
                        FilledTonalButton(
                            onClick = { onMoveLane("Today") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("← Move to Today", fontSize = 10.sp)
                        }
                    } else if (currentLane == "Review") {
                        Button(
                            onClick = { onToggleComplete() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Sign-off & Complete", fontSize = 10.sp)
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDelegate,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Delegate", fontSize = 10.sp)
                }
            }
        }
    }

}

@Composable
fun TaskCard(
    task: com.example.myapplication.data.model.DailyTaskItem,
    onToggle: () -> Unit
) {
    OperationalTaskCard(
        task = task,
        currentLane = task.lane,
        onToggleComplete = onToggle,
        onMoveLane = {},
        onDelegate = {}
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun CommandCenterScreenPreview() {
    com.example.myapplication.ui.theme.MyApplicationTheme {
        CommandCenterScreen(
            scmsRepository = com.example.myapplication.data.repository.ScmsRepositoryImpl(),
            onOpenRoleSwitcher = {}
        )
    }
}

