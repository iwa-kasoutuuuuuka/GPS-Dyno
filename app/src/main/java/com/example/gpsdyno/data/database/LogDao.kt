package com.example.gpsdyno.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.gpsdyno.domain.model.LogPoint
import com.example.gpsdyno.domain.model.LogSession
import com.example.gpsdyno.domain.model.VehicleSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {

    // --- ロギングセッション関連 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: LogSession): Long

    @Update
    suspend fun updateSession(session: LogSession)

    @Query("DELETE FROM log_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("SELECT * FROM log_sessions ORDER BY startTimeMillis DESC")
    fun getAllSessions(): Flow<List<LogSession>>

    @Query("SELECT * FROM log_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): LogSession?

    // --- ログデータポイント関連 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogPoint(point: LogPoint)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogPoints(points: List<LogPoint>)

    @Query("SELECT * FROM log_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getLogPointsForSessionFlow(sessionId: Long): Flow<List<LogPoint>>

    @Query("SELECT * FROM log_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getLogPointsForSession(sessionId: Long): List<LogPoint>

    @Query("DELETE FROM log_points WHERE sessionId = :sessionId")
    suspend fun deleteLogPointsForSession(sessionId: Long)

    // --- 車両設定関連 ---
    @Query("SELECT * FROM vehicle_settings WHERE id = 1 LIMIT 1")
    fun getVehicleSettingsFlow(): Flow<VehicleSettings?>

    @Query("SELECT * FROM vehicle_settings WHERE id = 1 LIMIT 1")
    suspend fun getVehicleSettings(): VehicleSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: VehicleSettings)
}
