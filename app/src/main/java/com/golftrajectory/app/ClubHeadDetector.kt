package com.golftrajectory.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import ai.onnxruntime.*
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * YOLOv8 クラブヘッド検出器（ONNX Runtime）
 */
class ClubHeadDetector(context: Context) {
    
    private val ortEnv = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    
    companion object {
        private const val INPUT_SIZE = 640
        private const val CONFIDENCE_THRESHOLD = 0.001f  // テスト用に極端に下げる
        private const val IOU_THRESHOLD = 0.45f
    }
    
    init {
        // ONNXモデルをロード
        try {
            val modelBytes = context.assets.open("clubhead_yolov8.onnx").readBytes()
            session = ortEnv.createSession(modelBytes)
        } catch (e: Exception) {
            // モデルファイルが存在しない場合のエラーハンドリング
            throw IllegalStateException(
                "YOLOv8モデルファイルが見つかりません。\n" +
                "app/src/main/assets/clubhead_yolov8.onnx を配置してください。\n" +
                "エラー: ${e.message}"
            )
        }
    }
    
    /**
     * クラブヘッドを検出
     */
    fun detect(bitmap: Bitmap): ClubHeadDetection? {
        android.util.Log.d("ClubHeadDetector", "detect()呼び出し: bitmap=${bitmap.width}x${bitmap.height}")
        
        // 前処理
        val inputTensor = preprocessImage(bitmap)
        android.util.Log.d("ClubHeadDetector", "前処理完了")
        
        // 推論
        val outputs = session.run(mapOf("images" to inputTensor))
        val output = outputs[0].value
        
        android.util.Log.d("ClubHeadDetector", "推論完了: output type=${output?.javaClass?.name}")
        
        // 出力形式を確認して1次元配列に変換
        val flatArray = when (output) {
            is Array<*> -> {
                // [[[F の場合: [1][5][8400] -> [5*8400]
                val firstDim = output[0] as? Array<*>
                if (firstDim != null && firstDim[0] is FloatArray) {
                    // [[F -> [F に変換
                    val result = mutableListOf<Float>()
                    for (row in firstDim) {
                        result.addAll((row as FloatArray).toList())
                    }
                    result.toFloatArray()
                } else {
                    android.util.Log.e("ClubHeadDetector", "予期しない配列構造")
                    return null
                }
            }
            is FloatArray -> output
            else -> {
                android.util.Log.e("ClubHeadDetector", "未知の出力形式: ${output?.javaClass?.name}")
                return null
            }
        }
        
        android.util.Log.d("ClubHeadDetector", "変換後配列サイズ: ${flatArray.size}")
        
        // 後処理
        val detections = postprocess(arrayOf(flatArray), bitmap.width, bitmap.height)
        android.util.Log.d("ClubHeadDetector", "検出数: ${detections.size}")
        
        // 信頼度が最も高い検出を返す
        return detections.maxByOrNull { it.confidence }
    }
    
    /**
     * 画像を前処理
     */
    private fun preprocessImage(bitmap: Bitmap): OnnxTensor {
        // リサイズ
        val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        android.util.Log.d("ClubHeadDetector", "リサイズ完了: ${INPUT_SIZE}x${INPUT_SIZE}")
        
        // RGB配列に変換
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        
        // 正規化 [0, 255] -> [0, 1]
        val floatBuffer = FloatBuffer.allocate(3 * INPUT_SIZE * INPUT_SIZE)
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = ((pixel shr 16) and 0xFF) / 255f
            val g = ((pixel shr 8) and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f
            
            // CHW形式 (Channel, Height, Width)
            floatBuffer.put(r)
            floatBuffer.put(g)
            floatBuffer.put(b)
        }
        
        floatBuffer.rewind()
        
        // テンソルを作成
        val shape = longArrayOf(1, 3, INPUT_SIZE.toLong(), INPUT_SIZE.toLong())
        return OnnxTensor.createTensor(ortEnv, floatBuffer, shape)
    }
    
    /**
     * 出力を後処理
     */
    private fun postprocess(
        output: Array<*>,
        originalWidth: Int,
        originalHeight: Int
    ): List<ClubHeadDetection> {
        val detections = mutableListOf<ClubHeadDetection>()
        
        // YOLOv8の出力形式を取得
        val data = when (val firstOutput = output[0]) {
            is FloatArray -> {
                // [1, 5, 8400] 形式を [8400, 5] に変換
                firstOutput
            }
            is Array<*> -> {
                // [1, 8400, 5] 形式
                (firstOutput[0] as FloatArray)
            }
            else -> {
                return emptyList()
            }
        }
        
        val scaleX = originalWidth.toFloat() / INPUT_SIZE
        val scaleY = originalHeight.toFloat() / INPUT_SIZE
        
        // YOLOv8の出力形式: [1, 5, 8400] (転置されている)
        // 5 = [x, y, w, h, confidence]
        val numDetections = 8400
        val numFeatures = 5
        
        // データサイズをチェック
        if (data.size < numDetections * numFeatures) {
            return emptyList()
        }
        
        var maxConfidence = 0f
        
        for (i in 0 until numDetections) {
            // [1, 5, 8400] 形式: data[feature * 8400 + i]
            val x = data[0 * numDetections + i] * scaleX
            val y = data[1 * numDetections + i] * scaleY
            val w = data[2 * numDetections + i] * scaleX
            val h = data[3 * numDetections + i] * scaleY
            val confidence = data[4 * numDetections + i]
            
            if (confidence > maxConfidence) {
                maxConfidence = confidence
            }
            
            if (confidence >= CONFIDENCE_THRESHOLD) {
                val left = x - w / 2
                val top = y - h / 2
                val right = x + w / 2
                val bottom = y + h / 2
                
                detections.add(
                    ClubHeadDetection(
                        position = android.graphics.PointF(x, y),
                        confidence = confidence,
                        boundingBox = RectF(left, top, right, bottom),
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
        
        android.util.Log.d("ClubHeadDetector", "最大信頼度: $maxConfidence, 検出数: ${detections.size}")
        
        // NMS（Non-Maximum Suppression）
        return applyNMS(detections)
    }
    
    /**
     * NMSを適用
     */
    private fun applyNMS(detections: List<ClubHeadDetection>): List<ClubHeadDetection> {
        if (detections.isEmpty()) return emptyList()
        
        val sorted = detections.sortedByDescending { it.confidence }
        val result = mutableListOf<ClubHeadDetection>()
        
        for (detection in sorted) {
            var shouldAdd = true
            
            for (selected in result) {
                val iou = calculateIoU(detection.boundingBox, selected.boundingBox)
                if (iou > IOU_THRESHOLD) {
                    shouldAdd = false
                    break
                }
            }
            
            if (shouldAdd) {
                result.add(detection)
            }
        }
        
        return result
    }
    
    /**
     * IoU（Intersection over Union）を計算
     */
    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val intersectLeft = max(box1.left, box2.left)
        val intersectTop = max(box1.top, box2.top)
        val intersectRight = min(box1.right, box2.right)
        val intersectBottom = min(box1.bottom, box2.bottom)
        
        if (intersectRight < intersectLeft || intersectBottom < intersectTop) {
            return 0f
        }
        
        val intersectArea = (intersectRight - intersectLeft) * (intersectBottom - intersectTop)
        val box1Area = (box1.right - box1.left) * (box1.bottom - box1.top)
        val box2Area = (box2.right - box2.left) * (box2.bottom - box2.top)
        val unionArea = box1Area + box2Area - intersectArea
        
        return intersectArea / unionArea
    }
    
    /**
     * リソースを解放
     */
    fun close() {
        session.close()
    }
}
