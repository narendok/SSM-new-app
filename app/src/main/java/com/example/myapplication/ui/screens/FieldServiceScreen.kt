package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.DepartmentRole
import com.example.myapplication.data.model.ServiceTicket
import com.example.myapplication.data.model.TicketReply
import com.example.myapplication.data.repository.ScmsRepository
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldServiceScreen(
    scmsRepository: ScmsRepository,
    onBack: (() -> Unit)? = null
) {
    val currentRole by scmsRepository.currentRole.collectAsState()
    val tickets by scmsRepository.serviceTickets.collectAsState()

    var selectedStatus by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTicketForDetail by remember { mutableStateOf<ServiceTicket?>(null) }

    val filteredTickets = remember(tickets, selectedStatus, searchQuery) {
        tickets.filter { ticket ->
            val matchesStatus = selectedStatus == "All" || ticket.status == selectedStatus
            val matchesSearch = searchQuery.isBlank() ||
                ticket.reference.contains(searchQuery, ignoreCase = true) ||
                ticket.subject.contains(searchQuery, ignoreCase = true) ||
                ticket.requesterName.contains(searchQuery, ignoreCase = true) ||
                (ticket.projectId?.contains(searchQuery, ignoreCase = true) == true)
            matchesStatus && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SERVICE & FIELD TICKETS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "${currentRole.code} Role Scoped",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (currentRole.isClient) "Client Support & RMA Observation Desk"
                                else "Governed Equipment, IT & Field Issue Register",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.Close, contentDescription = "Back")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search & Filters Row
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by ticket #, subject, or requester...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status Chips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All", "Open", "Assigned", "In Progress", "Resolved").forEach { status ->
                            FilterChip(
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = status },
                                label = { Text(status, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Ticket")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tickets (${filteredTickets.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (currentRole == DepartmentRole.SAL) "Sales Scope (Internal tickets hidden)"
                        else if (currentRole.isClient) "Client Scope (Internal notes protected)"
                        else "SIT Infrastructure Authority",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
            }

            if (filteredTickets.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No service tickets found in this scope",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Use '+' to register a new support or equipment ticket.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(filteredTickets) { ticket ->
                ServiceTicketCard(
                    ticket = ticket,
                    isClient = currentRole.isClient,
                    canManage = currentRole == DepartmentRole.ADMIN || currentRole == DepartmentRole.SIT,
                    onClick = { selectedTicketForDetail = ticket },
                    onUpdateStatus = { newStatus ->
                        scmsRepository.updateServiceTicketStatus(ticket.id, newStatus)
                    }
                )
            }
        }
    }

    // Ticket Detail Dialog
    if (selectedTicketForDetail != null) {
        val detail = selectedTicketForDetail!!
        AlertDialog(
            onDismissRequest = { selectedTicketForDetail = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(detail.reference, fontWeight = FontWeight.Bold)
                    Surface(
                        color = if (detail.source == "Client") Color(0xFFE3F2FD) else Color(0xFFF3E5F5),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = detail.source,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (detail.source == "Client") Color(0xFF1565C0) else Color(0xFF7B1FA2),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(detail.subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        detail.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Category: ${detail.category}", style = MaterialTheme.typography.labelSmall)
                        Text("Priority: ${detail.priority}", style = MaterialTheme.typography.labelSmall)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Requester: ${detail.requesterName}", style = MaterialTheme.typography.labelSmall)
                        Text("Status: ${detail.status}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    if (detail.projectId != null) {
                        Text("Linked Project: ${detail.projectId}", style = MaterialTheme.typography.labelSmall)
                    }
                    if (detail.assetId != null) {
                        Text("Linked Asset: ${detail.assetId}", style = MaterialTheme.typography.labelSmall)
                    }

                    HorizontalDivider()

                    Text("Activity & Communication Stream", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    detail.replies.forEach { reply ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (reply.isInternalNote) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${reply.authorName} (${reply.authorRole})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (reply.isInternalNote) {
                                        Text(
                                            text = "INTERNAL NOTE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFE65100),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(reply.message, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTicketForDetail = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Create Ticket Modal
    if (showCreateDialog) {
        var subject by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Hardware Fault") }
        var priority by remember { mutableStateOf("Normal") }
        var projectId by remember { mutableStateOf("HLPL-AI4T-01") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Service Ticket", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject / Issue *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description & Diagnostics *") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                    OutlinedTextField(
                        value = projectId,
                        onValueChange = { projectId = it },
                        label = { Text("Linked Project") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subject.isNotBlank() && description.isNotBlank()) {
                            val newTicket = ServiceTicket(
                                id = "tkt-${UUID.randomUUID().toString().take(6)}",
                                reference = "TKT-2026-${(tickets.size + 1).toString().padStart(4, '0')}",
                                source = if (currentRole.isClient) "Client" else "Internal",
                                requesterName = currentRole.leadName,
                                requesterEmail = currentRole.email,
                                requesterDepartment = currentRole.code,
                                clientOrganization = if (currentRole.isClient) "Tata Motors Limited" else null,
                                subject = subject.trim(),
                                description = description.trim(),
                                category = category,
                                priority = priority,
                                projectId = projectId.ifBlank { null },
                                assignedOwner = "Kai Morgan (SIT)",
                                status = "Open"
                            )
                            scmsRepository.createServiceTicket(newTicket)
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Submit Ticket")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ServiceTicketCard(
    ticket: ServiceTicket,
    isClient: Boolean,
    canManage: Boolean,
    onClick: () -> Unit,
    onUpdateStatus: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ticket.reference,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = if (ticket.source == "Client") Color(0xFFE3F2FD) else Color(0xFFF3E5F5),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = ticket.source,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (ticket.source == "Client") Color(0xFF1565C0) else Color(0xFF7B1FA2),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (ticket.status) {
                        "Open" -> Color(0xFFE8F5E9)
                        "In Progress" -> Color(0xFFE1F5FE)
                        "Resolved" -> Color(0xFFEDE7F6)
                        else -> Color(0xFFEEEEEE)
                    }
                ) {
                    Text(
                        text = ticket.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (ticket.status) {
                            "Open" -> Color(0xFF2E7D32)
                            "In Progress" -> Color(0xFF0277BD)
                            "Resolved" -> Color(0xFF512DA8)
                            else -> Color(0xFF616161)
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = ticket.subject,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Requester: ${ticket.requesterName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = ticket.priority,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (ticket.priority) {
                        "Critical" -> Color(0xFFC62828)
                        "High" -> Color(0xFFEF6C00)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            if (canManage && ticket.status != "Resolved") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (ticket.status == "Open") {
                        FilledTonalButton(
                            onClick = { onUpdateStatus("In Progress") },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Start Work", fontSize = 11.sp)
                        }
                    } else if (ticket.status == "In Progress") {
                        Button(
                            onClick = { onUpdateStatus("Resolved") },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Resolve", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
