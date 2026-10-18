package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.myapplication.ui.screens.AppShell
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleDeepLinkIntent(intent)

        setContent {
            MyApplicationTheme {
                AppShell(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "com.example.myapplication" && uri.host == "auth-callback") {
            val code = uri.getQueryParameter("code")
            if (!code.isNullOrBlank()) {
                viewModel.handleAuthCallbackCode(code)
            }
        }
    }
}
