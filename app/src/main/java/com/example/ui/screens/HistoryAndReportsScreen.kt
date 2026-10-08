package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AuditSessionEntity
import com.example.data.local.entity.TargetProfileEntity
import com.example.ui.MainAuditViewModel
import com.example.ui.components.EmptyAuditState
import com.example.ui.components.TerminalCodeBlock
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryAndReportsScreen(
    viewModel: MainAuditViewModel,
    onNavigateToScan: (target: String, tab: Int) -> Unit
) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableStateOf(0) } // 0: Audit History, 1: Target Bookmarks, 2: Report Exporter
    val sessions by viewModel.allAuditSessions.collectAsStateWithLifecycle()
    val targets by viewModel.allTargetProfiles.collectAsStateWithLifecycle()

    var showAddTargetDialog by remember { mutableStateOf(false) }
    var newTargetName by remember { mutableStateOf("") }
    var newTargetHost by remember { mutableStateOf("") }
    var newTargetCategory by remember { mutableStateOf("INFRASTRUCTURE") }
    var newTargetDesc by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tab Selector
        item {
            SecondaryTabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = DarkSurface,
                contentColor = CyberCyan
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = { Text("AUDIT HISTORY (${sessions.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = { Text("TARGET BOOKMARKS", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                )
                Tab(
                    selected = selectedSubTab == 2,
                    onClick = { selectedSubTab = 2 },
                    text = { Text("EXECUTIVE REPORT", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                )
            }
        }

        when (selectedSubTab) {
            0 -> {
                // Audit History Sessions List
                if (sessions.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STORED AUDIT SESSIONS",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            TextButton(onClick = { viewModel.clearAllHistory() }) {
                                Text("Clear History", color = CyberRed, fontSize = 11.sp)
                            }
                        }
                    }

                    items(sessions) { session ->
                        AuditSessionItemCard(
                            session = session,
                            onFavorite = { viewModel.toggleFavorite(session) },
                            onDelete = { viewModel.deleteSession(session.id) },
                            onRerun = {
                                val tab = when (session.scanType) {
                                    "PORT_SCAN" -> 1
                                    "TLS_AUDIT" -> 2
                                    "HTTP_HEADER_AUDIT" -> 3
                                    "DNS_INSPECT" -> 4
                                    "TRACEROUTE" -> 7
                                    else -> 1
                                }
                                onNavigateToScan(session.target, tab)
                            }
                        )
                    }
                } else {
                    item {
                        EmptyAuditState(
                            icon = Icons.Default.History,
                            title = "No Stored Audit Sessions",
                            subtitle = "Perform a port scan, TLS inspection, or HTTP header audit to persist historical records."
                        )
                    }
                }
            }

            1 -> {
                // Target Profiles & Bookmarks
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SAVED AUDIT TARGETS (${targets.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberGreen,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = { showAddTargetDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF00363A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ADD TARGET", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(targets) { target ->
                    TargetProfileItemCard(
                        target = target,
                        onDelete = { viewModel.deleteTargetProfile(target) },
                        onScan = { onNavigateToScan(target.targetHost, 1) }
                    )
                }
            }

            2 -> {
                // Executive Security Report Generator
                item {
                    val markdownReport = generateMarkdownReport(sessions, targets)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(14.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "EXECUTIVE SECURITY REPORT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )

                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("NetSentinel Audit Report", markdownReport)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Full Markdown Report Copied to Clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF00363A)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("COPY REPORT", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            TerminalCodeBlock(text = markdownReport, title = "GENERATED AUDIT MARKDOWN")
                        }
                    }
                }
            }
        }
    }

    // Add Target Dialog
    if (showAddTargetDialog) {
        AlertDialog(
            onDismissRequest = { showAddTargetDialog = false },
            title = { Text("Add New Target Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTargetName,
                        onValueChange = { newTargetName = it },
                        label = { Text("Profile Name") },
                        placeholder = { Text("e.g. Production Web Gateway") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTargetHost,
                        onValueChange = { newTargetHost = it },
                        label = { Text("Host or IP Address") },
                        placeholder = { Text("e.g. 192.168.1.1 or api.example.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTargetDesc,
                        onValueChange = { newTargetDesc = it },
                        label = { Text("Description (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTargetName.isNotBlank() && newTargetHost.isNotBlank()) {
                            viewModel.addTargetProfile(newTargetName, newTargetHost, newTargetDesc, newTargetCategory)
                            showAddTargetDialog = false
                            newTargetName = ""
                            newTargetHost = ""
                            newTargetDesc = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF00363A))
                ) {
                    Text("Save Target")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTargetDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun AuditSessionItemCard(
    session: AuditSessionEntity,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onRerun: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
    val formattedDate = remember(session.timestamp) { dateFormat.format(Date(session.timestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.scanType.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = session.target,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = session.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRerun, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Rerun Scan", tint = CyberGreen, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun TargetProfileItemCard(
    target: TargetProfileEntity,
    onDelete: () -> Unit,
    onScan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = target.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = target.targetHost,
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                if (target.description.isNotBlank()) {
                    Text(
                        text = target.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onScan,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF00363A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("AUDIT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

fun generateMarkdownReport(sessions: List<AuditSessionEntity>, targets: List<TargetProfileEntity>): String {
    val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
    val builder = StringBuilder()

    builder.appendLine("# NETSENTINEL PRO - ENTERPRISE DEFENSIVE AUDIT REPORT")
    builder.appendLine("**Generated Date:** $date")
    builder.appendLine("**Assessment Classification:** Authorized Defensive Network & Protocol Audit")
    builder.appendLine("**Engine Version:** NetSentinel Core v1.0 (Android Clean Architecture)")
    builder.appendLine()
    builder.appendLine("---")
    builder.appendLine("## 1. Executive Summary")
    builder.appendLine("This report summarizes defensive telemetry, protocol configuration, and vulnerability findings gathered across ${sessions.size} recorded audit sessions and ${targets.size} configured target profiles.")
    builder.appendLine()
    builder.appendLine("## 2. Audit Findings Breakdown")
    if (sessions.isEmpty()) {
        builder.appendLine("No historical sessions recorded.")
    } else {
        sessions.forEachIndexed { index, s ->
            builder.appendLine("### 2.${index + 1} Target: `${s.target}`")
            builder.appendLine("- **Module:** `${s.scanType}`")
            builder.appendLine("- **Score / Rating:** `${s.scoreOrGrade}`")
            builder.appendLine("- **Summary:** ${s.summary}")
            builder.appendLine()
        }
    }
    builder.appendLine("---")
    builder.appendLine("## 3. Recommended Remediation Roadmap")
    builder.appendLine("1. **Enforce HSTS Preload:** Ensure `Strict-Transport-Security: max-age=31536000; includeSubDomains; preload` is present on all production endpoints.")
    builder.appendLine("2. **Tighten CSP Directives:** Eliminate `'unsafe-inline'` and enforce nonce-based or hash-based script execution.")
    builder.appendLine("3. **Deprecate Legacy Ciphers:** Restrict TLS handshakes to TLSv1.3 and TLSv1.2 with AEAD ciphers (AES-GCM / CHACHA20-POLY1305).")
    builder.appendLine("4. **Close Unused Management Ports:** Restrict Telnet (23), SMB (445), and unauthenticated databases to VPN-only access.")
    builder.appendLine()
    builder.appendLine("---")
    builder.appendLine("*Report generated by NetSentinel Pro.*")

    return builder.toString()
}
