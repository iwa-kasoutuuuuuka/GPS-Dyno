package com.example.gpsdyno.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.gpsdyno.presentation.ui.screens.MainScreen
import com.example.gpsdyno.presentation.ui.theme.GPSDynoTheme
import com.example.gpsdyno.presentation.viewmodel.GraphViewModel
import com.example.gpsdyno.presentation.viewmodel.LogsViewModel
import com.example.gpsdyno.presentation.viewmodel.MeterViewModel
import com.example.gpsdyno.presentation.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    // ViewModelの手動インスタンス生成 (Context引き渡しのための初期化)
    private lateinit var meterViewModel: MeterViewModel
    private lateinit var logsViewModel: LogsViewModel
    private lateinit var graphViewModel: GraphViewModel
    private lateinit var settingsViewModel: SettingsViewModel

    // パーミッション要求ランチャー
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineGranted || coarseGranted) {
            // Android 10 (API 29) 以降はバックグラウンド位置情報許可を別要求する必要がある
            checkAndRequestBackgroundLocationPermission()
        } else {
            Toast.makeText(this, "位置情報パーミッションが拒否されたため、GPSメーター機能を利用できません。", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewModels の手動初期化（依存コンテキスト注入）
        meterViewModel = MeterViewModel()
        logsViewModel = LogsViewModel(applicationContext)
        graphViewModel = GraphViewModel(applicationContext)
        settingsViewModel = SettingsViewModel(applicationContext)

        // パーミッションチェック＆起動時GPSサービス有効化チェック
        checkLocationPermissions()
        checkGpsServiceEnabled()

        setContent {
            GPSDynoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        meterViewModel = meterViewModel,
                        logsViewModel = logsViewModel,
                        graphViewModel = graphViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }

    /**
     * 位置情報パーミッションの状態を確認し、未取得の場合は要求します。
     */
    private fun checkLocationPermissions() {
        val finePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarsePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        // Android 13 (API 33) 以降の通知パーミッション要求
        val permissionsToRequest = mutableListOf<String>()
        if (finePermission != PackageManager.PERMISSION_GRANTED || coarsePermission != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (notificationPermission != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            locationPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // バックグラウンド位置情報のチェック
            checkAndRequestBackgroundLocationPermission()
        }
    }

    /**
     * Android 10 (API 29) 以降でのバックグラウンド位置情報権限要求
     */
    private fun checkAndRequestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBackgroundLocation = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasBackgroundLocation) {
                // ユーザーにバックグラウンド許可（「常に許可」）が必要な旨をトースト表示して設定画面に誘導
                Toast.makeText(
                    this,
                    "画面消灯時（スリープ中）もロギングを継続するには、位置情報を「常に許可」に設定してください。",
                    Toast.LENGTH_LONG
                ).show()

                // バックグラウンド位置情報の直接要求
                registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            }
        }
    }

    /**
     * 端末のGPS（位置情報サービス）自体が有効かチェックし、OFFの場合は有効化設定へ誘導します。
     */
    private fun checkGpsServiceEnabled() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        
        if (!isGpsEnabled) {
            Toast.makeText(this, "端末のGPS（位置情報）が無効化されています。有効にしてください。", Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                // フォールバック
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
            }
        }
    }
}
