package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AuditSessionEntity
import com.example.data.local.entity.TargetProfileEntity
import com.example.data.repository.AuditRepository
import com.example.domain.engine.*
import com.example.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class MainAuditViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AuditRepository(db.auditDao())

    // Core Engines
    private val portScannerEngine = PortScannerEngine()
    private val subnetEngine = SubnetDiscoveryEngine()
    private val tlsEngine = TlsInspectorEngine()
    private val httpEngine = HttpHeaderAuditorEngine()
    private val dnsEngine = DnsSuiteEngine()
    private val wifiEngine = WifiTelemetryEngine(application)
    private val tracerouteEngine = TracerouteEngine()
    private val packetEngine = PacketDissectorEngine()
    private val platformEngine = PlatformSecurityEngine(application)

    // New Network & Diagnostic Engines
    private val wirelessEngine = WirelessDiagnosticEngine(application)
    private val mdnsEngine = MdnsDiscoveryEngine()
    private val wolEngine = WolEngine()
    private val cidrEngine = CidrCalculatorEngine()
    private val dnsRaceEngine = DnsRaceEngine()
    private val httpWorkbenchEngine = HttpWorkbenchEngine()
    private val whoisEngine = WhoisEngine()
    private val pingEngine = PingEngine()

    // --- Ping Utilities State ---
    private val _pingType = MutableStateFlow(PingType.ICMP_ECHO)
    val pingType: StateFlow<PingType> = _pingType.asStateFlow()

    private val _pingTarget = MutableStateFlow("8.8.8.8")
    val pingTarget: StateFlow<String> = _pingTarget.asStateFlow()

    private val _pingPort = MutableStateFlow(80)
    val pingPort: StateFlow<Int> = _pingPort.asStateFlow()

    private val _pingCount = MutableStateFlow(10) // 0 = continuous
    val pingCount: StateFlow<Int> = _pingCount.asStateFlow()

    private val _pingPayloadBytes = MutableStateFlow(64)
    val pingPayloadBytes: StateFlow<Int> = _pingPayloadBytes.asStateFlow()

    private val _pingIntervalMs = MutableStateFlow(500L)
    val pingIntervalMs: StateFlow<Long> = _pingIntervalMs.asStateFlow()

    private val _pingTimeoutMs = MutableStateFlow(2000)
    val pingTimeoutMs: StateFlow<Int> = _pingTimeoutMs.asStateFlow()

    private val _pingTtl = MutableStateFlow(64)
    val pingTtl: StateFlow<Int> = _pingTtl.asStateFlow()

    private val _isPinging = MutableStateFlow(false)
    val isPinging: StateFlow<Boolean> = _isPinging.asStateFlow()

    private val _pingPackets = MutableStateFlow<List<PingPacket>>(emptyList())
    val pingPackets: StateFlow<List<PingPacket>> = _pingPackets.asStateFlow()

    private val _pingSummary = MutableStateFlow<PingSummary?>(null)
    val pingSummary: StateFlow<PingSummary?> = _pingSummary.asStateFlow()

    private val _subnetSweepHosts = MutableStateFlow<List<SubnetPingHost>>(emptyList())
    val subnetSweepHosts: StateFlow<List<SubnetPingHost>> = _subnetSweepHosts.asStateFlow()

    private val _isSubnetSweeping = MutableStateFlow(false)
    val isSubnetSweeping: StateFlow<Boolean> = _isSubnetSweeping.asStateFlow()

    private val _benchmarkTargets = MutableStateFlow<List<PingBenchmarkTarget>>(emptyList())
    val benchmarkTargets: StateFlow<List<PingBenchmarkTarget>> = _benchmarkTargets.asStateFlow()

    private val _isBenchmarking = MutableStateFlow(false)
    val isBenchmarking: StateFlow<Boolean> = _isBenchmarking.asStateFlow()

    private var activePingJob: Job? = null
    private var activeSweepJob: Job? = null

    fun setPingTarget(target: String) { _pingTarget.value = target }
    fun setPingType(type: PingType) { _pingType.value = type }
    fun setPingPort(port: Int) { _pingPort.value = port }
    fun setPingCount(count: Int) { _pingCount.value = count }
    fun setPingPayloadBytes(bytes: Int) { _pingPayloadBytes.value = bytes }
    fun setPingIntervalMs(interval: Long) { _pingIntervalMs.value = interval }
    fun setPingTimeoutMs(timeout: Int) { _pingTimeoutMs.value = timeout }
    fun setPingTtl(ttl: Int) { _pingTtl.value = ttl }

    fun clearPingLogs() {
        _pingPackets.value = emptyList()
        _pingSummary.value = null
    }

    fun startPing() {
        if (_isPinging.value) return
        _isPinging.value = true
        _pingPackets.value = emptyList()
        _pingSummary.value = null

        activePingJob = viewModelScope.launch {
            val target = _pingTarget.value
            val type = _pingType.value
            val count = _pingCount.value
            val interval = _pingIntervalMs.value
            val payload = _pingPayloadBytes.value
            val timeout = _pingTimeoutMs.value
            val ttl = _pingTtl.value
            val port = _pingPort.value

            var seq = 1
            val currentPackets = mutableListOf<PingPacket>()

            while (_isPinging.value) {
                val packet = when (type) {
                    PingType.ICMP_ECHO -> pingEngine.executeIcmpPing(
                        targetHost = target,
                        seqNumber = seq,
                        payloadBytes = payload,
                        timeoutMs = timeout,
                        ttl = ttl
                    )
                    PingType.TCP_SYN -> pingEngine.executeTcpPing(
                        targetHost = target,
                        port = port,
                        seqNumber = seq,
                        timeoutMs = timeout
                    )
                    PingType.HTTP_WEB -> pingEngine.executeHttpPing(
                        targetUrl = target,
                        seqNumber = seq,
                        timeoutMs = timeout
                    )
                    PingType.SUBNET_SWEEP -> pingEngine.executeIcmpPing(
                        targetHost = target,
                        seqNumber = seq,
                        payloadBytes = payload,
                        timeoutMs = timeout,
                        ttl = ttl
                    )
                }

                currentPackets.add(packet)
                _pingPackets.value = currentPackets.toList()
                val summary = pingEngine.calculateSummary(target, type, currentPackets)
                _pingSummary.value = summary

                seq++
                if (count > 0 && seq > count) {
                    break
                }

                kotlinx.coroutines.delay(interval)
            }

            _isPinging.value = false

            // Save session to Audit History
            _pingSummary.value?.let { summary ->
                savePingAuditToHistory(summary)
            }
        }
    }

    fun stopPing() {
        _isPinging.value = false
        activePingJob?.cancel()
        activePingJob = null
    }

    fun startSubnetSweep(subnetPrefix: String = "192.168.1") {
        if (_isSubnetSweeping.value) return
        _isSubnetSweeping.value = true
        _subnetSweepHosts.value = emptyList()

        activeSweepJob = viewModelScope.launch {
            pingEngine.sweepSubnetRange(
                subnetPrefix = subnetPrefix,
                startHost = 1,
                endHost = 254,
                timeoutMs = 300
            ) { host ->
                val current = _subnetSweepHosts.value.toMutableList()
                current.add(host)
                _subnetSweepHosts.value = current.sortedBy { it.ip.substringAfterLast(".").toIntOrNull() ?: 0 }
            }
            _isSubnetSweeping.value = false
        }
    }

    fun stopSubnetSweep() {
        _isSubnetSweeping.value = false
        activeSweepJob?.cancel()
        activeSweepJob = null
    }

    fun runPublicBenchmarks() {
        if (_isBenchmarking.value) return
        _isBenchmarking.value = true
        viewModelScope.launch {
            _benchmarkTargets.value = pingEngine.benchmarkPublicEndpoints()
            _isBenchmarking.value = false
        }
    }

    private fun savePingAuditToHistory(summary: PingSummary) {
        viewModelScope.launch {
            val details = "Sent: ${summary.packetsSent}, Recv: ${summary.packetsReceived}, Loss: ${summary.packetLossPercent.toInt()}%\n" +
                    "Min/Avg/Max/Jitter: ${summary.minLatencyMs}/${summary.avgLatencyMs}/${summary.maxLatencyMs}/${summary.jitterMs} ms"
            val grade = if (summary.packetLossPercent == 0f && summary.avgLatencyMs < 50) "A+" else if (summary.packetLossPercent < 10f) "B" else "C"
            repository.saveSession(
                target = summary.target,
                scanType = "PING_UTILITY",
                summary = details,
                scoreOrGrade = grade,
                detailsJson = "{\"target\":\"${summary.target}\",\"sent\":${summary.packetsSent},\"recv\":${summary.packetsReceived},\"avg\":${summary.avgLatencyMs}}"
            )
        }
    }

    // Navigation / Tab Selection
    private val _currentNavIndex = MutableStateFlow(0)
    val currentNavIndex: StateFlow<Int> = _currentNavIndex.asStateFlow()

    private val _selectedWirelessSubTab = MutableStateFlow(0) // 0: Wi-Fi, 1: Mobile/5G, 2: Bluetooth, 3: NFC, 4: UWB, 5: Network Utils
    val selectedWirelessSubTab: StateFlow<Int> = _selectedWirelessSubTab.asStateFlow()

    fun setNavIndex(index: Int) {
        _currentNavIndex.value = index
    }

    fun setSelectedWirelessSubTab(subTab: Int) {
        _selectedWirelessSubTab.value = subTab
    }

    fun navigateToWirelessSubTab(subTab: Int) {
        _selectedWirelessSubTab.value = subTab
        _currentNavIndex.value = 5
    }

    // --- Dashboard Quick Audit State ---
    private val _quickAuditTargetInput = MutableStateFlow("google.com")
    val quickAuditTargetInput: StateFlow<String> = _quickAuditTargetInput.asStateFlow()

    private val _isQuickAuditing = MutableStateFlow(false)
    val isQuickAuditing: StateFlow<Boolean> = _isQuickAuditing.asStateFlow()

    private val _quickAuditResult = MutableStateFlow<QuickAuditResult?>(null)
    val quickAuditResult: StateFlow<QuickAuditResult?> = _quickAuditResult.asStateFlow()

    fun setQuickAuditTarget(target: String) {
        _quickAuditTargetInput.value = target
    }

    fun runQuickAudit(targetInput: String? = null) {
        val target = (targetInput ?: _quickAuditTargetInput.value).trim()
            .removePrefix("https://").removePrefix("http://").removeSuffix("/")
        if (target.isBlank()) return

        _isQuickAuditing.value = true
        viewModelScope.launch {
            try {
                val findings = mutableListOf<String>()

                val pingDeferred = async(Dispatchers.IO) {
                    val start = System.currentTimeMillis()
                    var isReachable = false
                    try {
                        Socket().use { socket ->
                            socket.connect(InetSocketAddress(target, 443), 800)
                            isReachable = true
                        }
                    } catch (_: Exception) {
                        try {
                            isReachable = InetAddress.getByName(target).isReachable(600)
                        } catch (_: Exception) {}
                    }
                    val latency = (System.currentTimeMillis() - start).coerceAtLeast(1)
                    Pair(isReachable, latency)
                }

                val portsDeferred = async(Dispatchers.IO) {
                    val testPorts = listOf(80, 443, 8080, 8443, 22)
                    val openPorts = mutableListOf<Int>()
                    for (p in testPorts) {
                        try {
                            Socket().use { socket ->
                                socket.connect(InetSocketAddress(target, p), 350)
                                openPorts.add(p)
                            }
                        } catch (_: Exception) {}
                    }
                    openPorts
                }

                val tlsDeferred = async(Dispatchers.IO) {
                    try {
                        val tlsResult = tlsEngine.inspectTls(target, 443, 2500)
                        val leaf = tlsResult.certificates.firstOrNull()
                        leaf?.daysUntilExpiration
                    } catch (_: Exception) {
                        null
                    }
                }

                val httpDeferred = async(Dispatchers.IO) {
                    try {
                        val httpRes = httpEngine.auditUrl("https://$target")
                        httpRes.grade.letter
                    } catch (_: Exception) {
                        null
                    }
                }

                val (isPingable, latency) = pingDeferred.await()
                val openPorts = portsDeferred.await()
                val tlsExpiryDays = tlsDeferred.await()
                val httpGrade = httpDeferred.await()

                if (isPingable) {
                    findings.add("Endpoint responsive (${latency}ms round-trip latency)")
                } else {
                    findings.add("High latency or ICMP echo filtered by target firewall")
                }

                if (openPorts.contains(443)) {
                    findings.add("TLS Encrypted Port 443 Active")
                }
                if (openPorts.contains(80)) {
                    findings.add("Plaintext HTTP Port 80 Open (Ensure HTTPS 301 Redirect)")
                }

                if (tlsExpiryDays != null) {
                    if (tlsExpiryDays > 30) {
                        findings.add("TLS Certificate Valid ($tlsExpiryDays days remaining)")
                    } else if (tlsExpiryDays > 0) {
                        findings.add("WARNING: TLS Certificate expires in $tlsExpiryDays days")
                    } else {
                        findings.add("CRITICAL: TLS Certificate is EXPIRED")
                    }
                }

                if (httpGrade != null) {
                    findings.add("Defensive Header Posture: Grade $httpGrade")
                }

                val result = QuickAuditResult(
                    target = target,
                    isPingable = isPingable,
                    pingLatencyMs = latency,
                    openWebPorts = openPorts,
                    tlsExpiresInDays = tlsExpiryDays,
                    httpSecurityGrade = httpGrade,
                    summaryFindings = findings
                )
                _quickAuditResult.value = result

                repository.saveSession(
                    target = target,
                    scanType = "QUICK_AUDIT",
                    summary = "Ping ${latency}ms, Ports: $openPorts, TLS: ${tlsExpiryDays ?: 0}d, Grade: ${httpGrade ?: "N/A"}",
                    scoreOrGrade = httpGrade ?: "OK",
                    detailsJson = "Unified quick audit"
                )
            } finally {
                _isQuickAuditing.value = false
            }
        }
    }

    // --- Wireless Telemetry Suite (Cellular, Bluetooth, NFC, UWB) ---
    private val _cellularTelemetry = MutableStateFlow<CellularTelemetry?>(null)
    val cellularTelemetry: StateFlow<CellularTelemetry?> = _cellularTelemetry.asStateFlow()

    private val _bluetoothDevices = MutableStateFlow<List<BluetoothDeviceTelemetry>>(emptyList())
    val bluetoothDevices: StateFlow<List<BluetoothDeviceTelemetry>> = _bluetoothDevices.asStateFlow()

    private val _nfcTelemetry = MutableStateFlow<NfcTelemetry?>(null)
    val nfcTelemetry: StateFlow<NfcTelemetry?> = _nfcTelemetry.asStateFlow()

    private val _scannedNfcTag = MutableStateFlow<ScannedNfcTag?>(null)
    val scannedNfcTag: StateFlow<ScannedNfcTag?> = _scannedNfcTag.asStateFlow()

    private val _uwbTelemetry = MutableStateFlow<UwbTelemetry?>(null)
    val uwbTelemetry: StateFlow<UwbTelemetry?> = _uwbTelemetry.asStateFlow()

    fun refreshWirelessTelemetry() {
        viewModelScope.launch {
            _cellularTelemetry.value = wirelessEngine.getCellularTelemetry()
            _bluetoothDevices.value = wirelessEngine.getBluetoothDevices()
            _nfcTelemetry.value = wirelessEngine.getNfcTelemetry()
            _uwbTelemetry.value = wirelessEngine.getUwbTelemetry()
        }
    }

    fun clearBluetoothDevices() {
        _bluetoothDevices.value = emptyList()
    }

    fun setScannedNfcTag(tag: ScannedNfcTag?) {
        _scannedNfcTag.value = tag
    }

    fun clearScannedNfcTag() {
        _scannedNfcTag.value = null
    }

    // --- mDNS & SSDP IoT Discovery ---
    private val _mdnsServices = MutableStateFlow<List<MdnsDiscoveredService>>(emptyList())
    val mdnsServices: StateFlow<List<MdnsDiscoveredService>> = _mdnsServices.asStateFlow()

    private val _isMdnsSearching = MutableStateFlow(false)
    val isMdnsSearching: StateFlow<Boolean> = _isMdnsSearching.asStateFlow()

    fun startMdnsDiscovery() {
        _isMdnsSearching.value = true
        viewModelScope.launch {
            try {
                val services = mdnsEngine.discoverLocalServices()
                _mdnsServices.value = services
            } finally {
                _isMdnsSearching.value = false
            }
        }
    }

    // --- Wake-on-LAN (WoL) State ---
    private val _wolMacInput = MutableStateFlow("00:11:22:33:44:55")
    val wolMacInput: StateFlow<String> = _wolMacInput.asStateFlow()

    private val _wolBroadcastInput = MutableStateFlow("255.255.255.255")
    val wolBroadcastInput: StateFlow<String> = _wolBroadcastInput.asStateFlow()

    private val _wolResult = MutableStateFlow<WolResult?>(null)
    val wolResult: StateFlow<WolResult?> = _wolResult.asStateFlow()

    fun setWolMac(mac: String) { _wolMacInput.value = mac }
    fun setWolBroadcast(ip: String) { _wolBroadcastInput.value = ip }

    fun sendWolMagicPacket() {
        viewModelScope.launch {
            val res = wolEngine.sendMagicPacket(_wolMacInput.value, _wolBroadcastInput.value)
            _wolResult.value = res
        }
    }

    // --- CIDR Subnet Calculator State ---
    private val _cidrInput = MutableStateFlow("192.168.1.0/24")
    val cidrInput: StateFlow<String> = _cidrInput.asStateFlow()

    private val _cidrCalculation = MutableStateFlow(cidrEngine.calculateCidr("192.168.1.0/24"))
    val cidrCalculation: StateFlow<CidrCalculation> = _cidrCalculation.asStateFlow()

    fun setCidrInput(input: String) {
        _cidrInput.value = input
        _cidrCalculation.value = cidrEngine.calculateCidr(input)
    }

    // --- DNS Speed Race State ---
    private val _dnsRaceResults = MutableStateFlow<List<DnsRaceEntry>>(emptyList())
    val dnsRaceResults: StateFlow<List<DnsRaceEntry>> = _dnsRaceResults.asStateFlow()

    private val _isDnsRaceRunning = MutableStateFlow(false)
    val isDnsRaceRunning: StateFlow<Boolean> = _isDnsRaceRunning.asStateFlow()

    fun runDnsRace() {
        _isDnsRaceRunning.value = true
        viewModelScope.launch {
            try {
                val results = dnsRaceEngine.runDnsBenchmark()
                _dnsRaceResults.value = results
            } finally {
                _isDnsRaceRunning.value = false
            }
        }
    }

    // --- Defensive HTTP REST / WebSocket Workbench State ---
    private val _workbenchUrl = MutableStateFlow("https://httpbin.org/get")
    val workbenchUrl: StateFlow<String> = _workbenchUrl.asStateFlow()

    private val _workbenchMethod = MutableStateFlow("GET")
    val workbenchMethod: StateFlow<String> = _workbenchMethod.asStateFlow()

    private val _workbenchBody = MutableStateFlow("")
    val workbenchBody: StateFlow<String> = _workbenchBody.asStateFlow()

    private val _workbenchHeaders = MutableStateFlow("User-Agent: NetSentinel/1.0\nAccept: application/json")
    val workbenchHeaders: StateFlow<String> = _workbenchHeaders.asStateFlow()

    private val _workbenchResponse = MutableStateFlow<HttpWorkbenchResponse?>(null)
    val workbenchResponse: StateFlow<HttpWorkbenchResponse?> = _workbenchResponse.asStateFlow()

    private val _isWorkbenchRunning = MutableStateFlow(false)
    val isWorkbenchRunning: StateFlow<Boolean> = _isWorkbenchRunning.asStateFlow()

    fun setWorkbenchUrl(u: String) { _workbenchUrl.value = u }
    fun setWorkbenchMethod(m: String) { _workbenchMethod.value = m }
    fun setWorkbenchBody(b: String) { _workbenchBody.value = b }
    fun setWorkbenchHeaders(h: String) { _workbenchHeaders.value = h }

    fun executeWorkbenchRequest() {
        val headersMap = mutableMapOf<String, String>()
        _workbenchHeaders.value.lines().forEach { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) headersMap[parts[0].trim()] = parts[1].trim()
        }

        val req = HttpWorkbenchRequest(
            url = _workbenchUrl.value,
            method = _workbenchMethod.value,
            headers = headersMap,
            body = if (_workbenchBody.value.isNotBlank()) _workbenchBody.value else null
        )

        _isWorkbenchRunning.value = true
        viewModelScope.launch {
            try {
                val res = httpWorkbenchEngine.executeRequest(req)
                _workbenchResponse.value = res
            } finally {
                _isWorkbenchRunning.value = false
            }
        }
    }

    // --- WHOIS & ASN Intelligence State ---
    private val _whoisTarget = MutableStateFlow("cloudflare.com")
    val whoisTarget: StateFlow<String> = _whoisTarget.asStateFlow()

    private val _whoisResult = MutableStateFlow<WhoisResult?>(null)
    val whoisResult: StateFlow<WhoisResult?> = _whoisResult.asStateFlow()

    private val _isWhoisLoading = MutableStateFlow(false)
    val isWhoisLoading: StateFlow<Boolean> = _isWhoisLoading.asStateFlow()

    fun setWhoisTarget(t: String) { _whoisTarget.value = t }

    fun queryWhois() {
        _isWhoisLoading.value = true
        viewModelScope.launch {
            try {
                val res = whoisEngine.queryWhois(_whoisTarget.value)
                _whoisResult.value = res
            } finally {
                _isWhoisLoading.value = false
            }
        }
    }

    // --- Port Scanner & Subnet Discovery State ---
    private val _portScanTarget = MutableStateFlow("scanme.nmap.org")
    val portScanTarget: StateFlow<String> = _portScanTarget.asStateFlow()

    private val _selectedPreset = MutableStateFlow(CommonPortPresets.TOP_20)
    val selectedPreset: StateFlow<PortPreset> = _selectedPreset.asStateFlow()

    private val _customPortsInput = MutableStateFlow("")
    val customPortsInput: StateFlow<String> = _customPortsInput.asStateFlow()

    private val _portScanTimeoutMs = MutableStateFlow(400)
    val portScanTimeoutMs: StateFlow<Int> = _portScanTimeoutMs.asStateFlow()

    private val _portScanConcurrency = MutableStateFlow(25)
    val portScanConcurrency: StateFlow<Int> = _portScanConcurrency.asStateFlow()

    private val _isPortScanning = MutableStateFlow(false)
    val isPortScanning: StateFlow<Boolean> = _isPortScanning.asStateFlow()

    private val _portScanProgress = MutableStateFlow(Pair(0, 0))
    val portScanProgress: StateFlow<Pair<Int, Int>> = _portScanProgress.asStateFlow()

    private val _portScanResults = MutableStateFlow<List<PortScanResult>>(emptyList())
    val portScanResults: StateFlow<List<PortScanResult>> = _portScanResults.asStateFlow()

    private val _isSubnetScanning = MutableStateFlow(false)
    val isSubnetScanning: StateFlow<Boolean> = _isSubnetScanning.asStateFlow()

    private val _subnetHosts = MutableStateFlow<List<DiscoveredHost>>(emptyList())
    val subnetHosts: StateFlow<List<DiscoveredHost>> = _subnetHosts.asStateFlow()

    private val _subnetProgress = MutableStateFlow(Pair(0, 254))
    val subnetProgress: StateFlow<Pair<Int, Int>> = _subnetProgress.asStateFlow()

    private var portScanJob: Job? = null
    private var subnetJob: Job? = null

    fun setPortScanTarget(target: String) { _portScanTarget.value = target }
    fun setSelectedPreset(preset: PortPreset) { _selectedPreset.value = preset }
    fun setCustomPortsInput(input: String) { _customPortsInput.value = input }
    fun setPortScanTimeout(ms: Int) { _portScanTimeoutMs.value = ms }
    fun setPortScanConcurrency(c: Int) { _portScanConcurrency.value = c }

    fun startPortScan() {
        val target = _portScanTarget.value.trim()
        if (target.isBlank()) return

        val ports = if (_customPortsInput.value.isNotBlank()) {
            _customPortsInput.value.split(",", " ", ";")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..65535 }
                .distinct()
                .ifEmpty { _selectedPreset.value.ports }
        } else {
            _selectedPreset.value.ports
        }

        portScanJob?.cancel()
        _isPortScanning.value = true
        _portScanResults.value = emptyList()
        _portScanProgress.value = Pair(0, ports.size)

        portScanJob = viewModelScope.launch {
            try {
                val results = portScannerEngine.scanPorts(
                    host = target,
                    ports = ports,
                    timeoutMs = _portScanTimeoutMs.value,
                    maxConcurrency = _portScanConcurrency.value,
                    grabBanners = true,
                    onProgress = { scanned, total, _ ->
                        _portScanProgress.value = Pair(scanned, total)
                    }
                )
                _portScanResults.value = results

                val openCount = results.count { it.isOpen }
                val summary = "Target: $target, Open: $openCount / ${ports.size} ports scanned"
                repository.saveSession(
                    target = target,
                    scanType = "PORT_SCAN",
                    summary = summary,
                    scoreOrGrade = "$openCount Open",
                    detailsJson = "${results.size} ports"
                )
            } finally {
                _isPortScanning.value = false
            }
        }
    }

    fun stopPortScan() {
        portScanJob?.cancel()
        _isPortScanning.value = false
    }

    fun startSubnetDiscovery() {
        subnetJob?.cancel()
        _isSubnetScanning.value = true
        _subnetHosts.value = emptyList()
        _subnetProgress.value = Pair(0, 254)

        subnetJob = viewModelScope.launch {
            try {
                val hosts = subnetEngine.discoverLocalSubnet(
                    onProgress = { scanned, total ->
                        _subnetProgress.value = Pair(scanned, total)
                    }
                )
                _subnetHosts.value = hosts
                repository.saveSession(
                    target = "Local Subnet (/24)",
                    scanType = "PORT_SCAN",
                    summary = "Discovered ${hosts.size} active hosts on local network",
                    scoreOrGrade = "${hosts.size} Active",
                    detailsJson = "Subnet host scan"
                )
            } finally {
                _isSubnetScanning.value = false
            }
        }
    }

    // --- Unified Site Inspector State ("Enter URL & Know Everything") ---
    private val _siteInspectUrl = MutableStateFlow("https://google.com")
    val siteInspectUrl: StateFlow<String> = _siteInspectUrl.asStateFlow()

    private val _isSiteInspectLoading = MutableStateFlow(false)
    val isSiteInspectLoading: StateFlow<Boolean> = _isSiteInspectLoading.asStateFlow()

    private val _siteInspectResult = MutableStateFlow<SiteInspectResult?>(null)
    val siteInspectResult: StateFlow<SiteInspectResult?> = _siteInspectResult.asStateFlow()

    fun setSiteInspectUrl(url: String) {
        _siteInspectUrl.value = url
        val cleanHost = url.trim().removePrefix("https://").removePrefix("http://").split("/").firstOrNull()?.split(":")?.firstOrNull() ?: url
        _tlsTargetHost.value = cleanHost
        _httpTargetUrl.value = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
        _dnsDomain.value = cleanHost
    }

    fun startSiteInspection(urlInput: String = _siteInspectUrl.value) {
        val trimmed = urlInput.trim()
        if (trimmed.isBlank()) return

        val normalizedUrl = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }
        _siteInspectUrl.value = normalizedUrl

        val cleanHost = try {
            val uri = java.net.URI(normalizedUrl)
            uri.host ?: trimmed.removePrefix("https://").removePrefix("http://").split("/").first().split(":").first()
        } catch (_: Exception) {
            trimmed.removePrefix("https://").removePrefix("http://").split("/").first().split(":").first()
        }

        val port = try {
            val uri = java.net.URI(normalizedUrl)
            if (uri.port > 0) uri.port else if (normalizedUrl.startsWith("http://")) 80 else 443
        } catch (_: Exception) { 443 }

        _isSiteInspectLoading.value = true

        viewModelScope.launch {
            val startMs = System.currentTimeMillis()
            try {
                val httpJob = async { try { httpEngine.auditUrl(normalizedUrl) } catch (_: Exception) { null } }
                val tlsJob = async { try { tlsEngine.inspectTls(cleanHost, port) } catch (_: Exception) { null } }
                val dnsJob = async { try { dnsEngine.queryDoh(cleanHost, DnsRecordType.A) } catch (_: Exception) { null } }
                val whoisJob = async { try { whoisEngine.queryWhois(cleanHost) } catch (_: Exception) { null } }
                val portJob = async { try { portScannerEngine.scanPorts(cleanHost, listOf(80, 443, 8080, 8443)).filter { it.isOpen }.map { it.port } } catch (_: Exception) { emptyList() } }

                val http = httpJob.await()
                val tls = tlsJob.await()
                val dns = dnsJob.await()
                val whois = whoisJob.await()
                val openPorts = portJob.await()
                val totalTimeMs = System.currentTimeMillis() - startMs

                val resolvedIp = http?.ipAddress ?: dns?.records?.firstOrNull { it.type == DnsRecordType.A }?.value ?: try { java.net.InetAddress.getByName(cleanHost).hostAddress } catch (_: Exception) { null }

                val inspectResult = SiteInspectResult(
                    url = normalizedUrl,
                    host = cleanHost,
                    port = port,
                    ipAddress = resolvedIp,
                    pingLatencyMs = totalTimeMs,
                    httpAudit = http,
                    tlsAudit = tls,
                    dnsAudit = dns,
                    whoisAudit = whois,
                    openWebPorts = openPorts
                )

                _siteInspectResult.value = inspectResult
                _tlsResult.value = tls
                _httpAuditResult.value = http
                if (dns != null) _dnsResult.value = dns

                repository.saveSession(
                    target = normalizedUrl,
                    scanType = "SITE_INSPECT",
                    summary = "HTTP ${http?.responseCode ?: "N/A"} • TLS ${tls?.negotiatedProtocol ?: "N/A"} • Grade ${http?.grade?.letter ?: "PASS"}",
                    scoreOrGrade = http?.grade?.letter ?: "PASS",
                    detailsJson = "Full Site Inspection ($cleanHost)"
                )
            } finally {
                _isSiteInspectLoading.value = false
            }
        }
    }

    // --- TLS / SSL Inspector State ---
    private val _tlsTargetHost = MutableStateFlow("google.com")
    val tlsTargetHost: StateFlow<String> = _tlsTargetHost.asStateFlow()

    private val _tlsTargetPort = MutableStateFlow(443)
    val tlsTargetPort: StateFlow<Int> = _tlsTargetPort.asStateFlow()

    private val _isTlsLoading = MutableStateFlow(false)
    val isTlsLoading: StateFlow<Boolean> = _isTlsLoading.asStateFlow()

    private val _tlsResult = MutableStateFlow<TlsAuditResult?>(null)
    val tlsResult: StateFlow<TlsAuditResult?> = _tlsResult.asStateFlow()

    fun setTlsTargetHost(host: String) { _tlsTargetHost.value = host }
    fun setTlsTargetPort(port: Int) { _tlsTargetPort.value = port }

    fun startTlsInspection() {
        val host = _tlsTargetHost.value.trim()
        if (host.isBlank()) return

        _isTlsLoading.value = true
        viewModelScope.launch {
            try {
                val result = tlsEngine.inspectTls(host, _tlsTargetPort.value)
                _tlsResult.value = result

                val primaryCert = result.certificates.firstOrNull()
                val grade = if (result.isTrusted && !result.certificates.any { it.isExpired }) "VALID" else "WARNING"
                val summary = "Protocol: ${result.negotiatedProtocol}, Exp: ${primaryCert?.daysUntilExpiration ?: 0}d, Warnings: ${result.securityWarnings.size}"

                repository.saveSession(
                    target = "$host:${_tlsTargetPort.value}",
                    scanType = "TLS_AUDIT",
                    summary = summary,
                    scoreOrGrade = grade,
                    detailsJson = result.negotiatedCipherSuite
                )
            } finally {
                _isTlsLoading.value = false
            }
        }
    }

    // --- HTTP Defensive Header Auditor State ---
    private val _httpTargetUrl = MutableStateFlow("https://github.com")
    val httpTargetUrl: StateFlow<String> = _httpTargetUrl.asStateFlow()

    private val _isHttpAuditing = MutableStateFlow(false)
    val isHttpAuditing: StateFlow<Boolean> = _isHttpAuditing.asStateFlow()

    private val _httpAuditResult = MutableStateFlow<HttpSecurityAudit?>(null)
    val httpAuditResult: StateFlow<HttpSecurityAudit?> = _httpAuditResult.asStateFlow()

    fun setHttpTargetUrl(url: String) { _httpTargetUrl.value = url }

    fun startHttpAudit() {
        val url = _httpTargetUrl.value.trim()
        if (url.isBlank()) return

        _isHttpAuditing.value = true
        viewModelScope.launch {
            try {
                val result = httpEngine.auditUrl(url)
                _httpAuditResult.value = result

                repository.saveSession(
                    target = result.url,
                    scanType = "HTTP_HEADER_AUDIT",
                    summary = "Score: ${result.totalScore}/100, HTTP ${result.responseCode}, ${result.headerChecks.count { it.status == HeaderStatus.SECURE }} Passed",
                    scoreOrGrade = result.grade.letter,
                    detailsJson = "Audited ${result.headerChecks.size} headers"
                )
            } finally {
                _isHttpAuditing.value = false
            }
        }
    }

    // --- DNS Suite State ---
    private val _dnsDomain = MutableStateFlow("cloudflare.com")
    val dnsDomain: StateFlow<String> = _dnsDomain.asStateFlow()

    private val _selectedDnsRecordType = MutableStateFlow(DnsRecordType.A)
    val selectedDnsRecordType: StateFlow<DnsRecordType> = _selectedDnsRecordType.asStateFlow()

    private val _selectedDohProvider = MutableStateFlow(DohProvider.CLOUDFLARE)
    val selectedDohProvider: StateFlow<DohProvider> = _selectedDohProvider.asStateFlow()

    private val _isDnsLoading = MutableStateFlow(false)
    val isDnsLoading: StateFlow<Boolean> = _isDnsLoading.asStateFlow()

    private val _dnsResult = MutableStateFlow<DnsQueryResult?>(null)
    val dnsResult: StateFlow<DnsQueryResult?> = _dnsResult.asStateFlow()

    fun setDnsDomain(domain: String) { _dnsDomain.value = domain }
    fun setSelectedDnsRecordType(type: DnsRecordType) { _selectedDnsRecordType.value = type }
    fun setSelectedDohProvider(p: DohProvider) { _selectedDohProvider.value = p }

    fun startDnsQuery() {
        val domain = _dnsDomain.value.trim()
        if (domain.isBlank()) return

        _isDnsLoading.value = true
        viewModelScope.launch {
            try {
                val result = dnsEngine.queryDoh(domain, _selectedDnsRecordType.value, _selectedDohProvider.value)
                _dnsResult.value = result

                repository.saveSession(
                    target = domain,
                    scanType = "DNS_INSPECT",
                    summary = "${_selectedDnsRecordType.value.name} query via ${_selectedDohProvider.value.providerName} (${result.records.size} records, ${result.queryTimeMs}ms)",
                    scoreOrGrade = "${result.records.size} Recs",
                    detailsJson = "${result.records.size} records"
                )
            } finally {
                _isDnsLoading.value = false
            }
        }
    }

    // --- Wi-Fi & Network Telemetry State ---
    private val _wifiTelemetry = MutableStateFlow<WifiTelemetry?>(null)
    val wifiTelemetry: StateFlow<WifiTelemetry?> = _wifiTelemetry.asStateFlow()

    private val _wifiSpectrums = MutableStateFlow<List<WifiChannelSpectrum>>(emptyList())
    val wifiSpectrums: StateFlow<List<WifiChannelSpectrum>> = _wifiSpectrums.asStateFlow()

    private val _isRefreshingTelemetry = MutableStateFlow(false)
    val isRefreshingTelemetry: StateFlow<Boolean> = _isRefreshingTelemetry.asStateFlow()

    fun refreshWifiTelemetry() {
        _isRefreshingTelemetry.value = true
        viewModelScope.launch {
            try {
                val tel = wifiEngine.getWifiTelemetryAsync()
                _wifiTelemetry.value = tel
                _wifiSpectrums.value = wifiEngine.getChannelSpectrums(tel.channelNumber)
            } finally {
                _isRefreshingTelemetry.value = false
            }
        }
    }

    // --- Traceroute State ---
    private val _tracerouteTarget = MutableStateFlow("1.1.1.1")
    val tracerouteTarget: StateFlow<String> = _tracerouteTarget.asStateFlow()

    private val _isTracerouteRunning = MutableStateFlow(false)
    val isTracerouteRunning: StateFlow<Boolean> = _isTracerouteRunning.asStateFlow()

    private val _tracerouteHops = MutableStateFlow<List<TracerouteHop>>(emptyList())
    val tracerouteHops: StateFlow<List<TracerouteHop>> = _tracerouteHops.asStateFlow()

    private var tracerouteJob: Job? = null

    fun setTracerouteTarget(t: String) { _tracerouteTarget.value = t }

    fun startTraceroute() {
        val target = _tracerouteTarget.value.trim()
        if (target.isBlank()) return

        tracerouteJob?.cancel()
        _isTracerouteRunning.value = true
        _tracerouteHops.value = emptyList()

        tracerouteJob = viewModelScope.launch {
            try {
                tracerouteEngine.executeTrace(target).collect { hop ->
                    _tracerouteHops.value = _tracerouteHops.value + hop
                }

                repository.saveSession(
                    target = target,
                    scanType = "TRACEROUTE",
                    summary = "Traceroute completed: ${_tracerouteHops.value.size} hops resolved",
                    scoreOrGrade = "${_tracerouteHops.value.size} Hops",
                    detailsJson = "Traceroute hops"
                )
            } finally {
                _isTracerouteRunning.value = false
            }
        }
    }

    fun stopTraceroute() {
        tracerouteJob?.cancel()
        _isTracerouteRunning.value = false
    }

    // --- App-Specific Packet Capture & Traffic Dissector State ---
    private val _isPacketCapturing = MutableStateFlow(false)
    val isPacketCapturing: StateFlow<Boolean> = _isPacketCapturing.asStateFlow()

    private val _capturedPackets = MutableStateFlow<List<DissectedPacket>>(emptyList())
    val capturedPackets: StateFlow<List<DissectedPacket>> = _capturedPackets.asStateFlow()

    private val _trafficStats = MutableStateFlow(TrafficStats())
    val trafficStats: StateFlow<TrafficStats> = _trafficStats.asStateFlow()

    private val _selectedPacket = MutableStateFlow<DissectedPacket?>(null)
    val selectedPacket: StateFlow<DissectedPacket?> = _selectedPacket.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _selectedTargetApp = MutableStateFlow<InstalledAppInfo?>(null)
    val selectedTargetApp: StateFlow<InstalledAppInfo?> = _selectedTargetApp.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _appEndpoints = MutableStateFlow<List<AppEndpoint>>(emptyList())
    val appEndpoints: StateFlow<List<AppEndpoint>> = _appEndpoints.asStateFlow()

    private var packetJob: Job? = null

    fun selectPacket(p: DissectedPacket?) {
        _selectedPacket.value = p
    }

    fun setSelectedTargetApp(app: InstalledAppInfo?) {
        _selectedTargetApp.value = app
        if (_isPacketCapturing.value) {
            togglePacketCapture()
            togglePacketCapture()
        }
    }

    fun loadInstalledApps() {
        _isLoadingApps.value = true
        viewModelScope.launch {
            try {
                val apps = packetEngine.getInstalledApplications(getApplication())
                _installedApps.value = apps
            } finally {
                _isLoadingApps.value = false
            }
        }
    }

    fun togglePacketCapture() {
        if (_isPacketCapturing.value) {
            _isPacketCapturing.value = false
            packetJob?.cancel()
        } else {
            _isPacketCapturing.value = true
            packetJob?.cancel()
            packetJob = viewModelScope.launch {
                val targetApp = _selectedTargetApp.value
                packetEngine.generatePacketStream(targetApp = targetApp) { _isPacketCapturing.value }.collect { packet ->
                    val updated = (listOf(packet) + _capturedPackets.value).take(200)
                    _capturedPackets.value = updated

                    val total = updated.size.toLong()
                    val totalBytes = updated.sumOf { it.packetLengthBytes.toLong() }
                    val tcpCount = updated.count { it.protocol == NetworkProtocol.TCP || it.protocol == NetworkProtocol.TLS || it.protocol == NetworkProtocol.HTTP }.toLong()
                    val udpCount = updated.count { it.protocol == NetworkProtocol.UDP }.toLong()
                    val dnsCount = updated.count { it.protocol == NetworkProtocol.DNS }.toLong()
                    val icmpCount = updated.count { it.protocol == NetworkProtocol.ICMP }.toLong()

                    _trafficStats.value = TrafficStats(
                        totalPackets = total,
                        totalBytes = totalBytes,
                        tcpPackets = tcpCount,
                        udpPackets = udpCount,
                        icmpPackets = icmpCount,
                        dnsPackets = dnsCount,
                        bytesPerSec = totalBytes * 3,
                        isCapturing = true,
                        targetApp = targetApp
                    )

                    val endpoints = updated.groupBy { "${it.destinationIp}:${it.destinationPort ?: 0}" }.map { (key, pkts) ->
                        val first = pkts.first()
                        AppEndpoint(
                            hostOrIp = first.destinationIp,
                            port = first.destinationPort ?: 0,
                            protocol = first.protocol,
                            packetCount = pkts.size,
                            bytesTransferred = pkts.sumOf { it.packetLengthBytes.toLong() },
                            lastSeenTimestamp = pkts.maxOf { it.timestampMillis }
                        )
                    }.sortedByDescending { it.packetCount }

                    _appEndpoints.value = endpoints
                }
            }
        }
    }

    fun clearPackets() {
        _capturedPackets.value = emptyList()
        _selectedPacket.value = null
        _appEndpoints.value = emptyList()
        _trafficStats.value = TrafficStats(targetApp = _selectedTargetApp.value)
    }

    fun exportPcapBytes(): ByteArray {
        return packetEngine.createStandardPcap(_capturedPackets.value)
    }

    // --- Platform Security & Sandboxing Matrix State ---
    private val _platformReport = MutableStateFlow<PlatformSecurityReport?>(null)
    val platformReport: StateFlow<PlatformSecurityReport?> = _platformReport.asStateFlow()

    fun refreshPlatformReport() {
        _platformReport.value = platformEngine.getPlatformSecurityReport()
    }

    // --- History & Saved Target Profiles ---
    val allAuditSessions: StateFlow<List<AuditSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTargetProfiles: StateFlow<List<TargetProfileEntity>> = repository.allTargets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(session: AuditSessionEntity) {
        viewModelScope.launch { repository.toggleFavorite(session) }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch { repository.deleteSession(id) }
    }

    fun clearAllHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    fun addTargetProfile(name: String, host: String, desc: String, category: String) {
        viewModelScope.launch {
            repository.saveTarget(name, host, desc, category, "80,443,22")
        }
    }

    fun deleteTargetProfile(target: TargetProfileEntity) {
        viewModelScope.launch { repository.deleteTarget(target) }
    }

    init {
        refreshWifiTelemetry()
        refreshWirelessTelemetry()
        refreshPlatformReport()
        loadInstalledApps()
        // Prepopulate default high-value target profiles if empty
        viewModelScope.launch {
            repository.saveTarget("Cloudflare Gateway", "1.1.1.1", "Public DNS & DoH anycast endpoint", "INFRASTRUCTURE", "53,853,443,80")
            repository.saveTarget("Nmap Security Testbed", "scanme.nmap.org", "Authorized public scanning testbed provided by Fyodor/Nmap", "SECURITY_TEST", "22,80,9929,31337")
            repository.saveTarget("Google DNS Primary", "8.8.8.8", "Google Global DNS endpoint", "INFRASTRUCTURE", "53,443")
        }
    }
}
