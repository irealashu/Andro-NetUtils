package com.example.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AppEndpoint
import com.example.domain.model.DissectedPacket
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.NetworkProtocol
import com.example.ui.MainAuditViewModel
import com.example.ui.components.EmptyAuditState
import com.example.ui.components.TerminalCodeBlock
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacketInspectorScreen(viewModel: MainAuditViewModel) {
    val context = LocalContext.current
    val isCapturing by viewModel.isPacketCapturing.collectAsStateWithLifecycle()
    val packets by viewModel.capturedPackets.collectAsStateWithLifecycle()
    val stats by viewModel.trafficStats.collectAsStateWithLifecycle()
    val selectedPacket by viewModel.selectedPacket.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val selectedTargetApp by viewModel.selectedTargetApp.collectAsStateWithLifecycle()
    val appEndpoints by viewModel.appEndpoints.collectAsStateWithLifecycle()

    var selectedViewSubTab by remember { mutableStateOf(0) } // 0: Live Stream, 1: App Endpoints, 2: VpnService Architecture
    var selectedProtocolFilter by remember { mutableStateOf<NetworkProtocol?>(null) }
    var showAppPickerDialog by remember { mutableStateOf(false) }
    var appSearchQuery by remember { mutableStateOf("") }
    var showOnlyUserApps by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App-Specific Isolation & Target Selector Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(
                            if (selectedTargetApp != null) CyberGreen.copy(alpha = 0.5f) else CyberCyan.copy(alpha = 0.4f),
                            DarkBorder
                        )
                    )
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FilterAlt,
                                contentDescription = null,
                                tint = if (selectedTargetApp != null) CyberGreen else CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedTargetApp != null) "APP-SPECIFIC CAPTURE ACTIVE" else "DEVICE-WIDE TRAFFIC CAPTURE",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedTargetApp != null) CyberGreen else CyberCyan,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.loadInstalledApps()
                                showAppPickerDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTargetApp != null) CyberGreen.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.15f),
                                contentColor = if (selectedTargetApp != null) CyberGreen else CyberCyan
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedTargetApp != null) "Change App" else "Select Target App",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (selectedTargetApp != null) {
                        val app = selectedTargetApp!!
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.horizontalGradient(listOf(CyberGreen.copy(alpha = 0.4f), Color.Transparent))
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CyberGreen.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = app.appName.take(2).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = CyberGreen,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = app.appName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${app.packageName} (UID ${app.uid})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CyberGreenGlow,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.setSelectedTargetApp(null) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Target App", tint = TextMuted)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Currently inspecting all traffic. Select a target app above to isolate only packets from that specific package (via VpnService addAllowedApplication).",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Live Capture Controller & Metric Gauges Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(if (isCapturing) CyberOrange.copy(alpha = 0.6f) else CyberCyan.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CAPTURE & PACKET DISSECTOR ENGINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberOrange,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isCapturing) "CAPTURE ENGINE ACTIVE" else "ENGINE IDLE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCapturing) CyberOrange else TextPrimary
                            )
                        }

                        IconButton(onClick = { viewModel.clearPackets() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PacketStatItem(label = "App Packets", value = stats.totalPackets.toString(), color = TextPrimary)
                        PacketStatItem(label = "Data Volume", value = "${stats.totalBytes / 1024} KB", color = CyberCyan)
                        PacketStatItem(label = "TLS / HTTPS", value = stats.tcpPackets.toString(), color = CyberGreen)
                        PacketStatItem(label = "DNS / UDP", value = (stats.udpPackets + stats.dnsPackets).toString(), color = CyberAmber)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.togglePacketCapture() },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("toggle_packet_capture_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCapturing) CyberRed else CyberOrange,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(if (isCapturing) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCapturing) "STOP CAPTURE" else if (selectedTargetApp != null) "CAPTURE ${selectedTargetApp!!.appName.uppercase()}" else "START CAPTURE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (packets.isNotEmpty()) {
                                    val pcapBytes = viewModel.exportPcapBytes()
                                    val appTag = selectedTargetApp?.appName?.replace(" ", "_") ?: "all_traffic"
                                    Toast.makeText(context, "Exported $appTag.pcap (${pcapBytes.size} bytes) ready for Wireshark", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Capture some packets first to export PCAP", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                            border = ButtonDefaults.outlinedButtonBorder().copy(brush = Brush.linearGradient(listOf(CyberCyan, DarkBorder)))
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("EXPORT PCAP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // View Sub-Tabs (Live Stream vs App Endpoints vs VPN Pipeline)
        item {
            SecondaryTabRow(
                selectedTabIndex = selectedViewSubTab,
                containerColor = DarkSurface,
                contentColor = CyberCyan
            ) {
                Tab(
                    selected = selectedViewSubTab == 0,
                    onClick = { selectedViewSubTab = 0 },
                    text = { Text("PACKET STREAM (${packets.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                )
                Tab(
                    selected = selectedViewSubTab == 1,
                    onClick = { selectedViewSubTab = 1 },
                    text = { Text("CONTACTED ENDPOINTS (${appEndpoints.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                )
                Tab(
                    selected = selectedViewSubTab == 2,
                    onClick = { selectedViewSubTab = 2 },
                    text = { Text("VPNSERVICE PIPELINE", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                )
            }
        }

        when (selectedViewSubTab) {
            0 -> {
                // Protocol Filter Chips
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedProtocolFilter == null,
                                onClick = { selectedProtocolFilter = null },
                                label = { Text("ALL (${packets.size})", fontSize = 11.sp) }
                            )
                        }
                        items(NetworkProtocol.values()) { proto ->
                            val count = packets.count { it.protocol == proto }
                            if (count > 0 || proto == NetworkProtocol.TCP || proto == NetworkProtocol.DNS || proto == NetworkProtocol.TLS) {
                                FilterChip(
                                    selected = selectedProtocolFilter == proto,
                                    onClick = { selectedProtocolFilter = if (selectedProtocolFilter == proto) null else proto },
                                    label = { Text("${proto.displayName} ($count)", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                // Selected Packet Detail View
                selectedPacket?.let { pkt ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.horizontalGradient(listOf(CyberCyan, Color.Transparent))
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "PACKET #${pkt.id} [${pkt.protocol.displayName}] • ${pkt.appName ?: "System"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyberCyan,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = { viewModel.selectPacket(null) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${pkt.sourceIp}:${pkt.sourcePort ?: ""} -> ${pkt.destinationIp}:${pkt.destinationPort ?: ""}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Package: ${pkt.packageName ?: "android"} • TTL=${pkt.ttl} • ${pkt.packetLengthBytes}B • ${pkt.flags ?: ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Payload Hex & ASCII Dissection:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Spacer(modifier = Modifier.height(4.dp))
                                TerminalCodeBlock(text = pkt.payloadPreviewHex, title = "RAW IP DATAGRAM BYTES")
                            }
                        }
                    }
                }

                val filteredPackets = if (selectedProtocolFilter != null) {
                    packets.filter { it.protocol == selectedProtocolFilter }
                } else {
                    packets
                }

                if (filteredPackets.isNotEmpty()) {
                    items(filteredPackets) { packet ->
                        PacketRowItem(
                            packet = packet,
                            isSelected = selectedPacket?.id == packet.id,
                            onClick = { viewModel.selectPacket(packet) }
                        )
                    }
                } else if (!isCapturing) {
                    item {
                        EmptyAuditState(
                            icon = Icons.Default.BugReport,
                            title = "No Packets Captured",
                            subtitle = if (selectedTargetApp != null) "Click START CAPTURE to stream isolated packets for ${selectedTargetApp!!.appName}." else "Click START CAPTURE to stream and dissect IP packets."
                        )
                    }
                }
            }

            1 -> {
                // Contacted Endpoints Discovered by App
                if (appEndpoints.isNotEmpty()) {
                    item {
                        Text(
                            text = "REMOTE SERVERS & APIS CONTACTED BY APP",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberGreen,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(appEndpoints) { endpoint ->
                        AppEndpointCard(endpoint = endpoint)
                    }
                } else {
                    item {
                        EmptyAuditState(
                            icon = Icons.Default.Hub,
                            title = "No Endpoints Discovered Yet",
                            subtitle = "Start packet capture to automatically discover and map remote servers, REST APIs, and DNS resolvers contacted by the target app."
                        )
                    }
                }
            }

            2 -> {
                // VpnService Architecture Explainer
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(14.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "HOW APP-SPECIFIC VPNSERVICE CAPTURE WORKS",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "1. Virtual TUN Interface:\nAndroid creates a virtual network interface (e.g. tun0) via VpnService.Builder.\n\n2. Per-App Routing (addAllowedApplication):\nBy calling builder.addAllowedApplication(\"com.example.package\"), the Android Linux kernel routes ONLY packets originating from that application's Linux UID through the virtual interface, leaving all other apps untouched on normal Wi-Fi/Cellular.\n\n3. Raw IP Datagram Loop:\nThe app reads raw IPv4/IPv6 packets from the ParcelFileDescriptor, parses IP/TCP/UDP headers in userland, and writes standard PCAP structures without needing root privileges.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            TerminalCodeBlock(
                                title = "VPNSERVICE KOTLIN IMPLEMENTATION",
                                text = "val builder = VpnService.Builder()\nbuilder.addAddress(\"10.0.0.2\", 32)\nbuilder.addRoute(\"0.0.0.0\", 0)\nbuilder.addAllowedApplication(\"${selectedTargetApp?.packageName ?: "com.target.app"}\")\nval vpnInterface = builder.establish()"
                            )
                        }
                    }
                }
            }
        }
    }

    // App Picker Dialog
    if (showAppPickerDialog) {
        AlertDialog(
            onDismissRequest = { showAppPickerDialog = false },
            title = {
                Text(
                    text = "Select Application to Capture",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        placeholder = { Text("Search installed apps...", color = TextMuted, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkBorder
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("User Apps Only", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Switch(
                            checked = showOnlyUserApps,
                            onCheckedChange = { showOnlyUserApps = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan)
                        )
                    }

                    val filteredApps = installedApps.filter { app ->
                        (!showOnlyUserApps || !app.isSystemApp) &&
                        (appSearchQuery.isBlank() ||
                         app.appName.contains(appSearchQuery, ignoreCase = true) ||
                         app.packageName.contains(appSearchQuery, ignoreCase = true))
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item {
                            Surface(
                                onClick = {
                                    viewModel.setSelectedTargetApp(null)
                                    showAppPickerDialog = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedTargetApp == null) CyberCyan.copy(alpha = 0.15f) else Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.DevicesOther, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("All Device Applications", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                        Text("Capture traffic across entire system", color = TextMuted, fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        items(filteredApps) { app ->
                            val isSelected = selectedTargetApp?.packageName == app.packageName
                            Surface(
                                onClick = {
                                    viewModel.setSelectedTargetApp(app)
                                    showAppPickerDialog = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CyberGreen.copy(alpha = 0.15f) else Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (app.isSystemApp) DarkSurfaceContainer else CyberGreen.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = app.appName.take(2).uppercase(),
                                            color = if (app.isSystemApp) TextMuted else CyberGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.appName,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = app.packageName,
                                            color = TextSecondary,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppPickerDialog = false }) {
                    Text("Close", color = CyberCyan)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun AppEndpointCard(endpoint: AppEndpoint) {
    val protoColor = when (endpoint.protocol) {
        NetworkProtocol.TLS -> CyberPurple
        NetworkProtocol.TCP -> CyberGreen
        NetworkProtocol.UDP -> CyberAmber
        NetworkProtocol.DNS -> CyberCyan
        else -> TextMuted
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(protoColor.copy(alpha = 0.15f))
                        .padding(vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = endpoint.protocol.displayName,
                        color = protoColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "${endpoint.hostOrIp}:${endpoint.port}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${endpoint.packetCount} packets • ${endpoint.bytesTransferred} bytes transferred",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CyberCyan.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "PORT ${endpoint.port}",
                    color = CyberCyan,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun PacketStatItem(label: String, value: String, color: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun PacketRowItem(
    packet: DissectedPacket,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val protoColor = when (packet.protocol) {
        NetworkProtocol.TLS -> CyberPurple
        NetworkProtocol.TCP -> CyberGreen
        NetworkProtocol.UDP -> CyberAmber
        NetworkProtocol.DNS -> CyberCyan
        NetworkProtocol.ICMP -> CyberRed
        NetworkProtocol.HTTP -> CyberGreenGlow
        else -> TextMuted
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) DarkSurfaceContainer else DarkSurface
        ),
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(if (isSelected) CyberCyan else Color.Transparent, Color.Transparent)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(protoColor.copy(alpha = 0.15f))
                        .padding(vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = packet.protocol.displayName,
                        color = protoColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${packet.sourceIp} -> ${packet.destinationIp}:${packet.destinationPort ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        if (!packet.appName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "[${packet.appName}]",
                                color = CyberGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = packet.summary,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1,
                        fontSize = 10.sp
                    )
                }
            }

            Text(
                text = "${packet.packetLengthBytes}B",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
    }
}
