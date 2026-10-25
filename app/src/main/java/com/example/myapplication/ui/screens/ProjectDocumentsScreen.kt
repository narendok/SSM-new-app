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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.myapplication.data.model.ClientDeliverable
import com.example.myapplication.data.model.ControlledDoc
import com.example.myapplication.data.repository.ScmsRepository
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDocumentsScreen(
    scmsRepository: ScmsRepository,
    onOpenRoleSwitcher: () -> Unit = {}
) {
    val documents by scmsRepository.documents.collectAsState()
    val clientDeliverables by scmsRepository.clientDeliverables.collectAsState()
    val apqpStages by scmsRepository.apqpStages.collectAsState()
    val currentRole by scmsRepository.currentRole.collectAsState()

    var selectedTopTab by remember { mutableIntStateOf(0) } // 0: 9-Stage APQP Dossier, 1: Controlled Docs, 2: Client Deliverables, 3: ISO/IATF Policy
    val selectedProject by scmsRepository.selectedProject.collectAsState()
    var selectedDeptFilter by remember { mutableStateOf("All") }
    var selectedDocForPreview by remember { mutableStateOf<ControlledDoc?>(null) }
    var showDraftModal by remember { mutableStateOf(false) }
    var signedUrlNotice by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP PROJECT CARD & HEADER
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
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PROJECT DOSSIER & DOCUMENT CONTROL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Text(
                                    text = "IATF 16949 / ISO 9001",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (selectedProject == "HLPL-AI4T-01") "Tata Ace EV 4G Telematics (HLPL-AI4T-01)" else "Controlled Documents Hub",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Client: Tata Motors Ltd · 50,000 units · ₹20.33 Cr · AIS-140 / IP67",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showDraftModal = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Draft Doc", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Draft", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Project Selector Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Project:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(
                        "HLPL-AI4T-01" to "Tata Ace EV",
                        "HLPL-SLCT-01" to "Select AIS-140",
                        "HLPL-SW-01" to "Fleet Cloud SW",
                        "HLPL-XLINK-03" to "Commercial CV",
                        "ALL" to "All Projects"
                    ).forEach { (id, label) ->
                        val isSelected = selectedProject == id
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) else null,
                            modifier = Modifier.clickable { scmsRepository.selectProject(id) }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Sub Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTopTab,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            val tabs = listOf("9-Stage APQP Dossier", "Controlled Docs (${documents.size})", "Client Deliverables (${clientDeliverables.size})", "Security & Keystore")
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTopTab == index,
                    onClick = { selectedTopTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedTopTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        // Department filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dept Filter:", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val depts = listOf("All", "SAL", "EHW", "ESW", "MECH", "PUR", "QA", "EMF", "FIN", "CLIENT")
                items(depts) { dept ->
                    val isDept = selectedDeptFilter == dept
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isDept) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (!isDept) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) else null,
                        modifier = Modifier.clickable { selectedDeptFilter = dept }
                    ) {
                        Text(
                            text = dept,
                            fontSize = 10.sp,
                            fontWeight = if (isDept) FontWeight.Bold else FontWeight.Normal,
                            color = if (isDept) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        signedUrlNotice?.let { url ->
            Surface(
                color = Color(0xFFE8F5E9),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("300s Secure Signed Storage Token Generated ✓", color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { signedUrlNotice = null }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // CONTENT
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (selectedTopTab) {
                0 -> {
                    // 9-STAGE APQP DOSSIER
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Automotive APQP 9-Stage Document Binder",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "All project engineering gates, feasibility reviews, schematics, and certificates linked to stages.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(apqpStages) { stage ->
                        val stageDocs = documents.filter { it.stage.contains("${stage.stageNumber}") || it.stage.contains(stage.name, ignoreCase = true) }
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
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "S${stage.stageNumber}",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stage.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (stage.status == "Approved") Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
                                    ) {
                                        Text(
                                            text = stage.status,
                                            color = if (stage.status == "Approved") Color(0xFF2E7D32) else Color(0xFF1565C0),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text("Prerequisite Gate: ${stage.prerequisiteDoc ?: "None"} · ${stage.description}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1)

                                if (stageDocs.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    stageDocs.forEach { doc ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                                .clickable { selectedDocForPreview = doc },
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(doc.docId, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    Text(doc.title, fontSize = 11.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("Inspect", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // ALL CONTROLLED DOCS REGISTER
                    val filteredDocs = documents.filter {
                        selectedDeptFilter == "All" || it.dept.equals(selectedDeptFilter, ignoreCase = true)
                    }
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Canonical Formula: HLPL-[PROJ_ID]-[DEPT]-[DOC_TYPE]-[REV]",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    items(filteredDocs) { doc ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDocForPreview = doc },
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
                                    Text(doc.docId, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (doc.status == "Approved") Color(0xFFE8F5E9) else Color(0xFFFFF9C4)
                                    ) {
                                        Text(
                                            text = doc.status,
                                            color = if (doc.status == "Approved") Color(0xFF2E7D32) else Color(0xFFF57F17),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(doc.title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Dept: ${doc.dept} · Stage: ${doc.stage} · Owner: ${doc.owner}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(doc.revision, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // CLIENT OEM DELIVERABLES
                    items(clientDeliverables) { deliv ->
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
                                    Text(deliv.code, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE8F5E9)) {
                                        Text(deliv.status, color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                    }
                                }
                                Text(deliv.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(deliv.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Standard: ${deliv.standard} · Owner: ${deliv.owner}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(deliv.date, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // SECURITY & KEYSTORE
                    item {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Android Keystore & Encrypted Storage", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("• Token Storage: EncryptedSharedPreferences (AES256_GCM)", fontSize = 11.sp)
                                Text("• Keystore Master Key: _androidx_security_master_key_", fontSize = 11.sp)
                                Text("• Plaintext SharedPreferences: Permanently Disabled", fontSize = 11.sp)
                                Text("• ISO/IEC 27001:2022 Private Storage TTL: 300 seconds", fontSize = 11.sp)
                                Text("• Service Keys / DB Passwords in APK: 0 (Zero)", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // FULL PAGE DOCUMENT PREVIEW MODAL WITH COMPANY HEADER & FOOTER
    selectedDocForPreview?.let { doc ->
        ModalBottomSheet(
            onDismissRequest = { selectedDocForPreview = null },
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            FullDocumentSheetView(
                doc = doc,
                scmsRepository = scmsRepository,
                onClose = { selectedDocForPreview = null },
                onSignedUrlGenerated = { url -> signedUrlNotice = url }
            )
        }
    }

    // DRAFT NEW DOCUMENT DIALOG
    if (showDraftModal) {
        DraftDocumentDialog(
            scmsRepository = scmsRepository,
            onDismiss = { showDraftModal = false },
            onSuccess = { showDraftModal = false }
        )
    }
}

// -----------------------------------------------------------------------------
// Full In-App Document Preview Sheet with Company Header & Compliance Footer
// -----------------------------------------------------------------------------

@Composable
fun FullDocumentSheetView(
    doc: ControlledDoc,
    scmsRepository: ScmsRepository,
    onClose: () -> Unit,
    onSignedUrlGenerated: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Action bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Controlled Document Preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        }

        // WHITE A4 SHEET DOCUMENT CONTAINER
        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // COMPANY HEADER TABLE
                    Surface(
                        color = Color(0xFFF8F9FA),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD0D7DE)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF1565C0),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("SS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("HEXENSE LABS PRIVATE LIMITED", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                                        Text("SIX SENSE MOBILITY · AUTOMOTIVE ELECTRONICS", color = Color.DarkGray, fontSize = 9.sp)
                                    }
                                }
                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE8F5E9)) {
                                    Text("IATF 16949:2016", color = Color(0xFF2E7D32), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFD0D7DE))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Document No:", color = Color.Gray, fontSize = 9.sp)
                                    Text(doc.docId, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Column {
                                    Text("Revision:", color = Color.Gray, fontSize = 9.sp)
                                    Text(doc.revision, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Column {
                                    Text("Status:", color = Color.Gray, fontSize = 9.sp)
                                    Text(doc.status, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // DOCUMENT TITLE
                    Text(
                        text = doc.title,
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Department: ${doc.dept} · Lifecycle Stage: ${doc.stage} · Author: ${doc.owner}",
                        color = Color.DarkGray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // SECTION 1.0 SCOPE & VEHICLE IDENTIFICATION
                    Text("1.0 Scope & Vehicle Identification", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        text = "This engineering specification governs the design, pre-compliance validation, SMT line configuration, and mass manufacturing of the AIS-140 Compliant 4G Telematics Gateway (HLPL-AI4T-01) for Tata Motors Limited (Tata Ace EV platform). Total volume: 50,000 units/year.",
                        color = Color(0xFF333333),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // SECTION 2.0 TECHNICAL SPECIFICATIONS TABLE
                    Text("2.0 Technical Architecture Parameters", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Surface(
                        color = Color(0xFFF8F9FA),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Microcontroller", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text("STM32G0B1VCT6 (ARM Cortex-M0+ 64MHz)", color = Color.DarkGray, fontSize = 10.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cellular Modem", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text("Quectel EG915N-EU LTE Cat-1 + GNSS", color = Color.DarkGray, fontSize = 10.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Vehicle Bus Interface", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text("ISO 11898-2 CAN J1939 @ 500kbps", color = Color.DarkGray, fontSize = 10.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Enclosure Protection", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text("IP67 Ingress Rated Die-Cast Aluminum", color = Color.DarkGray, fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // SECTION 3.0 RACI SIGN-OFF APPROVALS
                    Text("3.0 Engineering Approvals & Governance", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Surface(
                        color = Color(0xFFF1F8E9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC8E6C9)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text("✓ EHW Lead (Avery Vale) — Verified Netlist & Schematics Rev 02", color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Text("✓ QA Lead (Jordan Blake) — ISO 7637-2 Transient Test Passed", color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Text("✓ Client Rep (Rohan Verma · Tata Motors) — SOR Accepted", color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // COMPLIANCE FOOTER
                    HorizontalDivider(color = Color(0xFFD0D7DE))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Confidential & Proprietary · Hexense Labs Pvt Ltd", color = Color.Gray, fontSize = 9.sp)
                        Text("ISO 9001:2015 · Page 1 of 1", color = Color.Gray, fontSize = 9.sp)
                    }
                }
            }
        }

        // Actions: Generate Signed URL and Print
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val result = scmsRepository.requestSignedStorageUrl(doc.storageBucket, doc.storagePath)
                        result.onSuccess { onSignedUrlGenerated(it) }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("300s Signed URL", fontSize = 11.sp)
                }

                Button(
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close Preview", fontSize = 11.sp)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// -----------------------------------------------------------------------------
// Draft Document Dialog
// -----------------------------------------------------------------------------

@Composable
fun DraftDocumentDialog(
    scmsRepository: ScmsRepository,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val selectedProject by scmsRepository.selectedProject.collectAsState()
    val projRef = if (selectedProject == "ALL") "HLPL-AI4T-01" else selectedProject

    var docType by remember { mutableStateOf("PRS") }
    var dept by remember { mutableStateOf("EHW") }
    var title by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf("Stage 3") }

    val autoDocId = "$projRef-$dept-$docType-01"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Draft New Controlled Document", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Auto-Computed Document ID:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(autoDocId, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title") },
                    placeholder = { Text("e.g. Firmware Architecture Spec") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dept,
                        onValueChange = { dept = it },
                        label = { Text("Dept") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = docType,
                        onValueChange = { docType = it },
                        label = { Text("Type") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newDoc = ControlledDoc(
                        docId = autoDocId,
                        title = if (title.isNotBlank()) title else "Product Requirement Document",
                        dept = dept,
                        stage = stage,
                        owner = "Department Lead",
                        revision = "Rev 01",
                        status = "Under Review"
                    )
                    scmsRepository.addControlledDoc(newDoc)
                    onSuccess()
                }
            ) {
                Text("Register Document")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
