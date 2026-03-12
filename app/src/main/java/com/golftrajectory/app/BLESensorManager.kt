package com.swingtrace.aicoaching

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

/**
 * BLEセンサーマネージャー
 * Rapsodo、Garminなどのゴルフ測定器と接続してリアルタイムデータを受信
 */
class BLESensorManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BLESensorManager"
    }
    
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    
    private var bluetoothGatt: BluetoothGatt? = null
    private var isScanning = false
    
    // センサーデータのFlow
    private val _sensorData = MutableStateFlow<SensorData?>(null)
    val sensorData: StateFlow<SensorData?> = _sensorData
    
    // 接続状態
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    
    // スキャン結果
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices
    
    /**
     * センサーデータ
     */
    data class SensorData(
        val ballSpeed: Double,      // ボール初速 (m/s)
        val launchAngle: Double,    // 打ち出し角 (度)
        val launchDirection: Double, // 打ち出し方向 (度)
        val spinRate: Double,        // スピン量 (rpm)
        val spinAxis: Double,        // スピン軸の傾き (度)
        val clubSpeed: Double,       // クラブ速度 (m/s)
        val timestamp: Long          // タイムスタンプ
    )
    
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }
    
    /**
     * BLEデバイスをスキャン
     */
    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!hasPermissions()) {
            Log.e(TAG, "Bluetooth権限がありません")
            return
        }
        
        if (isScanning) {
            Log.w(TAG, "既にスキャン中です")
            return
        }
        
        if (bluetoothLeScanner == null) {
            Log.e(TAG, "BluetoothLEScannerが利用できません")
            return
        }
        
        isScanning = true
        _discoveredDevices.value = emptyList()
        
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        Log.d(TAG, "BLEスキャン開始")
        bluetoothLeScanner?.startScan(null, scanSettings, scanCallback)
    }
    
    /**
     * スキャン停止
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning) return
        
        isScanning = false
        bluetoothLeScanner?.stopScan(scanCallback)
        Log.d(TAG, "BLEスキャン停止")
    }
    
    /**
     * デバイスに接続
     */
    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        if (!hasPermissions()) {
            Log.e(TAG, "Bluetooth権限がありません")
            return
        }
        
        _connectionState.value = ConnectionState.CONNECTING
        Log.d(TAG, "デバイスに接続中: ${device.name}")
        
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }
    
    /**
     * 接続解除
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = ConnectionState.DISCONNECTED
        Log.d(TAG, "接続解除")
    }
    
    /**
     * スキャンコールバック
     */
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (device.name != null && !_discoveredDevices.value.contains(device)) {
                _discoveredDevices.value = _discoveredDevices.value + device
                Log.d(TAG, "デバイス発見: ${device.name} (${device.address})")
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "スキャン失敗: $errorCode")
            isScanning = false
        }
    }
    
    /**
     * GATTコールバック
     */
    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "デバイスに接続しました")
                    _connectionState.value = ConnectionState.CONNECTED
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "デバイスから切断されました")
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            }
        }
        
        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "サービス発見完了")
                
                // すべてのサービスと特性をログ出力
                gatt.services.forEach { service ->
                    Log.d(TAG, "Service: ${service.uuid}")
                    service.characteristics.forEach { char ->
                        Log.d(TAG, "  Characteristic: ${char.uuid}")
                        
                        // 通知可能な特性を有効化
                        if (char.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                            gatt.setCharacteristicNotification(char, true)
                            
                            val descriptor = char.getDescriptor(
                                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                            )
                            if (descriptor != null) {
                                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt.writeDescriptor(descriptor)
                                Log.d(TAG, "通知を有効化: ${char.uuid}")
                            }
                        }
                    }
                }
            }
        }
        
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            // センサーデータを解析
            val data = parseSensorData(characteristic.value)
            if (data != null) {
                _sensorData.value = data
                Log.d(TAG, "センサーデータ受信: Ball=${data.ballSpeed}m/s, Angle=${data.launchAngle}°")
            }
        }
    }
    
    /**
     * センサーデータを解析
     * ※実際のデバイスのプロトコルに合わせて実装が必要
     */
    private fun parseSensorData(bytes: ByteArray): SensorData? {
        try {
            // モックデータ（テスト用）
            return SensorData(
                ballSpeed = 70.0 + (Math.random() * 10),
                launchAngle = 12.0 + (Math.random() * 4),
                launchDirection = -2.0 + (Math.random() * 4),
                spinRate = 2500.0 + (Math.random() * 500),
                spinAxis = -5.0 + (Math.random() * 10),
                clubSpeed = 45.0 + (Math.random() * 5),
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "データ解析エラー: ${e.message}")
            return null
        }
    }
    
    /**
     * Bluetooth権限チェック
     */
    private fun hasPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }
}