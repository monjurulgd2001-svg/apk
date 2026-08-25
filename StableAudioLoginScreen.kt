package com.zedge.automation.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.zedge.automation.data.StableAudioAuth
import com.zedge.automation.ui.theme.HeaderDark
import com.zedge.automation.ui.theme.MintGreen
import com.zedge.automation.ui.theme.SoftRed
import com.zedge.automation.ui.theme.TextMuted
import com.zedge.automation.ui.theme.Violet
import com.zedge.automation.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StableAudioLoginScreen(
    vm: MainViewModel,
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val auth = remember { StableAudioAuth(vm.settings) }

    var statusMessage by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var successEmail by remember { mutableStateOf("") }
    var successPassword by remember { mutableStateOf("") }
    var successToken by remember { mutableStateOf("") }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val context = LocalContext.current

    // Smart back: block accidental exit while login/token extraction is running
    BackHandler {
        if (isBusy) {
            Toast.makeText(context, "Login in progress — please wait…", Toast.LENGTH_SHORT).show()
        } else {
            onBack()
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("Stable Audio Login", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {
                    if (isBusy) {
                        Toast.makeText(context, "Login in progress — please wait…", Toast.LENGTH_SHORT).show()
                    } else {
                        onBack()
                    }
                }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = HeaderDark)
        )

        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status card
            if (statusMessage.isNotBlank() || isBusy) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isError -> Color(0xFF2A1A1A)
                                showSuccess -> Color(0xFF1A2A1A)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when {
                                isBusy -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Violet,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(statusMessage, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                }
                                isError -> {
                                    Icon(Icons.Filled.Error, null, tint = SoftRed, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(statusMessage, color = SoftRed, style = MaterialTheme.typography.bodySmall)
                                }
                                showSuccess -> {
                                    Icon(Icons.Filled.CheckCircle, null, tint = MintGreen, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(statusMessage, color = MintGreen, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            // Success credential card
            if (showSuccess) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2E1A))
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, null, tint = MintGreen, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Token Synced Successfully!", color = MintGreen, fontWeight = FontWeight.Bold)
                            }
                            Text("New account created. Your bearer token is active.", color = TextMuted, style = MaterialTheme.typography.bodySmall)

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF23392E))
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CredentialRow("Email", successEmail)
                                    CredentialRow("Password", successPassword)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Info, null, tint = MintGreen, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Save these credentials for manual login at stableaudio.com",
                                    color = MintGreen,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Error card
            if (isError && !isBusy) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1A1A))
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Filled.Warning, null, tint = SoftRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Automation Failed", color = SoftRed, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(statusMessage, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Try again. If CAPTCHA appears, solve it in the WebView below.",
                                    color = TextMuted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            // Action buttons
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                isBusy = true
                                isError = false
                                showSuccess = false
                                statusMessage = "Starting automated account creation..."

                                val result = auth.performAutoAccountCreation(
                                    webView = webViewRef.value ?: return@launch,
                                    onStatus = { msg ->
                                        statusMessage = msg
                                    }
                                )

                                isBusy = false
                                if (result.success) {
                                    showSuccess = true
                                    successEmail = result.email ?: ""
                                    successPassword = result.password ?: ""
                                    successToken = result.token ?: ""
                                    statusMessage = "Account created and token synced!"
                                } else {
                                    isError = true
                                    statusMessage = result.error ?: "Unknown error"
                                }
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Violet)
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Auto-Create Account & Sync Token", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showLogoutConfirm = true },
                        enabled = !isBusy && vm.settings.hasStableAudioAccount(),
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Logout & Wipe")
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isBusy = true
                                statusMessage = "Extracting token from WebView..."
                                val result = auth.extractTokenFromWebView(webViewRef.value ?: return@launch)
                                isBusy = false
                                if (result.success) {
                                    showSuccess = true
                                    successToken = result.token ?: ""
                                    statusMessage = "Token extracted from current session!"
                                } else {
                                    isError = true
                                    statusMessage = result.error ?: "No token found"
                                }
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Extract Token")
                    }
                }
            }

            // Info text — automation runs silently in background
            item {
                Text(
                    "Automation runs in the background — account creation is fully automatic. No manual steps needed.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            // Hidden WebView — background-এ কাজ করে, ইউজার দেখবে না
            item {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                1  // 1px — DOM-এ থাকে কিন্তু দৃশ্যমান নয়
                            )
                            alpha = 0f
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.allowFileAccess = true
                            settings.userAgentString = settings.userAgentString.replace("wv", "")

                            val webViewInstance = this
                            CookieManager.getInstance().apply {
                                setAcceptCookie(true)
                                setAcceptThirdPartyCookies(webViewInstance, true)
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    settings.domStorageEnabled = true
                                }
                            }
                            webChromeClient = WebChromeClient()

                            webViewRef.value = this
                            loadUrl(StableAudioAuth.STABLE_AUDIO_URL)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(1.dp)  // invisible but functional
                )
            }
        }
    }

    // Logout confirmation dialog
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Logout & Wipe Session?") },
            text = {
                Text("This will log out from Stable Audio, wipe all cookies and session data, and clear saved credentials from this app.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    scope.launch {
                        isBusy = true
                        statusMessage = "Logging out and wiping session..."
                        auth.performFullLogout(webViewRef.value)
                        isBusy = false
                        showSuccess = false
                        isError = false
                        statusMessage = "Logged out and all data wiped."
                    }
                }) {
                    Text("Logout", color = SoftRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CredentialRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MintGreen, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
        Text(
            value,
            color = Color(0xFFBBF7D0),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
