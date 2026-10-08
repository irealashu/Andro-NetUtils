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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.TracerouteHop
import com.example.ui.MainAuditViewModel
import com.example.ui.components.EmptyAuditState
import com.example.ui.components.LatencyPill
import com.example.ui.theme.*

@Composable
fun TracerouteScreen(viewModel: MainAuditViewModel) {
    val target by viewModel.tracerouteTarget.collectAsStateWithLifecycle()
    val isRunning by viewModel.isTracerouteRunning.collectAsStateWithLifecycle()
    val hops by viewModel.tracerouteHops.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Target Input Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "VISUAL TRACEROUTE & ROUTE LATENCY",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = target,
                        onValueChange = { viewModel.setTracerouteTarget(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("traceroute_target_input"),
                        label = { Text("Target Host or IP", color = TextMuted) },
                        placeholder = { Text("1.1.1.1 or google.com", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberBlue,
                            unfocusedBorderColor = DarkBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (isRunning) viewModel.stopTraceroute() else viewModel.startTraceroute()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_traceroute_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) CyberRed else CyberBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("TRACING HOPS... STOP", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Timeline, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("EXECUTE TRACEROUTE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (hops.isNotEmpty()) {
            item {
                Text(
                    text = "RESOLVED NETWORK HOPS (${hops.size} HOPS)",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberBlue,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            items(hops) { hop ->
                TracerouteHopCard(hop = hop)
            }
        } else if (!isRunning) {
            item {
                EmptyAuditState(
                    icon = Icons.Default.Timeline,
                    title = "No Traceroute Performed",
                    subtitle = "Enter a destination host or IP to trace network hops, gateway latency, and autonomous system numbers (ASNs)."
                )
            }
        }
    }
}

@Composable
fun TracerouteHopCard(hop: TracerouteHop) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(CyberBlue.copy(alpha = 0.4f), Color.Transparent))
        )
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
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CyberBlue.copy(alpha = 0.2f))
                        .border(1.dp, CyberBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${hop.hopNumber}",
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = hop.ipAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    if (!hop.hostname.isNullOrBlank() && hop.hostname != hop.ipAddress) {
                        Text(
                            text = hop.hostname,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                    if (!hop.asn.isNullOrBlank()) {
                        Text(
                            text = hop.asn,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                LatencyPill(latencyMs = hop.avgRttMs)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${hop.rtt1Ms} / ${hop.rtt2Ms} / ${hop.rtt3Ms} ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
