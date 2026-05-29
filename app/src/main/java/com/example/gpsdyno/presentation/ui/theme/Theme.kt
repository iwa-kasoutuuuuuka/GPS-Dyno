package com.example.gpsdyno.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// スポーティでサイバーパンク感のあるダークメーター向けのカラーパレット
val DarkGray = Color(0xFF121212)
val CardGray = Color(0xFF1E1E1E)
val NeonGreen = Color(0xFF00FF66) // 現在速度、MAX速度、正常GPS
val NeonCyan = Color(0xFF00E5FF)  // 加速度、情報インジケータ
val NeonOrange = Color(0xFFFF5A00) // 推定馬力、警告
val PureWhite = Color(0xFFFFFFFF)
val MutedGray = Color(0xFF888888)

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    secondary = NeonCyan,
    tertiary = NeonOrange,
    background = DarkGray,
    surface = CardGray,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = PureWhite,
    onSurface = PureWhite
)

@Composable
fun GPSDynoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // 常にダークテーマ基調として表示
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
