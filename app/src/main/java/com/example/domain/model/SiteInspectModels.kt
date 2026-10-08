package com.example.domain.model

import java.io.Serializable

data class SiteInspectResult(
    val url: String,
    val host: String,
    val port: Int = 443,
    val ipAddress: String? = null,
    val pingLatencyMs: Long = 0,
    val httpAudit: HttpSecurityAudit? = null,
    val tlsAudit: TlsAuditResult? = null,
    val dnsAudit: DnsQueryResult? = null,
    val whoisAudit: WhoisResult? = null,
    val openWebPorts: List<Int> = emptyList(),
    val inspectTimestampMs: Long = System.currentTimeMillis()
) : Serializable
