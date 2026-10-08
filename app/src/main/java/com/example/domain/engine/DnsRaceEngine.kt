package com.example.domain.engine

import com.example.domain.model.DnsRaceEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class DnsRaceEngine {

    suspend fun runDnsBenchmark(testDomain: String = "google.com"): List<DnsRaceEntry> = withContext(Dispatchers.IO) {
        val resolvers = listOf(
            Triple("Cloudflare Primary", "1.1.1.1", "CF"),
            Triple("Google Public DNS", "8.8.8.8", "GG"),
            Triple("Quad9 Secure DNS", "9.9.9.9", "Q9"),
            Triple("OpenDNS Home", "208.67.222.222", "OD"),
            Triple("AdGuard Default", "94.140.14.14", "AG"),
            Triple("Local Gateway", "192.168.1.1", "LAN")
        )

        val deferreds = resolvers.map { (name, ip, tag) ->
            async {
                val start = System.currentTimeMillis()
                var reachable = false
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(ip, 53), 1200)
                        reachable = true
                    }
                } catch (_: Exception) {}

                val latency = (System.currentTimeMillis() - start).coerceAtLeast(2)
                DnsRaceEntry(
                    resolverName = name,
                    ipAddress = ip,
                    queryTimeMs = if (reachable) latency else 999,
                    isFastest = false,
                    isReachable = reachable,
                    providerTag = tag
                )
            }
        }

        val results = deferreds.awaitAll()
        val minLatency = results.filter { it.isReachable }.minOfOrNull { it.queryTimeMs } ?: 0L
        results.map { entry ->
            entry.copy(isFastest = entry.isReachable && entry.queryTimeMs == minLatency)
        }.sortedBy { if (it.isReachable) it.queryTimeMs else 9999 }
    }
}
