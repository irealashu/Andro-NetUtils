package com.example.domain.engine

import com.example.domain.model.CertificateDetail
import com.example.domain.model.TlsAuditResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

class TlsInspectorEngine {

    suspend fun inspectTls(
        host: String,
        port: Int = 443,
        timeoutMs: Int = 5000
    ): TlsAuditResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var isTrusted = true
        var trustErrorMessage: String? = null
        val capturedCerts = mutableListOf<X509Certificate>()
        val warnings = mutableListOf<String>()

        var socket: SSLSocket? = null
        try {
            // Setup custom TrustManager to capture server certificates even if untrusted/self-signed
            val trustManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    chain?.let { capturedCerts.addAll(it) }
                    // Also run standard check to determine if system trusts it
                    try {
                        val defaultTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                        defaultTmf.init(null as java.security.KeyStore?)
                        for (tm in defaultTmf.trustManagers) {
                            if (tm is X509TrustManager) {
                                tm.checkServerTrusted(chain, authType)
                            }
                        }
                    } catch (e: Exception) {
                        isTrusted = false
                        trustErrorMessage = e.message ?: "Untrusted certificate chain or self-signed certificate"
                    }
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(trustManager), java.security.SecureRandom())

            val s = sslContext.socketFactory.createSocket() as SSLSocket
            socket = s
            s.soTimeout = timeoutMs
            s.connect(InetSocketAddress(host, port), timeoutMs)
            s.startHandshake()

            val session = s.session
            val protocol = session.protocol
            val cipherSuite = session.cipherSuite
            val handshakeLatency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)

            // Peer certificates from session if trust manager didn't collect
            if (capturedCerts.isEmpty()) {
                try {
                    session.peerCertificates.forEach {
                        if (it is X509Certificate) capturedCerts.add(it)
                    }
                } catch (_: Exception) {}
            }

            val certDetails = capturedCerts.map { cert ->
                parseCertificate(cert, warnings)
            }

            // Security checks
            if (protocol == "TLSv1" || protocol == "TLSv1.1" || protocol == "SSLv3") {
                warnings.add("Obsolete and vulnerable TLS protocol version: $protocol")
            }
            if (cipherSuite.contains("CBC", ignoreCase = true)) {
                warnings.add("Cipher suite uses CBC mode (susceptible to Lucky13 / padding oracle attacks)")
            }
            if (cipherSuite.contains("RC4", ignoreCase = true) || cipherSuite.contains("3DES", ignoreCase = true) || cipherSuite.contains("DES", ignoreCase = true)) {
                warnings.add("Weak and broken legacy cipher: $cipherSuite")
            }
            if (cipherSuite.contains("NULL", ignoreCase = true) || cipherSuite.contains("EXPORT", ignoreCase = true)) {
                warnings.add("Unencrypted or export-grade cipher detected!")
            }

            val supportedProtocols = s.supportedProtocols.toList()

            TlsAuditResult(
                host = host,
                port = port,
                isTlsSupported = true,
                negotiatedProtocol = protocol,
                negotiatedCipherSuite = cipherSuite,
                isTrusted = isTrusted,
                trustErrorMessage = trustErrorMessage,
                certificates = certDetails,
                supportedProtocols = supportedProtocols,
                securityWarnings = warnings,
                handshakeLatencyMs = handshakeLatency
            )
        } catch (e: Exception) {
            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
            TlsAuditResult(
                host = host,
                port = port,
                isTlsSupported = false,
                isTrusted = false,
                trustErrorMessage = "TLS Handshake Failed: ${e.message ?: e.javaClass.simpleName}",
                securityWarnings = listOf("Unable to establish TLS connection: ${e.message}"),
                handshakeLatencyMs = latency
            )
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    private fun parseCertificate(cert: X509Certificate, warnings: MutableList<String>): CertificateDetail {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val now = Date()
        val notBefore = dateFormat.format(cert.notBefore)
        val notAfter = dateFormat.format(cert.notAfter)

        val diffMillis = cert.notAfter.time - now.time
        val daysUntilExpiration = TimeUnit.MILLISECONDS.toDays(diffMillis)
        val isExpired = daysUntilExpiration <= 0

        val isSelfSigned = cert.subjectX500Principal == cert.issuerX500Principal

        if (isExpired) {
            warnings.add("Certificate is EXPIRED (expired on $notAfter)")
        } else if (daysUntilExpiration < 14) {
            warnings.add("Certificate expires soon: $daysUntilExpiration days remaining")
        }

        if (isSelfSigned) {
            warnings.add("Self-signed certificate detected (Subject equals Issuer)")
        }

        val pubKey = cert.publicKey
        var keySize = 0
        val keyAlg = pubKey.algorithm
        when (pubKey) {
            is RSAPublicKey -> {
                keySize = pubKey.modulus.bitLength()
                if (keySize < 2048) {
                    warnings.add("Weak RSA key size: $keySize bits (minimum recommended is 2048 bits)")
                }
            }
            is ECPublicKey -> {
                keySize = pubKey.params.curve.field.fieldSize
                if (keySize < 256) {
                    warnings.add("Weak EC key size: $keySize bits")
                }
            }
        }

        if (cert.sigAlgName.contains("MD5", ignoreCase = true) || cert.sigAlgName.contains("SHA1", ignoreCase = true)) {
            warnings.add("Weak signature algorithm: ${cert.sigAlgName} (SHA-1/MD5 collision risk)")
        }

        val sans = mutableListOf<String>()
        try {
            val altNames = cert.subjectAlternativeNames
            altNames?.forEach { item ->
                if (item.size >= 2) {
                    sans.add(item[1].toString())
                }
            }
        } catch (_: Exception) {}

        val sha256 = getFingerprint(cert, "SHA-256")
        val sha1 = getFingerprint(cert, "SHA-1")
        val pem = convertToPem(cert)

        return CertificateDetail(
            subjectDn = cert.subjectX500Principal.name,
            issuerDn = cert.issuerX500Principal.name,
            serialNumber = cert.serialNumber.toString(16).uppercase(Locale.US),
            sigAlgName = cert.sigAlgName,
            publicKeyAlg = keyAlg,
            keySizeBits = keySize,
            notBefore = notBefore,
            notAfter = notAfter,
            daysUntilExpiration = daysUntilExpiration,
            isExpired = isExpired,
            isSelfSigned = isSelfSigned,
            subjectAlternativeNames = sans,
            sha256Fingerprint = sha256,
            sha1Fingerprint = sha1,
            pemEncoded = pem
        )
    }

    private fun convertToPem(cert: X509Certificate): String {
        return try {
            val mimeEncoder = Base64.getMimeEncoder(64, "\n".toByteArray())
            val base64Text = mimeEncoder.encodeToString(cert.encoded)
            "-----BEGIN CERTIFICATE-----\n$base64Text\n-----END CERTIFICATE-----"
        } catch (_: Exception) {
            "-----BEGIN CERTIFICATE-----\n[Unable to encode certificate bytes]\n-----END CERTIFICATE-----"
        }
    }

    private fun getFingerprint(cert: X509Certificate, algorithm: String): String {
        return try {
            val md = MessageDigest.getInstance(algorithm)
            val der = cert.encoded
            val digest = md.digest(der)
            digest.joinToString(":") { String.format("%02X", it) }
        } catch (_: Exception) {
            "N/A"
        }
    }
}
