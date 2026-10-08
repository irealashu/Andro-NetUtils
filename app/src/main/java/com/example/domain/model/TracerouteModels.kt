package com.example.domain.model

import java.io.Serializable

data class TracerouteHop(
    val hopNumber: Int,
    val ipAddress: String,
    val hostname: String?,
    val rtt1Ms: Long,
    val rtt2Ms: Long,
    val rtt3Ms: Long,
    val avgRttMs: Long,
    val asn: String? = null,
    val location: String? = null,
    val isTimedOut: Boolean = false
) : Serializable

data class TracerouteResult(
    val targetHost: String,
    val targetIp: String,
    val hops: List<TracerouteHop>,
    val totalTimeMs: Long,
    val isCompleted: Boolean,
    val packetLossPercent: Int
) : Serializable
