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
import com.example.domain.model.DnsRecord
import com.example.domain.model.DnsRecordType
import com.example.domain.model.DohProvider
import com.example.ui.MainAuditViewModel
import com.example.ui.components.EmptyAuditState
import com.example.ui.components.LatencyPill
import com.example.ui.theme.*

@Composable
fun DnsSuiteScreen(viewModel: MainAuditViewModel) {
    val domain by viewModel.dnsDomain.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedDnsRecordType.collectAsStateWithLifecycle()
    val selectedProvider by viewModel.selectedDohProvider.collectAsStateWithLifecycle()
    val isLoading by viewModel.isDnsLoading.collectAsStateWithLifecycle()
    val result by viewModel.dnsResult.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Query Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DNS-OVER-HTTPS & RECORD SUITE",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberAmber,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = domain,
                        onValueChange = { viewModel.setDnsDomain(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dns_domain_input"),
                        label = { Text("Domain Name", color = TextMuted) },
                        placeholder = { Text("e.g. cloudflare.com", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberAmber,
                            unfocusedBorderColor = DarkBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "RECORD TYPE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(DnsRecordType.values()) { type ->
                            val isSelected = type == selectedType
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setSelectedDnsRecordType(type) },
                                label = { Text(type.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberAmber.copy(alpha = 0.2f),
                                    selectedLabelColor = CyberAmber
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) CyberAmber else DarkBorder
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "DoH SECURE RESOLVER",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DohProvider.values()) { provider ->
                            val isSelected = provider == selectedProvider
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setSelectedDohProvider(provider) },
                                label = { Text(provider.providerName, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = CyberCyan
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.startDnsQuery() },
                        enabled = !isLoading && domain.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("run_dns_query_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberAmber,
                            contentColor = Color(0xFF332000)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RESOLVING ENCRYPTED DoH WIREFORMAT...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Dns, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("QUERY DNS RECORDS", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        val queryResult = result
        if (queryResult != null) {
            // Query Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(CyberAmber.copy(alpha = 0.5f), Color.Transparent))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DNS RESOLUTION SUMMARY",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberAmber,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${queryResult.domain} (${selectedType.name})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Via ${queryResult.providerUsed}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        LatencyPill(latencyMs = queryResult.queryTimeMs)
                    }
                }
            }

            // Email & DNS Security Hygiene Card (SPF / DMARC)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "EMAIL SPOOFING & DNS HYGIENE AUDIT",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DnsSecurityPill(
                                title = "SPF Record",
                                active = queryResult.hygiene.hasSpf,
                                modifier = Modifier.weight(1f)
                            )
                            DnsSecurityPill(
                                title = "DMARC Policy",
                                active = queryResult.hygiene.hasDmarc,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (!queryResult.hygiene.spfRecord.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("SPF: ${queryResult.hygiene.spfRecord}", style = MaterialTheme.typography.bodySmall, color = TerminalText, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }

                        if (!queryResult.hygiene.dmarcRecord.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("DMARC: ${queryResult.hygiene.dmarcRecord}", style = MaterialTheme.typography.bodySmall, color = TerminalText, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Resolved Records List
            item {
                Text(
                    text = "RESOLVED RECORDS (${queryResult.records.size} ANSWERS)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            if (queryResult.records.isNotEmpty()) {
                items(queryResult.records) { record ->
                    DnsRecordItemCard(record = record)
                }
            } else {
                item {
                    Text(
                        text = "No records of type ${selectedType.name} were returned for ${queryResult.domain}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else if (!isLoading) {
            item {
                EmptyAuditState(
                    icon = Icons.Default.Dns,
                    title = "No DNS Query Executed",
                    subtitle = "Select a record type and encrypted DoH provider above to resolve and inspect DNS records."
                )
            }
        }
    }
}

@Composable
fun DnsSecurityPill(title: String, active: Boolean, modifier: Modifier = Modifier) {
    val color = if (active) CyberGreen else CyberRed
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(
                if (active) "CONFIGURED" else "MISSING",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun DnsRecordItemCard(record: DnsRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberAmber.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = record.type.name,
                        color = CyberAmber,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "TTL: ${record.ttl}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = record.value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        }
    }
}
