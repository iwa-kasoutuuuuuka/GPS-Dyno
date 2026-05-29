package com.example.gpsdyno.domain.calculation

import com.example.gpsdyno.domain.model.VehicleSettings

/**
 * GPS速度、加速度、勾配から推定ホイール馬力を算出する物理演算エンジン
 */
object HorsepowerCalculator {
    private const val RHO = 1.225        // 空気密度 (kg/m^3)
    private const val G = 9.80665        // 重力加速度 (m/s^2)
    private const val PS_CONVERSION = 735.5 // WからPSへの換算定数

    /**
     * 各種抵抗補正を加味して推定ホイール馬力 (PS) を計算します。
     *
     * @param speed フィルタ適用後の速度 (m/s)
     * @param acceleration 加速度 (m/s^2)
     * @param slopeAngleDeg 道路傾斜角 (度)
     * @param settings ユーザーの車両設定 (重量、Cd、面積、Cr)
     * @return 推定ホイール馬力 (PS)
     */
    fun calculate(
        speed: Double,
        acceleration: Double,
        slopeAngleDeg: Double,
        settings: VehicleSettings
    ): Double {
        // 速度が極めて遅い場合（0.5 m/s 以下、約1.8km/h以下）は、馬力計算を安定させるために0.0にする
        if (speed < 0.5) return 0.0

        val m = settings.totalMass
        val v = speed

        // 1. 加速力 (F_acc = m * a)
        // 異常な加速度スパイク（例: Gが極端に高い）を除外する安全ガードレール
        // 通常の車両で2.0G (19.6 m/s^2) を超えることは稀
        val safeAcc = acceleration.coerceIn(-19.6, 19.6)
        val fAcc = m * safeAcc

        // 2. 空気抵抗 (Fd = 0.5 * rho * Cd * A * v^2)
        val fAir = 0.5 * RHO * settings.cd * settings.frontalArea * v * v

        // 3. 転がり抵抗 (Fr = Cr * m * g)
        // 減速または静止時には転がり抵抗の向きに注意（基本的に進行方向と逆向きに働く）
        val fRoll = settings.rollingRes * m * G

        // 4. 勾配抵抗 (Fg = m * g * sin(theta))
        val thetaRad = Math.toRadians(slopeAngleDeg)
        val fSlope = m * G * Math.sin(thetaRad)

        // 最終駆動力の総和 (F = ma + Fd + Fr + Fg)
        // 駆動力がマイナス（エンジンブレーキまたは減速中）の場合、駆動馬力は0とする
        val fTotal = fAcc + fAir + fRoll + fSlope
        
        // 最終仕事率 (P = F * v)
        val powerWatts = fTotal * v

        // 動力が負値（減速・ブレーキ）の時は馬力表示を0.0にする
        if (powerWatts < 0.0) return 0.0

        // PS馬力へ変換 (PS = W / 735.5)
        return powerWatts / PS_CONVERSION
    }
}
