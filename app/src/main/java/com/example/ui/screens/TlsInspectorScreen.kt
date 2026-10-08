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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.*
import com.example.ui.MainAuditViewModel
import com.example.ui.components.EmptyAuditState
import com.example.ui.components.LatencyPill
import com.example.ui.components.TerminalCodeBlock
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TlsInspectorScreen(viewModel: MainAuditViewModel) {
    val siteUrl by viewModel.siteInspectUrl.collectAsStateWithLifecycle()
    val isLoading by viewModel.isSiteInspectLoading.collectAsStateWithLifecycle()
    val result by viewModel.siteInspectResult.collectAsStateWithLifecycle()

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val categories = listOf("OVERVIEW", "HTTP & HEADERS", "TLS / CERTS", "DNS & EMAIL", "WHOIS & DOMAIN")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Target Configuration Card
        item {
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
                            text = "INSPECT SITE & TLS / HTTP / DNS AUDIT",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberPurple,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Enter URL & know everything",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = siteUrl,
                        onValueChange = { viewModel.setSiteInspectUrl(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inspect_site_url_input"),
                        label = { Text("Enter Target URL / Hostname", color = TextMuted) },
                        placeholder = { Text("https://example.com or google.com", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = CyberPurple) },
                        trailingIcon = {
                            if (siteUrl.isNotBlank()) {
                                IconButton(onClick = { viewModel.setSiteInspectUrl("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberPurple,
                            unfocusedBorderColor = DarkBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Quick Sample Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val samples = listOf("google.com", "cloudflare.com", "github.com", "wikipedia.org", "1.1.1.1")
                        items(samples) { sample ->
                            FilterChip(
                                selected = siteUrl.contains(sample),
                                onClick = {
                                    viewModel.setSiteInspectUrl("https://$sample")
                                    viewModel.startSiteInspection("https://$sample")
                                },
                                label = { Text(sample, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberPurple.copy(alpha = 0.2f),
                                    selectedLabelColor = CyberPurple,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = siteUrl.contains(sample),
                                    borderColor = DarkBorder,
                                    selectedBorderColor = CyberPurple
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.startSiteInspection() },
                        enabled = !isLoading && siteUrl.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_site_inspect_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberPurple,
                            contentColor = Color.White
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
                            Text("INSPECTING SITE, TLS, HTTP & DNS...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.TravelExplore, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("INSPECT SITE & DISSECT EVERYTHING", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        val inspect = result
        if (inspect != null) {
            // Hero Site Overview Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(CyberPurple.copy(alpha = 0.6f), Color.Transparent)
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = inspect.host,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "IP: ${inspect.ipAddress ?: "Resolved Domain"} • Port: ${inspect.port}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyberCyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            LatencyPill(latencyMs = inspect.pingLatencyMs)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Badges Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Grade Badge
                            inspect.httpAudit?.grade?.let { grade ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(grade.colorHex).copy(alpha = 0.15f))
                                        .border(1.dp, Color(grade.colorHex), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Grade ${grade.letter}",
                                        color = Color(grade.colorHex),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // HTTP Status Badge
                            inspect.httpAudit?.let { http ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (http.responseCode in 200..299) CyberGreen.copy(alpha = 0.15f) else CyberAmber.copy(alpha = 0.15f))
                                        .border(1.dp, if (http.responseCode in 200..299) CyberGreen else CyberAmber, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "HTTP ${http.responseCode}",
                                        color = if (http.responseCode in 200..299) CyberGreen else CyberAmber,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // TLS Status Badge
                            inspect.tlsAudit?.let { tls ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (tls.isTrusted) CyberPurple.copy(alpha = 0.15f) else CyberAmber.copy(alpha = 0.15f))
                                        .border(1.dp, if (tls.isTrusted) CyberPurple else CyberAmber, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (tls.negotiatedProtocol.isNotBlank()) tls.negotiatedProtocol else "TLS Active",
                                        color = if (tls.isTrusted) CyberPurple else CyberAmber,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Category Filter Tab Row
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories.size) { idx ->
                        val isSelected = selectedCategoryIndex == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyberPurple else DarkSurface)
                                .border(1.dp, if (isSelected) CyberPurple else DarkBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedCategoryIndex = idx }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = categories[idx],
                                color = if (isSelected) Color.White else TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Category Specific Sections
            when (selectedCategoryIndex) {
                0 -> {
                    // ALL OVERVIEW SUMMARY
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // HTTP Security Summary
                            inspect.httpAudit?.let { http ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                    shape = RoundedCornerShape(12.dp),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("HTTP SECURITY & SERVER BANNER", style = MaterialTheme.typography.labelSmall, color = CyberGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("• Server: ${http.serverBanner ?: "Protected / CDN"}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                        Text("• Score: ${http.totalScore}/${http.maxPossibleScore} (${http.headerChecks.count { it.status == HeaderStatus.SECURE }} Passed Security Checks)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }
                            }

                            // TLS Cert Summary
                            inspect.tlsAudit?.let { tls ->
                                val cert = tls.certificates.firstOrNull()
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                    shape = RoundedCornerShape(12.dp),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("TLS / SSL CERTIFICATE SUMMARY", style = MaterialTheme.typography.labelSmall, color = CyberPurple, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("• Protocol: ${tls.negotiatedProtocol}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                        Text("• Cipher: ${tls.negotiatedCipherSuite}", style = MaterialTheme.typography.bodySmall, color = CyberCyan, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                        if (cert != null) {
                                            Text("• Certificate Expiry: ${cert.daysUntilExpiration} days remaining (${cert.notAfter})", style = MaterialTheme.typography.bodySmall, color = if (cert.daysUntilExpiration > 30) CyberGreen else CyberAmber)
                                            Text("• Subject DN: ${cert.subjectDn}", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            // WHOIS & ASN Summary
                            inspect.whoisAudit?.let { whois ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                    shape = RoundedCornerShape(12.dp),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("DOMAIN & ASN INTELLIGENCE", style = MaterialTheme.typography.labelSmall, color = CyberAmber, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("• Registrar: ${whois.registrar}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                        Text("• ASN Network: ${whois.asnNumber} (${whois.asnOrganization})", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        Text("• Domain Dates: Created ${whois.creationDate} • Expires ${whois.expirationDate}", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // HTTP & SECURITY HEADERS TAB
                    item {
                        val http = inspect.httpAudit
                        if (http != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("HTTP RESPONSE HEADERS AUDIT (${http.headerChecks.size} CHECKS)", style = MaterialTheme.typography.labelSmall, color = CyberGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    http.headerChecks.forEach { check ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(check.headerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
                                                Text(check.title, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
                                                if (check.rawValue != null) {
                                                    Text(check.rawValue, style = MaterialTheme.typography.bodySmall, color = CyberCyan, fontFamily = FontFamily.Monospace, fontSize = 10.sp, maxLines = 1)
                                                }
                                            }

                                            val statusColor = when (check.status) {
                                                HeaderStatus.SECURE -> CyberGreen
                                                HeaderStatus.WARNING -> CyberAmber
                                                HeaderStatus.MISSING -> CyberOrange
                                                HeaderStatus.VULNERABLE -> CyberRed
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(statusColor.copy(alpha = 0.15f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = check.status.name,
                                                    color = statusColor,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                                    }
                                }
                            }
                        } else {
                            Text("No HTTP Audit Data available", color = TextMuted)
                        }
                    }
                }

                2 -> {
                    // TLS / CERTS TAB
                    item {
                        val tls = inspect.tlsAudit
                        if (tls != null && tls.isTlsSupported) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                    shape = RoundedCornerShape(12.dp),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("TLS HANDSHAKE & CIPHER PARAMETERS", style = MaterialTheme.typography.labelSmall, color = CyberPurple, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Negotiated Protocol: ${tls.negotiatedProtocol}", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        Text("Cipher Suite: ${tls.negotiatedCipherSuite}", style = MaterialTheme.typography.bodySmall, color = CyberCyan, fontFamily = FontFamily.Monospace)
                                        Text("Handshake Latency: ${tls.handshakeLatencyMs} ms", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                    }
                                }

                                Text("CERTIFICATE CHAIN (${tls.certificates.size})", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

                                tls.certificates.forEach { cert ->
                                    CertificateDetailCard(cert = cert)
                                }
                            }
                        } else {
                            Text("No TLS Data available for target", color = TextMuted)
                        }
                    }
                }

                3 -> {
                    // DNS & EMAIL TAB
                    item {
                        val dns = inspect.dnsAudit
                        if (dns != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("DNS RECORDS & EMAIL SECURITY", style = MaterialTheme.typography.labelSmall, color = CyberCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text("• SPF Record: ${if (dns.hygiene.hasSpf) dns.hygiene.spfRecord ?: "Configured" else "Missing"}", style = MaterialTheme.typography.bodySmall, color = if (dns.hygiene.hasSpf) CyberGreen else CyberRed)
                                    Text("• DMARC Record: ${if (dns.hygiene.hasDmarc) dns.hygiene.dmarcRecord ?: "Configured" else "Missing"}", style = MaterialTheme.typography.bodySmall, color = if (dns.hygiene.hasDmarc) CyberGreen else CyberAmber)
                                    Text("• DNSSEC: ${if (dns.hygiene.hasDnssec) "Delegation Signed" else "Unsigned"}", style = MaterialTheme.typography.bodySmall, color = TextMuted)

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = DarkBorder)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text("RESOLVED DNS RECORDS (${dns.records.size})", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    dns.records.forEach { record ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(record.type.name, style = MaterialTheme.typography.labelSmall, color = CyberCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            Text(record.value, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        } else {
                            Text("No DNS Data available", color = TextMuted)
                        }
                    }
                }

                4 -> {
                    // WHOIS & DOMAIN TAB
                    item {
                        val whois = inspect.whoisAudit
                        if (whois != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("WHOIS & NETWORK INTELLIGENCE", style = MaterialTheme.typography.labelSmall, color = CyberAmber, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text("Registrar: ${whois.registrar}", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                                    Text("ASN: ${whois.asnNumber}", style = MaterialTheme.typography.bodySmall, color = CyberCyan, fontFamily = FontFamily.Monospace)
                                    Text("Organization: ${whois.asnOrganization}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Text("IP Range: ${whois.ipRange}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                    Text("Status: ${whois.status}", style = MaterialTheme.typography.bodySmall, color = TextMuted)

                                    if (inspect.openWebPorts.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Open Web Ports: ${inspect.openWebPorts.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = CyberGreen, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        } else {
                            Text("No WHOIS Data available", color = TextMuted)
                        }
                    }
                }
            }
        } else if (!isLoading) {
            item {
                EmptyAuditState(
                    icon = Icons.Default.TravelExplore,
                    title = "Inspect Any Site or URL",
                    subtitle = "Enter any domain or URL above (e.g. google.com, cloudflare.com, https://github.com) to perform a full inspection including TLS/SSL certificates, HTTP headers, DNS records, and WHOIS/ASN intelligence."
                )
            }
        }
    }
}

@Composable
fun CertificateDetailCard(cert: CertificateDetail) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (cert.isSelfSigned) "Self-Signed Certificate" else cert.subjectDn.split(",").firstOrNull() ?: cert.subjectDn,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Issuer: ${cert.issuerDn.split(",").firstOrNull() ?: cert.issuerDn}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val daysColor = when {
                        cert.isExpired -> CyberRed
                        cert.daysUntilExpiration < 30 -> CyberAmber
                        else -> CyberGreen
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(daysColor.copy(alpha = 0.15f))
                            .border(1.dp, daysColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (cert.isExpired) "EXPIRED" else "${cert.daysUntilExpiration}d left",
                            color = daysColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            val pem = cert.pemEncoded.ifBlank {
                                "-----BEGIN CERTIFICATE-----\nSubject: ${cert.subjectDn}\nIssuer: ${cert.issuerDn}\nSerial: ${cert.serialNumber}\n-----END CERTIFICATE-----"
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("X.509 Certificate PEM", pem))
                            val domainName = cert.subjectDn.split("CN=").getOrNull(1)?.split(",")?.firstOrNull() ?: "cert"
                            Toast.makeText(context, "Certificate downloaded & copied ($domainName.crt)", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Certificate .crt",
                            tint = CyberPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Key: ${cert.publicKeyAlg} (${cert.keySizeBits}-bit)",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Sig: ${cert.sigAlgName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Validity Range:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("• Issued: ${cert.notBefore}", style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontSize = 11.sp)
                    Text("• Expires: ${cert.notAfter}", style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Subject Alternative Names (SANs):", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    if (cert.subjectAlternativeNames.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            items(cert.subjectAlternativeNames) { san ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(DarkSurfaceContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(san, color = CyberCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    } else {
                        Text("No SANs listed", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("SHA-256 Fingerprint:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(cert.sha256Fingerprint, style = MaterialTheme.typography.bodySmall, color = TerminalText, fontFamily = FontFamily.Monospace, fontSize = 10.sp)

                    if (cert.pemEncoded.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("PEM ENCODED X.509 CERTIFICATE:", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "Copy PEM",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberPurple,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("X.509 Certificate PEM", cert.pemEncoded))
                                    Toast.makeText(context, "Copied PEM to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        TerminalCodeBlock(text = cert.pemEncoded)
                    }
                }
            }
        }
    }
}
