package com.example.domain.engine

import com.example.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class HttpHeaderAuditorEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun auditUrl(targetUrl: String): HttpSecurityAudit = withContext(Dispatchers.IO) {
        val normalizedUrl = if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
            "https://$targetUrl"
        } else {
            targetUrl
        }

        val startTime = System.currentTimeMillis()
        var ipAddress: String? = null
        try {
            val uri = java.net.URI(normalizedUrl)
            val host = uri.host
            if (host != null) {
                ipAddress = InetAddress.getByName(host).hostAddress
            }
        } catch (_: Exception) {}

        val request = Request.Builder()
            .url(normalizedUrl)
            .header("User-Agent", "Mozilla/5.0 (Android; NetSentinel-Audit/1.0; Enterprise Network Scanner)")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseTime = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                val rawHeaders = mutableMapOf<String, String>()
                for (name in response.headers.names()) {
                    rawHeaders[name] = response.header(name) ?: ""
                }

                val serverBanner = response.header("Server")
                val poweredBy = response.header("X-Powered-By")
                val cookies = parseCookies(response.headers.values("Set-Cookie"))

                val headerChecks = performHeaderChecks(rawHeaders, normalizedUrl)
                val recommendations = mutableListOf<String>()

                var earnedScore = 0
                var maxScore = 0
                for (check in headerChecks) {
                    earnedScore += check.scoreContribution
                    maxScore += check.maxScore
                    if (check.status == HeaderStatus.MISSING || check.status == HeaderStatus.VULNERABLE) {
                        recommendations.add(check.riskSummary)
                    }
                }

                if (!serverBanner.isNullOrBlank()) {
                    recommendations.add("Suppress 'Server' banner ($serverBanner) to reduce fingerprinting.")
                }
                if (!poweredBy.isNullOrBlank()) {
                    recommendations.add("Remove 'X-Powered-By' header ($poweredBy) to obscure tech stack.")
                }

                val scorePercent = if (maxScore > 0) ((earnedScore.toDouble() / maxScore) * 100).toInt() else 0
                val grade = calculateGrade(scorePercent, normalizedUrl.startsWith("https://"))

                return@withContext HttpSecurityAudit(
                    url = normalizedUrl,
                    ipAddress = ipAddress,
                    responseCode = response.code,
                    httpVersion = response.protocol.toString(),
                    responseTimeMs = responseTime,
                    totalScore = scorePercent,
                    maxPossibleScore = 100,
                    grade = grade,
                    headerChecks = headerChecks,
                    serverBanner = serverBanner,
                    poweredBy = poweredBy,
                    cookiesAudited = cookies,
                    rawHeaders = rawHeaders,
                    summaryRecommendations = recommendations
                )
            }
        } catch (e: Exception) {
            val responseTime = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
            return@withContext HttpSecurityAudit(
                url = normalizedUrl,
                ipAddress = ipAddress,
                responseCode = 0,
                httpVersion = "ERR",
                responseTimeMs = responseTime,
                totalScore = 0,
                maxPossibleScore = 100,
                grade = SecurityGrade.F,
                headerChecks = emptyList(),
                serverBanner = null,
                poweredBy = null,
                cookiesAudited = emptyList(),
                rawHeaders = emptyMap(),
                summaryRecommendations = listOf("Failed to connect to target: ${e.message ?: e.javaClass.simpleName}")
            )
        }
    }

    private fun performHeaderChecks(headers: Map<String, String>, url: String): List<HeaderCheck> {
        val checks = mutableListOf<HeaderCheck>()
        val isHttps = url.startsWith("https://", ignoreCase = true)

        // 1. HSTS (Strict-Transport-Security)
        val hsts = findHeader(headers, "Strict-Transport-Security")
        if (hsts != null) {
            val hasMaxAge = hsts.contains("max-age", ignoreCase = true)
            val hasSubdomains = hsts.contains("includeSubDomains", ignoreCase = true)
            val hasPreload = hsts.contains("preload", ignoreCase = true)
            val isStrong = hasMaxAge && (hasSubdomains || hasPreload)

            checks.add(
                HeaderCheck(
                    headerName = "Strict-Transport-Security",
                    present = true,
                    rawValue = hsts,
                    status = if (isStrong) HeaderStatus.SECURE else HeaderStatus.WARNING,
                    scoreContribution = if (isStrong) 25 else 15,
                    maxScore = 25,
                    title = "HTTP Strict Transport Security (HSTS)",
                    description = "Enforces HTTPS connections and eliminates SSL-stripping attack vectors.",
                    riskSummary = if (!isStrong) "HSTS is configured but missing 'includeSubDomains' or recommended 1-year max-age." else "Properly configured HSTS.",
                    remediationExample = "add_header Strict-Transport-Security \"max-age=31536000; includeSubDomains; preload\" always;"
                )
            )
        } else {
            checks.add(
                HeaderCheck(
                    headerName = "Strict-Transport-Security",
                    present = false,
                    rawValue = null,
                    status = if (isHttps) HeaderStatus.MISSING else HeaderStatus.WARNING,
                    scoreContribution = 0,
                    maxScore = 25,
                    title = "HTTP Strict Transport Security (HSTS)",
                    description = "Enforces HTTPS connections and eliminates SSL-stripping attack vectors.",
                    riskSummary = "Missing HSTS header. Clients may connect via unencrypted plaintext HTTP first.",
                    remediationExample = "add_header Strict-Transport-Security \"max-age=31536000; includeSubDomains; preload\" always;"
                )
            )
        }

        // 2. Content-Security-Policy (CSP)
        val csp = findHeader(headers, "Content-Security-Policy")
        if (csp != null) {
            val hasUnsafeInline = csp.contains("'unsafe-inline'", ignoreCase = true)
            val hasUnsafeEval = csp.contains("'unsafe-eval'", ignoreCase = true)
            val hasDefaultSrc = csp.contains("default-src", ignoreCase = true)
            val status = if (hasUnsafeInline || hasUnsafeEval) HeaderStatus.WARNING else HeaderStatus.SECURE
            val score = if (status == HeaderStatus.SECURE) 25 else 15

            checks.add(
                HeaderCheck(
                    headerName = "Content-Security-Policy",
                    present = true,
                    rawValue = csp,
                    status = status,
                    scoreContribution = score,
                    maxScore = 25,
                    title = "Content Security Policy (CSP)",
                    description = "Restricts resources (scripts, styles, images, iframes) preventing XSS and injection.",
                    riskSummary = if (hasUnsafeInline) "CSP contains 'unsafe-inline', weakening XSS protections." else "CSP active and enforcing restricted origins.",
                    remediationExample = "add_header Content-Security-Policy \"default-src 'self'; script-src 'self' https://trusted.cdn.com; object-src 'none'; base-uri 'self';\" always;"
                )
            )
        } else {
            checks.add(
                HeaderCheck(
                    headerName = "Content-Security-Policy",
                    present = false,
                    rawValue = null,
                    status = HeaderStatus.MISSING,
                    scoreContribution = 0,
                    maxScore = 25,
                    title = "Content Security Policy (CSP)",
                    description = "Restricts resource loading to mitigate Cross-Site Scripting (XSS) and data injection.",
                    riskSummary = "Missing CSP header. Web browsers will execute any injected scripts without sandbox boundaries.",
                    remediationExample = "add_header Content-Security-Policy \"default-src 'self'; object-src 'none';\" always;"
                )
            )
        }

        // 3. X-Frame-Options
        val xfo = findHeader(headers, "X-Frame-Options")
        if (xfo != null) {
            val isSecure = xfo.equals("DENY", ignoreCase = true) || xfo.equals("SAMEORIGIN", ignoreCase = true)
            checks.add(
                HeaderCheck(
                    headerName = "X-Frame-Options",
                    present = true,
                    rawValue = xfo,
                    status = if (isSecure) HeaderStatus.SECURE else HeaderStatus.WARNING,
                    scoreContribution = if (isSecure) 15 else 8,
                    maxScore = 15,
                    title = "Anti-Clickjacking (X-Frame-Options)",
                    description = "Controls whether the browser is allowed to render a page in a <frame>, <iframe>, or <object>.",
                    riskSummary = if (isSecure) "Clickjacking defense enabled ($xfo)." else "Unrecognized X-Frame-Options directive.",
                    remediationExample = "add_header X-Frame-Options \"DENY\" always;"
                )
            )
        } else {
            checks.add(
                HeaderCheck(
                    headerName = "X-Frame-Options",
                    present = false,
                    rawValue = null,
                    status = HeaderStatus.MISSING,
                    scoreContribution = 0,
                    maxScore = 15,
                    title = "Anti-Clickjacking (X-Frame-Options)",
                    description = "Controls whether the browser is allowed to render a page in an iframe.",
                    riskSummary = "Missing X-Frame-Options. Site can be framed by malicious third parties to execute UI redressing/clickjacking.",
                    remediationExample = "add_header X-Frame-Options \"DENY\" always;"
                )
            )
        }

        // 4. X-Content-Type-Options
        val xcto = findHeader(headers, "X-Content-Type-Options")
        if (xcto != null && xcto.equals("nosniff", ignoreCase = true)) {
            checks.add(
                HeaderCheck(
                    headerName = "X-Content-Type-Options",
                    present = true,
                    rawValue = xcto,
                    status = HeaderStatus.SECURE,
                    scoreContribution = 10,
                    maxScore = 10,
                    title = "MIME Sniffing Defense (X-Content-Type-Options)",
                    description = "Forces browsers to respect declared Content-Type, preventing MIME confusion attacks.",
                    riskSummary = "Configured with 'nosniff'.",
                    remediationExample = "add_header X-Content-Type-Options \"nosniff\" always;"
                )
            )
        } else {
            checks.add(
                HeaderCheck(
                    headerName = "X-Content-Type-Options",
                    present = xcto != null,
                    rawValue = xcto,
                    status = HeaderStatus.MISSING,
                    scoreContribution = 0,
                    maxScore = 10,
                    title = "MIME Sniffing Defense (X-Content-Type-Options)",
                    description = "Forces browsers to respect declared Content-Type.",
                    riskSummary = "Missing 'nosniff'. Browsers may treat user-uploaded non-executable files as HTML/JavaScript.",
                    remediationExample = "add_header X-Content-Type-Options \"nosniff\" always;"
                )
            )
        }

        // 5. Referrer-Policy
        val refPolicy = findHeader(headers, "Referrer-Policy")
        if (refPolicy != null) {
            checks.add(
                HeaderCheck(
                    headerName = "Referrer-Policy",
                    present = true,
                    rawValue = refPolicy,
                    status = HeaderStatus.SECURE,
                    scoreContribution = 10,
                    maxScore = 10,
                    title = "Referrer Policy",
                    description = "Controls how much referrer information (URL parameters, tokens) is sent with requests.",
                    riskSummary = "Referrer-Policy is set ($refPolicy).",
                    remediationExample = "add_header Referrer-Policy \"strict-origin-when-cross-origin\" always;"
                )
            )
        } else {
            checks.add(
                HeaderCheck(
                    headerName = "Referrer-Policy",
                    present = false,
                    rawValue = null,
                    status = HeaderStatus.WARNING,
                    scoreContribution = 0,
                    maxScore = 10,
                    title = "Referrer Policy",
                    description = "Controls referrer information sent to external origins.",
                    riskSummary = "Missing Referrer-Policy. Sensitive query strings could leak to external analytics/CDNs.",
                    remediationExample = "add_header Referrer-Policy \"strict-origin-when-cross-origin\" always;"
                )
            )
        }

        // 6. Permissions-Policy
        val permPolicy = findHeader(headers, "Permissions-Policy") ?: findHeader(headers, "Feature-Policy")
        if (permPolicy != null) {
            checks.add(
                HeaderCheck(
                    headerName = "Permissions-Policy",
                    present = true,
                    rawValue = permPolicy,
                    status = HeaderStatus.SECURE,
                    scoreContribution = 10,
                    maxScore = 10,
                    title = "Browser Hardware Permissions Policy",
                    description = "Restricts access to browser APIs (camera, microphone, geolocation, payment).",
                    riskSummary = "Permissions-Policy restricts embedded API capabilities.",
                    remediationExample = "add_header Permissions-Policy \"camera=(), microphone=(), geolocation=()\" always;"
                )
            )
        } else {
            checks.add(
                HeaderCheck(
                    headerName = "Permissions-Policy",
                    present = false,
                    rawValue = null,
                    status = HeaderStatus.WARNING,
                    scoreContribution = 0,
                    maxScore = 10,
                    title = "Browser Hardware Permissions Policy",
                    description = "Restricts browser access to device sensors and APIs.",
                    riskSummary = "Missing Permissions-Policy. Embedded iframes might request sensor access without restriction.",
                    remediationExample = "add_header Permissions-Policy \"camera=(), microphone=(), geolocation=()\" always;"
                )
            )
        }

        // 7. CORS Wildcard Check
        val cors = findHeader(headers, "Access-Control-Allow-Origin")
        if (cors != null) {
            val isWildcard = cors.trim() == "*"
            checks.add(
                HeaderCheck(
                    headerName = "Access-Control-Allow-Origin",
                    present = true,
                    rawValue = cors,
                    status = if (isWildcard) HeaderStatus.WARNING else HeaderStatus.SECURE,
                    scoreContribution = if (isWildcard) 3 else 5,
                    maxScore = 5,
                    title = "Cross-Origin Resource Sharing (CORS)",
                    description = "Defines which origins are permitted to read HTTP response payloads.",
                    riskSummary = if (isWildcard) "CORS allows wildcard origin '*'. Avoid combining with credentials." else "CORS configured for specific origin.",
                    remediationExample = "add_header Access-Control-Allow-Origin \"https://app.example.com\" always;"
                )
            )
        }

        return checks
    }

    private fun findHeader(headers: Map<String, String>, name: String): String? {
        return headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
    }

    private fun parseCookies(rawCookieHeaders: List<String>): List<CookieAudit> {
        return rawCookieHeaders.map { raw ->
            val parts = raw.split(";")
            val nameValue = parts.firstOrNull()?.split("=") ?: listOf("unknown", "")
            val name = nameValue.firstOrNull()?.trim() ?: "cookie"

            val isSecure = raw.contains("Secure", ignoreCase = true)
            val isHttpOnly = raw.contains("HttpOnly", ignoreCase = true)
            val sameSite = when {
                raw.contains("SameSite=Strict", ignoreCase = true) -> "Strict"
                raw.contains("SameSite=Lax", ignoreCase = true) -> "Lax"
                raw.contains("SameSite=None", ignoreCase = true) -> "None"
                else -> null
            }

            CookieAudit(
                name = name,
                isSecure = isSecure,
                isHttpOnly = isHttpOnly,
                sameSite = sameSite,
                raw = raw
            )
        }
    }

    private fun calculateGrade(score: Int, isHttps: Boolean): SecurityGrade {
        if (!isHttps) return SecurityGrade.F
        return when {
            score >= 90 -> SecurityGrade.A_PLUS
            score >= 80 -> SecurityGrade.A
            score >= 65 -> SecurityGrade.B
            score >= 50 -> SecurityGrade.C
            score >= 35 -> SecurityGrade.D
            else -> SecurityGrade.F
        }
    }
}
