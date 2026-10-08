package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_sessions")
data class AuditSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val target: String,
    val scanType: String, // "PORT_SCAN", "TLS_AUDIT", "HTTP_HEADER_AUDIT", "DNS_INSPECT", "TRACEROUTE"
    val timestamp: Long = System.currentTimeMillis(),
    val summary: String,
    val scoreOrGrade: String = "",
    val detailsJson: String,
    val isFavorite: Boolean = false
)

@Entity(tableName = "target_profiles")
data class TargetProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetHost: String,
    val description: String = "",
    val category: String = "INFRASTRUCTURE", // INFRASTRUCTURE, WEB_API, DATABASE, IOT, CUSTOM
    val defaultPorts: String = "80,443,22",
    val createdAt: Long = System.currentTimeMillis()
)
