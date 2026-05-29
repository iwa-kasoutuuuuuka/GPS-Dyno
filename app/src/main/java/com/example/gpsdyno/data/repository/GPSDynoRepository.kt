package com.example.gpsdyno.data.repository

import android.content.Context
import com.example.gpsdyno.data.database.AppDatabase
import com.example.gpsdyno.data.database.LogDao
import com.example.gpsdyno.domain.model.LogPoint
import com.example.gpsdyno.domain.model.LogSession
import com.example.gpsdyno.domain.model.VehicleSettings
import kotlinx.coroutines.flow.Flow

/**
 * データベースとの通信を統括するリポジトリクラス
 */
class GPSDynoRepository(context: Context) {

    private val logDao: LogDao = AppDatabase.getDatabase(context).logDao()

    // 車両設定のFlow取得
    val vehicleSettingsFlow: Flow<VehicleSettings?> = logDao.getVehicleSettingsFlow()

    // 全走行セッションのFlow取得（日付降順）
    val allSessions: Flow<List<LogSession>> = logDao.getAllSessions()

    // キャッシュされた最新設定
    suspend fun getVehicleSettings(): VehicleSettings {
        return logDao.getVehicleSettings() ?: VehicleSettings()
    }

    // 車両設定の保存・更新
    suspend fun saveVehicleSettings(settings: VehicleSettings) {
        logDao.insertOrUpdateSettings(settings)
    }

    // ロギングセッションの新規作成
    suspend fun startNewSession(targetDurationMinutes: Int): Long {
        val session = LogSession(
            startTimeMillis = System.currentTimeMillis(),
            maxSpeed = 0.0,
            avgSpeed = 0.0,
            durationMillis = 0,
            targetDurationMinutes = targetDurationMinutes
        )
        return logDao.insertSession(session)
    }

    // セッションの更新
    suspend fun updateSession(session: LogSession) {
        logDao.updateSession(session)
    }

    // セッションの削除 (カスケードにより紐づくLogPointも自動削除)
    suspend fun deleteSession(sessionId: Long) {
        logDao.deleteLogPointsForSession(sessionId)
        logDao.deleteSession(sessionId)
    }

    // 特定のセッションIDに対応するログポイント挿入
    suspend fun saveLogPoint(point: LogPoint) {
        logDao.insertLogPoint(point)
    }

    // 特定セッションのデータポイントFlow
    fun getLogPointsFlow(sessionId: Long): Flow<List<LogPoint>> {
        return logDao.getLogPointsForSessionFlow(sessionId)
    }

    // 特定セッションのデータポイントリスト（非Flow）
    suspend fun getLogPoints(sessionId: Long): List<LogPoint> {
        return logDao.getLogPointsForSession(sessionId)
    }
}
