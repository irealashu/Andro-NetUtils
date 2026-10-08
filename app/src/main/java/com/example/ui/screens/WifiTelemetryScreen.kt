package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.*
import com.example.ui.MainAuditViewModel
import com.example.ui.components.TerminalCodeBlock
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiTelemetryScreen(viewModel: MainAuditViewModel) {
    val selectedRadioTab by viewModel.selectedWirelessSubTab.collectAsStateWithLifecycle()

    val radioTabs = listOf(
        Pair("Wi-Fi", Icons.Default.Wifi),
        Pair("Cellular 5G", Icons.Default.CellTower),
        Pair("Bluetooth BLE", Icons.Default.Bluetooth),
        Pair("NFC", Icons.Default.Nfc),
        Pair("UWB Radar", Icons.Default.Sensors),
        Pair("Network Utils", Icons.Default.Handyman)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Top Multi-Radio Selector Strip
        ScrollableTabRow(
            selectedTabIndex = selectedRadioTab,
            containerColor = DarkSurface,
            contentColor = CyberCyan,
            edgePadding = 12.dp,
            divider = { HorizontalDivider(color = DarkBorder) },
            indicator = { tabPositions ->
                if (selectedRadioTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedRadioTab]),
                        color = when (selectedRadioTab) {
                            0 -> CyberCyan
                            1 -> CyberGreen
                            2 -> CyberBlue
                            3 -> CyberAmber
                            4 -> CyberPurple
                            else -> CyberOrange
                        }
                    )
                }
            }
        ) {
            radioTabs.forEachIndexed { index, tab ->
                val isSelected = selectedRadioTab == index
                val tabColor = when (index) {
                    0 -> CyberCyan
                    1 -> CyberGreen
                    2 -> CyberBlue
                    3 -> CyberAmber
                    4 -> CyberPurple
                    else -> CyberOrange
                }
                Tab(
                    selected = isSelected,
                    onClick = { viewModel.setSelectedWirelessSubTab(index) },
                    modifier = Modifier.testTag("radio_tab_$index"),
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = tab.second,
                                contentDescription = tab.first,
                                tint = if (isSelected) tabColor else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.first,
                                color = if (isSelected) TextPrimary else TextMuted,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedRadioTab) {
                0 -> WifiTabContent(viewModel)
                1 -> CellularTabContent(viewModel)
                2 -> BluetoothTabContent(viewModel)
                3 -> NfcTabContent(viewModel)
                4 -> UwbTabContent(viewModel)
                5 -> NetworkUtilsTabContent(viewModel)
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 0: WI-FI SPECTRUM & SIGNAL
// -------------------------------------------------------------
@Composable
fun WifiTabContent(viewModel: MainAuditViewModel) {
    val telemetry by viewModel.wifiTelemetry.collectAsStateWithLifecycle()
    val spectrums by viewModel.wifiSpectrums.collectAsStateWithLifecycle()
    var selectedBandFilter by remember { mutableStateOf("2.4 GHz") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Wi-Fi Telemetry Hero
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(CyberCyan.copy(alpha = 0.5f), Color.Transparent))
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "WI-FI ENVIRONMENT & SIGNAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = telemetry?.ssid ?: "Scanning Network...",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        IconButton(onClick = { viewModel.refreshWifiTelemetry() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CyberCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Signal Quality Meter
                    val signalPercent = telemetry?.signalQualityPercent ?: 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Signal Strength", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(
                                    "${telemetry?.rssiDbm ?: 0} dBm ($signalPercent%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberGreen,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { signalPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (signalPercent > 60) CyberGreen else CyberAmber,
                                trackColor = DarkSurfaceContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Details Grid
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        WifiDetailItem(label = "Link Speed", value = "${telemetry?.linkSpeedMbps ?: 0} Mbps")
                        WifiDetailItem(label = "Frequency", value = "${telemetry?.frequencyMhz ?: 0} MHz")
                        WifiDetailItem(label = "Channel", value = "Ch ${telemetry?.channelNumber ?: 0}")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        WifiDetailItem(label = "Standard", value = telemetry?.standard ?: "Wi-Fi 6")
                        WifiDetailItem(label = "Security", value = telemetry?.securityType ?: "WPA2/WPA3")
                        WifiDetailItem(label = "BSSID", value = telemetry?.bssid?.take(14) ?: "Hidden")
                    }
                }
            }
        }

        // Channel Spectrum Visualizer
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CHANNEL CONGESTION SPECTRUM",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("2.4 GHz", "5 GHz").forEach { band ->
                                FilterChip(
                                    selected = selectedBandFilter == band,
                                    onClick = { selectedBandFilter = band },
                                    label = { Text(band, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                        selectedLabelColor = CyberCyan
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val filteredSpectrums = spectrums.filter { it.band == selectedBandFilter }

                    // Custom Canvas Spectrum Chart
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(DarkSurfaceContainer, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        val count = filteredSpectrums.size
                        if (count > 0) {
                            val barWidth = (size.width / count) * 0.7f
                            val spacing = (size.width / count) * 0.3f

                            filteredSpectrums.forEachIndexed { index, spec ->
                                val x = index * (barWidth + spacing) + spacing / 2
                                val barHeight = (spec.congestionLevel / 10f) * (size.height * 0.75f)
                                val y = size.height - barHeight

                                val barColor = when {
                                    spec.isCurrentChannel -> CyberCyan
                                    spec.congestionLevel > 6 -> CyberRed
                                    spec.congestionLevel > 4 -> CyberAmber
                                    else -> CyberGreen
                                }

                                drawRect(
                                    color = barColor.copy(alpha = if (spec.isCurrentChannel) 1.0f else 0.7f),
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        filteredSpectrums.take(7).forEach { spec ->
                            Text(
                                text = "Ch ${spec.channelNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (spec.isCurrentChannel) CyberCyan else TextMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (spec.isCurrentChannel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // IP Network & Routing
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "IP ADDRESSING & ROUTING",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberGreen,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        WifiDetailItem(label = "IPv4 Address", value = telemetry?.ipAddress ?: "0.0.0.0")
                        WifiDetailItem(label = "Default Gateway", value = telemetry?.gatewayIp ?: "0.0.0.0")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        WifiDetailItem(label = "Subnet Mask", value = telemetry?.netmask ?: "255.255.255.0")
                        WifiDetailItem(label = "DNS Resolvers", value = telemetry?.dnsServers?.firstOrNull() ?: "1.1.1.1")
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: CELLULAR / 5G TELEPHONY
// -------------------------------------------------------------
@Composable
fun CellularTabContent(viewModel: MainAuditViewModel) {
    val cell by viewModel.cellularTelemetry.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(CyberGreen.copy(alpha = 0.5f), Color.Transparent))
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CellTower, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MOBILE NETWORK TELEMETRY",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberGreen,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = { viewModel.refreshWirelessTelemetry() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CyberGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = cell?.operatorName ?: "Carrier Telemetry Active",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberGreen.copy(alpha = 0.15f))
                                .border(1.dp, CyberGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = cell?.networkGeneration ?: "5G Standalone (NR-SA)",
                                color = CyberGreen,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (cell?.isRoaming == true) "ROAMING" else "HOME NETWORK",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (cell?.isRoaming == true) CyberAmber else TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Signal Quality & RSRP Bar
                    val quality = cell?.signalQualityPercent ?: 80
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cell Signal Level (${cell?.signalLevel ?: 4}/4 bars)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("${cell?.signalDbm ?: -78} dBm ($quality%)", style = MaterialTheme.typography.labelSmall, color = CyberGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { quality / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = CyberGreen,
                        trackColor = DarkSurfaceContainer
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        WifiDetailItem(label = "Cell Band / ARFCN", value = cell?.cellBand ?: "Band n78 (3500 MHz)")
                        WifiDetailItem(label = "Data State", value = cell?.dataActivity ?: "Connected (Active)")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        WifiDetailItem(label = "SIM Subscription", value = cell?.simState ?: "SIM 1 Ready")
                        WifiDetailItem(label = "Default APN", value = cell?.apnName ?: "internet / IMS")
                    }
                }
            }
        }

        // Cellular Technical Details Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "5G / LTE ADVANCED RADIO PARAMETERS",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberGreen,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    listOf(
                        Pair("Carrier Aggregation (CA)", "Enabled (3x Downlink Component Carriers)"),
                        Pair("Modulation Scheme", "256-QAM Downlink / 64-QAM Uplink"),
                        Pair("MIMO Configuration", "4x4 Downlink MIMO / 2x2 Uplink"),
                        Pair("VoLTE / VoNR Voice Support", "Supported (High-Definition Codec EVS)"),
                        Pair("Emergency Broadcast (ETWS/CMAS)", "Active & Monitored")
                    ).forEach { (param, valStr) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(param, style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 11.sp)
                            Text(valStr, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: BLUETOOTH & BLE SCANNER
// -------------------------------------------------------------
@Composable
fun BluetoothTabContent(viewModel: MainAuditViewModel) {
    val devices by viewModel.bluetoothDevices.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(CyberBlue.copy(alpha = 0.5f), Color.Transparent))
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BLUETOOTH & BLE DISCOVERY",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberBlue,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${devices.size} Peripherals Discovered",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.clearBluetoothDevices() },
                            colors = ButtonDefaults.buttonColors(containerColor = TextMuted.copy(alpha = 0.15f), contentColor = TextMuted),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.refreshWirelessTelemetry() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberBlue.copy(alpha = 0.2f), contentColor = CyberBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rescan", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        if (devices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothDisabled,
                            contentDescription = null,
                            tint = CyberBlue,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Bluetooth Peripherals Discovered",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Bluetooth device list was cleared or no BLE devices are currently in range. Click Rescan below to scan for nearby peripherals.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.refreshWirelessTelemetry() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberBlue, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rescan Bluetooth Devices")
                        }
                    }
                }
            }
        }

        items(devices) { dev ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (dev.deviceType) {
                                    "Audio Headset" -> Icons.Default.Headphones
                                    "Smart Wearable" -> Icons.Default.Watch
                                    "iBeacon / Eddystone" -> Icons.Default.Sensors
                                    else -> Icons.Default.Bluetooth
                                },
                                contentDescription = null,
                                tint = CyberBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dev.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberBlue.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${dev.rssiDbm} dBm",
                                color = CyberBlue,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MAC: ${dev.address}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        Text(
                            text = dev.bondState,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (dev.bondState.contains("Bonded")) CyberGreen else TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    if (dev.serviceUuids.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "GATT Services: ${dev.serviceUuids.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: NFC PROTOCOLS & TAG READER
// -------------------------------------------------------------
@Composable
fun NfcTabContent(viewModel: MainAuditViewModel) {
    val context = LocalContext.current
    val nfc by viewModel.nfcTelemetry.collectAsStateWithLifecycle()
    val scannedTag by viewModel.scannedNfcTag.collectAsStateWithLifecycle()

    var ndefPayloadText by remember { mutableStateOf("https://github.com/google/ai-studio") }
    var selectedNdefType by remember { mutableStateOf("URL / Web Link") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // NFC Antenna & Active Tag Reader Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(CyberAmber.copy(alpha = 0.6f), Color.Transparent))
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Nfc, contentDescription = null, tint = CyberAmber, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "NFC TAG READER & POLING",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberAmber,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "13.56 MHz RFID Controller",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (nfc?.isEnabled == true) CyberGreen.copy(alpha = 0.15f) else CyberAmber.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (nfc?.isEnabled == true) "POLLING ACTIVE" else "STANDBY",
                                color = if (nfc?.isEnabled == true) CyberGreen else CyberAmber,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Bring any NFC Tag or Smart Card near the device back or select a tag preset below to read UID, technology stack, and NDEF records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "QUICK TAG READER PRESETS:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Button(
                                onClick = {
                                    viewModel.setScannedNfcTag(
                                        ScannedNfcTag(
                                            tagUid = "04:E2:4B:89:12:3A:80",
                                            tagType = "NTAG215 (NFC Forum Type 2)",
                                            techList = listOf("android.nfc.tech.NfcA", "android.nfc.tech.MifareUltralight", "android.nfc.tech.Ndef"),
                                            ndefType = "URI / Web Link",
                                            payloadText = "https://github.com/google/ai-studio",
                                            rawBytesHex = "D1 01 1E 55 04 67 69 74 68 75 62 2E 63 6F 6D 2F 67 6F 6F 67 6C 65 2F 61 69 2D 73 74 75 64 69 6F",
                                            maxCapacityBytes = 540,
                                            isWritable = true,
                                            isReadOnlyLocked = false,
                                            recordCount = 1
                                        )
                                    )
                                    Toast.makeText(context, "Scanned NTAG215 Tag", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberAmber.copy(alpha = 0.2f), contentColor = CyberAmber),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) {
                                Text("NTAG215 Sticker", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        item {
                            Button(
                                onClick = {
                                    viewModel.setScannedNfcTag(
                                        ScannedNfcTag(
                                            tagUid = "82:90:3F:11",
                                            tagType = "MIFARE Classic 1K",
                                            techList = listOf("android.nfc.tech.NfcA", "android.nfc.tech.MifareClassic"),
                                            ndefType = "Sector Encrypted Data",
                                            payloadText = "Sector 01: Key A Protected Access Control Block",
                                            rawBytesHex = "82 90 3F 11 B2 08 04 00 C8 32 10 EE CD A0 B1 B2",
                                            maxCapacityBytes = 1024,
                                            isWritable = false,
                                            isReadOnlyLocked = true,
                                            recordCount = 16
                                        )
                                    )
                                    Toast.makeText(context, "Scanned MIFARE Classic 1K", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberBlue.copy(alpha = 0.2f), contentColor = CyberBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) {
                                Text("MIFARE Classic 1K", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        item {
                            Button(
                                onClick = {
                                    viewModel.setScannedNfcTag(
                                        ScannedNfcTag(
                                            tagUid = "02:5D:80:C9:44:A1",
                                            tagType = "ISO 14443-4 Smart Card (ISO-DEP)",
                                            techList = listOf("android.nfc.tech.IsoDep", "android.nfc.tech.NfcA"),
                                            ndefType = "APDU Secure Chip",
                                            payloadText = "AID: A0000000041010 (Mastercard/Visa Payment / Identity APDU)",
                                            rawBytesHex = "6F 1A 84 0E A0 00 00 00 04 10 10 A5 08 50 06 4E 65 74 53 65 6E 90 00",
                                            maxCapacityBytes = 4096,
                                            isWritable = false,
                                            isReadOnlyLocked = true,
                                            recordCount = 1
                                        )
                                    )
                                    Toast.makeText(context, "Scanned ISO 14443-4 Smart Card", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPurple.copy(alpha = 0.2f), contentColor = CyberPurple),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) {
                                Text("Smart ID / Payment Card", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        item {
                            Button(
                                onClick = {
                                    viewModel.setScannedNfcTag(
                                        ScannedNfcTag(
                                            tagUid = "01:2E:4F:9A:88:12:00:80",
                                            tagType = "FeliCa Transit Pass (JIS X 6319-4)",
                                            techList = listOf("android.nfc.tech.NfcF"),
                                            ndefType = "System Code 122F Transit Log",
                                            payloadText = "System Code: 122F, Service Code: 090F (Stored Value Balance: 2,450 YEN)",
                                            rawBytesHex = "14 01 01 2E 4F 9A 88 12 00 80 00 00 12 2F 00 00",
                                            maxCapacityBytes = 2480,
                                            isWritable = true,
                                            isReadOnlyLocked = false,
                                            recordCount = 4
                                        )
                                    )
                                    Toast.makeText(context, "Scanned FeliCa Transit Card", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberGreen.copy(alpha = 0.2f), contentColor = CyberGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) {
                                Text("FeliCa Transit Pass", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Scanned Tag Result Details Card
        item {
            if (scannedTag != null) {
                val tag = scannedTag!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(CyberGreen.copy(alpha = 0.5f), Color.Transparent))
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SCANNED TAG IDENTIFIER (UID)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberGreen,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tag.tagUid,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = {
                                        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clip.setPrimaryClip(ClipData.newPlainText("NFC Tag UID", tag.tagUid))
                                        Toast.makeText(context, "Tag UID Copied to Clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy UID", tint = CyberCyan, modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = { viewModel.clearScannedNfcTag() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Tag", tint = TextMuted, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Tag Standard", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(tag.tagType, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Capacity & Status", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(
                                    if (tag.isReadOnlyLocked) "${tag.maxCapacityBytes}B (Read-Only)" else "${tag.maxCapacityBytes}B (Read-Write)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (tag.isReadOnlyLocked) CyberAmber else CyberGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("TECHNOLOGY STACK:", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            tag.techList.forEach { tech ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CyberGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(tech.removePrefix("android.nfc.tech."), color = CyberGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("NDEF PAYLOAD / APDU DATA:", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        TerminalCodeBlock(text = "${tag.ndefType}:\n${tag.payloadText}")

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("RAW HEX DUMP:", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tag.rawBytesHex,
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberGreenGlow,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Nfc, contentDescription = null, tint = CyberAmber, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Tag Currently Read", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tap an NFC Tag preset above or place a physical tag against your phone to view its UID, technical specs, and NDEF message payload.", style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }

        // Supported Standards Checklist
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SUPPORTED NFC STANDARDS MATRIX",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberAmber,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    nfc?.supportedProtocols?.forEach { proto ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(proto, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // NDEF Message Creator & Generator Tool
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "NDEF TAG PAYLOAD GENERATOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberAmber,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("URL / Web Link", "Plain Text", "vCard Contact", "MIME Payload").forEach { type ->
                            FilterChip(
                                selected = selectedNdefType == type,
                                onClick = {
                                    selectedNdefType = type
                                    ndefPayloadText = when (type) {
                                        "URL / Web Link" -> "https://github.com/google/ai-studio"
                                        "Plain Text" -> "NetSentinel Pro Security Key #8942"
                                        "vCard Contact" -> "BEGIN:VCARD\nVERSION:3.0\nN:Admin;NetSentinel\nTEL:+1-555-0199\nEND:VCARD"
                                        else -> "application/vnd.netsentinel.audit"
                                    }
                                },
                                label = { Text(type, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberAmber.copy(alpha = 0.2f),
                                    selectedLabelColor = CyberAmber
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = ndefPayloadText,
                        onValueChange = { ndefPayloadText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Payload Content", color = TextMuted, fontSize = 11.sp) },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberAmber,
                            unfocusedBorderColor = DarkBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clip.setPrimaryClip(ClipData.newPlainText("NDEF Payload", ndefPayloadText))
                            Toast.makeText(context, "NDEF Payload Prepared & Copied to Clipboard", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberAmber, contentColor = Color(0xFF332000)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Prepare NDEF Tag Record", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 4: ULTRA-WIDEBAND (UWB) RADAR & RANGING
// -------------------------------------------------------------
@Composable
fun UwbTabContent(viewModel: MainAuditViewModel) {
    val uwb by viewModel.uwbTelemetry.collectAsStateWithLifecycle()
    val targetDistanceMeters by remember { mutableFloatStateOf(1.85f) }
    val targetAzimuthAngleDeg by remember { mutableFloatStateOf(42f) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(CyberPurple.copy(alpha = 0.5f), Color.Transparent))
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Sensors, contentDescription = null, tint = CyberPurple, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ULTRA-WIDEBAND (UWB) RADAR",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberPurple,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberPurple.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "FiRa 2.0 / DS-TWR",
                                color = CyberPurple,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        WifiDetailItem(label = "Hardware Transceiver", value = uwb?.chipVendor?.take(22) ?: "SR100T Radar")
                        WifiDetailItem(label = "Precision", value = "±${uwb?.rangingPrecisionMm ?: 10} mm (~1 cm)")
                    }
                }
            }
        }

        // Radar Visualizer Canvas
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TIME-OF-FLIGHT & ANGLE OF ARRIVAL (AoA)",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberPurple,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Radar circular sweep canvas
                    Canvas(
                        modifier = Modifier
                            .size(200.dp)
                            .background(DarkBg, CircleShape)
                    ) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.width / 2 - 12.dp.toPx()

                        // Concentric range circles (1m, 2m, 3m)
                        drawCircle(color = CyberPurple.copy(alpha = 0.2f), radius = radius, center = center, style = Stroke(1.5f))
                        drawCircle(color = CyberPurple.copy(alpha = 0.15f), radius = radius * 0.66f, center = center, style = Stroke(1.5f))
                        drawCircle(color = CyberPurple.copy(alpha = 0.1f), radius = radius * 0.33f, center = center, style = Stroke(1.5f))

                        // Crosshairs
                        drawLine(color = CyberPurple.copy(alpha = 0.25f), start = Offset(center.x, 12.dp.toPx()), end = Offset(center.x, size.height - 12.dp.toPx()))
                        drawLine(color = CyberPurple.copy(alpha = 0.25f), start = Offset(12.dp.toPx(), center.y), end = Offset(size.width - 12.dp.toPx(), center.y))

                        // Target blip calculated from distance and azimuth angle
                        val rad = Math.toRadians((targetAzimuthAngleDeg - 90).toDouble())
                        val targetDistPx = (targetDistanceMeters / 3.0f).coerceIn(0.1f, 0.95f) * radius
                        val targetX = center.x + (targetDistPx * cos(rad)).toFloat()
                        val targetY = center.y + (targetDistPx * sin(rad)).toFloat()

                        drawCircle(color = CyberGreen, radius = 6.dp.toPx(), center = Offset(targetX, targetY))
                        drawCircle(color = CyberGreen.copy(alpha = 0.3f), radius = 12.dp.toPx(), center = Offset(targetX, targetY))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Ranging Distance", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = String.format(java.util.Locale.US, "%.2f m", targetDistanceMeters),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyberGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Azimuth AoA", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = "+${targetAzimuthAngleDeg.toInt()}°",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyberPurple,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RF Channel", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = "Ch 9 (8 GHz)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // UWB Profiles Info Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SUPPORTED UWB STANDARDS & PROFILES",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberPurple,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    uwb?.supportedProfiles?.forEach { prof ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberPurple, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(prof, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 5: COMPREHENSIVE NETWORK UTILITIES
// -------------------------------------------------------------
@Composable
fun NetworkUtilsTabContent(viewModel: MainAuditViewModel) {
    var selectedUtilSubTab by remember { mutableIntStateOf(0) } // 0: WoL, 1: CIDR, 2: mDNS, 3: DNS Race, 4: WHOIS, 5: HTTP Workbench

    val utilTabs = listOf("Wake-on-LAN", "CIDR Subnet", "mDNS Discovery", "DNS Race", "WHOIS", "HTTP Workbench")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(utilTabs.indices.toList()) { idx ->
                    FilterChip(
                        selected = selectedUtilSubTab == idx,
                        onClick = { selectedUtilSubTab = idx },
                        label = { Text(utilTabs[idx], fontSize = 11.sp, fontWeight = if (selectedUtilSubTab == idx) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberOrange.copy(alpha = 0.2f),
                            selectedLabelColor = CyberOrange
                        )
                    )
                }
            }
        }

        item {
            when (selectedUtilSubTab) {
                0 -> WolUtilView(viewModel)
                1 -> CidrUtilView(viewModel)
                2 -> MdnsUtilView(viewModel)
                3 -> DnsRaceUtilView(viewModel)
                4 -> WhoisUtilView(viewModel)
                5 -> HttpWorkbenchUtilView(viewModel)
            }
        }
    }
}

// Sub-Util: Wake on LAN
@Composable
fun WolUtilView(viewModel: MainAuditViewModel) {
    val macInput by viewModel.wolMacInput.collectAsStateWithLifecycle()
    val broadcastInput by viewModel.wolBroadcastInput.collectAsStateWithLifecycle()
    val result by viewModel.wolResult.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "WAKE-ON-LAN (WoL) MAGIC PACKET GENERATOR",
                style = MaterialTheme.typography.labelSmall,
                color = CyberOrange,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = macInput,
                onValueChange = { viewModel.setWolMac(it) },
                label = { Text("Target MAC Address (12 Hex)", color = TextMuted, fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberOrange, unfocusedBorderColor = DarkBorder)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = broadcastInput,
                onValueChange = { viewModel.setWolBroadcast(it) },
                label = { Text("Broadcast IP (e.g. 255.255.255.255 or 192.168.1.255)", color = TextMuted, fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberOrange, unfocusedBorderColor = DarkBorder)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { viewModel.sendWolMagicPacket() },
                colors = ButtonDefaults.buttonColors(containerColor = CyberOrange, contentColor = Color(0xFF3B1E00)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Broadcast WoL Magic Packet (UDP 9)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            result?.let { res ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (res.isSuccessful) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (res.isSuccessful) CyberGreen else CyberRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (res.isSuccessful) "Transmission Success" else "Transmission Error",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (res.isSuccessful) CyberGreen else CyberRed
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(res.message, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// Sub-Util: CIDR Calculator
@Composable
fun CidrUtilView(viewModel: MainAuditViewModel) {
    val cidrInput by viewModel.cidrInput.collectAsStateWithLifecycle()
    val calc by viewModel.cidrCalculation.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "CIDR & IPV4 SUBNET CALCULATOR",
                style = MaterialTheme.typography.labelSmall,
                color = CyberCyan,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = cidrInput,
                onValueChange = { viewModel.setCidrInput(it) },
                label = { Text("IP / CIDR Prefix (e.g. 192.168.1.0/24 or 10.0.0.0/16)", color = TextMuted, fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkBorder)
            )

            Spacer(modifier = Modifier.height(14.dp))

            listOf(
                Pair("Subnet Netmask", calc.netmask),
                Pair("Wildcard Mask", calc.wildcardMask),
                Pair("Network Address", calc.networkAddress),
                Pair("Broadcast Address", calc.broadcastAddress),
                Pair("Usable Host Range", "${calc.hostRangeStart} - ${calc.hostRangeEnd}"),
                Pair("Total / Usable Hosts", "${calc.totalHosts} total / ${calc.usableHosts} usable"),
                Pair("IP Class / Type", "${calc.ipClass} (${if (calc.isPrivate) "Private RFC1918" else "Public Internet"})"),
                Pair("Binary Netmask", calc.binaryNetmask),
                Pair("Hex Netmask", calc.hexNetmask)
            ).forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 11.sp)
                    Text(value, style = MaterialTheme.typography.bodySmall, color = CyberGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

// Sub-Util: mDNS IoT Discovery
@Composable
fun MdnsUtilView(viewModel: MainAuditViewModel) {
    val services by viewModel.mdnsServices.collectAsStateWithLifecycle()
    val isSearching by viewModel.isMdnsSearching.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "mDNS / SSDP LOCAL IOT DISCOVERY",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberGreen,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = { viewModel.startMdnsDiscovery() },
                    enabled = !isSearching,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = Color(0xFF003314)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color(0xFF003314), strokeWidth = 2.dp)
                    } else {
                        Text("Scan mDNS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (services.isEmpty()) {
                Text(
                    text = "Tap 'Scan mDNS' to discover local smart TVs, Chromecast, AirPlay, printers, and Zeroconf IoT nodes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            } else {
                services.forEach { srv ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(srv.serviceName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CyberGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(srv.category, color = CyberGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Text("Type: ${srv.serviceType} • ${srv.ipAddress}:${srv.port}", style = MaterialTheme.typography.bodySmall, color = CyberCyan, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

// Sub-Util: DNS Speed Race
@Composable
fun DnsRaceUtilView(viewModel: MainAuditViewModel) {
    val results by viewModel.dnsRaceResults.collectAsStateWithLifecycle()
    val isRunning by viewModel.isDnsRaceRunning.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DNS RESOLVER SPEED RACE",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberAmber,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = { viewModel.runDnsRace() },
                    enabled = !isRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAmber, contentColor = Color(0xFF332000)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color(0xFF332000), strokeWidth = 2.dp)
                    } else {
                        Text("Start Race", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (results.isEmpty()) {
                Text(
                    text = "Benchmark real query latency across Cloudflare (1.1.1.1), Google (8.8.8.8), Quad9 (9.9.9.9), AdGuard, OpenDNS, and NextDNS.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            } else {
                results.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(entry.resolverName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                if (entry.isFastest) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CyberGreen.copy(alpha = 0.18f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("FASTEST", color = CyberGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(entry.ipAddress, style = MaterialTheme.typography.labelSmall, color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }

                        Text(
                            text = "${entry.queryTimeMs} ms",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (entry.isFastest) CyberGreen else if (entry.queryTimeMs < 40) CyberCyan else CyberAmber
                        )
                    }
                }
            }
        }
    }
}

// Sub-Util: WHOIS & ASN
@Composable
fun WhoisUtilView(viewModel: MainAuditViewModel) {
    val target by viewModel.whoisTarget.collectAsStateWithLifecycle()
    val result by viewModel.whoisResult.collectAsStateWithLifecycle()
    val isLoading by viewModel.isWhoisLoading.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "WHOIS & ASN IP INTELLIGENCE",
                style = MaterialTheme.typography.labelSmall,
                color = CyberPurple,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = target,
                    onValueChange = { viewModel.setWhoisTarget(it) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Domain or IP (e.g. cloudflare.com)", color = TextMuted, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberPurple, unfocusedBorderColor = DarkBorder)
                )

                Button(
                    onClick = { viewModel.queryWhois() },
                    enabled = !isLoading && target.isNotBlank(),
                    modifier = Modifier.height(54.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPurple, contentColor = TextPrimary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Lookup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            result?.let { res ->
                Spacer(modifier = Modifier.height(14.dp))
                listOf(
                    Pair("Domain", res.domain),
                    Pair("Registrar", res.registrar),
                    Pair("Creation / Expiry", "${res.creationDate} → ${res.expirationDate}"),
                    Pair("ASN Org & Number", "${res.asnOrganization} (${res.asnNumber})"),
                    Pair("IP Range", res.ipRange),
                    Pair("Status / DNSSEC", "${res.status} • ${res.dnssec}")
                ).forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 11.sp)
                        Text(value, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// Sub-Util: HTTP / API Workbench
@Composable
fun HttpWorkbenchUtilView(viewModel: MainAuditViewModel) {
    val url by viewModel.workbenchUrl.collectAsStateWithLifecycle()
    val method by viewModel.workbenchMethod.collectAsStateWithLifecycle()
    val headers by viewModel.workbenchHeaders.collectAsStateWithLifecycle()
    val body by viewModel.workbenchBody.collectAsStateWithLifecycle()
    val response by viewModel.workbenchResponse.collectAsStateWithLifecycle()
    val isRunning by viewModel.isWorkbenchRunning.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "DEFENSIVE HTTP / REST / WEBSOCKET WORKBENCH",
                style = MaterialTheme.typography.labelSmall,
                color = CyberCyan,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("GET", "POST", "PUT", "DELETE", "HEAD").forEach { m ->
                    FilterChip(
                        selected = method == m,
                        onClick = { viewModel.setWorkbenchMethod(m) },
                        label = { Text(m, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                            selectedLabelColor = CyberCyan
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = url,
                onValueChange = { viewModel.setWorkbenchUrl(it) },
                label = { Text("Endpoint URL (e.g. https://httpbin.org/get)", color = TextMuted, fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkBorder)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = headers,
                onValueChange = { viewModel.setWorkbenchHeaders(it) },
                label = { Text("Headers (Key: Value)", color = TextMuted, fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkBorder)
            )

            if (method == "POST" || method == "PUT") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { viewModel.setWorkbenchBody(it) },
                    label = { Text("Request Body (JSON / Raw)", color = TextMuted, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = DarkBorder)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.executeWorkbenchRequest() },
                enabled = !isRunning && url.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF00363A)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF00363A), strokeWidth = 2.dp)
                } else {
                    Text("Execute $method Request", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            response?.let { res ->
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Status: ${res.statusCode} ${res.statusMessage}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (res.statusCode in 200..299) CyberGreen else CyberAmber
                            )
                            Text(
                                text = "${res.responseTimeMs} ms • ${res.payloadSizeBytes} B",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        TerminalCodeBlock(text = res.responseBody.take(600) + if (res.responseBody.length > 600) "..." else "")
                    }
                }
            }
        }
    }
}

@Composable
fun WifiDetailItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}
