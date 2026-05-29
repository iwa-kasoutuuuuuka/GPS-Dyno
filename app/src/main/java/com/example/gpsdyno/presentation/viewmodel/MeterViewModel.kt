package com.example.gpsdyno.presentation.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpsdyno.data.service.GPSLoggingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * リアルタイムメーター画面のデータおよびロギングサービス連携を担当するViewModel
 */
class MeterViewModel : ViewModel() {

    private val _isServiceBound = MutableStateFlow(false)
    val isServiceBound: StateFlow<Boolean> = _isServiceBound.asStateFlow()

    private var loggingService: GPSLoggingService? = null

    // UIに直接提供する状態変数 (初期値設定)
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

    private var serviceCollectJob: Job? = null

    // サービス接続の定義
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as GPSLoggingService.LocalBinder
            val boundService = binder.getService()
            loggingService = boundService
            _isServiceBound.value = true

            // サービスからリアルタイムデータを集約・転送するコルーチンを起動
            serviceCollectJob?.cancel()
            serviceCollectJob = viewModelScope.launch {
                launch { boundService.currentSpeedKmh.collect { _currentSpeedKmh.value = it } }
                launch { boundService.maxSpeedKmh.collect { _maxSpeedKmh.value = it } }
                launch { boundService.gpsAccuracy.collect { _gpsAccuracy.value = it } }
                launch { boundService.estimatedHp.collect { _estimatedHp.value = it } }
                launch { boundService.isLogging.collect { _isLogging.value = it } }
                launch { boundService.elapsedTimeMillis.collect { _elapsedTimeMillis.value = it } }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceCollectJob?.cancel()
            loggingService = null
            _isServiceBound.value = false
        }
    }

    /**
     * ロギングサービスへのバインドを開始します。
     */
    fun bindLoggingService(context: Context) {
        val intent = Intent(context, GPSLoggingService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * ロギングサービスからアンバインドします。
     */
    fun unbindLoggingService(context: Context) {
        if (_isServiceBound.value) {
            context.unbindService(serviceConnection)
            serviceCollectJob?.cancel()
            loggingService = null
            _isServiceBound.value = false
        }
    }

    /**
     * 走行ロギング（測定）を開始します。
     */
    fun startLogging(context: Context, durationMinutes: Int) {
        val intent = Intent(context, GPSLoggingService::class.java).apply {
            action = GPSLoggingService.ACTION_START_LOGGING
            putExtra(GPSLoggingService.EXTRA_DURATION_MINUTES, durationMinutes)
        }
        // フォアグラウンドサービスとして起動
        context.startForegroundService(intent)
        // サービスにバインドし、リアルタイムデータ連携を確保
        bindLoggingService(context)
    }

    /**
     * 走行ロギング（測定）を手動で停止します。
     */
    fun stopLogging(context: Context) {
        val intent = Intent(context, GPSLoggingService::class.java).apply {
            action = GPSLoggingService.ACTION_STOP_LOGGING
        }
        context.startService(intent)
    }

    /**
     * 坂道補正用の端末角度キャリブレーション（0点設定）を行います。
     */
    fun calibrateSlope() {
        loggingService?.calibrateSlope()
    }
}
