package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.ControlledDoc
import com.example.myapplication.data.model.DepartmentRole
import com.example.myapplication.data.repository.ScmsRepository
import java.util.UUID

/**
 * Smart Upload & Document Ingestion Modal Bottom Sheet (Phase 3 Slice 1).
 * Implements 3-step contextual mobile ingestion workflow with auto-detected Document Catalogue families,
 * SHA-256 binary hash computation, canonical entity badges, and strict Draft-only invariants.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartUploadModalBottomSheet(
    scmsRepository: ScmsRepository,
    selectedProjectId: String,
    isAuthenticated: Boolean = false,
    onDismiss: () -> Unit,
    onUploadSuccess: (ControlledDoc) -> Unit
) {
    val currentRole by scmsRepository.currentRole.collectAsState()
    var currentStep by remember { mutableIntStateOf(1) } // 1: Source, 2: Classification, 3: Review & Draft

    // Preset options
    data class UploadPreset(
        val label: String,
        val filename: String,
        val familyCode: String,
        val familyName: String,
        val dept: String,
        val vendor: String,
        val sha256: String,
        val sizeBytes: Long
    )

    val presets = listOf(
        UploadPreset(
            label = "Mouser Electronics Quotation (LOT-2026-MOU-0881)",
            filename = "MKT-QUOTE-MOU-0881-REV01.pdf",
            familyCode = "D18",
            familyName = "Supplier Quotation",
            dept = "PUR",
            vendor = "Mouser Electronics Inc. (Approved AVL)",
            sha256 = "c7b2a9446d32ff8709e3a73b22cfd841e21b0213d29b0a1fa3919b48f6c49e21",
            sizeBytes = 248600L
        ),
        UploadPreset(
            label = "SMT 10-Zone Reflow Thermal Profile SOP (SOP-EMF-042)",
            filename = "SOP-EMF-042-REFLOW-PROFILE-B.pdf",
            familyCode = "D01",
            familyName = "Standard Operating Procedure (SOP/WI)",
            dept = "EMF",
            vendor = "Internal Manufacturing Engineering",
            sha256 = "8f39b165c028e18471bca491295b28a45fe29630e28f1f516a693bdf4650a3ef",
            sizeBytes = 512400L
        ),
        UploadPreset(
            label = "Dual-CAN Gateway Hardware Bring-Up & Power Rail Test Report",
            filename = "EHW-BRINGUP-AI4T-REV02.pdf",
            familyCode = "D13",
            familyName = "Hardware Bring-Up & Signal Integrity Report",
            dept = "EHW",
            vendor = "Automotive Embedded Lab",
            sha256 = "3a91e56b4df9c95101a91e4822bc43f670e1329bf3301a9b21de8734a6bc9920",
            sizeBytes = 1204800L
        ),
        UploadPreset(
            label = "Mechanical 3D STEP Enclosure Drawing (ENC-AI4T-REV-B)",
            filename = "MECH-DWG-ENC-AI4T-REVB.step",
            familyCode = "D10",
            familyName = "Mechanical 3D CAD & Drawing",
            dept = "MECH",
            vendor = "Mechanical Design Cell",
            sha256 = "4f228b32098d5c41a549e3190dfca0193856b32a4e29bf49120de30a1bf82910",
            sizeBytes = 4194304L
        )
    )

    val visiblePresets = presets.filter { currentRole == DepartmentRole.ADMIN || it.dept.equals(currentRole.code, ignoreCase = true) }

    if (isAuthenticated || visiblePresets.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (isAuthenticated) "Shared mobile upload is not connected yet" else "No local upload sample for this profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (isAuthenticated)
                    "This screen currently uses fictional local presets and does not upload an original file. It is disabled while signed in so it cannot appear to save to Shared TEST."
                else
                    "This local demo has no preset mapped to ${currentRole.code}. Use the Web Smart Upload route for this profile until the Android profile is implemented.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onDismiss) { Text("Close") }
        }
        return
    }

    var selectedPreset by remember(currentRole) { mutableStateOf(visiblePresets.first()) }
    var docTitle by remember { mutableStateOf(selectedPreset.label) }
    var docDept by remember { mutableStateOf(selectedPreset.dept) }
    var docFamily by remember { mutableStateOf("${selectedPreset.familyCode} · ${selectedPreset.familyName}") }
    var docRevision by remember { mutableStateOf("Rev 01 (Draft)") }

    // When preset changes, update form fields
    LaunchedEffect(selectedPreset) {
        docTitle = selectedPreset.label
        docDept = selectedPreset.dept
        docFamily = "${selectedPreset.familyCode} · ${selectedPreset.familyName}"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Smart Upload & Ingestion Gate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "IATF 16949 / Master Plan §14 Document Ingestion",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }

            // Invariant Notice Banner
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFF3E0),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                    text = "Local demo only: fictional preset metadata is saved in this device's demo repository. No original binary is uploaded and no Shared TEST record is created. No stock, PO, payment or lifecycle action is triggered.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.5.sp,
                        color = Color(0xFFE65100),
                        lineHeight = 14.sp
                    )
                }
            }

            // Step Progress Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("1. Source Intake", "2. Classification", "3. Guarded Save").forEachIndexed { index, stepName ->
                    val stepNum = index + 1
                    val isActive = currentStep == stepNum
                    val isCompleted = currentStep > stepNum
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (isCompleted) Color(0xFF2E7D32) else if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isCompleted) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                } else {
                                    Text(
                                        text = "$stepNum",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stepName,
                            fontSize = 11.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // STEP 1: SOURCE INTAKE
            if (currentStep == 1) {
                Text(
                    text = "Select Document Source or Preset",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                visiblePresets.forEach { preset ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPreset = preset },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedPreset == preset) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedPreset == preset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "File: ${preset.filename} (${preset.sizeBytes / 1024} KB)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.5.sp
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = preset.familyCode,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { currentStep = 2 },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Camera Scan", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { currentStep = 2 },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Continue →", fontSize = 11.sp)
                    }
                }
            }

            // STEP 2: CLASSIFICATION & HASH COMPUTATION
            if (currentStep == 2) {
                Text(
                    text = "Intelligent Classification & Hash Extraction",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Entity Match Badges Card
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto-Detected Family:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(
                                    text = "${selectedPreset.familyCode} · ${selectedPreset.familyName}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Canonical Vendor Match:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE8F5E9)) {
                                Text(
                                    text = "EXACT_MATCH (AVL)",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Project Link:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                Text(
                                    text = selectedProjectId,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "SHA-256 Binary Integrity Hash:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = selectedPreset.sha256,
                                modifier = Modifier.padding(6.dp),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }

                // Editable Fields
                OutlinedTextField(
                    value = docTitle,
                    onValueChange = { docTitle = it },
                    label = { Text("Document Title", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = docDept,
                        onValueChange = { docDept = it },
                        label = { Text("Department", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        readOnly = currentRole != DepartmentRole.ADMIN
                    )
                    OutlinedTextField(
                        value = docRevision,
                        onValueChange = { docRevision = it },
                        label = { Text("Revision", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        readOnly = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { currentStep = 1 },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("← Back", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { currentStep = 3 },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Review Ingestion →", fontSize = 11.sp)
                    }
                }
            }

            // STEP 3: GUARDED DRAFT SAVE
            if (currentStep == 3) {
                Text(
                    text = "Review & Guarded Draft Ingestion",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(docTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Family: $docFamily · Dept: $docDept · Project: $selectedProjectId",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "File: ${selectedPreset.filename} (${selectedPreset.sizeBytes / 1024} KB)",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Initial Status: Draft (Pending Domain Stage Sign-off)",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                    }
                }

                // Guarded confirmation checklist
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE8F5E9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA5D6A7))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sample hash shown for the preset; no binary was uploaded or verified", fontSize = 10.5.sp, color = Color(0xFF1B5E20))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Zero automatic stock allocation (Quarantine hold preserved)", fontSize = 10.5.sp, color = Color(0xFF1B5E20))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("No automatic PO release or finance payment release", fontSize = 10.5.sp, color = Color(0xFF1B5E20))
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { currentStep = 2 },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("← Edit", fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            val newDoc = ControlledDoc(
                                docId = "${selectedPreset.familyCode}-${UUID.randomUUID().toString().take(6).uppercase()}",
                                title = docTitle.trim(),
                                dept = docDept.trim(),
                                stage = "Stage 3: Requirements & Procurement Review",
                                owner = currentRole.leadName,
                                revision = "Rev 01",
                                status = "Draft",
                                confidentiality = "Confidential",
                                storageBucket = "controlled-documents",
                                storagePath = "staging/${selectedProjectId}/${selectedPreset.filename}",
                                sha256Hex = selectedPreset.sha256,
                                byteSize = selectedPreset.sizeBytes,
                                projectId = selectedProjectId
                            )
                            scmsRepository.addControlledDoc(newDoc)
                            onUploadSuccess(newDoc)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save as Draft", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
