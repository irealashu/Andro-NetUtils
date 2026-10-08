package com.example.domain.engine

import com.example.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class DnsSuiteEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun queryDoh(
        domain: String,
        recordType: DnsRecordType,
        provider: DohProvider = DohProvider.CLOUDFLARE
    ): DnsQueryResult = withContext(Dispatchers.IO) {
        val cleanDomain = domain.trim().removePrefix("https://").removePrefix("http://").removeSuffix("/")
        val startTime = System.currentTimeMillis()
        val records = mutableListOf<DnsRecord>()

        try {
            val typeNumber = getRecordTypeNumber(recordType)
            val urlBuilder = provider.dohEndpoint.toHttpUrlOrNull()?.newBuilder()
                ?: throw IllegalArgumentException("Invalid DoH Endpoint: ${provider.dohEndpoint}")

            urlBuilder.addQueryParameter("name", cleanDomain)
            urlBuilder.addQueryParameter("type", recordType.name)

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("Accept", "application/dns-json")
                .header("User-Agent", "NetSentinel-DnsPro/1.0")
                .build()

            var rawJsonString: String? = null
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                rawJsonString = body
                val json = JSONObject(body)

                if (json.has("Answer")) {
                    val answers = json.getJSONArray("Answer")
                    for (i in 0 until answers.length()) {
                        val obj = answers.getJSONObject(i)
                        val name = obj.optString("name", cleanDomain)
                        val typeCode = obj.optInt("type")
                        val ttl = obj.optInt("TTL", 300)
                        val data = obj.optString("data", "").trim('\"')
                        val resolvedType = mapTypeNumberToEnum(typeCode) ?: recordType

                        records.add(
                            DnsRecord(
                                type = resolvedType,
                                name = name,
                                value = data,
                                ttl = ttl
                            )
                        )
                    }
                }
            }

            // Also query TXT for SPF and _dmarc for hygiene analysis if inspecting domain
            val hygiene = auditHygiene(cleanDomain, provider)
            val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(1)

            DnsQueryResult(
                domain = cleanDomain,
                providerUsed = provider.providerName,
                queryTimeMs = elapsed,
                records = records,
                hygiene = hygiene,
                rawJson = rawJsonString
            )
        } catch (e: Exception) {
            // Fallback: System DNS for A/AAAA records if DoH failed
            val fallbackRecords = querySystemDns(cleanDomain)
            val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(1)

            DnsQueryResult(
                domain = cleanDomain,
                providerUsed = "${provider.providerName} (System Fallback: ${e.message})",
                queryTimeMs = elapsed,
                records = fallbackRecords,
                hygiene = DnsSecurityHygiene(
                    hasSpf = false,
                    spfRecord = null,
                    hasDmarc = false,
                    dmarcRecord = null,
                    hygieneScore = 20,
                    recommendations = listOf("DoH query failed, used on-device resolver fallback.")
                ),
                rawJson = null
            )
        }
    }

    private fun auditHygiene(domain: String, provider: DohProvider): DnsSecurityHygiene {
        var hasSpf = false
        var spfRecord: String? = null
        var hasDmarc = false
        var dmarcRecord: String? = null
        val recommendations = mutableListOf<String>()

        try {
            // Query TXT for domain
            val txtUrl = provider.dohEndpoint.toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("name", domain)
                ?.addQueryParameter("type", "TXT")
                ?.build()

            if (txtUrl != null) {
                val reqTxt = Request.Builder().url(txtUrl).header("Accept", "application/dns-json").build()
                client.newCall(reqTxt).execute().use { res ->
                    val body = res.body?.string() ?: ""
                    val json = JSONObject(body)
                    val answers = json.optJSONArray("Answer")
                    if (answers != null) {
                        for (i in 0 until answers.length()) {
                            val data = answers.getJSONObject(i).optString("data", "").trim('\"')
                            if (data.startsWith("v=spf1", ignoreCase = true)) {
                                hasSpf = true
                                spfRecord = data
                                break
                            }
                        }
                    }
                }
            }

            // Query DMARC for _dmarc.domain
            val dmarcUrl = provider.dohEndpoint.toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("name", "_dmarc.$domain")
                ?.addQueryParameter("type", "TXT")
                ?.build()

            if (dmarcUrl != null) {
                val reqDmarc = Request.Builder().url(dmarcUrl).header("Accept", "application/dns-json").build()
                client.newCall(reqDmarc).execute().use { res ->
                    val body = res.body?.string() ?: ""
                    val json = JSONObject(body)
                    val answers = json.optJSONArray("Answer")
                    if (answers != null) {
                        for (i in 0 until answers.length()) {
                            val data = answers.getJSONObject(i).optString("data", "").trim('\"')
                            if (data.startsWith("v=DMARC1", ignoreCase = true)) {
                                hasDmarc = true
                                dmarcRecord = data
                                break
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        var score = 0
        if (hasSpf) {
            score += 50
        } else {
            recommendations.add("Missing SPF (Sender Policy Framework) record. Domain may be susceptible to email spoofing.")
        }

        if (hasDmarc) {
            score += 50
        } else {
            recommendations.add("Missing DMARC record (_dmarc.$domain). Recommended: 'v=DMARC1; p=reject; rua=mailto:dmarc@$domain'.")
        }

        return DnsSecurityHygiene(
            hasSpf = hasSpf,
            spfRecord = spfRecord,
            hasDmarc = hasDmarc,
            dmarcRecord = dmarcRecord,
            hasDnssec = false,
            hygieneScore = score,
            recommendations = recommendations
        )
    }

    private fun querySystemDns(domain: String): List<DnsRecord> {
        val records = mutableListOf<DnsRecord>()
        try {
            val addresses = InetAddress.getAllByName(domain)
            for (addr in addresses) {
                val isV6 = addr.hostAddress?.contains(":") == true
                records.add(
                    DnsRecord(
                        type = if (isV6) DnsRecordType.AAAA else DnsRecordType.A,
                        name = domain,
                        value = addr.hostAddress ?: "",
                        ttl = 300
                    )
                )
            }
        } catch (_: Exception) {}
        return records
    }

    private fun getRecordTypeNumber(type: DnsRecordType): Int = when (type) {
        DnsRecordType.A -> 1
        DnsRecordType.NS -> 2
        DnsRecordType.CNAME -> 5
        DnsRecordType.SOA -> 6
        DnsRecordType.PTR -> 12
        DnsRecordType.MX -> 15
        DnsRecordType.TXT -> 16
        DnsRecordType.AAAA -> 28
        DnsRecordType.SRV -> 33
        DnsRecordType.CAA -> 257
    }

    private fun mapTypeNumberToEnum(code: Int): DnsRecordType? = when (code) {
        1 -> DnsRecordType.A
        2 -> DnsRecordType.NS
        5 -> DnsRecordType.CNAME
        6 -> DnsRecordType.SOA
        12 -> DnsRecordType.PTR
        15 -> DnsRecordType.MX
        16 -> DnsRecordType.TXT
        28 -> DnsRecordType.AAAA
        33 -> DnsRecordType.SRV
        257 -> DnsRecordType.CAA
        else -> null
    }
}
