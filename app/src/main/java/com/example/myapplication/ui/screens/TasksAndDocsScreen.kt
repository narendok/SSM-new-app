package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.repository.PrivateStorageRepository
import com.example.myapplication.ui.components.DocumentTransitionBanner
import com.example.myapplication.ui.components.EmptyStateView
import com.example.myapplication.ui.components.PrivateFileSheet

@Composable
fun TasksAndDocsScreen(
    privateStorageRepository: PrivateStorageRepository
) {
    var showPrivateFileSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        DocumentTransitionBanner()

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Document Control & Audit Registers",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Safe Views: document_control_registers, controlled_document_audit_timeline",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showPrivateFileSheet = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Request Private File Signed URL")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        EmptyStateView(
            title = "Document Registers Unimplemented",
            description = "Read-only view projections for document registers and audit timelines stay unimplemented without fake queries until live database schema is verified with backend.",
            icon = Icons.Default.Assignment
        )
    }

    if (showPrivateFileSheet) {
        PrivateFileSheet(
            privateStorageRepository = privateStorageRepository,
            onDismiss = { showPrivateFileSheet = false }
        )
    }
}
