package com.example.gpsdyno.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ユーザーが設定画面で入力する車両設定情報
 */
@Entity(tableName = "vehicle_settings")
data class VehicleSettings(
    @PrimaryKey val id: Int = 1, // 単一レコードのみ保持するため固定値
    val vehicleWeight: Double = 150.0, // 車両重量 (kg)
    val riderWeight: Double = 70.0,   // 乗員重量 (kg)
    val cd: Double = 0.4,             // 空気抵抗係数 Cd
    val frontalArea: Double = 0.8,     // 前面投影面積 (m^2)
    val rollingRes: Double = 0.015,    // 転がり抵抗係数 Cr
    val theme: String = "Dark",        // UIテーマ設定 ("Dark" 等)
    val unit: String = "km/h"          // 速度単位設定 ("km/h" 等)
) {
    // 総重量 (m) のゲッター
    val totalMass: Double
        get() = vehicleWeight + riderWeight
}
