package com.example.gpsdyno.presentation.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpsdyno.presentation.ui.theme.NeonCyan
import com.example.gpsdyno.presentation.ui.theme.NeonGreen
import com.example.gpsdyno.presentation.ui.theme.NeonOrange
import com.example.gpsdyno.presentation.viewmodel.MeterViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterScreen(
    viewModel: MeterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 各状態の監視
    val speed by viewModel.currentSpeedKmh.collectAsState()
    val maxSpeed by viewModel.maxSpeedKmh.collectAsState()
    val accuracy by viewModel.gpsAccuracy.collectAsState()
    val estimatedHp by viewModel.estimatedHp.collectAsState()
    val isLogging by viewModel.isLogging.collectAsState()
    val elapsedTime by viewModel.elapsedTimeMillis.collectAsState()

    // 記録制限時間の設定用
    val durations = listOf(1, 3, 5, 10, 30, 60, 180, 300) // 分単位
    val durationLabels = listOf("1分", "3分", "5分", "10分", "30分", "1時間", "3時間", "5時間")
    var selectedDurationIndex by remember { mutableStateOf(2) } // デフォルト 5分
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // GPS精度の表示判定
    val (gpsStatusText, gpsStatusColor) = remember(accuracy) {
        when {
            accuracy == 0f -> Pair("未測位", Color.Gray)
            accuracy <= 10f -> Pair("良好 (Good)", NeonGreen)
            accuracy <= 15f -> Pair("注意 (Warning)", NeonCyan)
            else -> Pair("不安定 (Unstable)", NeonOrange)
        }
    }

    // 時間表示フォーマット (MM:SS)
    val formattedTime = remember(elapsedTime) {
        val seconds = (elapsedTime / 1000) % 60
        val minutes = (elapsedTime / (1000 * 60)) % 60
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    // サービスへの自動バインド処理
    LaunchedEffect(Unit) {
        viewModel.bindLoggingService(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.unbindLoggingService(context)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- 上部ステータス表示 ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // GPSステータス
            Column {
                Text(
                    text = "GPS 精度: ${if (accuracy > 0) String.format(Locale.getDefault(), "%.1fm", accuracy) else "--"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Text(
                    text = gpsStatusText,
                    color = gpsStatusColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // キャリブレーション（勾配補正0点調整）ボタン
            IconButton(
                onClick = {
                    viewModel.calibrateSlope()
                    Toast.makeText(context, "傾斜センサーを0点校正しました（静止状態推奨）", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "キャリブレーション",
                    tint = NeonCyan
                )
            }
        }

        // --- 中央大型スピードメーター ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(120.dp))
                    .border(
                        width = 4.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(NeonGreen, NeonCyan, NeonOrange, NeonGreen)
                        ),
                        shape = RoundedCornerShape(120.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 速度値
                    Text(
                        text = String.format(Locale.getDefault(), "%.0f", speed),
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = NeonGreen,
                        textAlign = TextAlign.Center
                    )
                    // 単位
                    Text(
                        text = "km/h",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 最高速度 (MAX Speed) と経過時間の行
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "MAX SPEED",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f km/h", maxSpeed),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                }

                if (isLogging) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "RECORDING",
                            fontSize = 12.sp,
                            color = NeonOrange,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formattedTime,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = NeonOrange
                        )
                    }
                }
            }
        }

        // --- 中下部 推定ホイール馬力 ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, NeonOrange.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Estimated Wheel Horsepower",
                    fontSize = 14.sp,
                    color = NeonOrange,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f", estimatedHp),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonOrange,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PS",
                        fontSize = 18.sp,
                        color = NeonOrange,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }

        // --- 最下部 記録設定＆START/STOPボタン ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ロギング時間選択ドロップダウン (測定開始前のみ変更可能)
            AnimatedVisibility(visible = !isLogging) {
                Box(
                    modifier = Modifier
                        .wrapContentSize(Alignment.TopStart)
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "記録上限時間: ${durationLabels[selectedDurationIndex]} ▼",
                        fontSize = 14.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { isDropdownExpanded = true }
                            .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        durations.forEachIndexed { index, _ ->
                            DropdownMenuItem(
                                text = { Text(durationLabels[index]) },
                                onClick = {
                                    selectedDurationIndex = index
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // メインの START / STOP アクションボタン
            Button(
                onClick = {
                    if (isLogging) {
                        viewModel.stopLogging(context)
                        Toast.makeText(context, "走行データを保存しました", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.startLogging(context, durations[selectedDurationIndex])
                        Toast.makeText(context, "走行データのロギングを開始しました", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLogging) NeonOrange else NeonGreen
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isLogging) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (isLogging) "STOP" else "START",
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLogging) "MEASURE STOP (手動停止)" else "MEASURE START (測定開始)",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
