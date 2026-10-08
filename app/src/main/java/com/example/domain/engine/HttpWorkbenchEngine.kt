package com.example.domain.engine

import com.example.domain.model.HttpWorkbenchRequest
import com.example.domain.model.HttpWorkbenchResponse
import com.example.domain.model.JwtTokenAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class HttpWorkbenchEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun executeRequest(req: HttpWorkbenchRequest): HttpWorkbenchResponse = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val normalizedUrl = if (!req.url.startsWith("http://") && !req.url.startsWith("https://")) {
            "https://${req.url}"
        } else req.url

        try {
            val builder = Request.Builder().url(normalizedUrl)

            // Add headers
            req.headers.forEach { (k, v) ->
                if (k.isNotBlank()) builder.header(k.trim(), v.trim())
            }

            // Body
            val mediaType = req.contentType.toMediaTypeOrNull()
            when (req.method.uppercase()) {
                "POST" -> builder.post((req.body ?: "").toRequestBody(mediaType))
                "PUT" -> builder.put((req.body ?: "").toRequestBody(mediaType))
                "PATCH" -> builder.patch((req.body ?: "").toRequestBody(mediaType))
                "DELETE" -> if (req.body != null) builder.delete(req.body.toRequestBody(mediaType)) else builder.delete()
                "HEAD" -> builder.head()
                else -> builder.get()
            }

            client.newCall(builder.build()).execute().use { response ->
                val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1)
                val bodyStr = response.body?.string() ?: ""
                val sizeBytes = bodyStr.toByteArray().size.toLong()

                val headersMap = mutableMapOf<String, String>()
                for (name in response.headers.names()) {
                    headersMap[name] = response.header(name) ?: ""
                }

                // Check for JWT in body or response headers
                val jwt = detectAndParseJwt(bodyStr) ?: detectAndParseJwt(response.header("Authorization") ?: "")

                HttpWorkbenchResponse(
                    statusCode = response.code,
                    statusMessage = response.message.ifBlank { if (response.isSuccessful) "OK" else "Response" },
                    responseTimeMs = elapsed,
                    responseBody = bodyStr,
                    responseHeaders = headersMap,
                    payloadSizeBytes = sizeBytes,
                    protocol = response.protocol.toString(),
                    isTls = normalizedUrl.startsWith("https://"),
                    jwtAnalysis = jwt
                )
            }
        } catch (e: Exception) {
            val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1)
            HttpWorkbenchResponse(
                statusCode = 0,
                statusMessage = "Connection Error: ${e.message ?: e.javaClass.simpleName}",
                responseTimeMs = elapsed,
                responseBody = "Failed to connect to host: ${e.message}",
                responseHeaders = emptyMap(),
                payloadSizeBytes = 0,
                protocol = "ERR",
                isTls = normalizedUrl.startsWith("https://"),
                jwtAnalysis = null
            )
        }
    }

    private fun detectAndParseJwt(raw: String): JwtTokenAnalysis? {
        try {
            val candidate = raw.trim().removePrefix("Bearer ").removePrefix("bearer ").trim()
            val parts = candidate.split(".")
            if (parts.size == 3 && parts[0].startsWith("eyJ")) {
                val header = String(android.util.Base64.decode(parts[0], android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))
                val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))

                val headerJson = JSONObject(header)
                val payloadJson = JSONObject(payload)

                val alg = headerJson.optString("alg", "JWT")
                val sub = if (payloadJson.has("sub")) payloadJson.optString("sub") else null
                val iss = if (payloadJson.has("iss")) payloadJson.optString("iss") else null
                val exp = payloadJson.optLong("exp", 0L)

                var expDateStr: String? = null
                var isExpired = false
                if (exp > 0) {
                    val expDate = Date(exp * 1000)
                    expDateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(expDate)
                    isExpired = expDate.before(Date())
                }

                return JwtTokenAnalysis(
                    isValidFormat = true,
                    algorithm = alg,
                    headerJson = headerJson.toString(2),
                    payloadJson = payloadJson.toString(2),
                    subject = sub,
                    issuer = iss,
                    expirationDate = expDateStr,
                    isExpired = isExpired
                )
            }
        } catch (_: Exception) {}
        return null
    }
}
