package com.example.domain.engine

import com.example.domain.model.DiscoveredHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

class SubnetDiscoveryEngine {

    suspend fun discoverLocalSubnet(
        baseSubnet: String? = null,
        timeoutMs: Int = 300,
        maxConcurrency: Int = 30,
        onProgress: (scanned: Int, total: Int) -> Unit = { _, _ -> }
    ): List<DiscoveredHost> = withContext(Dispatchers.IO) {
        val subnetPrefix = baseSubnet ?: getLocalSubnetPrefix() ?: "192.168.1"
        val ipList = (1..254).map { "$subnetPrefix.$it" }
        val semaphore = Semaphore(maxConcurrency)
        val discovered = mutableListOf<DiscoveredHost>()
        var progressCount = 0

        val tasks = ipList.map { ip ->
            async {
                semaphore.withPermit {
                    val host = probeHost(ip, timeoutMs)
                    synchronized(discovered) {
                        progressCount++
                        onProgress(progressCount, ipList.size)
                        if (host != null && host.isReachable) {
                            discovered.add(host)
                        }
                    }
                    host
                }
            }
        }

        tasks.awaitAll()
        discovered.sortedBy { ipToLong(it.ipAddress) }
    }

    private fun probeHost(ip: String, timeoutMs: Int): DiscoveredHost? {
        val start = System.currentTimeMillis()
        try {
            val inet = InetAddress.getByName(ip)
            var reachable = false

            // Try ICMP / standard reachable
            try {
                reachable = inet.isReachable(timeoutMs)
            } catch (_: Exception) {}

            // Fallback: Probe common quick ports if ICMP was blocked by OS/Android sandbox
            val commonPorts = listOf(80, 443, 22, 53, 445, 8080)
            val openPorts = mutableListOf<Int>()

            for (port in commonPorts) {
                if (reachable && openPorts.isNotEmpty()) break
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(ip, port), 120)
                        reachable = true
                        openPorts.add(port)
                    }
                } catch (_: Exception) {}
            }

            if (reachable) {
                val latency = (System.currentTimeMillis() - start).coerceAtLeast(1)
                var hostname: String? = null
                try {
                    hostname = inet.canonicalHostName
                    if (hostname == ip) hostname = null
                } catch (_: Exception) {}

                return DiscoveredHost(
                    ipAddress = ip,
                    hostname = hostname,
                    isReachable = true,
                    responseTimeMs = latency,
                    openPortsSummary = openPorts
                )
            }
            return null
        } catch (_: Exception) {
            return null
        }
    }

    fun getLocalSubnetPrefix(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue

                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        val hostAddr = addr.hostAddress ?: continue
                        val parts = hostAddr.split(".")
                        if (parts.size == 4) {
                            return "${parts[0]}.${parts[1]}.${parts[2]}"
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun ipToLong(ip: String): Long {
        return try {
            val parts = ip.split(".").map { it.toLong() }
            (parts[0] shl 24) + (parts[1] shl 16) + (parts[2] shl 8) + parts[3]
        } catch (_: Exception) {
            0L
        }
    }
}
