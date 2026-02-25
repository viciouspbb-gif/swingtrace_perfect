package com.swingtrace.aicoaching.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analysis_history")
data class AnalysisHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val videoUri: String,
    val timestamp: Long,
    val ballDetected: Boolean,
    val carryDistance: Double,
    val maxHeight: Double,
    val flightTime: Double,
    val confidence: Double,
    val aiAdvice: String?,
    val aiScore: Int?,
    val swingSpeed: Double?,
    val backswingTime: Double?,
    val downswingTime: Double?,
    val impactSpeed: Double?,
    val tempo: Double?,
    // スイング分析データ
    val totalScore: Int = 0,
    val backswingAngle: Double = 0.0,
    val downswingSpeed: Double = 0.0,
    val hipRotation: Double = 0.0,
    val shoulderRotation: Double = 0.0,
    val headStability: Double = 0.0,
    val weightTransfer: Double = 0.0,
    val swingPlane: String = "",
    // プロ類似度
    val topProName: String? = null,
    val topProSimilarity: Double? = null,
    // 推定飛距離（スイングスピードから計算）
    val estimatedDistance: Double = 0.0
)
