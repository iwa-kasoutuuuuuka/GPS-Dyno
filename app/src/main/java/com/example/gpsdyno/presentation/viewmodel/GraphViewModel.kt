package com.example.gpsdyno.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpsdyno.data.repository.GPSDynoRepository
import com.example.gpsdyno.domain.model.LogPoint
import com.example.gpsdyno.domain.model.LogSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ログの詳細グラフ表示画面のデータ制御を担当するViewModel
 */
class GraphViewModel(context: Context) : ViewModel() {

    private val repository = GPSDynoRepository(context)

    private val _selectedSession = MutableStateFlow<LogSession?>(null)
    val selectedSession: StateFlow<LogSession?> = _selectedSession.asStateFlow()

    private val _logPoints = MutableStateFlow<List<LogPoint>>(emptyList())
    val logPoints: StateFlow<List<LogPoint>> = _logPoints.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // グラフの表示種別
    enum class GraphType {
        SPEED,        // 速度 (km/h)
        ACCELERATION, // 加速度 (m/s^2)
        HORSEPOWER    // 推定ホイール馬力 (PS)
    }

    private val _activeGraphType = MutableStateFlow(GraphType.SPEED)
    val activeGraphType: StateFlow<GraphType> = _activeGraphType.asStateFlow()

    /**
     * 表示対象のセッションをロードし、紐づくログポイントを取得します。
     */
    fun loadSessionData(sessionId: Long) {
        _isLoading.value = true
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId)
            if (session != null) {
                _selectedSession.value = session
                val points = repository.getLogPoints(sessionId)
                _logPoints.value = points
            }
            _isLoading.value = false
        }
    }

    fun setGraphType(type: GraphType) {
        _activeGraphType.value = type
    }

    /**
     * データクリア（画面離脱時など）
     */
    fun clearData() {
        _selectedSession.value = null
        _logPoints.value = emptyList()
        _activeGraphType.value = GraphType.SPEED
    }
}
