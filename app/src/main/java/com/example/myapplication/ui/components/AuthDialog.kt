package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myapplication.BuildConfig
import com.example.myapplication.data.model.*
import com.example.myapplication.data.repository.AuthRepository

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AuthDialog(
    authRepository: AuthRepository,
    onSignIn: (String, String) -> Unit,
    authState: AuthState,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val busy = authState is AuthState.Authenticating
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("TEST workspace sign-in", style = MaterialTheme.typography.titleLarge)
            Text("Same existing accounts as web. Internet required. Server membership controls access.")
            Text("Prototype screens remain local demo data; sign-in alone does not sync those records.", style = MaterialTheme.typography.bodySmall)
            when (authState) {
                is AuthState.Authenticated -> {
                    Text("Signed in: ${authState.session.user?.email.orEmpty()}")
                    authState.session.testMembership?.let {
                        Text("${it.displayName} · ${it.role} · ${it.departmentCode ?: "Organization"}")
                    }
                    OutlinedButton(onClick = authRepository::signOut) { Text("Sign out") }
                }
                is AuthState.Authenticating -> { CircularProgressIndicator(); Text(authState.message) }
                is AuthState.Error -> Text(authState.message, color = MaterialTheme.colorScheme.error)
                else -> Unit
            }
            if (BuildConfig.DEBUG) {
                Text("One-click synthetic TEST accounts", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TestAccounts.all.forEach { account ->
                        OutlinedButton(enabled = !busy, onClick = { onSignIn(account.email, account.password) }) {
                            Text(account.label)
                        }
                    }
                }
            }
            OutlinedTextField(email, { email = it }, label = { Text("Email") }, enabled = !busy, singleLine = true)
            OutlinedTextField(password, { password = it }, label = { Text("Password") }, enabled = !busy,
                visualTransformation = PasswordVisualTransformation(), singleLine = true)
            Button(enabled = !busy && email.isNotBlank() && password.isNotBlank(),
                onClick = { onSignIn(email.trim(), password) }) { Text("Sign in") }
            OutlinedButton(enabled = !busy, onClick = {
                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(authRepository.buildGoogleWorkspaceAuthUrl())))
            }) { Text("Sign in with Google Workspace") }
            Spacer(Modifier.height(16.dp))
        }
    }
}
