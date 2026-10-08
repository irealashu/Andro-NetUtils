package com.example.domain.engine

import com.example.domain.model.CidrCalculation

class CidrCalculatorEngine {

    fun calculateCidr(cidrInput: String): CidrCalculation {
        val clean = cidrInput.trim()
        val parts = clean.split("/")
        val ipStr = parts[0].trim()
        val prefix = if (parts.size >= 2) parts[1].trim().toIntOrNull()?.coerceIn(0, 32) ?: 24 else 24

        val ipLong = ipToLong(ipStr)
        val maskLong = if (prefix == 0) 0L else (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
        val wildcardLong = maskLong.inv() and 0xFFFFFFFFL

        val networkLong = ipLong and maskLong
        val broadcastLong = networkLong or wildcardLong

        val totalHosts = if (prefix >= 32) 1L else (1L shl (32 - prefix))
        val usableHosts = when {
            prefix >= 32 -> 1L
            prefix == 31 -> 2L // RFC 3021 point-to-point links
            else -> (totalHosts - 2).coerceAtLeast(0)
        }

        val hostStartLong = if (prefix >= 31) networkLong else networkLong + 1
        val hostEndLong = if (prefix >= 31) broadcastLong else (broadcastLong - 1).coerceAtLeast(hostStartLong)

        val netmask = longToIp(maskLong)
        val wildcard = longToIp(wildcardLong)
        val networkIp = longToIp(networkLong)
        val broadcastIp = longToIp(broadcastLong)
        val hostStart = longToIp(hostStartLong)
        val hostEnd = longToIp(hostEndLong)

        val firstOctet = (ipLong shr 24).toInt() and 0xFF
        val ipClass = when {
            firstOctet in 1..126 -> "Class A"
            firstOctet in 128..191 -> "Class B"
            firstOctet in 192..223 -> "Class C"
            firstOctet in 224..239 -> "Class D (Multicast)"
            else -> "Class E (Experimental)"
        }

        val isPrivate = (firstOctet == 10) ||
                (firstOctet == 172 && ((ipLong shr 16).toInt() and 0xFF) in 16..31) ||
                (firstOctet == 192 && ((ipLong shr 16).toInt() and 0xFF) == 168)

        val binaryNetmask = maskLong.toString(2).padStart(32, '0').chunked(8).joinToString(".")
        val hexNetmask = "0x" + maskLong.toString(16).uppercase().padStart(8, '0')

        return CidrCalculation(
            inputCidr = "$ipStr/$prefix",
            ipAddress = ipStr,
            prefixLength = prefix,
            netmask = netmask,
            wildcardMask = wildcard,
            networkAddress = networkIp,
            broadcastAddress = broadcastIp,
            hostRangeStart = hostStart,
            hostRangeEnd = hostEnd,
            totalHosts = totalHosts,
            usableHosts = usableHosts,
            ipClass = ipClass,
            isPrivate = isPrivate,
            binaryNetmask = binaryNetmask,
            hexNetmask = hexNetmask
        )
    }

    private fun ipToLong(ip: String): Long {
        return try {
            val parts = ip.split(".")
            if (parts.size != 4) return 0L
            ((parts[0].toLong() and 0xFF) shl 24) or
            ((parts[1].toLong() and 0xFF) shl 16) or
            ((parts[2].toLong() and 0xFF) shl 8) or
            (parts[3].toLong() and 0xFF)
        } catch (_: Exception) {
            0L
        }
    }

    private fun longToIp(longIp: Long): String {
        return "${(longIp shr 24) and 0xFF}.${(longIp shr 16) and 0xFF}.${(longIp shr 8) and 0xFF}.${longIp and 0xFF}"
    }
}
