package com.example.myapplication.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.AuthState
import com.example.myapplication.data.repository.AuthRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthDialog(
    authRepository: AuthRepository,
    authState: AuthState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Google Workspace Identity & Auth",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            when (authState) {
                is AuthState.Authenticated -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Authenticated Session Active",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "User Email: ${authState.session.user?.email ?: "Google Workspace Account"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Backend authorization is evaluated on each request. Backend policy setup and enforcement are not yet verified.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            authRepository.signOut()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Out")
                    }
                }
                else -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "PKCE Browser Authentication",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Sign in via Google Workspace SSO. OAuth authorization token exchanged via PKCE & stored strictly in Android Keystore.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val authUrl = authRepository.buildGoogleWorkspaceAuthUrl()
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Login, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Sign In with Google Workspace")
                    }
                }
            }
        }
    }
}

