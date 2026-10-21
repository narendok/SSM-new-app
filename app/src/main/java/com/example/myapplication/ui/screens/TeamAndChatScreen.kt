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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.TeamChatMessage
import com.example.myapplication.data.model.TeamMemberItem
import com.example.myapplication.data.repository.ScmsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamAndChatScreen(
    scmsRepository: ScmsRepository,
    onOpenRoleSwitcher: () -> Unit = {},
    onOpenAuthDialog: () -> Unit = {}
) {
    var selectedTopTab by remember { mutableIntStateOf(0) } // 0: Project Channels & Chat, 1: Team Roster & Capacity, 2: My Profile
    var activeChannel by remember { mutableStateOf("hlpl-ai4t-01") }

    var messageInput by remember { mutableStateOf("") }
    var isUrgentMark by remember { mutableStateOf(false) }

    val chatMessages by scmsRepository.chatMessages.collectAsState()
    val teamMembers by scmsRepository.teamMembers.collectAsState()
    val currentRole by scmsRepository.currentRole.collectAsState()

    val channels = remember(currentRole.code) {
        if (currentRole.code == "CLIENT") {
            listOf("hlpl-ai4t-01" to "#hlpl-ai4t-01 (Tata Ace EV)")
        } else if (currentRole.code in listOf("ADMIN", "MD")) {
            listOf(
                "hlpl-ai4t-01" to "#hlpl-ai4t-01 (Tata Ace EV)",
                "chan-general" to "#general",
                "chan-engineering-hw" to "#engineering-hw",
                "chan-purchasing-scm" to "#purchasing-scm",
                "chan-quality-apqp" to "#quality-apqp",
                "chan-fin-budgets" to "#fin-budgets",
                "cross-dept-sync" to "#cross-dept-sync"
            )
        } else {
            val base = mutableListOf(
                "hlpl-ai4t-01" to "#hlpl-ai4t-01 (Tata Ace EV)",
                "chan-general" to "#general",
                "cross-dept-sync" to "#cross-dept-sync"
            )
            when (currentRole.code) {
                "EHW", "ESW", "MECH" -> base.add(2, "chan-engineering-hw" to "#engineering-hw")
                "PUR" -> base.add(2, "chan-purchasing-scm" to "#purchasing-scm")
                "QA", "EMF", "TST" -> base.add(2, "chan-quality-apqp" to "#quality-apqp")
                "FIN" -> {
                    base.add(2, "chan-fin-budgets" to "#fin-budgets")
                    base.add(3, "chan-purchasing-scm" to "#purchasing-scm")
                }
                "SAL" -> base.add(2, "chan-fin-budgets" to "#fin-budgets")
            }
            base
        }
    }
    LaunchedEffect(channels) {
        if (channels.none { it.first == activeChannel }) {
            activeChannel = channels.firstOrNull()?.first ?: "hlpl-ai4t-01"
        }
    }
    val tasks by scmsRepository.tasks.collectAsState()

    val filteredMessages = chatMessages.filter { it.channel == activeChannel }
    val myTasks = tasks.filter { it.assignee.contains(currentRole.leadName, ignoreCase = true) || it.dept.equals(currentRole.code, ignoreCase = true) }
    val employeeLetters by scmsRepository.employeeLetters.collectAsState()
    val assignedPolicies by scmsRepository.assignedPolicies.collectAsState()
    val assignedTrainings by scmsRepository.assignedTrainings.collectAsState()
    val assignedSurveys by scmsRepository.assignedSurveys.collectAsState()
    val assignedAssets by scmsRepository.assignedAssets.collectAsState()
    val expenseRequests by scmsRepository.expenseRequests.collectAsState()
    var myProfileSubTab by remember { mutableIntStateOf(0) } // 0: Letters & Policies, 1: Training & Surveys, 2: Assets & Expenses, 3: Tasks
    var selectedSurveyForModal by remember { mutableStateOf<com.example.myapplication.data.model.AssignedSurveyItem?>(null) }
    var surveyFeedbackInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP HEADER
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                    Column {
                        Text(
                            text = "COMMUNICATION & PEOPLE MANAGEMENT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Team Hub & Discussion",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Persona badge with quick switch
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.clickable { onOpenRoleSwitcher() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E7D32))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${currentRole.code} · ${currentRole.leadName.split(" ").first()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        // TOP TABS
        ScrollableTabRow(
            selectedTabIndex = selectedTopTab,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTopTab == 0,
                onClick = { selectedTopTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Project Chat", fontSize = 12.sp, fontWeight = if (selectedTopTab == 0) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            )
            Tab(
                selected = selectedTopTab == 1,
                onClick = { selectedTopTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SupervisorAccount, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Team Roster (${teamMembers.size})", fontSize = 12.sp, fontWeight = if (selectedTopTab == 1) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            )
            Tab(
                selected = selectedTopTab == 2,
                onClick = { selectedTopTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("My Profile", fontSize = 12.sp, fontWeight = if (selectedTopTab == 2) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            )
        }

        when (selectedTopTab) {
            0 -> {
                // TAB 0: PROJECT CHAT & DISCUSSION
                Column(modifier = Modifier.fillMaxSize()) {
                    // Channel Selection Row
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyRow(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(channels) { (chId, label) ->
                                val isSelected = activeChannel == chId
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)) else null,
                                    modifier = Modifier.clickable { activeChannel = chId }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Chat messages list
                    val listState = rememberLazyListState()
                    LaunchedEffect(filteredMessages.size) {
                        if (filteredMessages.isNotEmpty()) {
                            listState.animateScrollToItem(filteredMessages.size - 1)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Channel #${activeChannel} · Encrypted team discussion for automotive engineering & gates",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        items(filteredMessages) { msg ->
                            val isMe = msg.senderName.contains(currentRole.leadName, ignoreCase = true)
                            ChatMessageCard(msg = msg, isMe = isMe, onVotePoll = { mId, oId -> scmsRepository.castPollVote(mId, oId) })
                        }
                    }

                    // Message Composer Bar at the bottom
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Urgent flag toggle
                            IconButton(
                                onClick = { isUrgentMark = !isUrgentMark },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isUrgentMark) Color(0xFFFFEBEE) else Color.Transparent,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.PriorityHigh,
                                            contentDescription = "Urgent",
                                            tint = if (isUrgentMark) Color(0xFFC62828) else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Message Input
                            OutlinedTextField(
                                value = messageInput,
                                onValueChange = { messageInput = it },
                                placeholder = { Text("Message #${activeChannel}...", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            // Send Button
                            Button(
                                onClick = {
                                    if (messageInput.isNotBlank()) {
                                        scmsRepository.sendChatMessage(
                                            channel = activeChannel,
                                            message = messageInput.trim(),
                                            isUrgent = isUrgentMark
                                        )
                                        messageInput = ""
                                        isUrgentMark = false
                                    }
                                },
                                shape = CircleShape,
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            1 -> {
                // TAB 1: TEAM ROSTER & PEOPLE MANAGEMENT
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Department Workload & Staff Matrix",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Manage engineering team members, assigned tasks, and operational capacity.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(teamMembers) { member ->
                        TeamMemberCard(member = member)
                    }
                }
            }

            2 -> {
                // TAB 2: MY PROFILE & ACCOUNT
                val profileSubTabs = listOf("Letters & Policies", "Training & Surveys", "Assets & Claims", "My Tasks")
                val myLetters = employeeLetters.filter {
                    it.title.contains(currentRole.title, ignoreCase = true) || currentRole.code in listOf("ADMIN", "MD")
                }
                val myAssets = assignedAssets.filter {
                    it.custodian.contains(currentRole.leadName, ignoreCase = true) || currentRole.code in listOf("ADMIN", "MD")
                }
                val myExpenses = expenseRequests.filter {
                    it.requesterName.contains(currentRole.leadName, ignoreCase = true) || it.requesterDept.equals(currentRole.code, ignoreCase = true)
                }

                if (currentRole.code == "CLIENT") {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Lock, contentDescription = "Access Restricted", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Access Restricted — Employee Self-Service", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Employee self-service, official letters, compliance training, and company asset records are restricted to internal employees. Client accounts do not have access to internal employee self-service.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "SANDBOX DEMO MODE · Changes persist in local session for evaluation. Shared sync requires authenticated Enterprise API session.",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = currentRole.leadName.take(2).uppercase(),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = currentRole.leadName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "${currentRole.title} (${currentRole.code})",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Email: ${currentRole.email}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Facility Location", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text("Noida Tech Center & SMT 2", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("Security Clearance", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text("Level 4 Master", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    }
                                    Column {
                                        Text("Active Tasks", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text("${myTasks.size} Tasks", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }

                    // Profile Sub-tab Selector
                    item {
                        ScrollableTabRow(
                            selectedTabIndex = myProfileSubTab,
                            edgePadding = 0.dp,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        ) {
                            profileSubTabs.forEachIndexed { idx, title ->
                                Tab(
                                    selected = myProfileSubTab == idx,
                                    onClick = { myProfileSubTab = idx },
                                    text = {
                                        Text(
                                            text = title,
                                            fontSize = 11.sp,
                                            fontWeight = if (myProfileSubTab == idx) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                )
                            }
                        }
                    }

                    when (myProfileSubTab) {
                        0 -> {
                            // Sub-tab 0: Letters & Policies
                            item {
                                Text("Official HR & Governance Letters (${employeeLetters.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            items(employeeLetters) { letter ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(letter.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE8F5E9)) {
                                                Text(letter.status, color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                        Text("Ref: ${letter.referenceNo} · Effective: ${letter.effectiveDate} · Signer: ${letter.signerName}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(letter.summary, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Assigned Quality & Security Policies (${assignedPolicies.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            items(assignedPolicies) { pol ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(pol.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (pol.status == "Acknowledged") Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                            ) {
                                                Text(
                                                    text = pol.status,
                                                    color = if (pol.status == "Acknowledged") Color(0xFF2E7D32) else Color(0xFFE65100),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text("${pol.policyCode} · ${pol.revision} · Category: ${pol.category}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(pol.summary, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                        if (pol.status != "Acknowledged") {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Button(
                                                onClick = { scmsRepository.acknowledgePolicy(pol.policyId) },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("Acknowledge & Sign", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // Sub-tab 1: Training & Surveys
                            item {
                                Text("Assigned Automotive Training Courses (${assignedTrainings.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            items(assignedTrainings) { trn ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(trn.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (trn.status == "Completed") Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
                                            ) {
                                                Text(
                                                    text = trn.status,
                                                    color = if (trn.status == "Completed") Color(0xFF2E7D32) else Color(0xFF1565C0),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text("Course: ${trn.courseCode} · Trainer: ${trn.trainer} · Duration: ${trn.durationHours} hrs", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        trn.certificateRef?.let { cert ->
                                            Text("Certificate: $cert (Score: ${trn.scorePct}%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                        }

                                        if (trn.status != "Completed") {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Button(
                                                onClick = { scmsRepository.submitTrainingQuiz(trn.trainingId, 95) },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("Take Assessment Quiz", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Facility Forms & Quality Surveys (${assignedSurveys.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            items(assignedSurveys) { surv ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(surv.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (surv.status == "Submitted") Color(0xFFE8F5E9) else Color(0xFFFFF8E1)
                                            ) {
                                                Text(
                                                    text = surv.status,
                                                    color = if (surv.status == "Submitted") Color(0xFF2E7D32) else Color(0xFFF57F17),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text("Form: ${surv.formCode} · Deadline: ${surv.deadline} · ${if (surv.isAnonymous) "Anonymous" else "Named"}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(surv.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                        if (surv.status != "Submitted") {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Button(
                                                onClick = { selectedSurveyForModal = surv },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("Fill & Submit Survey", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // Sub-tab 2: Assets & Claims
                            item {
                                Text("My Assigned Hardware & Capital Assets (${assignedAssets.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            items(myAssets) { asset ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(asset.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE8F5E9)) {
                                                Text(asset.conditionStatus, color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                        Text("Tag: ${asset.assetTag} · S/N: ${asset.serialNumber} · Category: ${asset.category}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text("Issued: ${asset.issuedDate} · Custodian: ${asset.custodian}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("My Submitted Expense Claims (${myExpenses.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            items(myExpenses) { exp ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(exp.reference, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = when (exp.status) {
                                                    "Approved" -> Color(0xFFE8F5E9)
                                                    "Rejected" -> Color(0xFFFFEBEE)
                                                    else -> Color(0xFFFFF8E1)
                                                }
                                            ) {
                                                Text(
                                                    text = exp.status,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (exp.status) {
                                                        "Approved" -> Color(0xFF2E7D32)
                                                        "Rejected" -> Color(0xFFC62828)
                                                        else -> Color(0xFFF57F17)
                                                    },
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(exp.purpose, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("₹" + String.format(java.util.Locale.US, "%,.0f", exp.amount), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                            Text("Approver: ${exp.currentApproverRole}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            // Sub-tab 3: Tasks
                            item {
                                Text(
                                    text = "My Operational Tasks (${myTasks.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            items(myTasks) { task ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(task.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Text("Lane: ${task.lane} · Due: ${task.dueDate} · Priority: ${task.priority}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (task.isCompleted) Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
                                        ) {
                                            Text(
                                                text = if (task.isCompleted) "Completed" else "In Progress",
                                                color = if (task.isCompleted) Color(0xFF2E7D32) else Color(0xFF1565C0),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenRoleSwitcher,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Switch Role", fontSize = 11.sp)
                            }
                            Button(
                                onClick = onOpenAuthDialog,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Lead Sign In", fontSize = 11.sp)
                            }
                        }
                    }
                }
                }
            }
        }

        selectedSurveyForModal?.let { surv ->
            AlertDialog(
                onDismissRequest = { selectedSurveyForModal = null },
                title = { Text(text = surv.title, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Form: " + surv.formCode + " · Deadline: " + surv.deadline, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(text = surv.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "1. Were laboratory ESD & safety compliance standards observed? [Compliant]", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(text = "2. Operational feedback & suggestions:", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        OutlinedTextField(
                            value = surveyFeedbackInput,
                            onValueChange = { surveyFeedbackInput = it },
                            placeholder = { Text("Enter feedback remarks...", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().height(80.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scmsRepository.submitSurvey(surv.surveyId)
                            selectedSurveyForModal = null
                            surveyFeedbackInput = ""
                        }
                    ) {
                        Text("Submit Feedback")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { selectedSurveyForModal = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Chat Message Card
// -----------------------------------------------------------------------------

@Composable
fun ChatMessageCard(
    msg: TeamChatMessage,
    isMe: Boolean,
    onVotePoll: (messageId: String, optionId: String) -> Unit = { _, _ -> }
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.88f),
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (msg.senderDept) {
                        "EHW" -> Color(0xFFE3F2FD)
                        "EMF" -> Color(0xFFFFF3E0)
                        "QA" -> Color(0xFFE8F5E9)
                        "PUR" -> Color(0xFFEDE7F6)
                        "SAL" -> Color(0xFFFBE9E7)
                        else -> Color(0xFFECEFF1)
                    }
                ) {
                    Text(
                        text = msg.senderDept,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (msg.senderDept) {
                            "EHW" -> Color(0xFF1565C0)
                            "EMF" -> Color(0xFFE65100)
                            "QA" -> Color(0xFF2E7D32)
                            "PUR" -> Color(0xFF6A1B9A)
                            "SAL" -> Color(0xFFD84315)
                            else -> Color(0xFF37474F)
                        },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(msg.senderName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Text(msg.timestamp, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)

                if (msg.isUrgent) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFFEBEE)) {
                        Text("URGENT", color = Color(0xFFC62828), fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Surface(
                shape = RoundedCornerShape(
                    topStart = 10.dp,
                    topEnd = 10.dp,
                    bottomStart = if (isMe) 10.dp else 2.dp,
                    bottomEnd = if (isMe) 2.dp else 10.dp
                ),
                color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                    Text(
                        text = msg.message,
                        fontSize = 12.sp,
                        color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                    // Record Link Chip
                    msg.recordLinkRef?.let { ref ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$ref · ${msg.recordLinkTitle ?: "Linked Record"}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Interactive Technical Poll
                    msg.poll?.let { poll ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.Poll, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Technical Poll: ${poll.question}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (poll.isClosed) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFFEBEE)) {
                                            Text("CLOSED", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFFC62828), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val totalVotes = poll.options.sumOf { it.votes }.coerceAtLeast(1)
                                poll.options.forEach { opt ->
                                    val isUserVoted = poll.userVotedOptionId == opt.optionId
                                    val pct = (opt.votes.toFloat() / totalVotes)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isUserVoted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                            .clickable(enabled = !poll.isClosed) { onVotePoll(msg.id, opt.optionId) },
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isUserVoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(opt.text, fontSize = 10.sp, fontWeight = if (isUserVoted) FontWeight.Bold else FontWeight.Normal)
                                                Text("${opt.votes} votes", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            LinearProgressIndicator(
                                                progress = { pct },
                                                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(1.dp)),
                                                color = if (isUserVoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    msg.referenceId?.let { ref ->
                        if (msg.recordLinkRef == null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = "Ref: $ref",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Team Member Card
// -----------------------------------------------------------------------------

@Composable
fun TeamMemberCard(member: TeamMemberItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (member.avatarInitials.isNotBlank()) member.avatarInitials else member.name.take(2).uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(member.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${member.roleTitle} · ${member.deptCode}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (member.capacityStatus) {
                        "At Capacity" -> Color(0xFFFFEBEE)
                        "Optimal" -> Color(0xFFE8F5E9)
                        else -> Color(0xFFE3F2FD)
                    }
                ) {
                    Text(
                        text = member.capacityStatus,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (member.capacityStatus) {
                            "At Capacity" -> Color(0xFFC62828)
                            "Optimal" -> Color(0xFF2E7D32)
                            else -> Color(0xFF1565C0)
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Workload Progress Bar
            val taskProgress = (member.openTasksCount.toFloat() / 5f).coerceIn(0.1f, 1f)
            LinearProgressIndicator(
                progress = { taskProgress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = if (member.capacityStatus == "At Capacity") Color(0xFFC62828) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Contact info and Task count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (member.email.isNotBlank()) member.email else "Direct internal line",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${member.openTasksCount} Tasks Active",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
