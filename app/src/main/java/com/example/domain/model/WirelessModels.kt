package com.example.domain.model

import java.io.Serializable

data class CellularTelemetry(
    val isAvailable: Boolean,
    val operatorName: String,
    val networkGeneration: String, // "5G Standalone (SA)", "5G Non-Standalone (NSA)", "4G LTE-Advanced", "3G HSPA+"
    val dataActivity: String, // "Connected (Active)", "Dormant", "Disconnected"
    val simState: String, // "Ready (SIM 1 Active)", "No SIM", "Locked"
    val isRoaming: Boolean,
    val signalDbm: Int,
    val signalLevel: Int, // 0 to 4 bars
    val signalQualityPercent: Int,
    val cellBand: String, // "n78 (3500 MHz)", "B3 (1800 MHz)", etc.
    val apnName: String? = null,
    val ipAddress: String? = null
) : Serializable

data class BluetoothDeviceTelemetry(
    val name: String,
    val address: String, // MAC Address
    val rssiDbm: Int,
    val bondState: String, // "Bonded / Paired", "Available (Unpaired)", "Connecting"
    val isBle: Boolean,
    val txPowerLevel: Int? = null,
    val serviceUuids: List<String> = emptyList(),
    val deviceType: String = "BLE Peripheral" // "Audio Headset", "Smart Wearable", "Beacon", "IoT Peripheral"
) : Serializable

data class NfcTelemetry(
    val isSupported: Boolean,
    val isEnabled: Boolean,
    val supportedProtocols: List<String>, // "NFC-A (ISO 14443-3A)", "NFC-B", "NFC-F (FeliCa)", "NFC-V (ISO 15693)", "ISO-DEP (ISO 14443-4)", "NDEF"
    val isNdefReaderModeAvailable: Boolean,
    val isHostCardEmulationSupported: Boolean,
    val isSecureElementPresent: Boolean,
    val antennaStatus: String = "Active & Polling"
) : Serializable

data class UwbTelemetry(
    val isSupported: Boolean,
    val isRangingAvailable: Boolean,
    val supportedProfiles: List<String>, // "FiRa Consortium 2.0", "IEEE 802.15.4z", "Car Connectivity Consortium (CCC) Digital Key"
    val isAngleOfArrivalSupported: Boolean,
    val supportedChannels: List<Int> = listOf(5, 9), // Channel 5 (6.5 GHz), Channel 9 (8.0 GHz)
    val rangingPrecisionMm: Int = 10, // ~10 mm accuracy
    val chipVendor: String = "NXP / Qorvo Ultra-Wideband Radar"
) : Serializable

data class ScannedNfcTag(
    val tagUid: String,
    val tagType: String,
    val techList: List<String>,
    val ndefType: String,
    val payloadText: String,
    val rawBytesHex: String,
    val maxCapacityBytes: Int,
    val isWritable: Boolean,
    val isReadOnlyLocked: Boolean,
    val recordCount: Int = 1,
    val scannedAtTimestamp: Long = System.currentTimeMillis()
) : Serializable
