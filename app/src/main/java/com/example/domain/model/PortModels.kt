package com.example.domain.model

import java.io.Serializable

data class PortScanResult(
    val port: Int,
    val serviceName: String,
    val transport: String = "TCP",
    val isOpen: Boolean,
    val latencyMs: Long = 0,
    val banner: String? = null,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val description: String = ""
) : Serializable

enum class RiskLevel {
    INFO, LOW, MEDIUM, HIGH, CRITICAL
}

data class PortPreset(
    val id: String,
    val name: String,
    val description: String,
    val ports: List<Int>
)

object CommonPortPresets {
    val TOP_20 = PortPreset(
        id = "top_20",
        name = "Top 20 Critical",
        description = "Standard system and internet service ports",
        ports = listOf(21, 22, 23, 25, 53, 80, 110, 111, 135, 139, 143, 443, 445, 993, 995, 1723, 3306, 3389, 5900, 8080)
    )

    val WEB_SERVICES = PortPreset(
        id = "web",
        name = "Web & Proxies",
        description = "HTTP, HTTPS, Alternative Web, Proxies, API Gateways",
        ports = listOf(80, 443, 8000, 8008, 8080, 8443, 8888, 9000, 9090, 3000, 5000)
    )

    val DATABASES = PortPreset(
        id = "databases",
        name = "Databases & Caches",
        description = "MySQL, PostgreSQL, MongoDB, Redis, Memcached, Oracle, MSSQL",
        ports = listOf(1433, 1521, 3306, 5432, 6379, 11211, 27017, 9200, 9300, 8529)
    )

    val REMOTE_ADMIN = PortPreset(
        id = "remote_admin",
        name = "Remote Admin & Infrastructure",
        description = "SSH, Telnet, RDP, VNC, SNMP, WinRM, Kubernetes",
        ports = listOf(22, 23, 161, 3389, 5900, 5985, 5986, 6443, 8443, 10250)
    )

    val MAIL_SERVICES = PortPreset(
        id = "mail",
        name = "Mail & Messaging",
        description = "SMTP, POP3, IMAP, submission, MQTT, AMQP",
        ports = listOf(25, 110, 143, 465, 587, 993, 995, 1883, 5672, 8883)
    )

    val ALL_PRESETS = listOf(TOP_20, WEB_SERVICES, DATABASES, REMOTE_ADMIN, MAIL_SERVICES)

    fun getServiceName(port: Int): String = when (port) {
        21 -> "FTP (File Transfer)"
        22 -> "SSH (Secure Shell)"
        23 -> "Telnet (Insecure Plaintext)"
        25 -> "SMTP (Mail Routing)"
        53 -> "DNS (Domain Name System)"
        80 -> "HTTP (Plaintext Web)"
        110 -> "POP3 (Mail Retrieval)"
        111 -> "RPCBind"
        135 -> "MSRPC (Windows RPC)"
        139 -> "NetBIOS-SSN"
        143 -> "IMAP (Mail Access)"
        161 -> "SNMP (Network Management)"
        443 -> "HTTPS (TLS Encrypted Web)"
        445 -> "SMB / CIFS (File Sharing)"
        465 -> "SMTPS (Encrypted Mail)"
        587 -> "SMTP-Submission"
        993 -> "IMAPS (Encrypted IMAP)"
        995 -> "POP3S (Encrypted POP3)"
        1433 -> "MS-SQL Server"
        1521 -> "Oracle Database"
        1723 -> "PPTP VPN"
        1883 -> "MQTT IoT Broker"
        3000 -> "Node.js / React Dev Server"
        3306 -> "MySQL / MariaDB"
        3389 -> "RDP (Remote Desktop)"
        5000 -> "Flask / Docker Registry"
        5432 -> "PostgreSQL Database"
        5672 -> "RabbitMQ / AMQP"
        5900 -> "VNC Remote Desktop"
        5985 -> "WinRM (HTTP)"
        5986 -> "WinRM (HTTPS)"
        6379 -> "Redis In-Memory Store"
        6443 -> "Kubernetes API Server"
        8000 -> "Django / Python HTTP"
        8080 -> "HTTP Alternate / Tomcat / Spring"
        8443 -> "HTTPS Alternate / Admin"
        8883 -> "MQTT over TLS"
        8888 -> "Jupyter / HTTP Alt"
        9000 -> "PHP-FPM / MinIO S3"
        9090 -> "Prometheus Server"
        9200 -> "Elasticsearch REST"
        9300 -> "Elasticsearch Cluster"
        11211 -> "Memcached"
        27017 -> "MongoDB Database"
        else -> "Custom Port $port"
    }

    fun getRiskLevel(port: Int): RiskLevel = when (port) {
        23, 111, 135, 139, 445 -> RiskLevel.HIGH
        21, 80, 110, 143, 6379, 11211, 27017 -> RiskLevel.MEDIUM
        22, 25, 53, 443, 3306, 5432, 3389 -> RiskLevel.LOW
        else -> RiskLevel.INFO
    }
}

data class DiscoveredHost(
    val ipAddress: String,
    val hostname: String?,
    val isReachable: Boolean,
    val responseTimeMs: Long,
    val openPortsSummary: List<Int> = emptyList(),
    val macAddressHint: String? = null
)
