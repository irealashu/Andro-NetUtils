package com.example

import com.example.domain.engine.PacketDissectorEngine
import com.example.domain.model.CommonPortPresets
import com.example.domain.model.DissectedPacket
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.NetworkProtocol
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testCommonPortPresets() {
        val top20 = CommonPortPresets.TOP_20
        assertTrue(top20.ports.contains(80))
        assertTrue(top20.ports.contains(443))
        assertTrue(top20.ports.contains(22))
        assertEquals("SSH (Secure Shell)", CommonPortPresets.getServiceName(22))
        assertEquals("HTTPS (TLS Encrypted Web)", CommonPortPresets.getServiceName(443))
    }

    @Test
    fun testPcapGeneration() {
        val engine = PacketDissectorEngine()
        val packets = listOf(
            DissectedPacket(
                id = 1,
                timestampMillis = System.currentTimeMillis(),
                sourceIp = "192.168.1.100",
                destinationIp = "1.1.1.1",
                sourcePort = 54321,
                destinationPort = 53,
                protocol = NetworkProtocol.DNS,
                packetLengthBytes = 64,
                ttl = 64,
                flags = null,
                summary = "DNS standard query A google.com",
                payloadPreviewHex = "00 01 02 03",
                payloadPreviewAscii = "....",
                rawBytes = byteArrayOf(0, 1, 2, 3),
                appName = "Chrome Browser",
                packageName = "com.android.chrome",
                uid = 10045
            )
        )

        val pcapBytes = engine.createStandardPcap(packets)
        assertTrue(pcapBytes.size > 24) // Must contain at least 24-byte PCAP global header
    }

    @Test
    fun testAppSpecificTrafficModel() {
        val targetApp = InstalledAppInfo("Test App", "com.test.app", false, 10150, "1.0", 5)
        assertEquals("Test App", targetApp.appName)
        assertEquals("com.test.app", targetApp.packageName)
        assertEquals(10150, targetApp.uid)
    }
}
