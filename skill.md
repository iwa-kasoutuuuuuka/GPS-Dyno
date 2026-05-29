# GPS Dyno 開発スキル・仕様書 (SKILL.md)

## プロジェクト名
**GPS Dyno**

---

## プロジェクト概要
GPS Dynoは、高頻度GPS速度計、走行データロガー、およびGPS加速度に基づく推定ホイール馬力（Wheel Horsepower）分析機能を備えたAndroid用アプリケーションです。

*   **対象車両**: スクーター、オートバイ、自動車
*   **動作環境**: 外部ハードウェア（ESP32、OBD2等）は一切使用せず、Android端末の内蔵GPSおよびセンサー（加速度センサー、ジャイロセンサー等）のみで動作します。

---

## 主要機能と仕様

### 1. リアルタイムGPSメーター (Meter Screen)
*   **表示項目**:
    *   現在速度（km/h）
    *   最高速度（MAX Speed）
    *   GPS精度（Accuracy）
    *   ロギング状態（録画中インジケータ、経過時間）
    *   推定ホイール馬力（Estimated Wheel Horsepower）
*   **技術仕様**:
    *   GPSリクエスト間隔: 100ms
    *   UI更新間隔: 200ms
    *   `location.speed` を優先使用（座標間距離からの簡易計算は避ける）
    *   `FusedLocationProviderClient` を使用し、高精度モード（`PRIORITY_HIGH_ACCURACY`）を有効化する。
    *   デジタルメーターをイメージしたスポーティなダークテーマUI。

### 2. 走行ロガー (Driving Logger)
*   **制御**: 手動の START/STOP ボタンによる制御。
*   **タイマー（ロギング時間制限）設定**:
    *   1分、3分、5分、10分、30分、1時間、3時間、5時間
*   **自動停止条件**: 設定時間に達した時、または手動停止時。
*   **記録データ項目**:
    *   `timestamp` (ミリ秒)
    *   `latitude` (緯度)
    *   `longitude` (経度)
    *   `speed` (生のGPS速度 m/s)
    *   `filteredSpeed` (フィルタ適用後の速度 m/s)
    *   `accuracy` (GPS精度 m)
    *   `altitude` (高度 m)
    *   `bearing` (進行方向 deg)
    *   `acceleration` (加速度 m/s²)
    *   `estimatedHorsepower` (推定馬力 PS)
*   **保存先**: Room Database
*   **エクスポート**: CSV出力機能をサポート。

### 3. グラフ視覚化 (Graph Screen)
*   **表示グラフ**:
    *   速度（Speed）グラフ
    *   加速度（Acceleration）グラフ
    *   推定ホイール馬力（Estimated Wheel Horsepower）グラフ
*   **対話操作**: ピンチズーム（拡大・縮小）、スクロール、タップによる値のポップアップ表示。
*   **使用ライブラリ**: `MPAndroidChart`

### 4. 馬力推定アルゴリズム (Estimated Wheel Horsepower)
本アプリで算出するのはエンジン出力（軸馬力）ではなく、**「推定ホイール馬力（Estimated Wheel Horsepower）」**です。タイヤのスリップは「なし」と仮定します。

#### 基本公式
$$P = m \cdot a \cdot v$$
*   $P$: 動力・仕事率 (W)
*   $m$: 総重量（車両重量 + ライダー重量） (kg)
*   $a$: 加速度 (m/s²)
*   $v$: 速度 (m/s)

#### 馬力 (PS) への換算
$$PS = \frac{P}{735.5}$$

#### 補正項を加味した最終駆動ファンミュラ
最終駆動力 $F$ は、加速力、空気抵抗、転がり抵抗、および勾配抵抗の総和です。
$$F = m \cdot a + F_d + F_r + F_g$$
最終仕事率 $P$:
$$P = F \cdot v$$

1.  **空気抵抗補正 ($F_d$)**:
    $$F_d = \frac{1}{2} \cdot \rho \cdot C_d \cdot A \cdot v^2$$
    *   空気密度 $\rho = 1.225 \, \text{kg/m}^3$ (定数)
    *   $C_d$: 空気抵抗係数（設定画面でユーザー入力）
    *   $A$: 前面投影面積 ($m^2$, 設定画面でユーザー入力)
2.  **転がり抵抗補正 ($F_r$)**:
    $$F_r = C_r \cdot m \cdot g$$
    *   重力加速度 $g = 9.80665 \, \text{m/s}^2$ (定数)
    *   $C_r$: 転がり抵抗係数（設定画面でユーザー入力）
3.  **勾配抵抗補正 ($F_g$)**:
    Android内蔵の加速度センサー、重力センサー、IMU（慣性計測装置）から道路勾配角 $\theta$ を推定。
    $$F_g = m \cdot g \cdot \sin(\theta)$$

#### 設定画面での必須入力項目
*   車両重量 (Vehicle weight)
*   ライダー/同乗者重量 (Rider weight)
*   空気抵抗係数 ($C_d$ value)
*   前面投影面積 (Frontal area)
*   転がり抵抗係数 (Rolling resistance coefficient)

---

## 信号処理とノイズ低減 (Noise Filtering)
高精度かつ安定した馬力測定のため、GPS速度データに以下の処理を適用します。

1.  **移動平均フィルタ (Moving Average)**:
    *   5サンプルの移動平均を適用。
    *   適用タイミング: 加速度算出および馬力推定の**前**。
2.  **異常値・外れ値除去**:
    *   GPSの一時的なバグによる異常な速度ジャンプの除去。
    *   負の速度値のクリッピング（0に補正）。
    *   物理的に不可能な急加速スパイクの除去。

---

## GPS精度ハンドリング
GPSの精度誤差（Accuracy）を常時監視し、UI表示およびログの信頼性を評価します。
*   `accuracy <= 10m`: **Good**（良好、通常動作）
*   `accuracy > 10m && accuracy <= 15m`: **Warning**（警告、精度低下）
*   `accuracy > 15m`: **Unstable**（不安定、ロギングデータの無効化判定対象）

---

## Android システム要件と実装設計

### 1. アーキテクチャ
*   **MVVM (Model-View-ViewModel)** を採用。
*   **Repositoryパターン** によるデータアクセス抽象化。
*   **Clean Architecture** に準拠したレイヤー分割（Domain, Data, Presentation）。
*   非同期処理には Kotlin **Coroutines** と **Flow** を使用。

### 2. UIレイヤー
*   **Jetpack Compose** と **Material Design 3** を全面採用。
*   スポーティかつ視認性の高い「ダークテーマ」を基調とする。

### 3. バックグラウンド動作
*   **Foreground Service** を実装。
*   通知領域に「現在速度」「ロギングステータス」「経過時間」を表示する。
*   Androidの省電力機能に対応し、**画面OFF（スリープ）状態でもロギングを途切れなく継続**させる。

### 4. 必要なパーミッション
*   `ACCESS_FINE_LOCATION` (位置情報の高精度取得)
*   `ACCESS_BACKGROUND_LOCATION` (バックグラウンドでの位置情報取得)
*   `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_LOCATION` (フォアグラウンドサービス起動用)

---

## 開発フェーズ計画

*   **フェーズ 1 (基本機能実装)**:
    *   GPS速度計の構築（FusedLocationProviderClient連携）
    *   Foreground Serviceによるロギング機能とRoomへの保存
    *   MPAndroidChartを用いた速度・加速度・馬力グラフの描画
*   **フェーズ 2 (物理演算補正実装)**:
    *   馬力推定アルゴリズムの実装
    *   空気抵抗・転がり抵抗の計算処理
    *   IMU/重力センサーを用いた勾配補正アルゴリズム
*   **フェーズ 3 (高度な分析と洗練)**:
    *   CSVエクスポート機能の実装
    *   UIアニメーション・メーターデザインのブラッシュアップ
    *   GPS異常値フィルタの最適化
