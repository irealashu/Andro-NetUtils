package com.example.domain.model

import java.io.Serializable

data class MdnsDiscoveredService(
    val serviceName: String,
    val serviceType: String, // e.g. "_http._tcp", "_googlecast._tcp", "_airplay._tcp", "_printer._tcp", "_smb._tcp"
    val host: String,
    val ipAddress: String,
    val port: Int,
    val category: String, // "Media Streamer", "Smart Home IoT", "Printer", "NAS Storage", "Gateway Router"
    val txtRecords: Map<String, String> = emptyMap(),
    val responseTimeMs: Long = 0
) : Serializable

data class WolResult(
    val macAddress: String,
    val broadcastIp: String,
    val port: Int,
    val packetsSent: Int,
    val isSuccessful: Boolean,
    val message: String
) : Serializable

data class CidrCalculation(
    val inputCidr: String,
    val ipAddress: String,
    val prefixLength: Int,
    val netmask: String,
    val wildcardMask: String,
    val networkAddress: String,
    val broadcastAddress: String,
    val hostRangeStart: String,
    val hostRangeEnd: String,
    val totalHosts: Long,
    val usableHosts: Long,
    val ipClass: String, // Class A, B, C, CIDR
    val isPrivate: Boolean,
    val binaryNetmask: String,
    val hexNetmask: String
) : Serializable

data class DnsRaceEntry(
    val resolverName: String,
    val ipAddress: String,
    val queryTimeMs: Long,
    val isFastest: Boolean,
    val isReachable: Boolean,
    val providerTag: String
) : Serializable

data class HttpWorkbenchRequest(
    val url: String,
    val method: String = "GET", // GET, POST, PUT, DELETE, HEAD, OPTIONS, PATCH
    val headers: Map<String, String> = mapOf("User-Agent" to "NetSentinel-Workbench/1.0"),
    val body: String? = null,
    val contentType: String = "application/json"
) : Serializable

data class JwtTokenAnalysis(
    val isValidFormat: Boolean,
    val algorithm: String?,
    val headerJson: String?,
    val payloadJson: String?,
    val subject: String?,
    val issuer: String?,
    val expirationDate: String?,
    val isExpired: Boolean
) : Serializable

data class HttpWorkbenchResponse(
    val statusCode: Int,
    val statusMessage: String,
    val responseTimeMs: Long,
    val responseBody: String,
    val responseHeaders: Map<String, String>,
    val payloadSizeBytes: Long,
    val protocol: String,
    val isTls: Boolean,
    val jwtAnalysis: JwtTokenAnalysis? = null
) : Serializable

data class WhoisResult(
    val domain: String,
    val registrar: String,
    val creationDate: String,
    val expirationDate: String,
    val nameServers: List<String>,
    val asnNumber: String,
    val asnOrganization: String,
    val ipRange: String,
    val status: String,
    val dnssec: String
) : Serializable
