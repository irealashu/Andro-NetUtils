package com.example.domain.engine

import com.example.domain.model.WolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class WolEngine {

    suspend fun sendMagicPacket(
        macAddress: String,
        broadcastIp: String = "255.255.255.255",
        port: Int = 9
    ): WolResult = withContext(Dispatchers.IO) {
        val cleanMac = macAddress.trim().replace(":", "").replace("-", "").replace(".", "")
        if (cleanMac.length != 12) {
            return@withContext WolResult(
                macAddress = macAddress,
                broadcastIp = broadcastIp,
                port = port,
                packetsSent = 0,
                isSuccessful = false,
                message = "Invalid MAC Address format. Must contain 12 hexadecimal characters (e.g. 00:11:22:33:44:55)."
            )
        }

        try {
            val macBytes = ByteArray(6)
            for (i in 0 until 6) {
                macBytes[i] = cleanMac.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }

            // Magic Packet: 6 bytes 0xFF + 16x MAC Address
            val magicPacket = ByteArray(6 + 16 * 6)
            for (i in 0 until 6) {
                magicPacket[i] = 0xFF.toByte()
            }
            for (i in 0 until 16) {
                System.arraycopy(macBytes, 0, magicPacket, 6 + i * 6, 6)
            }

            val socket = DatagramSocket()
            socket.broadcast = true
            val address = InetAddress.getByName(broadcastIp)
            val packet = DatagramPacket(magicPacket, magicPacket.size, address, port)

            // Send 3 duplicate bursts to guarantee reception across switches
            socket.send(packet)
            socket.send(packet)
            socket.send(packet)
            socket.close()

            WolResult(
                macAddress = macAddress,
                broadcastIp = broadcastIp,
                port = port,
                packetsSent = 3,
                isSuccessful = true,
                message = "Wake-on-LAN 102-byte Magic Packet transmitted (3 UDP bursts) to $broadcastIp:$port"
            )
        } catch (e: Exception) {
            WolResult(
                macAddress = macAddress,
                broadcastIp = broadcastIp,
                port = port,
                packetsSent = 0,
                isSuccessful = false,
                message = "Failed to transmit WoL Magic Packet: ${e.message ?: e.javaClass.simpleName}"
            )
        }
    }
}
