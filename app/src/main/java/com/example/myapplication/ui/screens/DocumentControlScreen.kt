package com.example.myapplication.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.ControlledDoc
import com.example.myapplication.data.model.CustomerAccount
import com.example.myapplication.data.model.DepartmentRole
import com.example.myapplication.data.model.SalesOpportunity
import com.example.myapplication.data.repository.ScmsRepository
import java.util.UUID

@Composable
fun DocumentControlScreen(
    scmsRepository: ScmsRepository
) {
    val documents by scmsRepository.documents.collectAsState()
    val customers by scmsRepository.customers.collectAsState()
    val salesOpportunities by scmsRepository.salesOpportunities.collectAsState()

    var selectedTopTab by remember { mutableIntStateOf(0) } // 0: Controlled Documents, 1: Company Directory, 2: Security Vault, 3: Sales CRM
    var showSignedUrlModal by remember { mutableStateOf(false) }
    var selectedDocForSignedUrl by remember { mutableStateOf<ControlledDoc?>(null) }
    var signedUrlResult by remember { mutableStateOf<String?>(null) }
    var showAddCustomerModal by remember { mutableStateOf(false) }

    var newCustName by remember { mutableStateOf("") }
    var newCustDivision by remember { mutableStateOf("") }
    var newCustContact by remember { mutableStateOf("") }
    var newCustEmail by remember { mutableStateOf("") }
    var newCustLocation by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Row: Controlled Docs vs Company Directory vs Security Vault vs Sales CRM
        ScrollableTabRow(
            selectedTabIndex = selectedTopTab,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Tab(
                selected = selectedTopTab == 0,
                onClick = { selectedTopTab = 0 },
                text = { Text("Controlled Docs", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTopTab == 1,
                onClick = { selectedTopTab = 1 },
                text = { Text("Company Directory", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTopTab == 2,
                onClick = { selectedTopTab = 2 },
                text = { Text("Security Vault", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTopTab == 3,
                onClick = { selectedTopTab = 3 },
                text = { Text("Sales CRM & Accounts", fontWeight = FontWeight.Bold) }
            )
        }

        when (selectedTopTab) {
            0 -> {
                // Controlled Documents List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Canonical Document Formula (§1.3):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "HLPL-[PROJ_ID]-[DEPT]-[DOC_TYPE]-[REV]",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Private storage files require temporary HMAC signed URLs (300s TTL). Public downloads are disabled.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(documents) { doc ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = doc.docId,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Surface(
                                        color = when (doc.status) {
                                            "Approved" -> Color(0xFFE8F5E9)
                                            "Under Review" -> Color(0xFFFFF9C4)
                                            else -> Color(0xFFE3F2FD)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = doc.status,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = when (doc.status) {
                                                "Approved" -> Color(0xFF2E7D32)
                                                "Under Review" -> Color(0xFFF57F17)
                                                else -> Color(0xFF1976D2)
                                            }
                                        )
                                    }
                                }
                                Text(text = doc.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Dept: ${doc.dept} | Stage: ${doc.stage} | Owner: ${doc.owner}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = doc.revision,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                FilledTonalButton(
                                    onClick = {
                                        selectedDocForSignedUrl = doc
                                        val result = scmsRepository.requestSignedStorageUrl(doc.storageBucket, doc.storagePath)
                                        result.onSuccess { signedUrlResult = it }
                                    },
                                    modifier = Modifier.align(Alignment.End).height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Get 300s Signed URL", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Company Directory & Legal Hub
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Surface(
                            color = Color(0xFFEDE7F6),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Signed-In Company Directory (§2 & §8)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4A148C)
                                )
                                Text(
                                    text = "11 Functional Automotive Departments + Executive Leadership + Client OEM Access.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6A1B9A)
                                )
                            }
                        }
                    }

                    items(DepartmentRole.entries) { role ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (role.isClient) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = role.code.take(3),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = role.leadName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = role.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Security & Storage Vault
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Android Keystore Security Status",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("• Token Storage: EncryptedSharedPreferences (AES256_GCM)")
                                Text("• Keystore Master Key: _androidx_security_master_key_")
                                Text("• Plaintext SharedPreferences: Permanently Disabled")
                                Text("• Target Supabase Endpoint: ${com.example.myapplication.BuildConfig.SUPABASE_URL}")
                                Text("• OAuth Provider: Google Workspace PKCE (S256)")
                                Text("• Deep Link Callback: com.example.myapplication://auth-callback")
                                Text("• Service Keys / DB Passwords in APK: 0 (Zero)")
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Private Storage Policy",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("All CAD files (.stp), schematics (.pdf), DVP&R test records, and customer quotations are protected under ISO/IEC 27001:2022.")
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("To access, client generates an HMAC-signed token expiring in exactly 300 seconds.")
                            }
                        }
                    }
                }
            }
            3 -> {
                // Sales CRM & Customer Register (Master Plan §4 & §6)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sales CRM & Customer Register (§4)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Automotive OEM Client Accounts, Volume Commitments, and Enquiries.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Button(
                                    onClick = { showAddCustomerModal = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("+ Customer", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Client Accounts (${customers.size} Registered)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(customers) { cust ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cust.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${cust.division} · ${cust.location}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Surface(
                                        color = if (cust.tierRating.contains("Tier-1")) Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = cust.tierRating,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (cust.tierRating.contains("Tier-1")) Color(0xFF2E7D32) else Color(0xFF1976D2)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Contact: ${cust.contactPerson} | ${cust.email} | ${cust.phone}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "NDA: ${cust.ndaStatus}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (cust.ndaStatus.contains("Active")) Color(0xFF2E7D32) else Color(0xFFE65100)
                                    )
                                    if (cust.activeProjects.isNotEmpty()) {
                                        Text(
                                            text = "Projects: ${cust.activeProjects.joinToString()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sales Opportunities & Active Enquiries (${salesOpportunities.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(salesOpportunities) { opp ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = opp.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Client: ${opp.clientName} | SOR: ${opp.sorDocId}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Surface(
                                        color = when (opp.status) {
                                            "Contract Locked" -> Color(0xFFE8F5E9)
                                            "Feasibility Review" -> Color(0xFFE3F2FD)
                                            else -> Color(0xFFFFF3E0)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = opp.status,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = when (opp.status) {
                                                "Contract Locked" -> Color(0xFF2E7D32)
                                                "Feasibility Review" -> Color(0xFF1976D2)
                                                else -> Color(0xFFE65100)
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Volume: ${opp.volume} units",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Projected Value: ${opp.projectedValueInr}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Signed URL Result Dialog
    signedUrlResult?.let { url ->
        AlertDialog(
            onDismissRequest = { signedUrlResult = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Private Signed URL Generated")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Document: ${selectedDocForSignedUrl?.docId}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "TTL: 300 Seconds (Expires automatically)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = url,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { signedUrlResult = null }) { Text("Done") }
            }
        )
    }

    // Add Customer Account Dialog
    if (showAddCustomerModal) {
        AlertDialog(
            onDismissRequest = { showAddCustomerModal = false },
            title = { Text("Register Customer Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCustName,
                        onValueChange = { newCustName = it },
                        label = { Text("Company Name (e.g. Hero Electric)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCustDivision,
                        onValueChange = { newCustDivision = it },
                        label = { Text("Division (e.g. EV Commercial Fleet)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCustContact,
                        onValueChange = { newCustContact = it },
                        label = { Text("Contact Person") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCustEmail,
                        onValueChange = { newCustEmail = it },
                        label = { Text("Corporate Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCustLocation,
                        onValueChange = { newCustLocation = it },
                        label = { Text("Plant / Headquarter Location") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCustName.isNotBlank()) {
                            val newCust = CustomerAccount(
                                id = "CUST-${UUID.randomUUID().toString().take(6).uppercase()}",
                                name = newCustName,
                                division = newCustDivision.ifBlank { "Commercial EV Division" },
                                contactPerson = newCustContact.ifBlank { "Procurement Lead" },
                                email = newCustEmail.ifBlank { "contact@${newCustName.lowercase().replace(" ", "")}.com" },
                                phone = "+91 22 4000 0000",
                                location = newCustLocation.ifBlank { "India" },
                                activeProjects = emptyList(),
                                ndaStatus = "Active & Executed",
                                tierRating = "Tier-1 OEM"
                            )
                            scmsRepository.addCustomer(newCust)
                            newCustName = ""
                            newCustDivision = ""
                            newCustContact = ""
                            newCustEmail = ""
                            newCustLocation = ""
                            showAddCustomerModal = false
                        }
                    },
                    enabled = newCustName.isNotBlank()
                ) {
                    Text("Register Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun DocumentControlScreenPreview() {
    com.example.myapplication.ui.theme.MyApplicationTheme {
        DocumentControlScreen(
            scmsRepository = com.example.myapplication.data.repository.ScmsRepositoryImpl()
        )
    }
}

