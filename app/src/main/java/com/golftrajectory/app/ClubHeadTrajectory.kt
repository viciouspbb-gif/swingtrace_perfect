package com.golftrajectory.app

import android.graphics.PointF
import kotlinx.serialization.Serializable

/**
 * クラブヘッド軌道データ
 */
@Serializable
data class ClubHeadPoint(
    val x: Float,              // 画面上のX座標
    val y: Float,              // 画面上のY座標
    val timestamp: Long,       // タイムスタンプ（ミリ秒）
    val confidence: Float,     // YOLOv8の信頼度
    val phase: SwingPhase      // スイングフェーズ
)

/**
 * スイングフェーズ
 */
enum class SwingPhase {
    SETUP,          // アドレス
    TAKEAWAY,       // テイクバック開始
    BACKSWING,      // バックスイング
    TOP,            // トップ
    DOWNSWING,      // ダウンスイング
    IMPACT,         // インパクト
    FOLLOW_THROUGH, // フォロースルー
    FINISH          // フィニッシュ
}

/**
 * スイング軌道全体
 */
@Serializable
data class SwingTrajectory(
    val id: String,                      // 軌道ID
    val recordedAt: Long,                // 記録日時
    val points: List<ClubHeadPoint>,     // 軌道ポイント
    val videoPath: String? = null,       // 動画パス（オプション）
    val metadata: SwingMetadata          // メタデータ
)

/**
 * スイングメタデータ
 */
@Serializable
data class SwingMetadata(
    val duration: Long,           // スイング時間（ミリ秒）
    val maxSpeed: Float,          // 最大ヘッドスピード（m/s）
    val impactSpeed: Float? = null, // インパクト時のスピード
    val swingPlaneAngle: Float? = null, // スイングプレーン角度
    val clubType: String? = null  // クラブ種類（ドライバー、アイアンなど）
)

/**
 * クラブヘッド検出結果
 */
data class ClubHeadDetection(
    val position: PointF,
    val confidence: Float,
    val boundingBox: android.graphics.RectF,
    val timestamp: Long
)
