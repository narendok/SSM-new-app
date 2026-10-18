package com.example.myapplication.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.ResolvedScanResult
import com.example.myapplication.data.model.ScannedEntityType
import com.example.myapplication.data.repository.ScmsRepository

@Composable
fun UniversalScannerModal(
    scmsRepository: ScmsRepository,
    onNavigateToRoute: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("STM32G0B1VCT6") }
    var resolvedResult by remember { mutableStateOf<ResolvedScanResult?>(scmsRepository.resolveScanQuery("STM32G0B1VCT6")) }

    val quickSimulationPresets = listOf(
        "STM32G0B1VCT6" to "Reel QR",
        "A-12-04" to "Storage Bin",
        "HLPL-PO-2026-042" to "PO Barcode",
        "EQ-SMT-001" to "Plant Asset",
        "EHW-SCH-01" to "Controlled Doc",
        "GRN-2026-088" to "IQC Hold Lot",
        "GP-2026-019" to "Gate Pass",
        "HLPL-EMP-102" to "Staff Badge",
        "CAL-2026-089" to "Calibration Cert"
    )

    // Animated scanning laser bar
    val infiniteTransition = rememberInfiniteTransition(label = "scanLaser")
    val laserOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 90f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserPosition"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Universal Scanner",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Camera & Barcode Entity Resolver",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Viewfinder Viewport
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Target reticle corners
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                    )

                    // Laser line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(2.dp)
                            .offset(y = (laserOffsetY - 45).dp)
                            .background(Color(0xFFEF4444))
                    )

                    // Overlay HUD text
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "AIM AT 1D BARCODE OR 2D QR MATRIX",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input & Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        resolvedResult = scmsRepository.resolveScanQuery(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Scanned Barcode / QR Payload", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Simulation Shortcuts Row
                Text(
                    text = "Quick Simulation Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickSimulationPresets) { (code, label) ->
                        val isSelected = searchQuery.equals(code, ignoreCase = true)
                        Surface(
                            modifier = Modifier.clickable {
                                searchQuery = code
                                resolvedResult = scmsRepository.resolveScanQuery(code)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Resolved Entity Card
                resolvedResult?.let { result ->
                    val (badgeColor, badgeTextColor, entityIcon) = getEntityVisuals(result.entityType)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Header: Badge + Raw code
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = badgeColor
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = entityIcon,
                                            contentDescription = null,
                                            tint = badgeTextColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = result.entityType.name.replace("_", " "),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeTextColor,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Text(
                                    text = result.rawCode,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = result.primaryTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = result.secondarySubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Details Grid (2 items per row)
                            result.details.entries.chunked(2).forEach { rowEntries ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    rowEntries.forEach { entry ->
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.key,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = entry.value,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (rowEntries.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action Button
                            Button(
                                onClick = {
                                    onDismiss()
                                    onNavigateToRoute(result.actionRoute)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = result.actionLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun getEntityVisuals(type: ScannedEntityType): Triple<Color, Color, ImageVector> {
    return when (type) {
        ScannedEntityType.COMPONENT -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), Icons.Default.Category)
        ScannedEntityType.STORAGE_BIN -> Triple(Color(0xFFEDE9FE), Color(0xFF6D28D9), Icons.Default.LocationOn)
        ScannedEntityType.PURCHASE_ORDER -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), Icons.Default.LocalShipping)
        ScannedEntityType.PLANT_ASSET -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), Icons.Default.PrecisionManufacturing)
        ScannedEntityType.CONTROLLED_DOC -> Triple(Color(0xFFFCE7F3), Color(0xFFBE185D), Icons.Default.Description)
        ScannedEntityType.LOT_GRN -> Triple(Color(0xFFFFEDD5), Color(0xFFC2410C), Icons.Default.Inventory2)
        ScannedEntityType.GATE_PASS -> Triple(Color(0xFFE0E7FF), Color(0xFF3730A3), Icons.Default.LocalShipping)
        ScannedEntityType.EMPLOYEE_BADGE -> Triple(Color(0xFFF3E8FF), Color(0xFF6B21A8), Icons.Default.DirectionsRun)
        ScannedEntityType.CALIBRATION_CERT -> Triple(Color(0xFFCCFBF1), Color(0xFF0F766E), Icons.Default.CheckCircle)
        ScannedEntityType.UNKNOWN -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), Icons.Default.Warning)
    }
}
