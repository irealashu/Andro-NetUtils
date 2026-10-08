package com.example.domain.model

import java.io.Serializable

enum class NetworkProtocol(val displayName: String, val protocolNumber: Int) {
    TCP("TCP", 6),
    UDP("UDP", 17),
    ICMP("ICMP", 1),
    DNS("DNS", 53),
    TLS("TLS/SSL", 443),
    HTTP("HTTP", 80),
    OTHER("OTHER", 0)
}

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean = false,
    val uid: Int = 0,
    val version: String = "1.0",
    val permissionCount: Int = 0
) : Serializable

data class AppEndpoint(
    val hostOrIp: String,
    val port: Int,
    val protocol: NetworkProtocol,
    val packetCount: Int,
    val bytesTransferred: Long,
    val lastSeenTimestamp: Long
) : Serializable

data class DissectedPacket(
    val id: Long,
    val timestampMillis: Long,
    val sourceIp: String,
    val destinationIp: String,
    val sourcePort: Int?,
    val destinationPort: Int?,
    val protocol: NetworkProtocol,
    val packetLengthBytes: Int,
    val ttl: Int,
    val flags: String?,
    val summary: String,
    val payloadPreviewHex: String,
    val payloadPreviewAscii: String,
    val rawBytes: ByteArray = ByteArray(0),
    val appName: String? = null,
    val packageName: String? = null,
    val uid: Int? = null
) : Serializable

data class TrafficStats(
    val totalPackets: Long = 0,
    val totalBytes: Long = 0,
    val tcpPackets: Long = 0,
    val udpPackets: Long = 0,
    val icmpPackets: Long = 0,
    val dnsPackets: Long = 0,
    val bytesPerSec: Long = 0,
    val isCapturing: Boolean = false,
    val targetApp: InstalledAppInfo? = null
) : Serializable
