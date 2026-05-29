package com.example.gpsdyno.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ロギングセッションに紐づく、100ms毎の高頻度ログデータポイント
 */
@Entity(
    tableName = "log_points",
    foreignKeys = [
        ForeignKey(
            entity = LogSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class LogPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,                 // 親セッションID
    val timestamp: Long,                 // 計測タイムスタンプ (ミリ秒)
    val latitude: Double,                // 緯度
    val longitude: Double,               // 経度
    val speed: Double,                   // 生のGPS速度 (m/s)
    val filteredSpeed: Double,           // フィルタ処理後の速度 (m/s)
    val accuracy: Float,                 // GPS精度 (m)
    val altitude: Double,                // 高度 (m)
    val bearing: Float,                  // 進行方向 (度)
    val acceleration: Double,            // 加速度 (m/s^2)
    val estimatedHorsepower: Double,     // 推定ホイール馬力 (PS)
    val slopeAngle: Double               // 推定道路傾斜角 (度)
)
