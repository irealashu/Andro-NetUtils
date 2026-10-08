package com.example.domain.model

import java.io.Serializable

enum class DnsRecordType {
    A, AAAA, CNAME, MX, TXT, NS, SOA, PTR, SRV, CAA
}

data class DnsRecord(
    val type: DnsRecordType,
    val name: String,
    val value: String,
    val ttl: Int = 300,
    val priority: Int? = null,
    val additionalInfo: String? = null
) : Serializable

enum class DohProvider(
    val providerName: String,
    val dohEndpoint: String,
    val description: String,
    val iconTag: String
) {
    CLOUDFLARE("Cloudflare 1.1.1.1", "https://cloudflare-dns.com/dns-query", "Privacy-first resolver with 0-log policy", "CF"),
    GOOGLE("Google Public DNS", "https://dns.google/resolve", "High-capacity global anycast network", "GG"),
    QUAD9("Quad9 9.9.9.9", "https://dns.quad9.net/dns-query", "Built-in malware & threat intelligence blocking", "Q9"),
    ADGUARD("AdGuard DNS", "https://dns.adguard.com/resolve", "Ad-blocking & tracking protection resolver", "AG")
}

data class DnsSecurityHygiene(
    val hasSpf: Boolean,
    val spfRecord: String?,
    val hasDmarc: Boolean,
    val dmarcRecord: String?,
    val hasDnssec: Boolean = false,
    val hygieneScore: Int = 0,
    val recommendations: List<String> = emptyList()
) : Serializable

data class DnsQueryResult(
    val domain: String,
    val providerUsed: String,
    val queryTimeMs: Long,
    val records: List<DnsRecord>,
    val hygiene: DnsSecurityHygiene,
    val rawJson: String? = null,
    val serverIp: String? = null
) : Serializable
