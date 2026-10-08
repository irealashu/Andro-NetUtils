package com.example.domain.engine

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.domain.model.AppEndpoint
import com.example.domain.model.DissectedPacket
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.NetworkProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random

class PacketDissectorEngine {

    suspend fun getInstalledApplications(context: Context): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val appList = mutableListOf<InstalledAppInfo>()
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
            for (pkg in packages) {
                val appInfo = pkg.applicationInfo ?: continue
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val appName = try {
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    pkg.packageName
                }
                val permissionCount = pkg.requestedPermissions?.size ?: 0

                appList.add(
                    InstalledAppInfo(
                        appName = appName,
                        packageName = pkg.packageName,
                        isSystemApp = isSystem,
                        uid = appInfo.uid,
                        version = pkg.versionName ?: "1.0",
                        permissionCount = permissionCount
                    )
                )
            }
        } catch (_: Exception) {}

        // Fallback default apps if query was restricted by Android 11+ package visibility rules
        if (appList.isEmpty()) {
            appList.addAll(
                listOf(
                    InstalledAppInfo("NetSentinel Pro", "com.example", false, 10182, "1.0", 8),
                    InstalledAppInfo("Chrome Browser", "com.android.chrome", true, 10045, "124.0", 24),
                    InstalledAppInfo("Google Play Services", "com.google.android.gms", true, 10015, "24.12", 48),
                    InstalledAppInfo("Android System WebView", "com.google.android.webview", true, 10022, "124.0", 6),
                    InstalledAppInfo("Spotify Music", "com.spotify.music", false, 10240, "8.9.2", 18),
                    InstalledAppInfo("Slack Workspace", "com.Slack", false, 10255, "24.4", 15),
                    InstalledAppInfo("GitHub Mobile", "com.github.android", false, 10260, "1.140", 12)
                )
            )
        }

        appList.sortedWith(compareBy({ it.isSystemApp }, { it.appName.lowercase() }))
    }

    fun generatePacketStream(
        targetApp: InstalledAppInfo? = null,
        isRunning: () -> Boolean
    ): Flow<DissectedPacket> = flow {
        var packetId = 1L
        val random = Random(System.currentTimeMillis())

        // App-specific traffic templates
        val appSpecificTemplates = if (targetApp != null) {
            val domainSeed = targetApp.packageName.replace("com.", "").replace("org.", "").replace(".", "-")
            listOf(
                // DNS query for app's domain
                Triple("192.168.1.105", "1.1.1.1", NetworkProtocol.DNS to Pair(random.nextInt(49152, 65535), 53)),
                // TLS connection to app's API gateway
                Triple("192.168.1.105", "142.250.190.${random.nextInt(10, 200)}", NetworkProtocol.TLS to Pair(random.nextInt(49152, 65535), 443)),
                // HTTPS REST API Payload
                Triple("192.168.1.105", "104.244.42.${random.nextInt(1, 100)}", NetworkProtocol.TLS to Pair(random.nextInt(49152, 65535), 443)),
                // WebSocket / Realtime Push channel
                Triple("192.168.1.105", "172.217.16.${random.nextInt(1, 200)}", NetworkProtocol.TCP to Pair(random.nextInt(49152, 65535), 8443)),
                // CDN Content delivery
                Triple("192.168.1.105", "151.101.65.${random.nextInt(1, 250)}", NetworkProtocol.TLS to Pair(random.nextInt(49152, 65535), 443))
            )
        } else {
            listOf(
                Triple("192.168.1.105", "104.244.42.1", NetworkProtocol.TLS to Pair(54320, 443)),
                Triple("192.168.1.105", "1.1.1.1", NetworkProtocol.DNS to Pair(59812, 53)),
                Triple("142.250.190.46", "192.168.1.105", NetworkProtocol.TLS to Pair(443, 54320)),
                Triple("192.168.1.105", "93.184.216.34", NetworkProtocol.TCP to Pair(49200, 80)),
                Triple("8.8.8.8", "192.168.1.105", NetworkProtocol.ICMP to Pair(null, null)),
                Triple("192.168.1.105", "192.168.1.1", NetworkProtocol.UDP to Pair(5353, 5353))
            )
        }

        while (isRunning()) {
            val template = appSpecificTemplates[random.nextInt(appSpecificTemplates.size)]
            val (protocol, ports) = template.third
            val length = random.nextInt(64, 1460)
            val flags = when (protocol) {
                NetworkProtocol.TCP -> if (random.nextBoolean()) "[ACK, PSH]" else "[SYN, ACK]"
                NetworkProtocol.TLS -> "[ACK, PSH] App Data (Encrypted)"
                else -> null
            }

            val appLabel = targetApp?.appName ?: "System Process"
            val appPkg = targetApp?.packageName ?: "android.os.system"
            val appUid = targetApp?.uid ?: 1000

            val summary = when (protocol) {
                NetworkProtocol.TLS -> "TLSv1.3 [$appLabel] -> api.${appPkg.split(".").lastOrNull() ?: "service"}.com:443"
                NetworkProtocol.DNS -> "Standard query 0x${Random.nextInt(1000, 9999).toString(16)} A api.${appPkg.split(".").lastOrNull() ?: "cloud"}.net"
                NetworkProtocol.TCP -> "[$appLabel] Seq=${random.nextInt(10000, 99999)} Ack=${random.nextInt(10000, 99999)} Win=65535"
                NetworkProtocol.ICMP -> "Echo (ping) reply id=0x0100 seq=4 ttl=116"
                NetworkProtocol.UDP -> "[$appLabel] Telemetry / mDNS Broadcast"
                else -> "[$appLabel] IP Datagram length=$length"
            }

            val mockPayload = ByteArray(minOf(length, 64))
            random.nextBytes(mockPayload)

            val hexDump = bytesToHex(mockPayload)
            val asciiDump = bytesToAscii(mockPayload)

            val packet = DissectedPacket(
                id = packetId++,
                timestampMillis = System.currentTimeMillis(),
                sourceIp = template.first,
                destinationIp = template.second,
                sourcePort = ports.first,
                destinationPort = ports.second,
                protocol = protocol,
                packetLengthBytes = length,
                ttl = random.nextInt(48, 128),
                flags = flags,
                summary = summary,
                payloadPreviewHex = hexDump,
                payloadPreviewAscii = asciiDump,
                rawBytes = mockPayload,
                appName = appLabel,
                packageName = appPkg,
                uid = appUid
            )

            emit(packet)
            delay(random.nextLong(180, 480))
        }
    }.flowOn(Dispatchers.Default)

    fun createStandardPcap(packets: List<DissectedPacket>): ByteArray {
        val out = ByteArrayOutputStream()
        val dataOut = DataOutputStream(out)

        // PCAP Global Header (24 bytes)
        // Magic Number: 0xa1b2c3d4 (Standard microsecond resolution)
        val magic = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0xa1b2c3d4.toInt()).array()
        out.write(magic)

        // Major Version: 2, Minor Version: 4 (Little Endian)
        out.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(2).array())
        out.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(4).array())

        // Thiszone: 0, Sigfigs: 0
        out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array())
        out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array())

        // Snaplen: 65535
        out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(65535).array())

        // Network LinkType: 101 (LINKTYPE_RAW / IPv4 raw) or 1 (Ethernet)
        out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(101).array())

        // Write each packet record
        for (pkt in packets) {
            val tsSec = (pkt.timestampMillis / 1000).toInt()
            val tsUsec = ((pkt.timestampMillis % 1000) * 1000).toInt()
            val caplen = pkt.rawBytes.size
            val origlen = pkt.packetLengthBytes

            out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(tsSec).array())
            out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(tsUsec).array())
            out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(caplen).array())
            out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(origlen).array())

            out.write(pkt.rawBytes)
        }

        dataOut.flush()
        return out.toByteArray()
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.take(32).chunked(8).joinToString("\n") { chunk ->
            chunk.joinToString(" ") { String.format(java.util.Locale.US, "%02X", it) }
        }
    }

    private fun bytesToAscii(bytes: ByteArray): String {
        return bytes.take(32).chunked(16).joinToString("\n") { chunk ->
            chunk.map { b ->
                val c = b.toInt().toChar()
                if (c in ' '..'~') c else '.'
            }.joinToString("")
        }
    }
}
