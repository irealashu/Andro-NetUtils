package com.example.domain.model

import java.io.Serializable

data class TlsAuditResult(
    val host: String,
    val port: Int,
    val isTlsSupported: Boolean,
    val negotiatedProtocol: String = "",
    val negotiatedCipherSuite: String = "",
    val isTrusted: Boolean = false,
    val trustErrorMessage: String? = null,
    val certificates: List<CertificateDetail> = emptyList(),
    val supportedProtocols: List<String> = emptyList(),
    val securityWarnings: List<String> = emptyList(),
    val handshakeLatencyMs: Long = 0,
    val alpnSelected: String? = null
) : Serializable

data class CertificateDetail(
    val subjectDn: String,
    val issuerDn: String,
    val serialNumber: String,
    val sigAlgName: String,
    val publicKeyAlg: String,
    val keySizeBits: Int,
    val notBefore: String,
    val notAfter: String,
    val daysUntilExpiration: Long,
    val isExpired: Boolean,
    val isSelfSigned: Boolean,
    val subjectAlternativeNames: List<String> = emptyList(),
    val sha256Fingerprint: String = "",
    val sha1Fingerprint: String = "",
    val pemEncoded: String = ""
) : Serializable
