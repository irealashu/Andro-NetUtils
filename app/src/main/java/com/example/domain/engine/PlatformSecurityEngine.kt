package com.example.domain.engine

import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.example.domain.model.PlatformCapabilityItem
import com.example.domain.model.PlatformSecurityReport
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class PlatformSecurityEngine(private val context: Context) {

    fun getPlatformSecurityReport(): PlatformSecurityReport {
        val isRooted = checkRootStatus()
        val selinuxMode = getSelinuxStatus()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSave = powerManager?.isPowerSaveMode ?: false

        val capabilities = listOf(
            PlatformCapabilityItem(
                capability = "Asynchronous TCP Port Scanning",
                nonRootSupported = true,
                rootRequired = false,
                mechanism = "Non-blocking BSD Sockets + Kotlin Coroutines",
                androidConstraint = "Subject to socket descriptor file limits and thread pool starvation if unmanaged."
            ),
            PlatformCapabilityItem(
                capability = "TLS / SSL Certificate Chain Extraction",
                nonRootSupported = true,
                rootRequired = false,
                mechanism = "SSLSocket Handshake + Custom X509TrustManager",
                androidConstraint = "Fully supported on userland. Root not needed for certificate extraction."
            ),
            PlatformCapabilityItem(
                capability = "HTTP Defensive Header Audit",
                nonRootSupported = true,
                rootRequired = false,
                mechanism = "OkHttp / HTTP/2 & HTTP/1.1 Engine",
                androidConstraint = "Fully compliant across all Android versions (API 24+)."
            ),
            PlatformCapabilityItem(
                capability = "DNS-over-HTTPS (DoH) & Record Audit",
                nonRootSupported = true,
                rootRequired = false,
                mechanism = "RFC 8484 / JSON DoH wire format via Cloudflare/Google",
                androidConstraint = "Bypasses ISP DNS hijacking without needing elevated privileges."
            ),
            PlatformCapabilityItem(
                capability = "Wi-Fi Telemetry & Channel Analysis",
                nonRootSupported = true,
                rootRequired = false,
                mechanism = "WifiManager + ConnectivityManager SDK APIs",
                androidConstraint = "API 29+ throttles background Wi-Fi scan broadcasts (4 scans per 2-min window) and requires ACCESS_FINE_LOCATION."
            ),
            PlatformCapabilityItem(
                capability = "Packet Capture & Protocol Inspection",
                nonRootSupported = true,
                rootRequired = false,
                mechanism = "Android VpnService Virtual TUN Interface",
                androidConstraint = "Captures all device IP packets without root. User must accept the standard Android VPN prompt."
            ),
            PlatformCapabilityItem(
                capability = "ARP Cache Parsing (/proc/net/arp)",
                nonRootSupported = false,
                rootRequired = true,
                mechanism = "Direct Linux /proc/net/arp access",
                androidConstraint = "Hard-blocked on Android 10+ (API 29+) by SELinux sandbox policy for non-root apps."
            ),
            PlatformCapabilityItem(
                capability = "Raw Packet Injection / SYN Half-Open Scans",
                nonRootSupported = false,
                rootRequired = true,
                mechanism = "CAP_NET_RAW / su socket injection",
                androidConstraint = "Android kernel restricts raw socket creation (AF_PACKET/SOCK_RAW) exclusively to root (UID 0)."
            ),
            PlatformCapabilityItem(
                capability = "Native Toolchain Execution (Compiled Nmap/Tcpdump)",
                nonRootSupported = false,
                rootRequired = true,
                mechanism = "su root shell pipe execution",
                androidConstraint = "W^X (Write XOR Execute) memory restrictions in Android SELinux prevent executing dynamic binaries outside app private directory."
            )
        )

        return PlatformSecurityReport(
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            apiLevel = Build.VERSION.SDK_INT,
            isRooted = isRooted,
            selinuxMode = selinuxMode,
            isVpnActive = false,
            isWifiThrottlingActive = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P,
            isPowerSaveThrottling = isPowerSave,
            canAccessArpCache = isRooted || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q,
            canInjectRawPackets = isRooted,
            capabilities = capabilities
        )
    }

    private fun checkRootStatus(): Boolean {
        val paths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }

        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }
        return false
    }

    private fun getSelinuxStatus(): String {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec("getenforce")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            reader.close()
            line?.trim() ?: "Enforcing"
        } catch (_: Exception) {
            "Enforcing (Android Sandbox Active)"
        } finally {
            try {
                process?.destroy()
            } catch (_: Exception) {}
        }
    }
}
