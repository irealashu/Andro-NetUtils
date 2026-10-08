package com.example.domain.engine

import com.example.domain.model.CommonPortPresets
import com.example.domain.model.PortScanResult
import com.example.domain.model.RiskLevel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket

class PortScannerEngine {

    suspend fun scanPorts(
        host: String,
        ports: List<Int>,
        timeoutMs: Int = 450,
        maxConcurrency: Int = 20,
        grabBanners: Boolean = true,
        onProgress: (scanned: Int, total: Int, currentPort: Int) -> Unit = { _, _, _ -> }
    ): List<PortScanResult> = withContext(Dispatchers.IO) {
        val semaphore = Semaphore(maxConcurrency)
        val results = mutableListOf<PortScanResult>()
        val total = ports.size
        var completed = 0

        val deferreds = ports.map { port ->
            async {
                semaphore.withPermit {
                    val result = scanSinglePort(host, port, timeoutMs, grabBanners)
                    synchronized(results) {
                        results.add(result)
                        completed++
                        onProgress(completed, total, port)
                    }
                    result
                }
            }
        }

        deferreds.awaitAll()
        results.sortedBy { it.port }
    }

    fun scanPortsFlow(
        host: String,
        ports: List<Int>,
        timeoutMs: Int = 450,
        maxConcurrency: Int = 20,
        grabBanners: Boolean = true
    ): Flow<PortScanResult> = flow {
        val semaphore = Semaphore(maxConcurrency)
        coroutineScope {
            val deferreds = ports.map { port ->
                async {
                    semaphore.withPermit {
                        scanSinglePort(host, port, timeoutMs, grabBanners)
                    }
                }
            }
            for (deferred in deferreds) {
                emit(deferred.await())
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun scanSinglePort(
        host: String,
        port: Int,
        timeoutMs: Int,
        grabBanner: Boolean
    ): PortScanResult {
        val serviceName = CommonPortPresets.getServiceName(port)
        val risk = CommonPortPresets.getRiskLevel(port)
        val startTime = System.currentTimeMillis()

        var socket: Socket? = null
        try {
            socket = Socket()
            socket.tcpNoDelay = true
            socket.soTimeout = timeoutMs
            socket.connect(InetSocketAddress(host, port), timeoutMs)

            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
            var banner: String? = null

            if (grabBanner && socket.isConnected) {
                banner = tryGrabBanner(socket, port)
            }

            return PortScanResult(
                port = port,
                serviceName = serviceName,
                transport = "TCP",
                isOpen = true,
                latencyMs = latency,
                banner = banner,
                riskLevel = risk,
                description = "Open port responding in ${latency}ms"
            )
        } catch (e: Exception) {
            return PortScanResult(
                port = port,
                serviceName = serviceName,
                transport = "TCP",
                isOpen = false,
                latencyMs = 0,
                banner = null,
                riskLevel = RiskLevel.INFO,
                description = "Closed / Filtered (${e.javaClass.simpleName})"
            )
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    private fun tryGrabBanner(socket: Socket, port: Int): String? {
        return try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = OutputStreamWriter(socket.getOutputStream())

            // Depending on service, probe or wait for initial server greeting
            when (port) {
                80, 8080, 8000, 3000, 5000 -> {
                    writer.write("HEAD / HTTP/1.0\r\nHost: localhost\r\n\r\n")
                    writer.flush()
                }
                21, 22, 25, 110, 143, 220 -> {
                    // Servers send banner immediately on connect
                }
                else -> {
                    writer.write("\r\n")
                    writer.flush()
                }
            }

            val line = reader.readLine()
            if (!line.isNullOrBlank()) {
                line.trim().take(120)
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
