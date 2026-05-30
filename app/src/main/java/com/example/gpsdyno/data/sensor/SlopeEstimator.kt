package com.example.gpsdyno.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * スマートフォンの内蔵IMU（重力センサー・加速度センサー）を利用して、
 * 進行方向の道路傾斜角（ロードスロープ θ）を推定・監視するクラス。
 */
class SlopeEstimator(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var gravityValues = FloatArray(3)
    private var accelValues = FloatArray(3)
    private var hasGravity = false
    private var hasAccel = false

    // キャリブレーションオフセット（デバイスが車載マウントに固定された角度）
    private var pitchOffsetDegrees: Double = 0.0

    // 推定された現在の路面傾斜角（度）
    @Volatile
    var currentSlopeAngleDegrees: Double = 0.0
        private set

    private var isListening = false

    fun start() {
        if (isListening) return
        // 重力センサーを優先使用。無ければ加速度センサーを使用。
        if (gravitySensor != null) {
            if (sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_GAME)) {
                isListening = true
            }
        } else if (accelerometer != null) {
            if (sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)) {
                isListening = true
            }
        }
    }

    fun stop() {
        if (!isListening) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    /**
     * 現在の端末の固定傾斜角度を0度とするキャリブレーションを実行します。
     * （静止状態で実行することを推奨）
     */
    fun calibrate() {
        pitchOffsetDegrees = calculateCurrentRawPitch()
    }

    private fun calculateCurrentRawPitch(): Double {
        val y = if (hasGravity) gravityValues[1] else accelValues[1]
        val z = if (hasGravity) gravityValues[2] else accelValues[2]
        
        // デバイスのピッチ（前後の傾き）をアークタンジェントから算出
        // y: 上下方向（車載時は縦マウントなら進行方向成分に投影される）
        // z: 垂直（画面手前）方向
        val pitchRad = Math.atan2(y.toDouble(), z.toDouble())
        return Math.toDegrees(pitchRad)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_GRAVITY -> {
                System.arraycopy(event.values, 0, gravityValues, 0, 3)
                hasGravity = true
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelValues, 0, 3)
                hasAccel = true
            }
        }

        if (hasGravity || hasAccel) {
            // キャリブレーションオフセットを引いて、純粋な道路勾配（路面傾斜）を推定
            val rawPitch = calculateCurrentRawPitch()
            val computedSlope = rawPitch - pitchOffsetDegrees

            // 道路勾配としてあり得る範囲（例: -30度〜+30度）に制限し、ノイズ変動を防ぐためローパスフィルタ適用
            val clampedSlope = computedSlope.coerceIn(-30.0, 30.0)
            currentSlopeAngleDegrees = currentSlopeAngleDegrees * 0.95 + clampedSlope * 0.05
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
