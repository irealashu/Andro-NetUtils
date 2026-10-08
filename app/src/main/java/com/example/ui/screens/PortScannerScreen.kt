package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.CommonPortPresets
import com.example.domain.model.DiscoveredHost
import com.example.domain.model.PortScanResult
import com.example.domain.model.RiskLevel
import com.example.ui.MainAuditViewModel
import com.example.ui.components.EmptyAuditState
import com.example.ui.components.LatencyPill
import com.example.ui.components.TerminalCodeBlock
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortScannerScreen(viewModel: MainAuditViewModel) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0: TCP Port Scan, 1: Subnet Sweep

    val target by viewModel.portScanTarget.collectAsStateWithLifecycle()
    val selectedPreset by viewModel.selectedPreset.collectAsStateWithLifecycle()
    val customPorts by viewModel.customPortsInput.collectAsStateWithLifecycle()
    val timeoutMs by viewModel.portScanTimeoutMs.collectAsStateWithLifecycle()
    val concurrency by viewModel.portScanConcurrency.collectAsStateWithLifecycle()
    val isScanning by viewModel.isPortScanning.collectAsStateWithLifecycle()
    val progress by viewModel.portScanProgress.collectAsStateWithLifecycle()
    val results by viewModel.portScanResults.collectAsStateWithLifecycle()

    val isSubnetScanning by viewModel.isSubnetScanning.collectAsStateWithLifecycle()
    val subnetProgress by viewModel.subnetProgress.collectAsStateWithLifecycle()
    val subnetHosts by viewModel.subnetHosts.collectAsStateWithLifecycle()

    var showOnlyOpen by remember { mutableStateOf(false) }
    var selectedPortResult by remember { mutableStateOf<PortScanResult?>(null) }
    var showAdvancedConfig by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mode Selector Tab (TCP Port Scan vs Subnet Sweep)
        item {
            SecondaryTabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = DarkSurface,
                contentColor = CyberCyan
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = {
                        Text(
                            "TCP PORT SCANNER",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = {
                        Text(
                            "SUBNET HOST SWEEP",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        if (selectedSubTab == 0) {
            // Target Input & Presets
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "TARGET HOST OR IP",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = target,
                            onValueChange = { viewModel.setPortScanTarget(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("port_scan_target_input"),
                            singleLine = true,
                            placeholder = { Text("e.g. scanme.nmap.org or 192.168.1.1", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            trailingIcon = {
                                if (target.isNotBlank()) {
                                    IconButton(onClick = { viewModel.setPortScanTarget("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "PORT PRESETS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(CommonPortPresets.ALL_PRESETS) { preset ->
                                val isSelected = preset.id == selectedPreset.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setSelectedPreset(preset) },
                                    label = { Text(preset.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                        selectedLabelColor = CyberCyan
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) CyberCyan else DarkBorder
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Advanced Config Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvancedConfig = !showAdvancedConfig },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Socket Timeouts & Concurrency Settings",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                            Icon(
                                imageVector = if (showAdvancedConfig) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(visible = showAdvancedConfig) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Text(
                                    text = "Socket Timeout: ${timeoutMs}ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Slider(
                                    value = timeoutMs.toFloat(),
                                    onValueChange = { viewModel.setPortScanTimeout(it.toInt()) },
                                    valueRange = 100f..1500f,
                                    steps = 14,
                                    colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                                )

                                Text(
                                    text = "Concurrent Sockets: $concurrency threads",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Slider(
                                    value = concurrency.toFloat(),
                                    onValueChange = { viewModel.setPortScanConcurrency(it.toInt()) },
                                    valueRange = 5f..50f,
                                    steps = 9,
                                    colors = SliderDefaults.colors(thumbColor = CyberGreen, activeTrackColor = CyberGreen)
                                )

                                OutlinedTextField(
                                    value = customPorts,
                                    onValueChange = { viewModel.setCustomPortsInput(it) },
                                    label = { Text("Custom Ports (comma separated)", color = TextMuted) },
                                    placeholder = { Text("e.g. 80, 443, 8080, 2222", color = TextMuted) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = DarkBorder
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Button
                        Button(
                            onClick = {
                                if (isScanning) viewModel.stopPortScan() else viewModel.startPortScan()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("start_port_scan_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScanning) CyberRed else CyberCyan,
                                contentColor = if (isScanning) Color.White else Color(0xFF00363A)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("STOP SCAN (${progress.first}/${progress.second})", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("LAUNCH TCP PORT AUDIT", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Results Header & Filter
            if (results.isNotEmpty() || isScanning) {
                item {
                    val openCount = results.count { it.isOpen }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PORT AUDIT RESULTS ($openCount OPEN / ${results.size} SCANNED)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Open Only",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                checked = showOnlyOpen,
                                onCheckedChange = { showOnlyOpen = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CyberGreen,
                                    checkedTrackColor = CyberGreen.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }

                val filteredResults = if (showOnlyOpen) results.filter { it.isOpen } else results
                items(filteredResults) { item ->
                    PortResultItemCard(
                        result = item,
                        onClick = { selectedPortResult = item }
                    )
                }
            } else if (!isScanning) {
                item {
                    EmptyAuditState(
                        icon = Icons.Default.TravelExplore,
                        title = "No Scan Performed",
                        subtitle = "Select a target host and port preset above to launch a real-time asynchronous TCP socket audit."
                    )
                }
            }
        } else {
            // Subnet Host Sweep Sub-Tab
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SUBNET ACTIVE HOST DISCOVERY",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Probes the local /24 subnet using ICMP and TCP echo fallbacks to detect online hosts, hostnames, and active services.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { viewModel.startSubnetDiscovery() },
                            enabled = !isSubnetScanning,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberGreen,
                                contentColor = Color(0xFF003915)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isSubnetScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SWEEPING (${subnetProgress.first}/${subnetProgress.second})", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Radar, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SCAN LOCAL SUBNET (/24)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (subnetHosts.isNotEmpty()) {
                item {
                    Text(
                        text = "DISCOVERED HOSTS (${subnetHosts.size} ONLINE)",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberGreen,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(subnetHosts) { host ->
                    DiscoveredHostCard(host = host, onScanHost = {
                        viewModel.setPortScanTarget(host.ipAddress)
                        selectedSubTab = 0
                        viewModel.startPortScan()
                    })
                }
            } else if (!isSubnetScanning) {
                item {
                    EmptyAuditState(
                        icon = Icons.Default.Hub,
                        title = "No Subnet Sweep Run",
                        subtitle = "Click 'SCAN LOCAL SUBNET' to discover alive devices connected to your LAN."
                    )
                }
            }
        }
    }

    // Detail Dialog for Banner & Port Information
    selectedPortResult?.let { result ->
        AlertDialog(
            onDismissRequest = { selectedPortResult = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (result.isOpen) CyberGreen else CyberRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Port ${result.port} (${result.serviceName})", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Status: ${if (result.isOpen) "OPEN / RESPONSIVE" else "CLOSED / FILTERED"}",
                        color = if (result.isOpen) CyberGreen else CyberRed,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text("Latency: ${result.latencyMs} ms", color = TextSecondary)
                    Text("Risk Level: ${result.riskLevel.name}", color = when (result.riskLevel) {
                        RiskLevel.CRITICAL, RiskLevel.HIGH -> CyberRed
                        RiskLevel.MEDIUM -> CyberAmber
                        else -> CyberGreen
                    }, fontWeight = FontWeight.Bold)

                    if (!result.banner.isNullOrBlank()) {
                        Text("Service Banner Captured:", fontWeight = FontWeight.Bold, color = TextPrimary)
                        TerminalCodeBlock(text = result.banner)
                    } else if (result.isOpen) {
                        Text("No immediate text banner greeting was returned on connect.", color = TextMuted)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPortResult = null }) {
                    Text("Close", color = CyberCyan)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun PortResultItemCard(
    result: PortScanResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (result.isOpen) DarkSurfaceVariant else DarkSurface
        ),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(
                    if (result.isOpen) CyberGreen.copy(alpha = 0.5f) else Color.Transparent,
                    Color.Transparent
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (result.isOpen) CyberGreen else TextMuted.copy(alpha = 0.4f))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${result.port} / ${result.transport}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (result.isOpen) TextPrimary else TextMuted,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        if (result.isOpen) {
                            Text(
                                text = "OPEN",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = result.serviceName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (result.isOpen) CyberCyan else TextMuted,
                        fontSize = 12.sp
                    )

                    if (!result.banner.isNullOrBlank()) {
                        Text(
                            text = "Banner: ${result.banner.take(45)}...",
                            style = MaterialTheme.typography.labelSmall,
                            color = TerminalText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (result.isOpen) {
                LatencyPill(latencyMs = result.latencyMs)
            } else {
                Text(
                    text = "CLOSED",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun DiscoveredHostCard(
    host: DiscoveredHost,
    onScanHost: () -> Unit
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = null,
                        tint = CyberGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = host.ipAddress,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = host.hostname ?: "Host Active",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                LatencyPill(latencyMs = host.responseTimeMs)
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onScanHost) {
                    Icon(
                        imageVector = Icons.Default.TravelExplore,
                        contentDescription = "Scan Ports",
                        tint = CyberCyan
                    )
                }
            }
        }
    }
}
