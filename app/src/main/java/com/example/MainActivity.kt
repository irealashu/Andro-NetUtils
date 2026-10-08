package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainAuditViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                NetSentinelApp()
            }
        }
    }
}

data class NavItem(
    val title: String,
    val icon: ImageVector,
    val index: Int
)

val NAV_ITEMS = listOf(
    NavItem("Dashboard", Icons.Default.Dashboard, 0),
    NavItem("Port Scan", Icons.Default.TravelExplore, 1),
    NavItem("Inspect Site", Icons.Default.Language, 2),
    NavItem("HTTP Audit", Icons.Default.Http, 3),
    NavItem("DNS Suite", Icons.Default.Dns, 4),
    NavItem("Wireless", Icons.Default.Sensors, 5),
    NavItem("Packets", Icons.Default.BugReport, 6),
    NavItem("Traceroute", Icons.Default.Timeline, 7),
    NavItem("Ping Utils", Icons.Default.NetworkCheck, 8),
    NavItem("History", Icons.Default.History, 9),
    NavItem("Sandbox", Icons.Default.Security, 10)
)

@Composable
fun NetSentinelApp(viewModel: MainAuditViewModel = viewModel()) {
    val currentNavIndex by viewModel.currentNavIndex.collectAsStateWithLifecycle()

    // Handle back button on sub-screens to return to Dashboard
    BackHandler(enabled = currentNavIndex != 0) {
        viewModel.setNavIndex(0)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding(),
        containerColor = DarkBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            if (currentNavIndex != 0) {
                val currentTitle = when (currentNavIndex) {
                    1 -> "TCP Port Scanner & Subnet Sweep"
                    2 -> "Inspect Site & TLS Security"
                    3 -> "HTTP Defensive Header Auditor"
                    4 -> "DNS & DoH Security Suite"
                    5 -> "Wireless & Multi-Radio Spectrum"
                    6 -> "Packet Dissector & Live Capture"
                    7 -> "Traceroute & Hop Path Inspector"
                    8 -> "Ping Utilities & Latency Suite"
                    9 -> "Audit History & Security Reports"
                    10 -> "Security Architecture & Posture"
                    else -> "Audit Module"
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkSurface
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.setNavIndex(0) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurfaceVariant)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back to Home Dashboard",
                                        tint = CyberCyan
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = currentTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "NetSentinel Pro Security Tools",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.setNavIndex(0) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.15f), contentColor = CyberCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        HorizontalDivider(color = DarkBorder)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentNavIndex) {
                0 -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTab = { index -> viewModel.setNavIndex(index) }
                )
                1 -> PortScannerScreen(viewModel = viewModel)
                2 -> TlsInspectorScreen(viewModel = viewModel)
                3 -> HttpAuditorScreen(viewModel = viewModel)
                4 -> DnsSuiteScreen(viewModel = viewModel)
                5 -> WifiTelemetryScreen(viewModel = viewModel)
                6 -> PacketInspectorScreen(viewModel = viewModel)
                7 -> TracerouteScreen(viewModel = viewModel)
                8 -> PingUtilitiesScreen(viewModel = viewModel)
                9 -> HistoryAndReportsScreen(
                    viewModel = viewModel,
                    onNavigateToScan = { target, tab ->
                        viewModel.setPortScanTarget(target)
                        viewModel.setTlsTargetHost(target)
                        viewModel.setHttpTargetUrl("https://$target")
                        viewModel.setDnsDomain(target)
                        viewModel.setTracerouteTarget(target)
                        viewModel.setPingTarget(target)
                        viewModel.setNavIndex(tab)
                    }
                )
                10 -> PlatformArchitectureScreen(viewModel = viewModel)
                else -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTab = { index -> viewModel.setNavIndex(index) }
                )
            }
        }
    }
}
