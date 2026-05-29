package com.example.gpsdyno.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.gpsdyno.data.repository.GPSDynoRepository
import com.example.gpsdyno.data.sensor.SlopeEstimator
import com.example.gpsdyno.domain.calculation.HorsepowerCalculator
import com.example.gpsdyno.domain.calculation.MovingAverageFilter
import com.example.gpsdyno.domain.model.LogPoint
import com.example.gpsdyno.domain.model.LogSession
import com.example.gpsdyno.domain.model.VehicleSettings
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class GPSLoggingService : Service() {

    private val binder = LocalBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var repository: GPSDynoRepository
    private lateinit var slopeEstimator: SlopeEstimator
    private lateinit var wakeLock: PowerManager.WakeLock

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    // フィルタと状態管理
    private val speedFilter = MovingAverageFilter(5)
    private var lastLocation: Location? = null
    private var lastFilteredSpeed = 0.0
    private var lastTimestamp = 0L

    // リアルタイムデータ (UI用)
    private val _currentSpeedKmh = MutableStateFlow(0.0)
    val currentSpeedKmh: StateFlow<Double> = _currentSpeedKmh.asStateFlow()

    private val _maxSpeedKmh = MutableStateFlow(0.0)
    val maxSpeedKmh: StateFlow<Double> = _maxSpeedKmh.asStateFlow()

    private val _gpsAccuracy = MutableStateFlow(0f)
    val gpsAccuracy: StateFlow<Float> = _gpsAccuracy.asStateFlow()

    private val _estimatedHp = MutableStateFlow(0.0)
    val estimatedHp: StateFlow<Double> = _estimatedHp.asStateFlow()

    private val _isLogging = MutableStateFlow(false)
    val isLogging: StateFlow<Boolean> = _isLogging.asStateFlow()

    private val _elapsedTimeMillis = MutableStateFlow(0L)
    val elapsedTimeMillis: StateFlow<Long> = _elapsedTimeMillis.asStateFlow()

    // セッション管理
    private var currentSessionId: Long? = null
    private var startTimeMillis = 0L
    private var limitDurationMillis = 0L
    private var vehicleSettings = VehicleSettings()
    private var logPointsBuffer = mutableListOf<LogPoint>()

    // 位置情報コールバック
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val location = locationResult.lastLocation ?: return
            processNewLocation(location)
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): GPSLoggingService = this@GPSLoggingService
    }

    override fun onCreate() {
        super.onCreate()
        repository = GPSDynoRepository(this)
        slopeEstimator = SlopeEstimator(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // スリープ対策の WakeLock 取得
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GPSDyno::LoggingWakeLock")

        // センサーの稼働開始
        slopeEstimator.start()
        createNotificationChannel()

        // 最新車両設定の取得・監視
        serviceScope.launch {
            repository.vehicleSettingsFlow.collect { settings ->
                if (settings != null) {
                    vehicleSettings = settings
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_START_LOGGING) {
            val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 5)
            startLoggingSession(durationMinutes)
        } else if (action == ACTION_STOP_LOGGING) {
            stopLoggingSession()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        stopGPSUpdates()
        slopeEstimator.stop()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        serviceScope.launch {
            if (_isLogging.value) {
                stopLoggingSession()
            }
        }
    }

    // キャリブレーション指示の転送
    fun calibrateSlope() {
        slopeEstimator.calibrate()
    }

    private fun startLoggingSession(durationMinutes: Int) {
        if (_isLogging.value) return

        serviceScope.launch {
            // スリープ中の動作保証のため WakeLock を取得
            if (!wakeLock.isHeld) {
                wakeLock.acquire(durationMinutes * 60 * 1000L + 60000L)
            }

            currentSessionId = repository.startNewSession(durationMinutes)
            startTimeMillis = System.currentTimeMillis()
            limitDurationMillis = durationMinutes * 60 * 1000L

            speedFilter.reset()
            lastLocation = null
            lastFilteredSpeed = 0.0
            lastTimestamp = System.currentTimeMillis()
            logPointsBuffer.clear()
            _maxSpeedKmh.value = 0.0
            _elapsedTimeMillis.value = 0L

            _isLogging.value = true

            startForeground(NOTIFICATION_ID, buildNotification("測定中...", 0.0, 0))
            startGPSUpdates()
        }
    }

    private fun stopLoggingSession() {
        if (!_isLogging.value) return

        serviceScope.launch {
            _isLogging.value = false
            stopGPSUpdates()

            if (wakeLock.isHeld) {
                wakeLock.release()
            }

            val sessionId = currentSessionId
            if (sessionId != null) {
                val currentMaxSpeed = _maxSpeedKmh.value
                val points = repository.getLogPoints(sessionId)
                val avgSpeed = if (points.isNotEmpty()) {
                    points.map { it.speed * 3.6 }.average()
                } else {
                    0.0
                }

                // 親セッションの統計情報を最終更新
                val duration = System.currentTimeMillis() - startTimeMillis
                val session = LogSession(
                    id = sessionId,
                    startTimeMillis = startTimeMillis,
                    maxSpeed = currentMaxSpeed,
                    avgSpeed = avgSpeed,
                    durationMillis = duration,
                    targetDurationMinutes = (limitDurationMillis / 60000).toInt()
                )
                repository.updateSession(session)
            }

            currentSessionId = null
            _elapsedTimeMillis.value = 0L
            _estimatedHp.value = 0.0
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun startGPSUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            100L // 100ms 間隔
        ).apply {
            setMinUpdateIntervalMillis(100L)
            setWaitForAccurateLocation(false)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        } catch (e: SecurityException) {
            _isLogging.value = false
        }
    }

    private fun stopGPSUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun processNewLocation(location: Location) {
        val now = System.currentTimeMillis()
        val dt = (now - lastTimestamp) / 1000.0 // 秒単位

        // GPS精度
        _gpsAccuracy.value = location.accuracy

        // 生の速度 (location.speed)。無ければ距離計算するフォールバック
        val rawSpeed = if (location.hasSpeed()) location.speed.toDouble() else {
            val lastLoc = lastLocation
            if (lastLoc != null && dt > 0.0) {
                lastLoc.distanceTo(location).toDouble() / dt
            } else {
                0.0
            }
        }

        // 5サンプル移動平均適用
        val filteredSpeed = speedFilter.filter(rawSpeed)
        val speedKmh = filteredSpeed * 3.6
        _currentSpeedKmh.value = speedKmh

        if (speedKmh > _maxSpeedKmh.value) {
            _maxSpeedKmh.value = speedKmh
        }

        // 加速度算出 (a = dv / dt)
        val acceleration = if (dt > 0.0) {
            val computedAcc = (filteredSpeed - lastFilteredSpeed) / dt
            // 加速度ノイズを抑えるためのリミッター
            computedAcc.coerceIn(-9.8, 9.8)
        } else {
            0.0
        }

        // 道路勾配推定値の取得
        val slopeAngle = slopeEstimator.currentSlopeAngleDegrees

        // 推定ホイール馬力計算
        val hp = HorsepowerCalculator.calculate(filteredSpeed, acceleration, slopeAngle, vehicleSettings)
        _estimatedHp.value = hp

        // ロギング中の場合はRoomへの書き込み処理
        val sessionId = currentSessionId
        if (_isLogging.value && sessionId != null) {
            val elapsedTime = now - startTimeMillis
            _elapsedTimeMillis.value = elapsedTime

            // 時間制限に達した場合は自動停止
            if (elapsedTime >= limitDurationMillis) {
                stopLoggingSession()
                return
            }

            val point = LogPoint(
                sessionId = sessionId,
                timestamp = now,
                latitude = location.latitude,
                longitude = location.longitude,
                speed = rawSpeed,
                filteredSpeed = filteredSpeed,
                accuracy = location.accuracy,
                altitude = location.altitude,
                bearing = location.bearing,
                acceleration = acceleration,
                estimatedHorsepower = hp,
                slopeAngle = slopeAngle
            )

            serviceScope.launch {
                repository.saveLogPoint(point)
            }

            // 通知をリアルタイム更新
            updateNotification(speedKmh, elapsedTime)
        }

        lastLocation = location
        lastFilteredSpeed = filteredSpeed
        lastTimestamp = now
    }

    // --- 通知関連 ---
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GPS Dyno Logging",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "走行ログの高頻度データ取得を知らせる通知です。"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String, speedKmh: Double, elapsedMillis: Long): Notification {
        val intent = Intent(this, Class.forName("com.example.gpsdyno.presentation.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val seconds = (elapsedMillis / 1000) % 60
        val minutes = (elapsedMillis / (1000 * 60)) % 60
        val timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GPS Dyno - $statusText")
            .setContentText(String.format(Locale.getDefault(), "現在速度: %.1f km/h  経過時間: %s", speedKmh, timeString))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(speedKmh: Double, elapsedMillis: Long) {
        val notification = buildNotification("測定中", speedKmh, elapsedMillis)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "gps_dyno_channel"
        private const val NOTIFICATION_ID = 2026

        const val ACTION_START_LOGGING = "com.example.gpsdyno.action.START_LOGGING"
        const val ACTION_STOP_LOGGING = "com.example.gpsdyno.action.STOP_LOGGING"
        const val EXTRA_DURATION_MINUTES = "com.example.gpsdyno.extra.DURATION_MINUTES"
    }
}
