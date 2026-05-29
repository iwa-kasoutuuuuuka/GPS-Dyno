package com.example.gpsdyno.presentation.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.gpsdyno.presentation.ui.theme.NeonGreen
import com.example.gpsdyno.presentation.viewmodel.GraphViewModel
import com.example.gpsdyno.presentation.viewmodel.LogsViewModel
import com.example.gpsdyno.presentation.viewmodel.MeterViewModel
import com.example.gpsdyno.presentation.viewmodel.SettingsViewModel

// タブ定義
enum class AppTab {
    METER,
    LOGS,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    meterViewModel: MeterViewModel,
    logsViewModel: LogsViewModel,
    graphViewModel: GraphViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(AppTab.METER) }
    
    // 現在詳細表示しているグラフのセッションID (nullの場合は一覧を表示)
    var selectedSessionIdForGraph by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        bottomBar = {
            // グラフ詳細画面に入っている間は、ボトムバーを非表示にして全画面表示
            if (selectedSessionIdForGraph == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = Color.White
                ) {
                    // 1. メーター
                    NavigationBarItem(
                        selected = activeTab == AppTab.METER,
                        onClick = { activeTab = AppTab.METER },
                        icon = { Icon(Icons.Default.Speed, contentDescription = "メーター") },
                        label = { Text("メーター") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = NeonGreen,
                            indicatorColor = NeonGreen
                        )
                    )

                    // 2. ログ一覧
                    NavigationBarItem(
                        selected = activeTab == AppTab.LOGS,
                        onClick = { activeTab = AppTab.LOGS },
                        icon = { Icon(Icons.Default.History, contentDescription = "ログ履歴") },
                        label = { Text("走行ログ") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = NeonGreen,
                            indicatorColor = NeonGreen
                        )
                    )

                    // 3. 設定
                    NavigationBarItem(
                        selected = activeTab == AppTab.SETTINGS,
                        onClick = { activeTab = AppTab.SETTINGS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "設定") },
                        label = { Text("設定") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = NeonGreen,
                            indicatorColor = NeonGreen
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        val screenModifier = Modifier.padding(innerPadding)

        // グラフ画面の割込み表示判定
        val currentGraphId = selectedSessionIdForGraph
        if (currentGraphId != null) {
            GraphScreen(
                viewModel = graphViewModel,
                sessionId = currentGraphId,
                onBackClick = { selectedSessionIdForGraph = null },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            // 通常のタブ画面遷移
            when (activeTab) {
                AppTab.METER -> {
                    MeterScreen(
                        viewModel = meterViewModel,
                        modifier = screenModifier
                    )
                }
                AppTab.LOGS -> {
                    LogsScreen(
                        viewModel = logsViewModel,
                        onSessionSelected = { sessionId ->
                            selectedSessionIdForGraph = sessionId
                        },
                        modifier = screenModifier
                    )
                }
                AppTab.SETTINGS -> {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        modifier = screenModifier
                    )
                }
            }
        }
    }
}
