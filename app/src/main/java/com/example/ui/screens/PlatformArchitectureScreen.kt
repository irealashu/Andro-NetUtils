package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.PlatformCapabilityItem
import com.example.ui.MainAuditViewModel
import com.example.ui.components.TerminalCodeBlock
import com.example.ui.theme.*

@Composable
fun PlatformArchitectureScreen(viewModel: MainAuditViewModel) {
    val report by viewModel.platformReport.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // System Sandboxing State Hero
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(CyberPurple.copy(alpha = 0.5f), Color.Transparent))
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ANDROID PLATFORM & SANDBOX ARCHITECTURE",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberPurple,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = report?.androidVersion ?: "Android Runtime",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SandboxPostureTag(
                            label = "SELinux Policy",
                            value = report?.selinuxMode ?: "Enforcing",
                            isOk = true
                        )
                        SandboxPostureTag(
                            label = "Elevation Mode",
                            value = if (report?.isRooted == true) "Root (Elevated)" else "Standard Non-Root",
                            isOk = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SandboxPostureTag(
                            label = "Wi-Fi Throttle (API 28+)",
                            value = if (report?.isWifiThrottlingActive == true) "Active (4 scans/2min)" else "Disabled",
                            isOk = false
                        )
                        SandboxPostureTag(
                            label = "ARP Cache Sandbox",
                            value = if (report?.canAccessArpCache == true) "Direct Access" else "Restricted (API 29+)",
                            isOk = false
                        )
                    }
                }
            }
        }

        // Architecture Deep Dive Explanations
        item {
            Text(
                text = "NON-ROOT VS ELEVATED CAPABILITY MATRIX",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        report?.capabilities?.let { caps ->
            items(caps) { cap ->
                PlatformCapabilityCard(item = cap)
            }
        }

        // Technical Blueprint Note
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "VpnService Virtual TUN Interface Architecture:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "NetSentinel leverages standard BSD non-blocking sockets and VpnService virtual interfaces (fd) to capture and inspect IP datagrams in userland without violating Android platform sandboxing or requiring root privileges.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SandboxPostureTag(label: String, value: String, isOk: Boolean) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (isOk) CyberGreen else CyberAmber,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
    }
}

@Composable
fun PlatformCapabilityCard(item: PlatformCapabilityItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.capability,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (item.nonRootSupported) CyberGreen.copy(alpha = 0.15f) else CyberPurple.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (item.nonRootSupported) "USERLAND (NON-ROOT)" else "ROOT ELEVATED",
                        color = if (item.nonRootSupported) CyberGreen else CyberPurple,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Mechanism: ${item.mechanism}",
                style = MaterialTheme.typography.bodySmall,
                color = CyberCyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sandbox Policy: ${item.androidConstraint}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
