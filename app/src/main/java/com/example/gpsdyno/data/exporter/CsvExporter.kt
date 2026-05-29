package com.example.gpsdyno.data.exporter

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.gpsdyno.domain.model.LogPoint
import com.example.gpsdyno.domain.model.LogSession
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 取得した走行ログデータをCSV形式に変換し、外部へ共有・エクスポートするユーティリティクラス
 */
object CsvExporter {

    /**
     * 特定セッションのデータポイント群をCSVファイルとして出力し、共有用インテントのURIを取得します。
     */
    fun exportToCsv(context: Context, session: LogSession, points: List<LogPoint>): Uri? {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val dateStr = dateFormat.format(Date(session.startTimeMillis))
        val fileName = "GPSDyno_Log_$dateStr.csv"

        // アプリ固有のキャッシュディレクトリ内に一時CSVファイルを作成
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val csvFile = File(exportDir, fileName)

        try {
            val writer = FileWriter(csvFile)

            // ヘッダー行の書き込み
            writer.append("Timestamp,Latitude,Longitude,Speed(m/s),Speed(km/h),FilteredSpeed(m/s),FilteredSpeed(km/h),Accuracy(m),Altitude(m),Bearing(deg),Acceleration(m/s2),EstimatedHorsepower(PS),SlopeAngle(deg)\n")

            // データ行の書き込み
            for (p in points) {
                writer.append("${p.timestamp},")
                writer.append("${p.latitude},")
                writer.append("${p.longitude},")
                writer.append("${p.speed},")
                writer.append("${p.speed * 3.6},")
                writer.append("${p.filteredSpeed},")
                writer.append("${p.filteredSpeed * 3.6},")
                writer.append("${p.accuracy},")
                writer.append("${p.altitude},")
                writer.append("${p.bearing},")
                writer.append("${p.acceleration},")
                writer.append("${p.estimatedHorsepower},")
                writer.append("${p.slopeAngle}\n")
            }

            writer.flush()
            writer.close()

            // FileProviderを使用して共有可能なUriを生成
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * CSVファイルを共有するための標準的なAndroid共有インテントを構築します。
     */
    fun getShareIntent(context: Context, uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
