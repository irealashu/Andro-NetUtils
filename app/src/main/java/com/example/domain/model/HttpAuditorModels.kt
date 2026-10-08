package com.example.domain.model

import java.io.Serializable

enum class SecurityGrade(val letter: String, val colorHex: Long) {
    A_PLUS("A+", 0xFF00E676),
    A("A", 0xFF00C853),
    B("B", 0xFF64DD17),
    C("C", 0xFFFFD600),
    D("D", 0xFFFF9100),
    F("F", 0xFFFF3D00)
}

enum class HeaderStatus {
    SECURE,
    WARNING,
    MISSING,
    VULNERABLE
}

data class HeaderCheck(
    val headerName: String,
    val present: Boolean,
    val rawValue: String?,
    val status: HeaderStatus,
    val scoreContribution: Int,
    val maxScore: Int,
    val title: String,
    val description: String,
    val riskSummary: String,
    val remediationExample: String
) : Serializable

data class HttpSecurityAudit(
    val url: String,
    val ipAddress: String?,
    val responseCode: Int,
    val httpVersion: String,
    val responseTimeMs: Long,
    val totalScore: Int,
    val maxPossibleScore: Int,
    val grade: SecurityGrade,
    val headerChecks: List<HeaderCheck>,
    val serverBanner: String?,
    val poweredBy: String?,
    val cookiesAudited: List<CookieAudit>,
    val rawHeaders: Map<String, String>,
    val summaryRecommendations: List<String>
) : Serializable

data class CookieAudit(
    val name: String,
    val isSecure: Boolean,
    val isHttpOnly: Boolean,
    val sameSite: String?,
    val raw: String
) : Serializable
