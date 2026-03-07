package com.golftrajectory.app

/**
 * ユーザーのクラブ設定データクラス
 * 14本のクラブセットとシャフト情報を管理
 */
data class UserClubSetting(
    val clubId: String,
    val clubName: String,
    val clubType: ClubType,
    val loftAngle: Double, // ロフト角（度）
    val shaftMaker: String, // シャフトメーカー
    val shaftModel: String, // シャフトモデル（例: Modus 125X）
    val shaftFlex: ShaftFlex, // シャフトフレックス
    val shaftWeight: Int, // シャフト重量（g）
    val isCustom: Boolean = false, // カスタムクラブかどうか
    val notes: String = "" // メモ
)

/**
 * クラブタイプの列挙型
 */
enum class ClubType(val displayName: String) {
    DRIVER("1W"),
    FAIRWAY_WOOD_3("3W"),
    FAIRWAY_WOOD_5("5W"),
    UTILITY_2("2U"),
    UTILITY_3("3U"),
    UTILITY_4("4U"),
    UTILITY_5("5U"),
    IRON_5("5I"),
    IRON_6("6I"),
    IRON_7("7I"),
    IRON_8("8I"),
    IRON_9("9I"),
    PITCHING_WEDGE("PW"),
    SAND_WEDGE("SW"),
    APPROACH_WEDGE("AW"),
    PUTTER("PT")
}

/**
 * シャフトフレックスの列挙型
 */
enum class ShaftFlex(val displayName: String, val swingSpeedRange: ClosedFloatingPointRange<Double>) {
    L("L", 70.0..80.0),        // Ladies
    A("A", 80.0..90.0),        // Senior
    R("R", 90.0..100.0),       // Regular
    S("S", 100.0..110.0),      // Stiff
    X("X", 110.0..120.0),      // Extra Stiff
    XX("XX", 120.0..130.0),    // Double Extra Stiff
    TX("TX", 130.0..140.0)     // Tour Extra Stiff
}

/**
 * デフォルトクラブセットを生成するファクトリーメソッド
 */
object DefaultClubSetFactory {
    
    /**
     * 標準的な14本セットを生成
     */
    fun createStandardSet(): List<UserClubSetting> {
        return listOf(
            // ドライバー
            UserClubSetting(
                clubId = "driver_1",
                clubName = "ドライバー",
                clubType = ClubType.DRIVER,
                loftAngle = 9.5,
                shaftMaker = "Nippon Shaft",
                shaftModel = "Modus 125X",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 125
            ),
            
            // フェアウェイウッド
            UserClubSetting(
                clubId = "fw_3",
                clubName = "3番ウッド",
                clubType = ClubType.FAIRWAY_WOOD_3,
                loftAngle = 15.0,
                shaftMaker = "Nippon Shaft",
                shaftModel = "Modus 105",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 105
            ),
            
            UserClubSetting(
                clubId = "fw_5",
                clubName = "5番ウッド",
                clubType = ClubType.FAIRWAY_WOOD_5,
                loftAngle = 18.0,
                shaftMaker = "Nippon Shaft",
                shaftModel = "Modus 105",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 105
            ),
            
            // ユーティリティ
            UserClubSetting(
                clubId = "ut_2",
                clubName = "2番ユーティリティ",
                clubType = ClubType.UTILITY_2,
                loftAngle = 18.0,
                shaftMaker = "Nippon Shaft",
                shaftModel = "Modus 120",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 120
            ),
            
            UserClubSetting(
                clubId = "ut_3",
                clubName = "3番ユーティリティ",
                clubType = ClubType.UTILITY_3,
                loftAngle = 21.0,
                shaftMaker = "Nippon Shaft",
                shaftModel = "Modus 120",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 120
            ),
            
            UserClubSetting(
                clubId = "ut_4",
                clubName = "4番ユーティリティ",
                clubType = ClubType.UTILITY_4,
                loftAngle = 24.0,
                shaftMaker = "Nippon Shaft",
                shaftModel = "Modus 120",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 120
            ),
            
            // アイアンセット
            UserClubSetting(
                clubId = "ir_5",
                clubName = "5番アイアン",
                clubType = ClubType.IRON_5,
                loftAngle = 26.0,
                shaftMaker = "Nippon Shaft",
                shaftModel = "Modus 125X",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 125
            ),
            
            UserClubSetting(
                clubId = "ir_6",
                clubName = "6番アイアン",
                clubType = ClubType.IRON_6,
                loftAngle = 30.0,
                shaftMaker = "Nippon Shaft",
                shaftModel = "Modus 125X",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 125
            ),
            
            UserClubSetting(
                clubId = "ir_7",
                clubName = "7番アイアン",
                clubType = ClubType.IRON_7,
                loftAngle = 34.0,
                shaftMaker = "Nippon Shaft",
                shaftModel = "Modus 125X",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 125
            ),
            
            UserClubSetting(
                clubId = "ir_8",
                clubName = "8番アイアン",
                clubType = ClubType.IRON_8,
                loftAngle = 38.0,
                shaftMaker = "Nippon Shaft",
                shaftModel = "Modus 125X",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 125
            ),
            
            UserClubSetting(
                clubId = "ir_9",
                clubName = "9番アイアン",
                clubType = ClubType.IRON_9,
                loftAngle = 42.0,
                shaftMaker = "Nippon Shaft",
                shaftModel = "Modus 125X",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 125
            ),
            
            // ウェッジ
            UserClubSetting(
                clubId = "pw",
                clubName = "ピッチングウェッジ",
                clubType = ClubType.PITCHING_WEDGE,
                loftAngle = 46.0,
                shaftMaker = "Nippon Shaft",
                shaftModel = "Modus 125",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 125
            ),
            
            UserClubSetting(
                clubId = "sw",
                clubName = "サンドウェッジ",
                clubType = ClubType.SAND_WEDGE,
                loftAngle = 56.0,
                shaftMaker = "Nippon Shaft",
                shaftModel = "Modus 125",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 125
            ),
            
            // パター
            UserClubSetting(
                clubId = "pt",
                clubName = "パター",
                clubType = ClubType.PUTTER,
                loftAngle = 3.0,
                shaftMaker = "Scotty Cameron",
                shaftModel = "Select Newport",
                shaftFlex = ShaftFlex.L,
                shaftWeight = 340
            )
        )
    }
    
    /**
     * プレミアムセット（Modus 125X全クラブ）を生成
     */
    fun createPremiumSet(): List<UserClubSetting> {
        val standardSet = createStandardSet()
        return standardSet.map { club ->
            if (club.clubType != ClubType.PUTTER) {
                club.copy(
                    shaftMaker = "Nippon Shaft",
                    shaftModel = "Modus 125X",
                    shaftFlex = ShaftFlex.X,
                    shaftWeight = 125,
                    isCustom = true,
                    notes = "プレミアム仕様"
                )
            } else {
                club
            }
        }
    }
}

/**
 * クラブセットのユーティリティ関数
 */
object ClubSetUtils {
    
    /**
     * クラブIDからクラブ設定を検索
     */
    fun findClubById(clubId: String, clubSet: List<UserClubSetting>): UserClubSetting? {
        return clubSet.find { it.clubId == clubId }
    }
    
    /**
     * クラブタイプからクラブ設定を検索
     */
    fun findClubByType(clubType: ClubType, clubSet: List<UserClubSetting>): UserClubSetting? {
        return clubSet.find { it.clubType == clubType }
    }
    
    /**
     * ドライバーを取得
     */
    fun getDriver(clubSet: List<UserClubSetting>): UserClubSetting? {
        return findClubByType(ClubType.DRIVER, clubSet)
    }
    
    /**
     * パターを取得
     */
    fun getPutter(clubSet: List<UserClubSetting>): UserClubSetting? {
        return findClubByType(ClubType.PUTTER, clubSet)
    }
    
    /**
     * アイアンセットのみを取得
     */
    fun getIronSet(clubSet: List<UserClubSetting>): List<UserClubSetting> {
        return clubSet.filter { 
            it.clubType in listOf(
                ClubType.IRON_5, ClubType.IRON_6, ClubType.IRON_7, 
                ClubType.IRON_8, ClubType.IRON_9
            )
        }.sortedBy { it.loftAngle }
    }
    
    /**
     * ウェッジのみを取得
     */
    fun getWedges(clubSet: List<UserClubSetting>): List<UserClubSetting> {
        return clubSet.filter { 
            it.clubType in listOf(
                ClubType.PITCHING_WEDGE, ClubType.SAND_WEDGE, ClubType.APPROACH_WEDGE
            )
        }.sortedBy { it.loftAngle }
    }
    
    /**
     * 木製クラブ（ウッド、ユーティリティ）を取得
     */
    fun getWoods(clubSet: List<UserClubSetting>): List<UserClubSetting> {
        return clubSet.filter { 
            it.clubType in listOf(
                ClubType.DRIVER, ClubType.FAIRWAY_WOOD_3, ClubType.FAIRWAY_WOOD_5,
                ClubType.UTILITY_2, ClubType.UTILITY_3, ClubType.UTILITY_4, ClubType.UTILITY_5
            )
        }.sortedBy { it.loftAngle }
    }
    
    /**
     * スイング速度に適したシャフトフレックスを推奨
     */
    fun recommendShaftFlex(swingSpeed: Double): ShaftFlex {
        return ShaftFlex.values().find { flex ->
            swingSpeed in flex.swingSpeedRange
        } ?: ShaftFlex.R // デフォルトはRegular
    }
}
