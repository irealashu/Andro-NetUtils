package com.example.domain.model

import java.io.Serializable

data class PlatformSecurityReport(
    val androidVersion: String,
    val apiLevel: Int,
    val isRooted: Boolean,
    val selinuxMode: String, // Enforcing, Permissive, Disabled
    val isVpnActive: Boolean,
    val isWifiThrottlingActive: Boolean,
    val isPowerSaveThrottling: Boolean,
    val canAccessArpCache: Boolean,
    val canInjectRawPackets: Boolean,
    val capabilities: List<PlatformCapabilityItem>
) : Serializable

data class PlatformCapabilityItem(
    val capability: String,
    val nonRootSupported: Boolean,
    val rootRequired: Boolean,
    val mechanism: String,
    val androidConstraint: String
) : Serializable
