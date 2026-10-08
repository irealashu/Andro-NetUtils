package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.AuditSessionEntity
import com.example.data.local.entity.TargetProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<AuditSessionEntity>>

    @Query("SELECT * FROM audit_sessions WHERE scanType = :type ORDER BY timestamp DESC")
    fun getSessionsByType(type: String): Flow<List<AuditSessionEntity>>

    @Query("SELECT * FROM audit_sessions WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteSessions(): Flow<List<AuditSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AuditSessionEntity): Long

    @Update
    suspend fun updateSession(session: AuditSessionEntity)

    @Query("DELETE FROM audit_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("DELETE FROM audit_sessions")
    suspend fun clearAllSessions()

    // Targets
    @Query("SELECT * FROM target_profiles ORDER BY name ASC")
    fun getAllTargets(): Flow<List<TargetProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarget(target: TargetProfileEntity): Long

    @Delete
    suspend fun deleteTarget(target: TargetProfileEntity)
}
