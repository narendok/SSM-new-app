package com.example.myapplication.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.myapplication.data.model.DvprTestItem
import com.example.myapplication.data.model.IngestionRow
import com.example.myapplication.data.repository.ScmsRepository

@Composable
fun AdaptiveIngestionAndQualityScreen(
    scmsRepository: ScmsRepository
) {
    val currentRole by scmsRepository.currentRole.collectAsState()
    val ingestionRows by scmsRepository.ingestionRows.collectAsState()
    val dvprTests by scmsRepository.dvprTests.collectAsState()

    var selectedTopTab by remember { mutableIntStateOf(0) } // 0: Adaptive Ingestion Gate, 1: DVP&R Pre-Compliance
    var activeDistributorPreset by remember { mutableStateOf("All Presets") }
    var selectedDvprForSignOff by remember { mutableStateOf<DvprTestItem?>(null) }
    var inwardSuccessGrn by remember { mutableStateOf<String?>(null) }

    val presets = listOf("All Presets", "Mouser", "Digi-Key", "Custom Harness")

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Row: Ingestion Gate vs QA DVP&R Certification vs Architecture
        TabRow(
            selectedTabIndex = selectedTopTab,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Tab(
                selected = selectedTopTab == 0,
                onClick = { selectedTopTab = 0 },
                text = { Text("Ingestion Gate", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTopTab == 1,
                onClick = { selectedTopTab = 1 },
                text = { Text("QA DVP&R", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTopTab == 2,
                onClick = { selectedTopTab = 2 },
                text = { Text("Device Architecture", fontWeight = FontWeight.Bold) }
            )
        }

        when (selectedTopTab) {
            0 -> {
                // Adaptive Ingestion Staging Gate
                LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Invariant Banner: ZERO Automatic Stock Posting
                item {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Strict Invariant: ZERO Automatic Stock Posting (§3.B)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Arbitrary supplier spreadsheets (Mouser, Digi-Key, Arrow, Fabricator) are staged with fuzzy column matching. Accepted inwards strictly generate GRN drafts on 100% IQC Quarantine Hold (IQC-HOLD-STAGE-01) with Qty Accepted = 0 pcs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                // Distributor Presets Filter
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Text("Distributor:", style = MaterialTheme.typography.labelSmall)
                        }
                        items(presets) { preset ->
                            FilterChip(
                                selected = activeDistributorPreset == preset,
                                onClick = { activeDistributorPreset = preset },
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Staging Gate Rows
                items(
                    ingestionRows.filter {
                        activeDistributorPreset == "All Presets" || it.distributorPreset.contains(activeDistributorPreset, ignoreCase = true)
                    }
                ) { row ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (row.isProcessed) Color(0xFFF5F5F5) else MaterialTheme.colorScheme.surfaceContainer
                        ),
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
                                    text = row.mpn,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    color = if (row.isProcessed) Color(0xFFE0E0E0) else Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (row.isProcessed) "✓ Inward Staged" else "⚡ 120% Fuzzy Match",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (row.isProcessed) Color(0xFF757575) else Color(0xFF2E7D32)
                                    )
                                }
                            }
                            Text(text = "Vendor: ${row.vendorName}", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(4.dp))

                            // 3-Way Reconciliation Indicators
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Vendor: ${row.vendorStatus}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (row.vendorStatus == "Verified ASL") Color(0xFF2E7D32) else Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Catalog: ${row.catalogStatus}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (row.catalogStatus.contains("Exact")) Color(0xFF2E7D32) else Color(0xFFE65100),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Ordered: ${row.orderedQty} | Billed: ${row.billedQty} pcs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Price: $${row.unitPrice} (Var: ${if (row.priceVariancePct > 0) "+${row.priceVariancePct}%" else "0%"})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (row.priceVariancePct > 0) Color(0xFFC62828) else Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (!row.isProcessed) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val result = scmsRepository.generateGrnFromIngestion(row.id)
                                        result.onSuccess { inwardSuccessGrn = it }
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    contentPadding = ButtonDefaults.TextButtonContentPadding
                                ) {
                                    Icon(imageVector = Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Accept Inward to IQC Quarantine Hold")
                                }
                            }
                        }
                    }
                }
            }
        }
        1 -> {
            // QA DVP&R Pre-Compliance Certification
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Surface(
                        color = Color(0xFFE1F5FE),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Quality Assurance DVP&R Pre-Compliance Certification (§5)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0277BD)
                            )
                            Text(
                                text = "Tracks 8 automotive tests across ISO 7637-2, ISO 16750-2, CISPR 25, ISO 10605, IP67, and AIS-140. Zero-deviation sign-off required to unlock manufacturing gates.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF01579B)
                            )
                        }
                    }
                }

                items(dvprTests) { test ->
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
                                Text(
                                    text = "${test.testId}: ${test.standard}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = when (test.status) {
                                        "Passed" -> Color(0xFFE8F5E9)
                                        "In Progress" -> Color(0xFFE3F2FD)
                                        else -> Color(0xFFFFF9C4)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = test.status,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when (test.status) {
                                            "Passed" -> Color(0xFF2E7D32)
                                            "In Progress" -> Color(0xFF1976D2)
                                            else -> Color(0xFFF57F17)
                                        }
                                    )
                                }
                            }
                            Text(text = test.testTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Condition: ${test.condition}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Criteria: ${test.acceptanceCriteria}",
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
                                    text = "Lab: ${test.lab}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (test.lab.contains("Internal")) Color(0xFF1565C0) else Color(0xFF6A1B9A)
                                )
                                if (test.status != "Passed") {
                                    Button(
                                        onClick = { selectedDvprForSignOff = test },
                                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("Sign-Off")
                                    }
                                } else {
                                    Text(
                                        text = "✓ ${test.technician} (${test.signOffTimestamp})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        2 -> {
            // Device Architecture & Block Diagram
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
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "HLPL-AI4T-01 Architecture & Block Diagram",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Hardware Subsystem Blueprint for Tata Ace EV 4G Telematics Gateway. Automotive Grade Component Layout.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "1. Power Subsystem & Surge Protection",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• Input Voltage: 9V - 36V DC nominal with ISO 7637-2 Transient Protection (Pulse 1, 2a, 3a/b, 5b Load Dump to 87V).\n• Primary Regulator: TI LMR14050 40V 5A High-Efficiency Buck Converter.\n• Regulated System Rails: 3.3V Logic Rail (@ 1.5A) & 3.8V High-Peak Cellular Modem Rail (@ 3.0A peak).\n• Reverse Polarity & OVP: P-Channel MOSFET reverse polarity gate interlock + SMAJ40A TVS array.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "2. Processing Core & Cryptographic Root-of-Trust",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• MCU: STMicroelectronics STM32H753 ARM Cortex-M7 @ 480 MHz, 2MB Flash, 1MB SRAM.\n• Hardware Root-of-Trust: Microchip ATECC608B CryptoAuthentication (ECDSA P-256 private key vault).\n• Storage Architecture: Dual-Bank 1MB + 1MB flash partition for zero-downtime A/B FOTA delta rollback.\n• External Memory: 32MB SPI NOR Flash for offline AIS-140 emergency health telemetry buffer.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "3. Cellular Telematics & Global Navigation Engine",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• Modem: Quectel EG915N-EU LTE Cat-1 (B1/B3/B5/B7/B8/B20/B28) with 10 Mbps DL / 5 Mbps UL.\n• GNSS Receiver: Concurrent GPS, GLONASS, BeiDou, Galileo multi-constellation engine with 2.5m CEP.\n• SIM Architecture: Industrial e-SIM (MFF2 soldered) with primary failover to Micro-SIM slot.\n• Antennas: Dual internal ceramic patch antennas with external SMA antenna connectors.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "4. Automotive Vehicle Bus & Isolated I/O Interfaces",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• CAN Bus: Dual ISO 11898-2 CAN-FD channels with TI TCAN1042VDRQ1 transceivers & Wake-on-CAN.\n• Digital Inputs: 4x 24V Optoisolated inputs (Ignition Sense, Panic SOS, Tamper Switch, PTO Sense).\n• Driver Outputs: 2x 2.5A High-Side protected solid-state switches for immobilizer relay trigger.\n• Connector Interface: Molex MX150 16-Pin Sealed Automotive Dual-Row Header (IP6K9K mating).",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "5. Mechanical Packaging & Environmental Sealing",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• Enclosure: Die-Cast ADC12 Aluminum Alloy with integrated thermal dissipation heatsink fins.\n• Ingress Protection: IP67 submersion-proof perimeter compression silicone gasket seal.\n• Pressure Relief: Gore Automotive Breather Membrane to equalize internal pressure and prevent fogging.\n• Temperature Rating: -40°C to +85°C Operating / -40°C to +105°C Storage (AEC-Q Grade 2).",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Inwarding to IQC Quarantine Success Dialog
    inwardSuccessGrn?.let { grnNo ->
        AlertDialog(
            onDismissRequest = { inwardSuccessGrn = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Inward Delivery Staged")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Draft GRN Created: $grnNo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Placed into Bin: IQC-HOLD-STAGE-01",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Quantity Accepted: 0 pcs (Strict Zero Auto Stock Posting)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Quantity in Quarantine: 100% of Billed Quantity",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Stock is strictly non-issueable until Quality team releases disposition in Tab 4 (GRN & IQC Hold).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { inwardSuccessGrn = null }) { Text("Acknowledge") }
            }
        )
    }

    // DVP&R Sign-Off Dialog
    selectedDvprForSignOff?.let { test ->
        AlertDialog(
            onDismissRequest = { selectedDvprForSignOff = null },
            title = { Text("DVP&R Test Sign-Off") },
            text = {
                Column {
                    Text("Standard: ${test.standard} - ${test.testTitle}")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Accredited Lab: ${test.lab}")
                    Text("Criteria: ${test.acceptanceCriteria}")
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Mark test as Passed with SHA-256 digital signature recorded under ${currentRole.leadName}?",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scmsRepository.updateDvprTestStatus(test.testId, "Passed", currentRole.leadName)
                        selectedDvprForSignOff = null
                    }
                ) {
                    Text("Approve & Pass Test")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDvprForSignOff = null }) { Text("Cancel") }
            }
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun AdaptiveIngestionAndQualityScreenPreview() {
    com.example.myapplication.ui.theme.MyApplicationTheme {
        AdaptiveIngestionAndQualityScreen(
            scmsRepository = com.example.myapplication.data.repository.ScmsRepositoryImpl()
        )
    }
}

