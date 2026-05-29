package com.example.gpsdyno.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpsdyno.data.repository.GPSDynoRepository
import com.example.gpsdyno.domain.model.VehicleSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 車両パラメータおよびアプリ設定値の保存・編集を制御するViewModel
 */
class SettingsViewModel(context: Context) : ViewModel() {

    private val repository = GPSDynoRepository(context)

    private val _settings = MutableStateFlow(VehicleSettings())
    val settings: StateFlow<VehicleSettings> = _settings.asStateFlow()

    init {
        // 設定Flowの監視を開始し、ローカル変数に同期
        viewModelScope.launch {
            repository.vehicleSettingsFlow.collect { newSettings ->
                if (newSettings != null) {
                    _settings.value = newSettings
                }
            }
        }
    }

    /**
     * 新しい設定内容を非同期に保存します。
     */
    fun saveSettings(
        vehicleWeight: Double,
        riderWeight: Double,
        cd: Double,
        frontalArea: Double,
        rollingRes: Double,
        theme: String,
        unit: String
    ) {
        viewModelScope.launch {
            val updated = VehicleSettings(
                vehicleWeight = vehicleWeight,
                riderWeight = riderWeight,
                cd = cd,
                frontalArea = frontalArea,
                rollingRes = rollingRes,
                theme = theme,
                unit = unit
            )
            repository.saveVehicleSettings(updated)
        }
    }
}
