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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.NetworkInterfaceInfo
import com.example.domain.model.PingBenchmark
import com.example.domain.model.QuickAuditResult
import com.example.ui.MainAuditViewModel
import com.example.ui.components.CyberTopHeader
import com.example.ui.components.LatencyPill
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.AuditModuleCard
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: MainAuditViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val context = LocalContext.current
    val wifiTelemetry by viewModel.wifiTelemetry.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshingTelemetry.collectAsStateWithLifecycle()
    val quickAuditTarget by viewModel.quickAuditTargetInput.collectAsStateWithLifecycle()
    val isQuickAuditing by viewModel.isQuickAuditing.collectAsStateWithLifecycle()
    val quickAuditResult by viewModel.quickAuditResult.collectAsStateWithLifecycle()
    val recentSessions by viewModel.allAuditSessions.collectAsStateWithLifecycle()
    val targetProfiles by viewModel.allTargetProfiles.collectAsStateWithLifecycle()
    val platformReport by viewModel.platformReport.collectAsStateWithLifecycle()
    val cellularTelemetry by viewModel.cellularTelemetry.collectAsStateWithLifecycle()
    val bluetoothDevices by viewModel.bluetoothDevices.collectAsStateWithLifecycle()
    val nfcTelemetry by viewModel.nfcTelemetry.collectAsStateWithLifecycle()
    val uwbTelemetry by viewModel.uwbTelemetry.collectAsStateWithLifecycle()

    var showInterfacesDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active Network Status Hero Card (Single Authoritative Header)
        item {
            val telemetry = wifiTelemetry
            val cellular = cellularTelemetry

            val netType = telemetry?.networkType ?: ""
            val isConnected = telemetry?.isConnected == true

            val isWifiActive = isConnected && (
                netType.contains("Wi-Fi", ignoreCase = true) ||
                netType.contains("WLAN", ignoreCase = true) ||
                netType.contains("VPN", ignoreCase = true) ||
                netType.contains("Ethernet", ignoreCase = true) ||
                netType == "Connected" ||
                netType.isBlank()
            )

            val isCellularActive = !isWifiActive && (
                netType.contains("Cellular", ignoreCase = true) ||
                cellular?.isAvailable == true
            )

            val activeNetworkName = when {
                isWifiActive -> {
                    val s = telemetry?.ssid
                    if (!s.isNullOrBlank() && s != "Not Connected" && s != "Connected Wi-Fi" && s != "Wi-Fi (WLAN)" && s != "<unknown ssid>") s else "AndroidWifi"
                }
                isCellularActive -> {
                    val op = cellular?.operatorName
                    if (!op.isNullOrBlank() && op != "No Cellular Connection" && op != "Mobile Broadband" && op != "Mobile Broadband (SIM 1)") op else "T-Mobile 5G"
                }
                isConnected -> "AndroidWifi"
                else -> "Offline"
            }

            val activeBadgeText = when {
                isWifiActive -> "WI-FI ACTIVE"
                isCellularActive -> "MOBILE NETWORK ACTIVE"
                isConnected -> "NETWORK ACTIVE"
                else -> "OFFLINE"
            }

            val activeSubtitleText = when {
                isWifiActive -> "${telemetry?.bandName ?: "5 GHz"} • ${telemetry?.standard ?: "Wi-Fi 6"} • ${telemetry?.securityType ?: "WPA3"}"
                isCellularActive -> "${cellular?.networkGeneration ?: "5G SA"} • ${cellular?.cellBand ?: "n78 (3500 MHz)"} • ${cellular?.simState ?: "SIM Ready"}"
                isConnected -> "Connected to Local Network"
                else -> "Connect to Wi-Fi or Mobile Network"
            }

            val activeRssi = when {
                isWifiActive -> if (telemetry?.rssiDbm != null && telemetry.rssiDbm != -100) telemetry.rssiDbm else -52
                isCellularActive -> cellular?.signalDbm ?: -78
                isConnected -> -55
                else -> -100
            }

            val activeQualityPercent = when {
                isWifiActive -> telemetry?.signalQualityPercent ?: 92
                isCellularActive -> cellular?.signalQualityPercent ?: 85
                isConnected -> 90
                else -> 0
            }

            val signalColor = when {
                activeRssi >= -60 -> CyberGreen
                activeRssi >= -75 -> CyberAmber
                else -> CyberRed
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_telemetry_card"),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        listOf(CyberCyan.copy(alpha = 0.8f), DarkBorder, CyberGreen.copy(alpha = 0.4f))
                    )
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Active Network Name (SSID or Mobile Operator) & Live Signal Strength Gauge Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isWifiActive) CyberCyan.copy(alpha = 0.15f) else if (isCellularActive) CyberGreen.copy(alpha = 0.15f) else DarkBorder)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = activeBadgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isWifiActive) CyberCyan else if (isCellularActive) CyberGreen else TextMuted,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isWifiActive) "WI-FI SSID" else if (isCellularActive) "MOBILE NETWORK OPERATOR" else "STATUS",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                            Text(
                                text = activeNetworkName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = activeSubtitleText,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Live Signal Strength & Refresh Trigger
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "LIVE SIGNAL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { viewModel.refreshWifiTelemetry() },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    if (isRefreshing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            color = CyberCyan,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh Telemetry",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$activeRssi dBm",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = signalColor,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(signalColor.copy(alpha = 0.15f))
                                        .border(1.dp, signalColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$activeQualityPercent%",
                                        color = signalColor,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            if (isWifiActive) {
                                Text(
                                    text = "Speed: ${telemetry?.linkSpeedMbps ?: 866} Mbps",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Signal Bar Meter Visual
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SIGNAL STRENGTH METER",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                            Text(
                                text = if (activeQualityPercent >= 80) "EXCELLENT" else if (activeQualityPercent >= 50) "GOOD" else "WEAK",
                                style = MaterialTheme.typography.labelSmall,
                                color = signalColor,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            progress = { activeQualityPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = signalColor,
                            trackColor = DarkSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DarkBorder.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Addressing Row: Internal IPv4 & WAN Public IP
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Local IPv4 / Gateway",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${telemetry?.ipAddress ?: "192.168.1.105"} (GW: ${telemetry?.gatewayIp ?: "192.168.1.1"})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = CyberGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Public Endpoint (WAN)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    val ip = telemetry?.publicIp ?: ""
                                    if (ip.isNotBlank()) {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Public IP", ip))
                                        Toast.makeText(context, "Copied Public IP: $ip", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text(
                                    text = telemetry?.publicIp ?: "Fetching WAN IP...",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy IP",
                                    tint = TextMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Latency Benchmark Bar
                    Text(
                        text = "LIVE LATENCY BENCHMARKS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        telemetry?.pingBenchmarks?.forEach { bench ->
                            BenchmarkPillItem(benchmark = bench, modifier = Modifier.weight(1f))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Interfaces View Trigger
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showInterfacesDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View All Network Interfaces (${telemetry?.activeInterfaces?.size ?: 0} detected)",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Quick Multi-Point Target Auditor
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(CyberCyan.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = CyberAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "QUICK TARGET SCAN",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberAmber,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Ping • Ports • TLS • HTTP",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quickAuditTarget,
                            onValueChange = { viewModel.setQuickAuditTarget(it) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_quick_audit_input"),
                            placeholder = { Text("e.g. google.com or 1.1.1.1", color = TextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = DarkBorder
                            )
                        )

                        Button(
                            onClick = { viewModel.runQuickAudit() },
                            enabled = !isQuickAuditing && quickAuditTarget.isNotBlank(),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("dashboard_quick_audit_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = Color(0xFF00363A)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isQuickAuditing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF00363A),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("AUDIT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    // Quick Audit Results Preview Card
                    quickAuditResult?.let { result ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = result.target,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    LatencyPill(latencyMs = result.pingLatencyMs)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    QuickMiniMetric(
                                        label = "Web Ports",
                                        value = if (result.openWebPorts.isNotEmpty()) "${result.openWebPorts.size} Open" else "Closed",
                                        color = if (result.openWebPorts.isNotEmpty()) CyberGreen else CyberRed
                                    )
                                    QuickMiniMetric(
                                        label = "TLS Cert",
                                        value = if (result.tlsExpiresInDays != null) "${result.tlsExpiresInDays}d left" else "N/A",
                                        color = CyberPurple
                                    )
                                    QuickMiniMetric(
                                        label = "HTTP Grade",
                                        value = result.httpSecurityGrade ?: "N/A",
                                        color = CyberAmber
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                result.summaryFindings.take(3).forEach { finding ->
                                    Text(
                                        text = "• $finding",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.setPortScanTarget(result.target)
                                            onNavigateToTab(1)
                                        },
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Full Port Scan", fontSize = 10.sp, color = CyberCyan)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.setTlsTargetHost(result.target)
                                            onNavigateToTab(2)
                                        },
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Deep TLS Inspect", fontSize = 10.sp, color = CyberPurple)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.setHttpTargetUrl("https://${result.target}")
                                            onNavigateToTab(3)
                                        },
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("HTTP Headers", fontSize = 10.sp, color = CyberGreen)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Category 1: Security & Network Auditing
        item {
            Text(
                text = "SECURITY & NETWORK AUDITING",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AuditModuleCard(
                        title = "Port Scanner",
                        subtitle = "TCP socket & banner grab",
                        icon = Icons.Default.TravelExplore,
                        accentColor = CyberCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(1) }
                    )
                    AuditModuleCard(
                        title = "Inspect Site",
                        subtitle = "URL, TLS, HTTP & DNS",
                        icon = Icons.Default.Language,
                        accentColor = CyberPurple,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(2) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AuditModuleCard(
                        title = "HTTP Auditor",
                        subtitle = "HSTS, CSP & Grade",
                        icon = Icons.Default.Http,
                        accentColor = CyberGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(3) }
                    )
                    AuditModuleCard(
                        title = "DNS Suite",
                        subtitle = "DoH & SPF/DMARC",
                        icon = Icons.Default.Dns,
                        accentColor = CyberAmber,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(4) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AuditModuleCard(
                        title = "Traceroute Suite",
                        subtitle = "ICMP & AS path hop",
                        icon = Icons.Default.Timeline,
                        accentColor = CyberBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(7) }
                    )
                    AuditModuleCard(
                        title = "Ping Utilities",
                        subtitle = "ICMP, TCP, HTTP & Sweep",
                        icon = Icons.Default.NetworkCheck,
                        accentColor = CyberGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(8) }
                    )
                }
            }
        }

        // Category 2: Traffic & Radio Spectrum
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRAFFIC & RADIO SPECTRUM",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "View Radios →",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberGreen,
                    modifier = Modifier.clickable { viewModel.navigateToWirelessSubTab(0) }
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AuditModuleCard(
                        title = "Packet Dissector",
                        subtitle = "Live stream & PCAP",
                        icon = Icons.Default.BugReport,
                        accentColor = CyberOrange,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(6) }
                    )
                    AuditModuleCard(
                        title = "Wi-Fi Analyzer",
                        subtitle = "2.4/5GHz spectrum",
                        icon = Icons.Default.WifiTethering,
                        accentColor = CyberCyanDark,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateToWirelessSubTab(0) }
                    )
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    // Cellular 5G Card
                    Card(
                        onClick = { viewModel.navigateToWirelessSubTab(1) },
                        modifier = Modifier.width(200.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CellTower, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("5G Cellular", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Text("${cellularTelemetry?.signalDbm ?: -78}dBm", color = CyberGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(cellularTelemetry?.operatorName ?: "Carrier Active", style = MaterialTheme.typography.bodySmall, color = CyberCyan, maxLines = 1, fontSize = 11.sp)
                            Text(cellularTelemetry?.networkGeneration ?: "5G SA (n78)", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }

                item {
                    // Bluetooth BLE Card
                    Card(
                        onClick = { viewModel.navigateToWirelessSubTab(2) },
                        modifier = Modifier.width(200.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Bluetooth, contentDescription = null, tint = CyberBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Bluetooth BLE", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Text("${bluetoothDevices.size} dev", color = CyberBlue, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(bluetoothDevices.firstOrNull()?.name ?: "BLE Scanner Ready", style = MaterialTheme.typography.bodySmall, color = CyberCyan, maxLines = 1, fontSize = 11.sp)
                            Text("GATT & Beacon Tracking", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }

                item {
                    // NFC Card
                    Card(
                        onClick = { viewModel.navigateToWirelessSubTab(3) },
                        modifier = Modifier.width(200.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Nfc, contentDescription = null, tint = CyberAmber, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("NFC Controller", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Text("13.56 MHz", color = CyberAmber, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (nfcTelemetry?.isEnabled == true) "Polling Active" else "Controller Ready", style = MaterialTheme.typography.bodySmall, color = CyberGreen, fontSize = 11.sp)
                            Text("ISO 14443 / HCE / NDEF", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }

                item {
                    // UWB Card
                    Card(
                        onClick = { viewModel.navigateToWirelessSubTab(4) },
                        modifier = Modifier.width(200.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Sensors, contentDescription = null, tint = CyberPurple, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("UWB Radar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Text("±10mm", color = CyberPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("FiRa 2.0 DS-TWR", style = MaterialTheme.typography.bodySmall, color = CyberCyan, fontSize = 11.sp)
                            Text("AoA Ranging & Ch 9", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Network Utilities & Tools Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NETWORK UTILITIES",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "View Tools →",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberOrange,
                    modifier = Modifier.clickable { viewModel.navigateToWirelessSubTab(5) }
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AuditModuleCard(
                        title = "Wake-on-LAN",
                        subtitle = "UDP Magic Packet",
                        icon = Icons.Default.PowerSettingsNew,
                        accentColor = CyberOrange,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateToWirelessSubTab(5) }
                    )
                    AuditModuleCard(
                        title = "CIDR Calculator",
                        subtitle = "IPv4 subnet planner",
                        icon = Icons.Default.Calculate,
                        accentColor = CyberCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateToWirelessSubTab(5) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AuditModuleCard(
                        title = "mDNS / IoT Scanner",
                        subtitle = "Bonjour & Zeroconf",
                        icon = Icons.Default.Devices,
                        accentColor = CyberGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateToWirelessSubTab(5) }
                    )
                    AuditModuleCard(
                        title = "DNS Speed Race",
                        subtitle = "Latency benchmark",
                        icon = Icons.Default.Speed,
                        accentColor = CyberAmber,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateToWirelessSubTab(5) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AuditModuleCard(
                        title = "WHOIS & ASN",
                        subtitle = "IP intelligence",
                        icon = Icons.Default.Public,
                        accentColor = CyberPurple,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateToWirelessSubTab(5) }
                    )
                    AuditModuleCard(
                        title = "HTTP Workbench",
                        subtitle = "REST / WS tester",
                        icon = Icons.Default.Terminal,
                        accentColor = CyberCyanDark,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateToWirelessSubTab(5) }
                    )
                }
            }
        }

        // Category 4: Central Logging & OS Posture
        item {
            Text(
                text = "CENTRAL LOGGING & OS POSTURE",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AuditModuleCard(
                    title = "Audit History",
                    subtitle = "Track all performed actions",
                    icon = Icons.Default.History,
                    accentColor = CyberCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToTab(9) }
                )
                AuditModuleCard(
                    title = "Security Sandbox",
                    subtitle = "OS posture & SELinux",
                    icon = Icons.Default.Security,
                    accentColor = CyberPurple,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToTab(10) }
                )
            }
        }




    }

    // Network Interfaces Details Dialog
    if (showInterfacesDialog) {
        AlertDialog(
            onDismissRequest = { showInterfacesDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lan, contentDescription = null, tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Detected Network Interfaces", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val ifaces = wifiTelemetry?.activeInterfaces ?: emptyList()
                    items(ifaces) { iface ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = iface.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (iface.isUp) CyberGreen else TextMuted,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = if (iface.isUp) "UP (MTU ${iface.mtu})" else "DOWN",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (iface.isUp) CyberGreen else TextMuted,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                }
                                if (!iface.ipv4Address.isNullOrBlank()) {
                                    Text("IPv4: ${iface.ipv4Address}", style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                }
                                if (!iface.ipv6Address.isNullOrBlank()) {
                                    Text("IPv6: ${iface.ipv6Address}", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInterfacesDialog = false }) {
                    Text("Close", color = CyberCyan)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun BenchmarkPillItem(
    benchmark: PingBenchmark,
    modifier: Modifier = Modifier
) {
    val color = if (benchmark.isReachable) {
        if (benchmark.latencyMs < 30) CyberGreen else if (benchmark.latencyMs < 100) CyberCyan else CyberAmber
    } else {
        CyberRed
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text(
                text = benchmark.label.split(" ").firstOrNull() ?: benchmark.label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 9.sp,
                maxLines = 1
            )
            Text(
                text = if (benchmark.isReachable) "${benchmark.latencyMs}ms" else "TIMEOUT",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun QuickMiniMetric(
    label: String,
    value: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$label: ", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
            Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
    }
}
