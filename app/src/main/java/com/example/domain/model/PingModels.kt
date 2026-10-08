package com.example.domain.model

import java.io.Serializable

enum class PingType {
    ICMP_ECHO,
    TCP_SYN,
    HTTP_WEB,
    SUBNET_SWEEP
}

data class PingPacket(
    val sequenceNumber: Int,
    val targetHost: String,
    val ipAddress: String? = null,
    val bytes: Int = 64,
    val ttl: Int? = null,
    val latencyMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true,
    val errorMessage: String? = null,
    val httpStatusCode: Int? = null,
    val port: Int? = null
) : Serializable

data class PingSummary(
    val target: String,
    val pingType: PingType = PingType.ICMP_ECHO,
    val packetsSent: Int = 0,
    val packetsReceived: Int = 0,
    val packetLossPercent: Float = 0f,
    val minLatencyMs: Long = 0,
    val maxLatencyMs: Long = 0,
    val avgLatencyMs: Long = 0,
    val jitterMs: Long = 0,
    val stdDevMs: Double = 0.0,
    val mdevMs: Double = 0.0
) : Serializable

data class SubnetPingHost(
    val ip: String,
    val hostname: String? = null,
    val latencyMs: Long = 0,
    val isReachable: Boolean = false,
    val openPorts: List<Int> = emptyList(),
    val statusMessage: String = "Offline"
) : Serializable

data class PingBenchmarkTarget(
    val name: String,
    val address: String,
    val category: String, // e.g. "Public DNS", "Global CDN", "Local Gateway"
    val latencyMs: Long? = null,
    val isReachable: Boolean = false,
    val packetLossPercent: Float = 0f,
    val isTesting: Boolean = false
) : Serializable
