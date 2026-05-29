package com.example.gpsdyno.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpsdyno.data.exporter.CsvExporter
import com.example.gpsdyno.data.repository.GPSDynoRepository
import com.example.gpsdyno.domain.model.LogSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 履歴一覧画面のデータ取得、ソート、CSV出力、削除処理を担当するViewModel
 */
class LogsViewModel(context: Context) : ViewModel() {

    private val repository = GPSDynoRepository(context)

    // ソート順の種類
    enum class SortType {
        DATE,      // 日時順 (新 -> 古)
        MAX_SPEED, // 最高速度順 (高 -> 低)
        DURATION   // 記録時間順 (長 -> 短)
    }

    private val _sortType = MutableStateFlow(SortType.DATE)
    val sortType: StateFlow<SortType> = _sortType

    // エクスポート成功時の共有Uri管理
    private val _shareUri = MutableStateFlow<Uri?>(null)
    val shareUri: StateFlow<Uri?> = _shareUri

    // セッションリストとソート状態を結合してUIに流すFlow
    val sessions: StateFlow<List<LogSession>> = repository.allSessions
        .combine(_sortType) { rawSessions, sort ->
            when (sort) {
                SortType.DATE -> rawSessions.sortedByDescending { it.startTimeMillis }
                SortType.MAX_SPEED -> rawSessions.sortedByDescending { it.maxSpeed }
                SortType.DURATION -> rawSessions.sortedByDescending { it.durationMillis }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun changeSortType(type: SortType) {
        _sortType.value = type
    }

    /**
     * 特定のセッションをデータベースから削除します。
     */
    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }

    /**
     * CSVファイルのエクスポートを実行します。
     * 成功すると `shareUri` が更新されます。
     */
    fun exportSessionToCsv(context: Context, session: LogSession) {
        viewModelScope.launch {
            val points = repository.getLogPoints(session.id)
            val uri = CsvExporter.exportToCsv(context, session, points)
            _shareUri.value = uri
        }
    }

    /**
     * 共有処理完了後にUri状態をリセットします。
     */
    fun clearShareUri() {
        _shareUri.value = null
    }
}
