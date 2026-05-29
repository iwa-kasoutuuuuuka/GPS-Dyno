package com.example.gpsdyno.presentation.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gpsdyno.presentation.ui.theme.NeonGreen
import com.example.gpsdyno.presentation.viewmodel.SettingsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentSettings by viewModel.settings.collectAsState()

    // ユーザー入力用のローカルテキストステート
    var vehicleWeightStr by remember { mutableStateOf("") }
    var riderWeightStr by remember { mutableStateOf("") }
    var cdStr by remember { mutableStateOf("") }
    var frontalAreaStr by remember { mutableStateOf("") }
    var rollingResStr by remember { mutableStateOf("") }

    // 現在の設定値がロードされたらテキストフィールドを初期化
    LaunchedEffect(currentSettings) {
        vehicleWeightStr = currentSettings.vehicleWeight.toString()
        riderWeightStr = currentSettings.riderWeight.toString()
        cdStr = currentSettings.cd.toString()
        frontalAreaStr = currentSettings.frontalArea.toString()
        rollingResStr = currentSettings.rollingRes.toString()
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "車両および環境設定",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "※推定ホイール馬力を正確に計測するため、正しい値を設定してください。",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // --- 入力フィールド群 ---
        SettingsInputField(
            label = "車両重量 (kg)",
            value = vehicleWeightStr,
            onValueChange = { vehicleWeightStr = it },
            placeholder = "例: 150.0"
        )

        SettingsInputField(
            label = "乗員/同乗者重量 (kg)",
            value = riderWeightStr,
            onValueChange = { riderWeightStr = it },
            placeholder = "例: 70.0"
        )

        SettingsInputField(
            label = "空気抵抗係数 (Cd値)",
            value = cdStr,
            onValueChange = { cdStr = it },
            placeholder = "オートバイ例: 0.45"
        )

        SettingsInputField(
            label = "前面投影面積 (A: m²)",
            value = frontalAreaStr,
            onValueChange = { frontalAreaStr = it },
            placeholder = "オートバイ例: 0.8"
        )

        SettingsInputField(
            label = "転がり抵抗係数 (Cr値)",
            value = rollingResStr,
            onValueChange = { rollingResStr = it },
            placeholder = "アスファルト舗装例: 0.015"
        )

        Spacer(modifier = Modifier.height(30.dp))

        // --- 保存ボタン ---
        Button(
            onClick = {
                // 入力値のバリデーションチェック
                val vWeight = vehicleWeightStr.toDoubleOrNull()
                val rWeight = riderWeightStr.toDoubleOrNull()
                val cd = cdStr.toDoubleOrNull()
                val fArea = frontalAreaStr.toDoubleOrNull()
                val rRes = rollingResStr.toDoubleOrNull()

                if (vWeight == null || rWeight == null || cd == null || fArea == null || rRes == null) {
                    Toast.makeText(context, "無効な入力値があります。数値を正しく入力してください。", Toast.LENGTH_LONG).show()
                } else {
                    viewModel.saveSettings(
                        vehicleWeight = vWeight,
                        riderWeight = rWeight,
                        cd = cd,
                        frontalArea = fArea,
                        rollingRes = rRes,
                        theme = "Dark",
                        unit = "km/h"
                    )
                    Toast.makeText(context, "設定を保存しました", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "設定を保存する",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.Gray) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonGreen,
                unfocusedBorderColor = Color.DarkGray
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
