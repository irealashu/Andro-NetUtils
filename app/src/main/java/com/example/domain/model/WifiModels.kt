package com.example.domain.model

import java.io.Serializable

data class NetworkInterfaceInfo(
    val name: String, // e.g. "wlan0", "rmnet0", "tun0", "lo"
    val displayName: String,
    val ipv4Address: String?,
    val ipv6Address: String?,
    val isUp: Boolean,
    val isLoopback: Boolean,
    val isVpn: Boolean,
    val mtu: Int
) : Serializable

data class PingBenchmark(
    val label: String,
    val target: String,
    val latencyMs: Long,
    val isReachable: Boolean
) : Serializable

data class WifiTelemetry(
    val isConnected: Boolean,
    val networkType: String = "Wi-Fi", // "Wi-Fi", "Cellular", "Ethernet", "VPN", "Offline"
    val ssid: String,
    val bssid: String,
    val rssiDbm: Int,
    val linkSpeedMbps: Int,
    val frequencyMhz: Int,
    val channelNumber: Int,
    val bandName: String, // "2.4 GHz", "5 GHz", "6 GHz"
    val standard: String, // "Wi-Fi 6 (802.11ax)", "Wi-Fi 5 (802.11ac)", etc.
    val securityType: String, // "WPA3-SAE", "WPA2-PSK", "Enterprise", etc.
    val ipAddress: String,
    val ipv6Address: String? = null,
    val publicIp: String? = null,
    val ispName: String? = null,
    val gatewayIp: String,
    val netmask: String,
    val dnsServers: List<String>,
    val signalQualityPercent: Int,
    val securityScore: Int = 85,
    val activeInterfaces: List<NetworkInterfaceInfo> = emptyList(),
    val pingBenchmarks: List<PingBenchmark> = emptyList()
) : Serializable

data class WifiChannelSpectrum(
    val channelNumber: Int,
    val centerFrequencyMhz: Int,
    val band: String, // "2.4 GHz" or "5 GHz"
    val congestionLevel: Int, // 0 to 10
    val activeAccessPointsCount: Int,
    val isCurrentChannel: Boolean = false,
    val channelWidthMhz: Int = 20
) : Serializable

data class QuickAuditResult(
    val target: String,
    val isPingable: Boolean,
    val pingLatencyMs: Long,
    val openWebPorts: List<Int>,
    val tlsExpiresInDays: Long?,
    val httpSecurityGrade: String?,
    val summaryFindings: List<String>
) : Serializable
