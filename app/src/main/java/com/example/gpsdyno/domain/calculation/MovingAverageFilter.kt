package com.example.gpsdyno.domain.calculation

import java.util.LinkedList
import java.util.Queue

/**
 * GPS速度のノイズを低減し、加速度や馬力推定を安定化するための5サンプル移動平均フィルタ
 */
class MovingAverageFilter(private val windowSize: Int = 5) {
    private val speedWindow: Queue<Double> = LinkedList()
    private var lastValidSpeed = 0.0

    /**
     * 新しい生速度（m/s）を入力し、フィルタ適用後の速度を返します。
     * 異常値の検出・除去（急激な速度ジャンプ、負値）もここで行います。
     */
    fun filter(rawSpeed: Double): Double {
        // 1. 負値の除外（0以下は強制的に0.0にクリップ）
        var speed = if (rawSpeed < 0.0) 0.0 else rawSpeed

        // 2. 急激な速度ジャンプの除外（例: 100msの間に物理的にあり得ない急激な加減速が発生した場合は、前回の有効値を維持）
        // 100msで 10m/s (約 36km/h) 以上の変化は異常値（約10G相当）として無視
        if (speedWindow.isNotEmpty()) {
            val delta = Math.abs(speed - lastValidSpeed)
            if (delta > 10.0) {
                speed = lastValidSpeed
            }
        }

        // 移動平均バッファの更新
        speedWindow.add(speed)
        if (speedWindow.size > windowSize) {
            speedWindow.poll()
        }

        // 平均値の計算
        val sum = speedWindow.sum()
        val filtered = sum / speedWindow.size

        lastValidSpeed = speed
        return filtered
    }

    /**
     * フィルターをリセット（セッション開始時などに使用）
     */
    fun reset() {
        speedWindow.clear()
        lastValidSpeed = 0.0
    }
}
