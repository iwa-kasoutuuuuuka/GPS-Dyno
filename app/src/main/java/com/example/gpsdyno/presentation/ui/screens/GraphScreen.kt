package com.example.gpsdyno.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpsdyno.presentation.ui.components.LineChartWrapper
import com.example.gpsdyno.presentation.ui.theme.NeonCyan
import com.example.gpsdyno.presentation.ui.theme.NeonGreen
import com.example.gpsdyno.presentation.ui.theme.NeonOrange
import com.example.gpsdyno.presentation.viewmodel.GraphViewModel
import com.example.gpsdyno.presentation.viewmodel.GraphViewModel.GraphType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    viewModel: GraphViewModel,
    sessionId: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedSession by viewModel.selectedSession.collectAsState()
    val logPoints by viewModel.logPoints.collectAsState()
    val activeGraphType by viewModel.activeGraphType.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // タップで選択されたデータ点の詳細保持
    var selectedTimeSec by remember { mutableStateOf<Double?>(null) }
    var selectedValue by remember { mutableStateOf<Double?>(null) }

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    // 画面表示時に指定のセッションデータをロードする
    LaunchedEffect(sessionId) {
        viewModel.loadSessionData(sessionId)
        // タップ詳細をクリア
        selectedTimeSec = null
        selectedValue = null
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearData()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- 上部ツールバー ---
        TopAppBar(
            title = {
                Text(
                    text = "走行ログ解析",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "戻る",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NeonGreen)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- セッション概要ヘッダー ---
                selectedSession?.let { session ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = dateFormat.format(Date(session.startTimeMillis)),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "MAX: ${String.format(Locale.getDefault(), "%.1f", session.maxSpeed)} km/h",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "測定時間",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                                val seconds = (session.durationMillis / 1000) % 60
                                val minutes = (session.durationMillis / (1000 * 60)) % 60
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d分%02d秒", minutes, seconds),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // --- グラフ種別切り替えタブ ---
                TabRow(
                    selectedTabIndex = activeGraphType.ordinal,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeGraphType.ordinal]),
                            color = when (activeGraphType) {
                                GraphType.SPEED -> NeonGreen
                                GraphType.ACCELERATION -> NeonCyan
                                GraphType.HORSEPOWER -> NeonOrange
                            }
                        )
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    val tabs = listOf(
                        Triple(GraphType.SPEED, "速度", NeonGreen),
                        Triple(GraphType.ACCELERATION, "加速度", NeonCyan),
                        Triple(GraphType.HORSEPOWER, "馬力", NeonOrange)
                    )

                    tabs.forEachIndexed { index, (type, label, color) ->
                        Tab(
                            selected = activeGraphType == type,
                            onClick = {
                                viewModel.setGraphType(type)
                                selectedTimeSec = null
                                selectedValue = null
                            },
                            text = {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (activeGraphType == type) color else Color.Gray
                                )
                            }
                        )
                    }
                }

                // --- タップされた詳細値の表示領域 ---
                if (selectedTimeSec != null && selectedValue != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Text(
                                text = "経過: ${String.format(Locale.getDefault(), "%.1f秒", selectedTimeSec)}",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "値: ${
                                    String.format(
                                        Locale.getDefault(),
                                        "%.2f",
                                        selectedValue
                                    )
                                } ${
                                    when (activeGraphType) {
                                        GraphType.SPEED -> "km/h"
                                        GraphType.ACCELERATION -> "m/s²"
                                        GraphType.HORSEPOWER -> "PS"
                                    }
                                }",
                                fontWeight = FontWeight.Bold,
                                color = when (activeGraphType) {
                                    GraphType.SPEED -> NeonGreen
                                    GraphType.ACCELERATION -> NeonCyan
                                    GraphType.HORSEPOWER -> NeonOrange
                                },
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // --- グラフ（LineChartWrapper）描画 ---
                if (logPoints.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "描画可能な走行ログポイントがありません。",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        LineChartWrapper(
                            points = logPoints,
                            graphType = activeGraphType,
                            modifier = Modifier.fillMaxSize(),
                            onValueSelected = { time, value ->
                                selectedTimeSec = time
                                selectedValue = value
                            }
                        )
                    }
                }
            }
        }
    }
}
