package com.golftrajectory.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.golftrajectory.app.*

/**
 * My Bag設定画面 - アスリート向け高級感UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBagScreen(
    userPreferences: UserPreferences,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    // 現在のクラブ設定を取得
    var clubSettings by remember { mutableStateOf(userPreferences.getClubSettings()) }
    
    // 各クラブの編集状態を管理
    var editingStates by remember { mutableStateOf(mutableMapOf<String, Boolean>()) }
    
    // 保存中フラグ
    var isSaving by remember { mutableStateOf(false) }
    
    // 14本のクラブタイプ定義（表示順）
    val clubTypes = listOf(
        ClubType.DRIVER to "1W ドライバー",
        ClubType.FAIRWAY_WOOD_3 to "3W フェアウェイウッド",
        ClubType.FAIRWAY_WOOD_5 to "5W フェアウェイウッド",
        ClubType.UTILITY_2 to "2U ユーティリティ",
        ClubType.UTILITY_3 to "3U ユーティリティ",
        ClubType.UTILITY_4 to "4U ユーティリティ",
        ClubType.UTILITY_5 to "5U ユーティリティ",
        ClubType.IRON_5 to "5I アイアン",
        ClubType.IRON_6 to "6I アイアン",
        ClubType.IRON_7 to "7I アイアン",
        ClubType.IRON_8 to "8I アイアン",
        ClubType.IRON_9 to "9I アイアン",
        ClubType.PITCHING_WEDGE to "PW ピッチングウェッジ",
        ClubType.SAND_WEDGE to "SW サンドウェッジ"
    )
    
    // フレックス選択肢
    val flexOptions = listOf("L", "A", "R", "S", "X", "XX", "TX")
    
    // 保存処理
    fun saveSettings() {
        isSaving = true
        userPreferences.saveClubSettings(clubSettings)
        isSaving = false
        onSave()
    }
    
    // デフォルトロフト角
    fun getDefaultLoftAngle(clubType: ClubType): Double {
        return when (clubType) {
            ClubType.DRIVER -> 9.5
            ClubType.FAIRWAY_WOOD_3 -> 15.0
            ClubType.FAIRWAY_WOOD_5 -> 18.0
            ClubType.UTILITY_2 -> 18.0
            ClubType.UTILITY_3 -> 21.0
            ClubType.UTILITY_4 -> 24.0
            ClubType.UTILITY_5 -> 27.0
            ClubType.IRON_5 -> 26.0
            ClubType.IRON_6 -> 30.0
            ClubType.IRON_7 -> 34.0
            ClubType.IRON_8 -> 38.0
            ClubType.IRON_9 -> 42.0
            ClubType.PITCHING_WEDGE -> 46.0
            ClubType.SAND_WEDGE -> 56.0
            else -> 0.0
        }
    }
    
    // デフォルトシャフト重量
    fun getDefaultShaftWeight(clubType: ClubType): Int {
        return when (clubType) {
            ClubType.DRIVER -> 125
            ClubType.FAIRWAY_WOOD_3, ClubType.FAIRWAY_WOOD_5 -> 105
            ClubType.UTILITY_2, ClubType.UTILITY_3, ClubType.UTILITY_4, ClubType.UTILITY_5 -> 120
            ClubType.IRON_5, ClubType.IRON_6, ClubType.IRON_7, ClubType.IRON_8, ClubType.IRON_9 -> 125
            ClubType.PITCHING_WEDGE, ClubType.SAND_WEDGE -> 125
            else -> 100
        }
    }
    
    // クラブ設定を更新
    fun updateClubSetting(clubType: ClubType, shaftModel: String, shaftFlex: String) {
        val existingClub = clubSettings.find { it.clubType == clubType }
        val updatedClub = if (existingClub != null) {
            existingClub.copy(
                shaftModel = shaftModel,
                shaftFlex = ShaftFlex.values().find { it.displayName == shaftFlex } ?: ShaftFlex.R
            )
        } else {
            // 新規クラブ設定を作成
            val clubId = "club_${clubType.name.lowercase()}"
            UserClubSetting(
                clubId = clubId,
                clubName = clubType.displayName,
                clubType = clubType,
                loftAngle = getDefaultLoftAngle(clubType),
                shaftMaker = "Custom",
                shaftModel = shaftModel,
                shaftFlex = ShaftFlex.values().find { it.displayName == shaftFlex } ?: ShaftFlex.R,
                shaftWeight = getDefaultShaftWeight(clubType),
                isCustom = true,
                notes = "ユーザー設定"
            )
        }
        
        clubSettings = clubSettings.map { 
            if (it.clubType == clubType) updatedClub else it 
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ヘッダー
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color.Black),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "戻る",
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "MY BAG",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 保存ボタン
            Button(
                onClick = { saveSettings() },
                enabled = !isSaving,
                modifier = Modifier
                    .background(Color(0xFF4169E1), RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4169E1),
                    contentColor = Color.White
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = "保存", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存")
                }
            }
        }
        
        // クラブリスト
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(clubTypes) { (clubType, displayName) ->
                ClubSettingItem(
                    clubType = clubType,
                    displayName = displayName,
                    clubSetting = clubSettings.find { it.clubType == clubType },
                    flexOptions = flexOptions,
                    onShaftModelChange = { shaftModel ->
                        val currentFlex = clubSettings.find { it.clubType == clubType }?.shaftFlex?.displayName ?: "R"
                        updateClubSetting(clubType, shaftModel, currentFlex)
                    },
                    onShaftFlexChange = { shaftFlex ->
                        val currentModel = clubSettings.find { it.clubType == clubType }?.shaftModel ?: ""
                        updateClubSetting(clubType, currentModel, shaftFlex)
                    }
                )
            }
        }
    }
}

/**
 * クラブ設定アイテム
 */
@Composable
private fun ClubSettingItem(
    clubType: ClubType,
    displayName: String,
    clubSetting: UserClubSetting?,
    flexOptions: List<String>,
    onShaftModelChange: (String) -> Unit,
    onShaftFlexChange: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var tempShaftModel by remember { mutableStateOf(clubSetting?.shaftModel ?: "") }
    var tempShaftFlex by remember { mutableStateOf(clubSetting?.shaftFlex?.displayName ?: "R") }
    
    val isEmpty = clubSetting == null || clubSetting.shaftModel.isEmpty()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isEmpty) Color(0xFF1A1A1A) else Color(0xFF2A2A2A)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEmpty) 2.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // クラブ名
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isEmpty) Color.Gray else Color.White
                )
                
                if (isEmpty) {
                    Text(
                        text = "未セット (Empty)",
                        fontSize = 12.sp,
                        color = Color(0xFF4169E1),
                        modifier = Modifier
                            .background(Color(0xFF4169E1).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isEditing) {
                // 編集モード
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // シャフト名入力
                    OutlinedTextField(
                        value = tempShaftModel,
                        onValueChange = { tempShaftModel = it },
                        label = { Text("シャフト名", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4169E1),
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White)
                    )
                    
                    // フレックス選択
                    var expanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = tempShaftFlex,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("フレックス", color = Color.Gray) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4169E1),
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color(0xFF2A2A2A))
                        ) {
                            flexOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, color = Color.White) },
                                    onClick = {
                                        tempShaftFlex = option
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // 保存/キャンセルボタン
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onShaftModelChange(tempShaftModel)
                                onShaftFlexChange(tempShaftFlex)
                                isEditing = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4169E1))
                        ) {
                            Text("保存", color = Color.White)
                        }
                        
                        OutlinedButton(
                            onClick = {
                                tempShaftModel = clubSetting?.shaftModel ?: ""
                                tempShaftFlex = clubSetting?.shaftFlex?.displayName ?: "R"
                                isEditing = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                        ) {
                            Text("キャンセル", color = Color.White)
                        }
                    }
                }
            } else {
                // 表示モード
                if (!isEmpty) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "シャフト:",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = clubSetting?.shaftModel ?: "",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "フレックス:",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = clubSetting?.shaftFlex?.displayName ?: "R",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF4169E1)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 編集ボタン
                Button(
                    onClick = {
                        tempShaftModel = clubSetting?.shaftModel ?: ""
                        tempShaftFlex = clubSetting?.shaftFlex?.displayName ?: "R"
                        isEditing = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEmpty) Color(0xFF4169E1) else Color(0xFF333333)
                    )
                ) {
                    Text(
                        text = if (isEmpty) "クラブを設定" else "編集",
                        color = Color.White
                    )
                }
            }
        }
    }
}
