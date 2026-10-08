package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.HeaderCheck
import com.example.domain.model.HeaderStatus
import com.example.domain.model.HttpSecurityAudit
import com.example.ui.MainAuditViewModel
import com.example.ui.components.EmptyAuditState
import com.example.ui.components.GradeBadgeView
import com.example.ui.components.LatencyPill
import com.example.ui.components.TerminalCodeBlock
import com.example.ui.theme.*

@Composable
fun HttpAuditorScreen(viewModel: MainAuditViewModel) {
    val targetUrl by viewModel.httpTargetUrl.collectAsStateWithLifecycle()
    val isAuditing by viewModel.isHttpAuditing.collectAsStateWithLifecycle()
    val auditResult by viewModel.httpAuditResult.collectAsStateWithLifecycle()

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
                        text = "HTTP DEFENSIVE SECURITY HEADER AUDITOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberGreen,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = targetUrl,
                        onValueChange = { viewModel.setHttpTargetUrl(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("http_url_input"),
                        label = { Text("Target URL (HTTP / HTTPS)", color = TextMuted) },
                        placeholder = { Text("https://github.com", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = DarkBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.startHttpAudit() },
                        enabled = !isAuditing && targetUrl.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_http_audit_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberGreen,
                            contentColor = Color(0xFF003915)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isAuditing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PROBING HEADERS & COOKIES...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Security, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RUN DEFENSIVE HEADER AUDIT", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        val audit = auditResult
        if (audit != null) {
            // Score & Grade Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(Color(audit.grade.colorHex).copy(alpha = 0.6f), Color.Transparent)
                        )
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
                                text = "SECURITY GRADE",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = audit.url,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "HTTP ${audit.responseCode} (${audit.httpVersion})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (audit.responseCode in 200..299) CyberGreen else CyberAmber,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                LatencyPill(latencyMs = audit.responseTimeMs)
                            }
                        }

                        GradeBadgeView(grade = audit.grade, score = audit.totalScore)
                    }
                }
            }

            // Recommendations List
            if (audit.summaryRecommendations.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "ACTIONABLE MITIGATIONS (${audit.summaryRecommendations.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberAmber,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            audit.summaryRecommendations.forEach { rec ->
                                Row(
                                    modifier = Modifier.padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("• ", color = CyberAmber, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = rec,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Individual Header Breakdown
            item {
                Text(
                    text = "DEFENSIVE HEADER ANALYSIS (${audit.headerChecks.size} CHECKED)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            items(audit.headerChecks) { check ->
                HeaderCheckItemCard(check = check)
            }

            // Cookies Audited Section
            if (audit.cookiesAudited.isNotEmpty()) {
                item {
                    Text(
                        text = "COOKIE SECURITY FLAGS (${audit.cookiesAudited.size} COOKIES)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(audit.cookiesAudited) { cookie ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = cookie.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CookieFlagBadge(name = "Secure", enabled = cookie.isSecure)
                                CookieFlagBadge(name = "HttpOnly", enabled = cookie.isHttpOnly)
                                CookieFlagBadge(name = "SameSite=${cookie.sameSite ?: "None"}", enabled = cookie.sameSite != null)
                            }
                        }
                    }
                }
            }
        } else if (!isAuditing) {
            item {
                EmptyAuditState(
                    icon = Icons.Default.Http,
                    title = "No Header Audit Run",
                    subtitle = "Enter an HTTP or HTTPS endpoint above to evaluate HSTS, Content-Security-Policy, anti-clickjacking, CORS, and cookie security flags."
                )
            }
        }
    }
}

@Composable
fun HeaderCheckItemCard(check: HeaderCheck) {
    var expanded by remember { mutableStateOf(false) }

    val (statusColor, statusText) = when (check.status) {
        HeaderStatus.SECURE -> CyberGreen to "SECURE"
        HeaderStatus.WARNING -> CyberAmber to "WARNING"
        HeaderStatus.MISSING -> CyberRed to "MISSING"
        HeaderStatus.VULNERABLE -> CyberRed to "VULNERABLE"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(statusColor.copy(alpha = 0.4f), Color.Transparent)
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = check.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = check.headerName,
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$statusText (${check.scoreContribution}/${check.maxScore})",
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = check.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 12.sp
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (!check.rawValue.isNullOrBlank()) {
                        Text("Current Raw Header Value:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        TerminalCodeBlock(text = check.rawValue)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text("Remediation / Hardening Configuration:", style = MaterialTheme.typography.labelSmall, color = CyberGreen)
                    TerminalCodeBlock(text = check.remediationExample, title = "NGINX CONFIG SNIPPET")
                }
            }
        }
    }
}

@Composable
fun CookieFlagBadge(name: String, enabled: Boolean) {
    val color = if (enabled) CyberGreen else CyberRed
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = name,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
    }
}
