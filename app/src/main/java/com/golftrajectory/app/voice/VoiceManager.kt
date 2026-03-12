package com.swingtrace.aicoaching.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.swingtrace.aicoaching.utils.TTSController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

/**
 * 音声認識・音声合成マネージャー
 */
class VoiceManager(private val context: Context) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening
    
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking
    
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText
    
    companion object {
        private const val TAG = "VoiceManager"
        private const val DEFAULT_SPEECH_RATE = 0.75f  // デフォルト速度（0.5～2.0、1.0が通常）
        private const val DEFAULT_PITCH = 1.0f          // デフォルトピッチ（0.5～2.0、1.0が通常）
    }
    
    init {
        initializeTts()
        initializeSpeechRecognizer()
    }
    
    /**
     * Text-to-Speech 初期化
     */
    private fun initializeTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.JAPANESE)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "日本語がサポートされていません")
                    // 英語にフォールバック
                    textToSpeech?.setLanguage(Locale.ENGLISH)
                }
                isTtsInitialized = true
                
                // 読み上げ速度とピッチを設定
                textToSpeech?.setSpeechRate(DEFAULT_SPEECH_RATE)
                textToSpeech?.setPitch(DEFAULT_PITCH)
                
                // TTSControllerに登録
                textToSpeech?.let { TTSController.register(it) }
                
                // 音声合成の進行状況を監視
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }
                    
                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                    
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        Log.e(TAG, "TTS Error: $utteranceId")
                    }
                })
            } else {
                Log.e(TAG, "TTS初期化失敗")
            }
        }
    }
    
    /**
     * 音声認識初期化
     */
    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "音声認識が利用できません")
            return
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    }
    
    /**
     * 音声認識開始
     */
    fun startListening(language: String = "ja-JP", onResult: (String) -> Unit) {
        if (_isListening.value) {
            Log.w(TAG, "既に音声認識中です")
            return
        }
        
        // AI音声を停止
        TTSController.stopAll()
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                Log.d(TAG, "音声認識準備完了")
            }
            
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "音声入力開始")
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // 音量レベル（必要に応じて使用）
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                _isListening.value = false
                Log.d(TAG, "音声入力終了")
            }
            
            override fun onError(error: Int) {
                _isListening.value = false
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "音声エラー"
                    SpeechRecognizer.ERROR_CLIENT -> "クライアントエラー"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "権限不足"
                    SpeechRecognizer.ERROR_NETWORK -> "ネットワークエラー"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ネットワークタイムアウト"
                    SpeechRecognizer.ERROR_NO_MATCH -> "認識できませんでした"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "認識エンジンビジー"
                    SpeechRecognizer.ERROR_SERVER -> "サーバーエラー"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "音声タイムアウト"
                    else -> "不明なエラー"
                }
                Log.e(TAG, "音声認識エラー: $errorMessage")
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    _recognizedText.value = text
                    onResult(text)
                    Log.d(TAG, "認識結果: $text")
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    _recognizedText.value = matches[0]
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        
        speechRecognizer?.startListening(intent)
    }
    
    /**
     * 音声認識停止
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }
    
    /**
     * テキストを音声で読み上げ
     */
    fun speak(text: String, language: String = "ja-JP") {
        if (!isTtsInitialized) {
            Log.e(TAG, "TTS未初期化")
            return
        }
        
        // 言語設定
        val locale = if (language.startsWith("en")) Locale.ENGLISH else Locale.JAPANESE
        textToSpeech?.language = locale
        
        // 読み上げ
        val utteranceId = UUID.randomUUID().toString()
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }
    
    /**
     * 音声合成停止
     */
    fun stopSpeaking() {
        textToSpeech?.stop()
        _isSpeaking.value = false
    }
    
    /**
     * 読み上げ速度を設定
     * @param rate 速度（0.5～2.0、1.0が通常、0.75が少し遅め）
     */
    fun setSpeechRate(rate: Float) {
        textToSpeech?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }
    
    /**
     * ピッチを設定
     * @param pitch ピッチ（0.5～2.0、1.0が通常）
     */
    fun setPitch(pitch: Float) {
        textToSpeech?.setPitch(pitch.coerceIn(0.5f, 2.0f))
    }
    
    /**
     * リソース解放
     */
    fun release() {
        speechRecognizer?.destroy()
        textToSpeech?.let {
            it.stop()
            it.shutdown()
            TTSController.unregister(it)
        }
        speechRecognizer = null
        textToSpeech = null
    }
}
