package com.example.domain.engine

import com.example.domain.model.PingBenchmarkTarget
import com.example.domain.model.PingPacket
import com.example.domain.model.PingSummary
import com.example.domain.model.PingType
import com.example.domain.model.SubnetPingHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class PingEngine {

    /**
     * Executes a single ICMP echo request using system binary or fallback socket connection.
     */
    suspend fun executeIcmpPing(
        targetHost: String,
        seqNumber: Int,
        payloadBytes: Int = 64,
        timeoutMs: Int = 2000,
        ttl: Int = 64
    ): PingPacket = withContext(Dispatchers.IO) {
        val cleanHost = cleanTargetHost(targetHost)
        val startTime = System.currentTimeMillis()

        try {
            val timeoutSec = (timeoutMs / 1000).coerceAtLeast(1)
            // Execute native Linux ping binary
            val process = ProcessBuilder(
                "ping",
                "-c", "1",
                "-w", timeoutSec.toString(),
                "-s", payloadBytes.toString(),
                "-t", ttl.toString(),
                cleanHost
            ).redirectErrorStream(true).start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var capturedTimeMs: Long? = null
            var capturedTtl: Int? = null
            var capturedIp: String? = null
            var capturedBytes = payloadBytes

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                
                // Parse IP address: e.g. "64 bytes from 8.8.8.8: ..." or "PING google.com (142.250.190.46) ..."
                if (currentLine.contains("bytes from")) {
                    val ipMatch = Regex("""bytes from ([^\s:]+)""").find(currentLine)
                    if (ipMatch != null) {
                        capturedIp = ipMatch.groupValues[1]
                    }

                    val timeMatch = Regex("""time=([0-9.]+)\s*ms""").find(currentLine)
                    if (timeMatch != null) {
                        capturedTimeMs = timeMatch.groupValues[1].toDoubleOrNull()?.toLong()
                    }

                    val ttlMatch = Regex("""ttl=(\d+)""").find(currentLine)
                    if (ttlMatch != null) {
                        capturedTtl = ttlMatch.groupValues[1].toIntOrNull()
                    }

                    val bytesMatch = Regex("""(\d+)\s+bytes from""").find(currentLine)
                    if (bytesMatch != null) {
                        capturedBytes = bytesMatch.groupValues[1].toIntOrNull() ?: payloadBytes
                    }
                } else if (currentLine.contains("PING ") && capturedIp == null) {
                    val ipMatch = Regex("""\(([^)]+)\)""").find(currentLine)
                    if (ipMatch != null) {
                        capturedIp = ipMatch.groupValues[1]
                    }
                }
            }

            process.waitFor()

            if (capturedTimeMs != null) {
                return@withContext PingPacket(
                    sequenceNumber = seqNumber,
                    targetHost = cleanHost,
                    ipAddress = capturedIp ?: resolveIp(cleanHost),
                    bytes = capturedBytes,
                    ttl = capturedTtl ?: ttl,
                    latencyMs = capturedTimeMs,
                    isSuccess = true
                )
            }
        } catch (_: Exception) {
            // Native ping process failed or restricted; fallback to socket latency measurement
        }

        // Fallback: Java Socket connection / InetAddress reachability
        val resolvedIp = resolveIp(cleanHost) ?: cleanHost
        val isReachable = try {
            val inet = InetAddress.getByName(cleanHost)
            val socketStart = System.currentTimeMillis()
            val reachable = inet.isReachable(timeoutMs)
            val socketEnd = System.currentTimeMillis()
            val socketLatency = socketEnd - socketStart

            if (reachable) {
                return@withContext PingPacket(
                    sequenceNumber = seqNumber,
                    targetHost = cleanHost,
                    ipAddress = resolvedIp,
                    bytes = payloadBytes,
                    ttl = ttl,
                    latencyMs = socketLatency.coerceAtLeast(1),
                    isSuccess = true
                )
            }
            false
        } catch (_: Exception) {
            false
        }

        // Secondary fallback: TCP Port 80/443 handshake ping
        val tcpFallback = executeTcpPing(cleanHost, port = 80, seqNumber = seqNumber, timeoutMs = timeoutMs)
        if (tcpFallback.isSuccess) {
            return@withContext tcpFallback.copy(bytes = payloadBytes, ttl = ttl)
        }

        val elapsed = System.currentTimeMillis() - startTime
        PingPacket(
            sequenceNumber = seqNumber,
            targetHost = cleanHost,
            ipAddress = resolvedIp,
            bytes = payloadBytes,
            latencyMs = elapsed,
            isSuccess = false,
            errorMessage = "Request timed out ($timeoutMs ms)"
        )
    }

    /**
     * Executes a TCP SYN socket ping to a specific host and port.
     */
    suspend fun executeTcpPing(
        targetHost: String,
        port: Int,
        seqNumber: Int,
        timeoutMs: Int = 2000
    ): PingPacket = withContext(Dispatchers.IO) {
        val cleanHost = cleanTargetHost(targetHost)
        val startTime = System.currentTimeMillis()
        var socket: Socket? = null

        try {
            val resolvedIp = resolveIp(cleanHost) ?: cleanHost
            socket = Socket()
            val socketAddress = InetSocketAddress(cleanHost, port)

            val connectStart = System.currentTimeMillis()
            socket.connect(socketAddress, timeoutMs)
            val latency = System.currentTimeMillis() - connectStart

            PingPacket(
                sequenceNumber = seqNumber,
                targetHost = cleanHost,
                ipAddress = resolvedIp,
                bytes = 64,
                port = port,
                latencyMs = latency.coerceAtLeast(1),
                isSuccess = true
            )
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            val isHostAlive = e.message?.contains("refused", ignoreCase = true) == true
            val resolvedIp = resolveIp(cleanHost) ?: cleanHost

            PingPacket(
                sequenceNumber = seqNumber,
                targetHost = cleanHost,
                ipAddress = resolvedIp,
                port = port,
                bytes = 64,
                latencyMs = elapsed,
                isSuccess = isHostAlive, // Connection refused means host is alive and responded with RST!
                errorMessage = if (isHostAlive) "Port $port closed (RST received)" else (e.message ?: "Connection timed out")
            )
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Executes an HTTP/HTTPS Web Ping.
     */
    suspend fun executeHttpPing(
        targetUrl: String,
        seqNumber: Int,
        timeoutMs: Int = 3000
    ): PingPacket = withContext(Dispatchers.IO) {
        val formattedUrl = if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
            "https://$targetUrl"
        } else targetUrl

        val startTime = System.currentTimeMillis()
        var connection: HttpURLConnection? = null

        try {
            val url = URL(formattedUrl)
            val host = url.host
            val resolvedIp = resolveIp(host) ?: host

            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "NetSentinel-Ping/2.0")

            val connectStart = System.currentTimeMillis()
            connection.connect()
            val statusCode = connection.responseCode
            val latency = System.currentTimeMillis() - connectStart

            PingPacket(
                sequenceNumber = seqNumber,
                targetHost = host,
                ipAddress = resolvedIp,
                bytes = connection.contentLength.let { if (it > 0) it else 512 },
                latencyMs = latency.coerceAtLeast(1),
                httpStatusCode = statusCode,
                isSuccess = statusCode in 200..399
            )
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            PingPacket(
                sequenceNumber = seqNumber,
                targetHost = targetUrl,
                latencyMs = elapsed,
                isSuccess = false,
                errorMessage = e.message ?: "HTTP Connection failed"
            )
        } finally {
            try { connection?.disconnect() } catch (_: Exception) {}
        }
    }

    /**
     * Calculates statistics summary over a list of packet results.
     */
    fun calculateSummary(target: String, pingType: PingType, packets: List<PingPacket>): PingSummary {
        if (packets.isEmpty()) {
            return PingSummary(target = target, pingType = pingType)
        }

        val totalSent = packets.size
        val successful = packets.filter { it.isSuccess }
        val totalReceived = successful.size
        val lossPercent = ((totalSent - totalReceived).toFloat() / totalSent.toFloat()) * 100f

        if (successful.isEmpty()) {
            return PingSummary(
                target = target,
                pingType = pingType,
                packetsSent = totalSent,
                packetsReceived = 0,
                packetLossPercent = 100f
            )
        }

        val latencies = successful.map { it.latencyMs }
        val minLatency = latencies.minOrNull() ?: 0L
        val maxLatency = latencies.maxOrNull() ?: 0L
        val avgLatency = latencies.average().toLong()

        // Calculate StdDev & Jitter
        val variance = latencies.fold(0.0) { accum, item -> accum + (item - avgLatency).toDouble().pow(2) } / latencies.size
        val stdDev = sqrt(variance)

        // Jitter: Average absolute difference between consecutive latencies
        var jitterSum = 0L
        if (latencies.size > 1) {
            for (i in 0 until latencies.size - 1) {
                jitterSum += abs(latencies[i + 1] - latencies[i])
            }
        }
        val jitter = if (latencies.size > 1) jitterSum / (latencies.size - 1) else 0L

        return PingSummary(
            target = target,
            pingType = pingType,
            packetsSent = totalSent,
            packetsReceived = totalReceived,
            packetLossPercent = lossPercent,
            minLatencyMs = minLatency,
            maxLatencyMs = maxLatency,
            avgLatencyMs = avgLatency,
            jitterMs = jitter,
            stdDevMs = stdDev,
            mdevMs = stdDev
        )
    }

    /**
     * Conducts a fast parallel subnet ping sweep across a range of IP addresses.
     */
    suspend fun sweepSubnetRange(
        subnetPrefix: String, // e.g. "192.168.1"
        startHost: Int = 1,
        endHost: Int = 254,
        timeoutMs: Int = 300,
        onHostDiscovered: suspend (SubnetPingHost) -> Unit
    ): List<SubnetPingHost> = coroutineScope {
        val discoveredHosts = mutableListOf<SubnetPingHost>()

        val jobs = (startHost..endHost).map { hostSuffix ->
            async(Dispatchers.IO) {
                val targetIp = "$subnetPrefix.$hostSuffix"
                val packet = executeIcmpPing(
                    targetHost = targetIp,
                    seqNumber = hostSuffix,
                    payloadBytes = 32,
                    timeoutMs = timeoutMs
                )

                if (packet.isSuccess) {
                    val hostInfo = SubnetPingHost(
                        ip = targetIp,
                        hostname = try { InetAddress.getByName(targetIp).hostName } catch (_: Exception) { targetIp },
                        latencyMs = packet.latencyMs,
                        isReachable = true,
                        statusMessage = "${packet.latencyMs} ms"
                    )
                    synchronized(discoveredHosts) {
                        discoveredHosts.add(hostInfo)
                    }
                    onHostDiscovered(hostInfo)
                }
            }
        }

        jobs.forEach { it.await() }
        discoveredHosts.sortedBy { host -> host.ip.substringAfterLast(".").toIntOrNull() ?: 0 }
    }

    /**
     * Benchmarks public DNS resolvers and major CDN endpoints.
     */
    suspend fun benchmarkPublicEndpoints(): List<PingBenchmarkTarget> = coroutineScope {
        val targets = listOf(
            PingBenchmarkTarget("Cloudflare Primary", "1.1.1.1", "Public DNS"),
            PingBenchmarkTarget("Google Primary", "8.8.8.8", "Public DNS"),
            PingBenchmarkTarget("Quad9 Secure", "9.9.9.9", "Security DNS"),
            PingBenchmarkTarget("OpenDNS Home", "208.67.222.222", "Public DNS"),
            PingBenchmarkTarget("Cloudflare Secondary", "1.0.0.1", "Public DNS"),
            PingBenchmarkTarget("Google Secondary", "8.8.4.4", "Public DNS"),
            PingBenchmarkTarget("Level3 Core", "4.2.2.2", "Global Carrier"),
            PingBenchmarkTarget("AdGuard DNS", "94.140.14.14", "Privacy DNS"),
            PingBenchmarkTarget("Local Loopback", "127.0.0.1", "System Network")
        )

        targets.map { target ->
            async(Dispatchers.IO) {
                val ping = executeIcmpPing(target.address, seqNumber = 1, timeoutMs = 2000)
                target.copy(
                    latencyMs = if (ping.isSuccess) ping.latencyMs else null,
                    isReachable = ping.isSuccess,
                    packetLossPercent = if (ping.isSuccess) 0f else 100f
                )
            }
        }.map { it.await() }.sortedBy { it.latencyMs ?: Long.MAX_VALUE }
    }

    private fun cleanTargetHost(target: String): String {
        return target.trim()
            .replace(Regex("""^https?://"""), "")
            .replace(Regex("""/.*$"""), "")
            .replace(Regex(""":\d+$"""), "")
    }

    private fun resolveIp(host: String): String? {
        return try {
            InetAddress.getByName(host).hostAddress
        } catch (_: Exception) {
            null
        }
    }
}
