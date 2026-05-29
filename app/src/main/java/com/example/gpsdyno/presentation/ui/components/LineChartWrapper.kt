package com.example.gpsdyno.presentation.ui.components

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.gpsdyno.domain.model.LogPoint
import com.example.gpsdyno.presentation.viewmodel.GraphViewModel.GraphType
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

/**
 * Jetpack Compose内で MPAndroidChart (LineChart) をレンダリングし、
 * ピンチズーム、スクロール、タップ時のハイライト値表示をサポートするラッパー。
 */
@Composable
fun LineChartWrapper(
    points: List<LogPoint>,
    graphType: GraphType,
    modifier: Modifier = Modifier,
    onValueSelected: (Double, Double) -> Unit = { _, _ -> }
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            LineChart(context).apply {
                // 基本グラフ設定
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                isScaleXEnabled = true
                isScaleYEnabled = true
                setPinchZoom(true)
                setDrawGridBackground(false)
                
                // スポーティなダークテーマ配色設定
                setBackgroundColor(Color.parseColor("#121212")) // 漆黒
                
                // 凡例の設定
                legend.textColor = Color.WHITE
                legend.isEnabled = true

                // X軸設定 (経過時間秒)
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = Color.WHITE
                    gridColor = Color.parseColor("#333333") // 暗いグリッド
                    setDrawGridLines(true)
                }

                // Y軸(左)設定
                axisLeft.apply {
                    textColor = Color.WHITE
                    gridColor = Color.parseColor("#333333")
                    setDrawGridLines(true)
                }

                // Y軸(右)は非表示にする
                axisRight.isEnabled = false

                // 値タップ選択時のイベントリスナー
                setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        if (e != null) {
                            onValueSelected(e.x.toDouble(), e.y.toDouble())
                        }
                    }

                    override fun onNothingSelected() {}
                })
            }
        },
        update = { chart ->
            if (points.isEmpty()) {
                chart.clear()
                chart.invalidate()
                return@AndroidView
            }

            val startTime = points.first().timestamp
            val entries = mutableListOf<Entry>()

            // グラフの種類に応じてデータ系列を構築
            val label: String
            val lineColorHex: String
            
            when (graphType) {
                GraphType.SPEED -> {
                    label = "速度 (km/h)"
                    lineColorHex = "#00FF66" // ネオングリーン
                    points.forEach { p ->
                        val x = (p.timestamp - startTime) / 1000.0 // 経過時間(秒)
                        val y = p.filteredSpeed * 3.6 // km/h
                        entries.add(Entry(x.toFloat(), y.toFloat()))
                    }
                }
                GraphType.ACCELERATION -> {
                    label = "加速度 (m/s²)"
                    lineColorHex = "#00E5FF" // ネオンブルー
                    points.forEach { p ->
                        val x = (p.timestamp - startTime) / 1000.0
                        val y = p.acceleration
                        entries.add(Entry(x.toFloat(), y.toFloat()))
                    }
                }
                GraphType.HORSEPOWER -> {
                    label = "推定ホイール馬力 (PS)"
                    lineColorHex = "#FF3D00" // ネオンオレンジ/レッド
                    points.forEach { p ->
                        val x = (p.timestamp - startTime) / 1000.0
                        val y = p.estimatedHorsepower
                        entries.add(Entry(x.toFloat(), y.toFloat()))
                    }
                }
            }

            val dataSet = LineDataSet(entries, label).apply {
                color = Color.parseColor(lineColorHex)
                lineWidth = 2.5f
                setDrawCircles(false)      // パフォーマンス向上のためドットは非表示
                setDrawValues(false)       // 数値テキストは非表示
                highLightColor = Color.WHITE
                setDrawHorizontalHighlightIndicator(true)
                setDrawVerticalHighlightIndicator(true)
                // グラデーション塗りつぶし設定
                setDrawFilled(true)
                fillColor = Color.parseColor(lineColorHex)
                fillAlpha = 30
            }

            chart.data = LineData(dataSet)
            chart.notifyDataSetChanged()
            chart.invalidate()
        }
    )
}
