package com.example.data.repository

import com.example.data.local.dao.AuditDao
import com.example.data.local.entity.AuditSessionEntity
import com.example.data.local.entity.TargetProfileEntity
import kotlinx.coroutines.flow.Flow

class AuditRepository(private val auditDao: AuditDao) {
    val allSessions: Flow<List<AuditSessionEntity>> = auditDao.getAllSessions()
    val allTargets: Flow<List<TargetProfileEntity>> = auditDao.getAllTargets()
    val favoriteSessions: Flow<List<AuditSessionEntity>> = auditDao.getFavoriteSessions()

    fun getSessionsByType(type: String): Flow<List<AuditSessionEntity>> =
        auditDao.getSessionsByType(type)

    suspend fun saveSession(
        target: String,
        scanType: String,
        summary: String,
        scoreOrGrade: String = "",
        detailsJson: String
    ): Long {
        val entity = AuditSessionEntity(
            target = target,
            scanType = scanType,
            summary = summary,
            scoreOrGrade = scoreOrGrade,
            detailsJson = detailsJson
        )
        return auditDao.insertSession(entity)
    }

    suspend fun toggleFavorite(session: AuditSessionEntity) {
        auditDao.updateSession(session.copy(isFavorite = !session.isFavorite))
    }

    suspend fun deleteSession(id: Long) {
        auditDao.deleteSessionById(id)
    }

    suspend fun clearHistory() {
        auditDao.clearAllSessions()
    }

    suspend fun saveTarget(
        name: String,
        targetHost: String,
        description: String,
        category: String,
        defaultPorts: String
    ): Long {
        val entity = TargetProfileEntity(
            name = name,
            targetHost = targetHost,
            description = description,
            category = category,
            defaultPorts = defaultPorts
        )
        return auditDao.insertTarget(entity)
    }

    suspend fun deleteTarget(target: TargetProfileEntity) {
        auditDao.deleteTarget(target)
    }
}
