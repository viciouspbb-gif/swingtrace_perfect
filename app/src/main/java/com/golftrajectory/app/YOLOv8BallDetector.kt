package com.golftrajectory.app

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * YOLOv8によるゴルフボール自動検出
 * 完全無料・オフライン動作
 */
class YOLOv8BallDetector(private val context: Context) {
    
    private var interpreter: Interpreter? = null
    private val inputSize = 640 // YOLOv8の標準入力サイズ
    private val confidenceThreshold = 0.5f // 検出信頼度の閾値
    
    data class Detection(
        val position: Offset,
        val confidence: Float,
        val boundingBox: BoundingBox
    )
    
    data class BoundingBox(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )
    
    init {
        loadModel()
    }
    
    /**
     * YOLOv8モデルをロード
     */
    private fun loadModel() {
        try {
            val options = Interpreter.Options()
            
            // CPUで実行（4スレッド）
            // GPU使用は一時的に無効化
            options.setNumThreads(4)
            
            // モデルファイルをロード
            // 注: assets/yolov8n.tflite にモデルファイルを配置する必要があります
            val modelFile = loadModelFile("yolov8n.tflite")
            interpreter = Interpreter(modelFile, options)
            
        } catch (e: Exception) {
            e.printStackTrace()
            // モデルがない場合は警告のみ（アプリは動作）
            println("⚠️ YOLOv8モデルが見つかりません: ${e.message}")
        }
    }
    
    /**
     * assetsからモデルファイルをロード
     */
    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    /**
     * ビットマップからボールを検出
     */
    fun detectBall(bitmap: Bitmap): Detection? {
        if (interpreter == null) {
            // モデルがロードされていない場合はnull
            return null
        }
        
        try {
            // 画像を前処理
            val inputBuffer = preprocessImage(bitmap)
            
            // 推論実行
            val outputBuffer = Array(1) { Array(25200) { FloatArray(85) } }
            interpreter?.run(inputBuffer, outputBuffer)
            
            // 結果を解析してボールを検出
            val detections = parseOutput(outputBuffer[0], bitmap.width, bitmap.height)
            
            // 最も信頼度の高い検出を返す
            return detections.maxByOrNull { it.confidence }
            
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * 画像を前処理（YOLOv8入力形式に変換）
     */
    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        inputBuffer.order(ByteOrder.nativeOrder())
        
        // リサイズ
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        
        val intValues = IntArray(inputSize * inputSize)
        resizedBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)
        
        // 正規化（0-255 → 0-1）
        var pixel = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val value = intValues[pixel++]
                
                // RGB順で格納
                inputBuffer.putFloat(((value shr 16) and 0xFF) / 255.0f)
                inputBuffer.putFloat(((value shr 8) and 0xFF) / 255.0f)
                inputBuffer.putFloat((value and 0xFF) / 255.0f)
            }
        }
        
        return inputBuffer
    }
    
    /**
     * YOLOv8の出力を解析
     */
    private fun parseOutput(
        output: Array<FloatArray>,
        imageWidth: Int,
        imageHeight: Int
    ): List<Detection> {
        val detections = mutableListOf<Detection>()
        
        // YOLOv8の出力形式: [x, y, w, h, confidence, class_scores...]
        for (i in output.indices) {
            val detection = output[i]
            
            // 信頼度チェック
            val confidence = detection[4]
            if (confidence < confidenceThreshold) continue
            
            // バウンディングボックス
            val centerX = detection[0] * imageWidth / inputSize
            val centerY = detection[1] * imageHeight / inputSize
            val width = detection[2] * imageWidth / inputSize
            val height = detection[3] * imageHeight / inputSize
            
            val left = centerX - width / 2
            val top = centerY - height / 2
            val right = centerX + width / 2
            val bottom = centerY + height / 2
            
            // 小さい物体（ボール）のみ検出
            val boxArea = width * height
            val imageArea = imageWidth * imageHeight
            val areaRatio = boxArea / imageArea
            
            // ボールは画面の0.1%〜5%程度のサイズ
            if (areaRatio in 0.001f..0.05f) {
                detections.add(
                    Detection(
                        position = Offset(centerX, centerY),
                        confidence = confidence,
                        boundingBox = BoundingBox(left, top, right, bottom)
                    )
                )
            }
        }
        
        return detections
    }
    
    /**
     * リソース解放
     */
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
