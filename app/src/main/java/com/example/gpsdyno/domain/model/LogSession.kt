package com.example.gpsdyno.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 1回のロギング走行セッションを管理するRoomエンティティ
 */
@Entity(tableName = "log_sessions")
data class LogSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTimeMillis: Long = System.currentTimeMillis(), // 開始日時 (Unix Time)
    val maxSpeed: Double = 0.0,       // セッション中の最高速度 (km/h)
    val avgSpeed: Double = 0.0,       // セッション中の平均速度 (km/h)
    val durationMillis: Long = 0,     // 記録時間 (ミリ秒)
    val targetDurationMinutes: Int = 1 // 設定された制限時間 (1, 3, 5, 10, 30, 60...)
)
