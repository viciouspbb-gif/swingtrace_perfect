package com.swingtrace.aicoaching

import android.Manifest
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.golftrajectory.app.TrajectoryEngine
import com.golftrajectory.app.UnitConverter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RealTimeTrajectoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bleManager = remember { BLESensorManager(context) }

    val connectionState by bleManager.connectionState.collectAsState()
    val sensorData by bleManager.sensorData.collectAsState()
    val discoveredDevices by bleManager.discoveredDevices.collectAsState()

    var showDeviceList by remember { mutableStateOf(false) }
    var trajectoryResult by remember { mutableStateOf<TrajectoryEngine.TrajectoryResult?>(null) }
    var animationProgress by remember { mutableStateOf(0f) }
    var distanceUnit by remember { mutableStateOf(UnitConverter.DistanceUnit.METERS) }

    val trajectoryEngine = remember { TrajectoryEngine() }

    val bluetoothPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    LaunchedEffect(sensorData) {
        sensorData?.let { data ->
            trajectoryResult = trajectoryEngine.calculateTrajectory(
                ballSpeed = data.ballSpeed,
                launchAngle = data.launchAngle,
                spinRate = data.spinRate
            )
            animationProgress = 0f
            for (i in 0..100) {
                animationProgress = i / 100f
                delay(20)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            bleManager.stopScan()
            bleManager.disconnect()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1A1A1A),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "REAL-TIME MODE",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FF88),
                        letterSpacing = 2.sp
                    )
                }

                Row {
                    IconButton(
                        onClick = {
                            distanceUnit = if (distanceUnit == UnitConverter.DistanceUnit.METERS) {
                                UnitConverter.DistanceUnit.YARDS
                            } else {
                                UnitConverter.DistanceUnit.METERS
                            }
                        }
                    ) {
                        Text(
                            text = if (distanceUnit == UnitConverter.DistanceUnit.METERS) "m" else "yd",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FF88)
                        )
                    }

                    Button(
                        onClick = {
                            if (connectionState == BLESensorManager.ConnectionState.CONNECTED) {
                                bleManager.disconnect()
                            } else {
                                if (bluetoothPermissions.allPermissionsGranted) {
                                    showDeviceList = true
                                    bleManager.startScan()
                                } else {
                                    bluetoothPermissions.launchMultiplePermissionRequest()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (connectionState) {
                                BLESensorManager.ConnectionState.CONNECTED -> Color(0xFF00FF88)
                                BLESensorManager.ConnectionState.CONNECTING -> Color(0xFFFFAA00)
                                else -> Color(0xFF444444)
                            }
                        )
                    ) {
                        Icon(
                            when (connectionState) {
                                BLESensorManager.ConnectionState.CONNECTED -> Icons.Default.Bluetooth
                                BLESensorManager.ConnectionState.CONNECTING -> Icons.Default.BluetoothSearching
                                else -> Icons.Default.BluetoothDisabled
                            },
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when (connectionState) {
                                BLESensorManager.ConnectionState.CONNECTED -> "接続中"
                                BLESensorManager.ConnectionState.CONNECTING -> "接続中..."
                                else -> "センサー接続"
                            },
                            color = Color.White
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0A0A0A))
        ) {
            if (trajectoryResult != null) {
                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    val width = size.width
                    val height = size.height
                    val maxDistance = (trajectoryResult?.totalDistance ?: 1.0).toFloat()
                    val maxHeight = (trajectoryResult?.maxHeight ?: 1.0).toFloat()
                    val scaleX = width / (maxDistance * 1.1f)
                    val scaleY = height / (maxHeight * 1.5f)

                    val points = trajectoryResult?.points ?: emptyList()
                    val visiblePoints = (points.size * animationProgress).toInt().coerceAtLeast(2)

                    if (visiblePoints >= 2) {
                        val path = androidx.compose.ui.graphics.Path()
                        for (i in 0 until visiblePoints) {
                            val point = points[i]
                            val x = point.x.toFloat() * scaleX
                            val y = height - (point.y.toFloat() * scaleY)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        drawPath(path, color = Color(0xFF00FF88), style = Stroke(width = 8f, cap = StrokeCap.Round))
                        drawPath(path, color = Color(0xFF00FF88).copy(alpha = 0.3f), style = Stroke(width = 16f, cap = StrokeCap.Round))

                        if (visiblePoints > 0) {
                            val currentPoint = points[visiblePoints - 1]
                            val ballX = currentPoint.x.toFloat() * scaleX
                            val ballY = height - (currentPoint.y.toFloat() * scaleY)
                            drawCircle(color = Color.White, radius = 12f, center = Offset(ballX, ballY))
                            drawCircle(color = Color(0xFF00FF88).copy(alpha = 0.5f), radius = 20f, center = Offset(ballX, ballY))
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.SportsGolf, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "センサーを接続してください", fontSize = 18.sp, color = Color(0xFF666666))
                }
            }
        }

        if (sensorData != null && trajectoryResult != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                color = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "📊 ショットデータ", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FF88))
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DataItem("ボール初速", "${String.format("%.1f", sensorData!!.ballSpeed)} m/s")
                        DataItem("打ち出し角", "${String.format("%.1f", sensorData!!.launchAngle)}°")
                        DataItem("スピン量", "${String.format("%.0f", sensorData!!.spinRate)} rpm")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DataItem("飛距離", UnitConverter.formatDistance(trajectoryResult!!.totalDistance, distanceUnit), highlight = true)
                        DataItem("最高到達点", UnitConverter.formatDistance(trajectoryResult!!.maxHeight, distanceUnit))
                        DataItem("滞空時間", "${String.format("%.2f", trajectoryResult!!.flightTime)} 秒")
                    }
                }
            }
        }
    }

    if (showDeviceList) {
        AlertDialog(
            onDismissRequest = { bleManager.stopScan(); showDeviceList = false },
            title = { Text("BLEデバイスを選択") },
            text = {
                LazyColumn {
                    if (discoveredDevices.isEmpty()) {
                        item { Text("デバイスが見つかりません...", modifier = Modifier.padding(16.dp), color = Color.Gray) }
                    } else {
                        items(discoveredDevices) { device ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { bleManager.stopScan(); bleManager.connect(device); showDeviceList = false }.padding(vertical = 8.dp),
                                color = Color(0xFF2A2A2A),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Color(0xFF00D4FF))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = device.name ?: "Unknown Device", fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(text = device.address, fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { bleManager.stopScan(); showDeviceList = false }) { Text("キャンセル") } }
        )
    }
}

@Composable
fun DataItem(label: String, value: String, highlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF888888))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = if (highlight) 24.sp else 18.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) Color(0xFF00FF88) else Color.White
        )
    }
}