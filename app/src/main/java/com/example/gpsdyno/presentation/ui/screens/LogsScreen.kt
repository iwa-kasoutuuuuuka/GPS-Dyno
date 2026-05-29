package com.example.gpsdyno.presentation.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpsdyno.data.exporter.CsvExporter
import com.example.gpsdyno.domain.model.LogSession
import com.example.gpsdyno.presentation.ui.theme.NeonCyan
import com.example.gpsdyno.presentation.ui.theme.NeonGreen
import com.example.gpsdyno.presentation.ui.theme.NeonOrange
import com.example.gpsdyno.presentation.viewmodel.LogsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(
    viewModel: LogsViewModel,
    onSessionSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sessions by viewModel.sessions.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val shareUri by viewModel.shareUri.collectAsState()

    // CSVエクスポート完了時の共有アクションハンドリング
    LaunchedEffect(shareUri) {
        val uri = shareUri
        if (uri != null) {
            val shareIntent = CsvExporter.getShareIntent(context, uri)
            context.startActivity(Intent.createChooser(shareIntent, "走行ログ CSV のエクスポート"))
            viewModel.clearShareUri() // 処理完了後の初期化
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "走行ログ履歴",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // --- 並び替え用のソートトグルボタン ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "並び替え:",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Row {
                val sortOptions = listOf(
                    Triple(LogsViewModel.SortType.DATE, "日時", NeonGreen),
                    Triple(LogsViewModel.SortType.MAX_SPEED, "最高速", NeonCyan),
                    Triple(LogsViewModel.SortType.DURATION, "記録時間", NeonOrange)
                )

                sortOptions.forEach { (type, label, color) ->
                    val isSelected = sortType == type
                    Button(
                        onClick = { viewModel.changeSortType(type) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) color else MaterialTheme.colorScheme.surface,
                            contentColor = if (isSelected) Color.Black else Color.White
                        ),
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- 履歴リスト表示 ---
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "走行ログはまだ記録されていません。\nメーター画面から測定を行ってください。",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sessions) { session ->
                    LogSessionItem(
                        session = session,
                        onClick = { onSessionSelected(session.id) },
                        onShareClick = { viewModel.exportSessionToCsv(context, session) },
                        onDeleteClick = { viewModel.deleteSession(session.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun LogSessionItem(
    session: LogSession,
    onClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val dateStr = remember(session.startTimeMillis) { dateFormat.format(Date(session.startTimeMillis)) }

    val formattedDuration = remember(session.durationMillis) {
        val seconds = (session.durationMillis / 1000) % 60
        val minutes = (session.durationMillis / (1000 * 60)) % 60
        val hours = session.durationMillis / (1000 * 60 * 60)
        if (hours > 0) {
            String.format(Locale.getDefault(), "%02d時間%02d分%02d秒", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d分%02d秒", minutes, seconds)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )

                // 操作アクション（共有・削除）アイコン
                Row {
                    IconButton(onClick = onShareClick) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "CSV共有",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "削除",
                            tint = NeonOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "MAX SPEED",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f km/h", session.maxSpeed),
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen,
                        fontSize = 16.sp
                    )
                }

                Column {
                    Text(
                        text = "AVG SPEED",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f km/h", session.avgSpeed),
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 16.sp
                    )
                }

                Column {
                    Text(
                        text = "DURATION",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Text(
                        text = formattedDuration,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
