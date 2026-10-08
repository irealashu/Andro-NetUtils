package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.*
import com.example.ui.MainAuditViewModel
import com.example.ui.components.CyberBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PingUtilitiesScreen(
    viewModel: MainAuditViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pingType by viewModel.pingType.collectAsStateWithLifecycle()
    val pingTarget by viewModel.pingTarget.collectAsStateWithLifecycle()
    val pingPort by viewModel.pingPort.collectAsStateWithLifecycle()
    val pingCount by viewModel.pingCount.collectAsStateWithLifecycle()
    val pingPayloadBytes by viewModel.pingPayloadBytes.collectAsStateWithLifecycle()
    val pingIntervalMs by viewModel.pingIntervalMs.collectAsStateWithLifecycle()
    val pingTimeoutMs by viewModel.pingTimeoutMs.collectAsStateWithLifecycle()
    val pingTtl by viewModel.pingTtl.collectAsStateWithLifecycle()

    val isPinging by viewModel.isPinging.collectAsStateWithLifecycle()
    val pingPackets by viewModel.pingPackets.collectAsStateWithLifecycle()
    val pingSummary by viewModel.pingSummary.collectAsStateWithLifecycle()

    val subnetSweepHosts by viewModel.subnetSweepHosts.collectAsStateWithLifecycle()
    val isSubnetSweeping by viewModel.isSubnetSweeping.collectAsStateWithLifecycle()

    val benchmarkTargets by viewModel.benchmarkTargets.collectAsStateWithLifecycle()
    val isBenchmarking by viewModel.isBenchmarking.collectAsStateWithLifecycle()

    var showConfigPanel by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0: Ping Engine, 1: Subnet Sweep, 2: DNS Benchmarks
    val logListState = rememberLazyListState()

    // Auto scroll log to bottom on new packet
    LaunchedEffect(pingPackets.size) {
        if (pingPackets.isNotEmpty()) {
            logListState.animateScrollToItem(pingPackets.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hero Title & Description
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PING UTILITIES & LATENCY SUITE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "ICMP Echo, TCP SYN, HTTP Web Ping & Subnet Sweeper",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                CyberBadge(
                    text = if (isPinging) "PINGING..." else "READY",
                    color = if (isPinging) CyberAmber else CyberCyan
                )
            }
        }

        // Sub-Tab Switcher (0: Ping Engine, 1: Subnet Sweep, 2: Benchmarks)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Ping Engine", "Subnet Sweep", "DNS Benchmarks").forEachIndexed { idx, label ->
                val selected = activeTab == idx
                Button(
                    onClick = { activeTab = idx },
                    modifier = Modifier.weight(1f).height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) CyberCyan.copy(alpha = 0.2f) else DarkSurface,
                        contentColor = if (selected) CyberCyan else TextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selected) CyberCyan else DarkBorder
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        when (activeTab) {
            0 -> {
                // --- PING ENGINE TAB ---
                // Target Input & Quick Controls
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = pingTarget,
                                onValueChange = { viewModel.setPingTarget(it) },
                                label = { Text("Target IP / Hostname / URL") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ping_target_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedContainerColor = DarkSurfaceVariant,
                                    unfocusedContainerColor = DarkSurfaceVariant,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Button(
                                onClick = {
                                    if (isPinging) {
                                        viewModel.stopPing()
                                    } else {
                                        viewModel.startPing()
                                    }
                                },
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("start_ping_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPinging) CyberRed else CyberCyan,
                                    contentColor = if (isPinging) Color.White else DarkBg
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPinging) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isPinging) "STOP" else "PING", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Ping Type Selector Row (ICMP, TCP, HTTP)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PingType.values().filter { it != PingType.SUBNET_SWEEP }.forEach { type ->
                                val selected = pingType == type
                                OutlinedButton(
                                    onClick = { viewModel.setPingType(type) },
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) CyberCyan.copy(alpha = 0.15f) else Color.Transparent,
                                        contentColor = if (selected) CyberCyan else TextSecondary
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selected) CyberCyan else DarkBorder
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = when (type) {
                                            PingType.ICMP_ECHO -> "ICMP Echo"
                                            PingType.TCP_SYN -> "TCP Port"
                                            PingType.HTTP_WEB -> "HTTP Web"
                                            else -> "Ping"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }

                            // Toggle Config Panel Button
                            IconButton(
                                onClick = { showConfigPanel = !showConfigPanel },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Configure Parameters",
                                    tint = if (showConfigPanel) CyberCyan else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Presets Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Presets:", fontSize = 10.sp, color = TextMuted, modifier = Modifier.align(Alignment.CenterVertically))
                            listOf("8.8.8.8", "1.1.1.1", "google.com", "cloudflare.com", "192.168.1.1", "127.0.0.1").forEach { preset ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { viewModel.setPingTarget(preset) },
                                    color = DarkSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder)
                                ) {
                                    Text(
                                        text = preset,
                                        fontSize = 10.sp,
                                        color = CyberCyan,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Expandable Advanced Configurations
                        AnimatedVisibility(visible = showConfigPanel) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkBg.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("ADVANCED PING PARAMETERS", fontSize = 10.sp, color = CyberCyan, fontWeight = FontWeight.Bold)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Count
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Count (0=Continuous)", fontSize = 9.sp, color = TextMuted)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf(4, 10, 20, 0).forEach { cnt ->
                                                val sel = pingCount == cnt
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { viewModel.setPingCount(cnt) },
                                                    color = if (sel) CyberCyan.copy(alpha = 0.2f) else DarkSurface,
                                                    shape = RoundedCornerShape(4.dp),
                                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, if (sel) CyberCyan else DarkBorder)
                                                ) {
                                                    Text(
                                                        text = if (cnt == 0) "∞" else "$cnt",
                                                        fontSize = 10.sp,
                                                        color = if (sel) CyberCyan else TextSecondary,
                                                        modifier = Modifier.padding(vertical = 4.dp),
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Payload Bytes
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Payload Size (Bytes)", fontSize = 9.sp, color = TextMuted)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf(32, 64, 56, 1472).forEach { bytes ->
                                                val sel = pingPayloadBytes == bytes
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { viewModel.setPingPayloadBytes(bytes) },
                                                    color = if (sel) CyberCyan.copy(alpha = 0.2f) else DarkSurface,
                                                    shape = RoundedCornerShape(4.dp),
                                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, if (sel) CyberCyan else DarkBorder)
                                                ) {
                                                    Text(
                                                        text = "$bytes",
                                                        fontSize = 10.sp,
                                                        color = if (sel) CyberCyan else TextSecondary,
                                                        modifier = Modifier.padding(vertical = 4.dp),
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (pingType == PingType.TCP_SYN) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Target Port:", fontSize = 10.sp, color = TextMuted)
                                        listOf(80, 443, 22, 53, 3389).forEach { port ->
                                            val sel = pingPort == port
                                            Surface(
                                                modifier = Modifier.clickable { viewModel.setPingPort(port) },
                                                color = if (sel) CyberCyan.copy(alpha = 0.2f) else DarkSurface,
                                                shape = RoundedCornerShape(4.dp),
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, if (sel) CyberCyan else DarkBorder)
                                            ) {
                                                Text(
                                                    text = ":$port",
                                                    fontSize = 10.sp,
                                                    color = if (sel) CyberCyan else TextSecondary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Summary Stats Bar (Packets, Latency, Loss %, Jitter)
                pingSummary?.let { summary ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DarkSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatBox(title = "Sent / Recv", value = "${summary.packetsSent} / ${summary.packetsReceived}")
                            StatBox(
                                title = "Loss Rate",
                                value = "${summary.packetLossPercent.toInt()}%",
                                color = if (summary.packetLossPercent == 0f) CyberGreen else CyberRed
                            )
                            StatBox(title = "Avg RTT", value = "${summary.avgLatencyMs} ms", color = CyberCyan)
                            StatBox(title = "Min / Max", value = "${summary.minLatencyMs} / ${summary.maxLatencyMs} ms")
                            StatBox(title = "Jitter", value = "${summary.jitterMs} ms", color = CyberAmber)
                        }
                    }
                }

                // Live Latency Visual Graph
                if (pingPackets.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        color = DarkSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("LIVE RTT LATENCY WAVEFORM (MS)", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                Text("${pingPackets.lastOrNull()?.latencyMs ?: 0} ms", fontSize = 10.sp, color = CyberCyan, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LatencyCanvasChart(packets = pingPackets, modifier = Modifier.fillMaxSize())
                        }
                    }
                }

                // Terminal Packet Log Feed
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    color = DarkSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TERMINAL OUTPUT LOG", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Clear",
                                    fontSize = 10.sp,
                                    color = CyberAmber,
                                    modifier = Modifier.clickable { viewModel.clearPingLogs() }
                                )
                                Text(
                                    text = "Copy Log",
                                    fontSize = 10.sp,
                                    color = CyberCyan,
                                    modifier = Modifier.clickable {
                                        val text = pingPackets.joinToString("\n") { p ->
                                            if (p.isSuccess) "64 bytes from ${p.ipAddress ?: p.targetHost}: icmp_seq=${p.sequenceNumber} ttl=${p.ttl ?: 64} time=${p.latencyMs} ms"
                                            else "Request timeout for icmp_seq ${p.sequenceNumber}: ${p.errorMessage}"
                                        }
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Ping Logs", text))
                                        Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        if (pingPackets.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(DarkBg.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Ready to ping target host. Press 'PING' above.", fontSize = 12.sp, color = TextMuted)
                            }
                        } else {
                            LazyColumn(
                                state = logListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(DarkBg.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(pingPackets) { packet ->
                                    PingPacketRow(packet)
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // --- SUBNET SWEEP TAB ---
                SubnetSweepPanel(
                    hosts = subnetSweepHosts,
                    isSweeping = isSubnetSweeping,
                    onStartSweep = { prefix -> viewModel.startSubnetSweep(prefix) },
                    onStopSweep = { viewModel.stopSubnetSweep() }
                )
            }

            2 -> {
                // --- DNS / CDN BENCHMARKS TAB ---
                DnsBenchmarkPanel(
                    targets = benchmarkTargets,
                    isBenchmarking = isBenchmarking,
                    onRunBenchmark = { viewModel.runPublicBenchmarks() }
                )
            }
        }
    }
}

@Composable
fun PingPacketRow(packet: PingPacket) {
    val timeStr = remember(packet.timestamp) {
        SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(packet.timestamp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(if (packet.isSuccess) CyberGreen else CyberRed, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "[$timeStr] seq=${packet.sequenceNumber}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (packet.isSuccess) "${packet.bytes}B from ${packet.ipAddress ?: packet.targetHost}"
                else "Timeout: ${packet.errorMessage ?: "No response"}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = if (packet.isSuccess) TextPrimary else CyberRed
            )
        }

        if (packet.isSuccess) {
            Text(
                text = "${packet.latencyMs} ms",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = when {
                    packet.latencyMs < 30 -> CyberGreen
                    packet.latencyMs < 100 -> CyberCyan
                    packet.latencyMs < 200 -> CyberAmber
                    else -> CyberOrange
                }
            )
        } else {
            Text(text = "FAIL", fontSize = 10.sp, color = CyberRed, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LatencyCanvasChart(
    packets: List<PingPacket>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (packets.isEmpty()) return@Canvas

        val maxLatency = (packets.maxOfOrNull { it.latencyMs } ?: 100L).coerceAtLeast(50L).toFloat()
        val width = size.width
        val height = size.height
        val stepX = width / (packets.size - 1).coerceAtLeast(1)

        val path = Path()
        val points = mutableListOf<Offset>()

        packets.forEachIndexed { i, packet ->
            val x = i * stepX
            val yRatio = (packet.latencyMs.toFloat() / maxLatency).coerceIn(0f, 1f)
            val y = height - (yRatio * (height - 10f))
            points.add(Offset(x, y))

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // Draw line graph
        drawPath(
            path = path,
            color = CyberCyan,
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw data points
        points.forEachIndexed { idx, point ->
            val isOk = packets.getOrNull(idx)?.isSuccess == true
            drawCircle(
                color = if (isOk) CyberGreen else CyberRed,
                radius = 3.dp.toPx(),
                center = point
            )
        }
    }
}

@Composable
fun StatBox(
    title: String,
    value: String,
    color: Color = TextPrimary
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 9.sp, color = TextMuted)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun SubnetSweepPanel(
    hosts: List<SubnetPingHost>,
    isSweeping: Boolean,
    onStartSweep: (String) -> Unit,
    onStopSweep: () -> Unit
) {
    var subnetPrefix by remember { mutableStateOf("192.168.1") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSurface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("PARALLEL SUBNET ICMP SWEEPER", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberCyan)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = subnetPrefix,
                onValueChange = { subnetPrefix = it },
                label = { Text("Subnet Prefix (e.g. 192.168.1)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Button(
                onClick = {
                    if (isSweeping) onStopSweep() else onStartSweep(subnetPrefix)
                },
                modifier = Modifier.height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isSweeping) CyberRed else CyberCyan, contentColor = DarkBg)
            ) {
                Text(if (isSweeping) "STOP" else "SWEEP (1-254)", fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("DISCOVERED LIVE HOSTS: ${hosts.size}", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
            if (isSweeping) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = CyberCyan, strokeWidth = 2.dp)
            }
        }

        if (hosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBg, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSweeping) "Sweeping subnet range $subnetPrefix.1 - $subnetPrefix.254..." else "Press SWEEP to discover active hosts on local subnet.",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBg, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(hosts) { host ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(CyberGreen, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(host.ip, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                                if (host.hostname != null && host.hostname != host.ip) {
                                    Text(host.hostname, fontSize = 10.sp, color = TextMuted)
                                }
                            }
                        }

                        CyberBadge(text = "${host.latencyMs} ms", color = CyberCyan)
                    }
                }
            }
        }
    }
}

@Composable
fun DnsBenchmarkPanel(
    targets: List<PingBenchmarkTarget>,
    isBenchmarking: Boolean,
    onRunBenchmark: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSurface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("GLOBAL DNS & CDN LATENCY COMPARATOR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                Text("Measures response speed across public DNS resolvers & carriers", fontSize = 10.sp, color = TextMuted)
            }

            Button(
                onClick = onRunBenchmark,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkBg),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isBenchmarking) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = DarkBg, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("BENCHMARK ALL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (targets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBg, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Tap 'BENCHMARK ALL' to compare latencies against global DNS resolvers.", fontSize = 11.sp, color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBg, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(targets) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("(${item.address})", fontSize = 10.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            }
                            Text(item.category, fontSize = 9.sp, color = CyberAmber)
                        }

                        if (item.isReachable && item.latencyMs != null) {
                            Text(
                                text = "${item.latencyMs} ms",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = when {
                                    item.latencyMs < 25 -> CyberGreen
                                    item.latencyMs < 60 -> CyberCyan
                                    else -> CyberAmber
                                }
                            )
                        } else {
                            Text("UNREACHABLE", fontSize = 10.sp, color = CyberRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
