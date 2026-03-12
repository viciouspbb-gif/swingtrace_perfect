package com.golftrajectory.app

/**
 * デフォルトクラブセットファクトリー
 * 世界標準の一般的なスペックを提供
 */
object DefaultClubSetFactory {
    
    /**
     * 世界標準のデフォルトクラブセットを生成
     * PGAツアー平均と一般アマチュアの最も一般的な組み合わせ
     */
    fun createStandardClubSet(): List<UserClubSetting> {
        return listOf(
            // ドライバー - 最も一般的なSフレックス
            createClub(
                clubType = ClubType.DRIVER,
                clubName = "1W ドライバー",
                loftAngle = 9.5,
                shaftMaker = "Mitsubishi",
                shaftModel = "Diamana",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 65
            ),
            
            // フェアウェイウッド
            createClub(
                clubType = ClubType.FAIRWAY_WOOD_3,
                clubName = "3W フェアウェイウッド",
                loftAngle = 15.0,
                shaftMaker = "Mitsubishi",
                shaftModel = "Diamana",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 75
            ),
            
            createClub(
                clubType = ClubType.FAIRWAY_WOOD_5,
                clubName = "5W フェアウェイウッド",
                loftAngle = 18.0,
                shaftMaker = "Mitsubishi",
                shaftModel = "Diamana",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 80
            ),
            
            // ユーティリティ（ハイブリッド）
            createClub(
                clubType = ClubType.UTILITY_2,
                clubName = "2U ユーティリティ",
                loftAngle = 18.0,
                shaftMaker = "True Temper",
                shaftModel = "Dynamic Gold",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 95
            ),
            
            createClub(
                clubType = ClubType.UTILITY_3,
                clubName = "3U ユーティリティ",
                loftAngle = 21.0,
                shaftMaker = "True Temper",
                shaftModel = "Dynamic Gold",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 95
            ),
            
            createClub(
                clubType = ClubType.UTILITY_4,
                clubName = "4U ユーティリティ",
                loftAngle = 24.0,
                shaftMaker = "True Temper",
                shaftModel = "Dynamic Gold",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 95
            ),
            
            createClub(
                clubType = ClubType.UTILITY_5,
                clubName = "5U ユーティリティ",
                loftAngle = 27.0,
                shaftMaker = "True Temper",
                shaftModel = "Dynamic Gold",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 95
            ),
            
            // アイアンセット - 標準スチールシャフト
            createClub(
                clubType = ClubType.IRON_5,
                clubName = "5I アイアン",
                loftAngle = 26.0,
                shaftMaker = "True Temper",
                shaftModel = "Dynamic Gold S300",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 127
            ),
            
            createClub(
                clubType = ClubType.IRON_6,
                clubName = "6I アイアン",
                loftAngle = 30.0,
                shaftMaker = "True Temper",
                shaftModel = "Dynamic Gold S300",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 127
            ),
            
            createClub(
                clubType = ClubType.IRON_7,
                clubName = "7I アイアン",
                loftAngle = 34.0,
                shaftMaker = "True Temper",
                shaftModel = "Dynamic Gold S300",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 127
            ),
            
            createClub(
                clubType = ClubType.IRON_8,
                clubName = "8I アイアン",
                loftAngle = 38.0,
                shaftMaker = "True Temper",
                shaftModel = "Dynamic Gold S300",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 127
            ),
            
            createClub(
                clubType = ClubType.IRON_9,
                clubName = "9I アイアン",
                loftAngle = 42.0,
                shaftMaker = "True Temper",
                shaftModel = "Dynamic Gold S300",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 127
            ),
            
            // ウェッジ
            createClub(
                clubType = ClubType.PITCHING_WEDGE,
                clubName = "PW ピッチングウェッジ",
                loftAngle = 46.0,
                shaftMaker = "True Temper",
                shaftModel = "Dynamic Gold S300",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 127
            ),
            
            createClub(
                clubType = ClubType.SAND_WEDGE,
                clubName = "SW サンドウェッジ",
                loftAngle = 56.0,
                shaftMaker = "True Temper",
                shaftModel = "Dynamic Gold S300",
                shaftFlex = ShaftFlex.S,
                shaftWeight = 127
            )
        )
    }
    
    /**
     * シニア向け柔らかめのクラブセット
     */
    fun createSeniorClubSet(): List<UserClubSetting> {
        return listOf(
            createClub(
                clubType = ClubType.DRIVER,
                clubName = "1W ドライバー",
                loftAngle = 10.5,
                shaftMaker = "Fujikura",
                shaftModel = "Ventus",
                shaftFlex = ShaftFlex.R,
                shaftWeight = 55
            ),
            
            createClub(
                clubType = ClubType.FAIRWAY_WOOD_3,
                clubName = "3W フェアウェイウッド",
                loftAngle = 16.0,
                shaftMaker = "Fujikura",
                shaftModel = "Ventus",
                shaftFlex = ShaftFlex.R,
                shaftWeight = 65
            ),
            
            createClub(
                clubType = ClubType.IRON_7,
                clubName = "7I アイアン",
                loftAngle = 34.0,
                shaftMaker = "Nippon",
                shaftModel = "NS Pro 950GH",
                shaftFlex = ShaftFlex.R,
                shaftWeight = 95
            ),
            
            createClub(
                clubType = ClubType.PITCHING_WEDGE,
                clubName = "PW ピッチングウェッジ",
                loftAngle = 46.0,
                shaftMaker = "Nippon",
                shaftModel = "NS Pro 950GH",
                shaftFlex = ShaftFlex.R,
                shaftWeight = 95
            )
        )
    }
    
    /**
     * 女性向け軽量クラブセット
     */
    fun createLadiesClubSet(): List<UserClubSetting> {
        return listOf(
            createClub(
                clubType = ClubType.DRIVER,
                clubName = "1W ドライバー",
                loftAngle = 12.0,
                shaftMaker = "Fujikura",
                shaftModel = "Speeder",
                shaftFlex = ShaftFlex.L,
                shaftWeight = 45
            ),
            
            createClub(
                clubType = ClubType.FAIRWAY_WOOD_3,
                clubName = "3W フェアウェイウッド",
                loftAngle = 17.0,
                shaftMaker = "Fujikura",
                shaftModel = "Speeder",
                shaftFlex = ShaftFlex.L,
                shaftWeight = 50
            ),
            
            createClub(
                clubType = ClubType.IRON_7,
                clubName = "7I アイアン",
                loftAngle = 34.0,
                shaftMaker = "Nippon",
                shaftModel = "NS Pro 850GH",
                shaftFlex = ShaftFlex.L,
                shaftWeight = 85
            ),
            
            createClub(
                clubType = ClubType.PITCHING_WEDGE,
                clubName = "PW ピッチングウェッジ",
                loftAngle = 46.0,
                shaftMaker = "Nippon",
                shaftModel = "NS Pro 850GH",
                shaftFlex = ShaftFlex.L,
                shaftWeight = 85
            )
        )
    }
    
    /**
     * プロ向け剛性の高いクラブセット
     */
    fun createProClubSet(): List<UserClubSetting> {
        return listOf(
            createClub(
                clubType = ClubType.DRIVER,
                clubName = "1W ドライバー",
                loftAngle = 8.5,
                shaftMaker = "Graphite Design",
                shaftModel = "Tour AD",
                shaftFlex = ShaftFlex.X,
                shaftWeight = 75
            ),
            
            createClub(
                clubType = ClubType.IRON_7,
                clubName = "7I アイアン",
                loftAngle = 34.0,
                shaftMaker = "True Temper",
                shaftModel = "Dynamic Gold X100",
                shaftFlex = ShaftFlex.X,
                shaftWeight = 130
            ),
            
            createClub(
                clubType = ClubType.PITCHING_WEDGE,
                clubName = "PW ピッチングウェッジ",
                loftAngle = 46.0,
                shaftMaker = "True Temper",
                shaftModel = "Dynamic Gold X100",
                shaftFlex = ShaftFlex.X,
                shaftWeight = 130
            )
        )
    }
    
    /**
     * 個別クラブ設定のヘルパー関数
     */
    private fun createClub(
        clubType: ClubType,
        clubName: String,
        loftAngle: Double,
        shaftMaker: String,
        shaftModel: String,
        shaftFlex: ShaftFlex,
        shaftWeight: Int
    ): UserClubSetting {
        return UserClubSetting(
            clubId = "club_${clubType.name.lowercase()}",
            clubName = clubName,
            clubType = clubType,
            loftAngle = loftAngle,
            shaftMaker = shaftMaker,
            shaftModel = shaftModel,
            shaftFlex = shaftFlex,
            shaftWeight = shaftWeight,
            isCustom = false,
            notes = "世界標準スペック"
        )
    }
    
    /**
     * ユーザーのレベルに応じた推奨クラブセットを取得
     */
    fun getRecommendedClubSet(skillLevel: String, gender: String = "male", age: Int = 30): List<UserClubSetting> {
        return when {
            // プロレベル
            skillLevel.lowercase() == "pro" -> createProClubSet()
            
            // シニア（50歳以上）
            age >= 50 -> createSeniorClubSet()
            
            // 女性向け
            gender.lowercase() == "female" -> createLadiesClubSet()
            
            // 一般男性（デフォルト）
            else -> createStandardClubSet()
        }
    }
    
    /**
     * 特定のフレックスに対応する推奨シャフト重量を取得
     */
    fun getRecommendedShaftWeight(shaftFlex: ShaftFlex, clubType: ClubType): Int {
        return when (shaftFlex) {
            ShaftFlex.L -> when (clubType) {
                ClubType.DRIVER -> 45
                ClubType.FAIRWAY_WOOD_3, ClubType.FAIRWAY_WOOD_5 -> 55
                else -> 85
            }
            ShaftFlex.A, ShaftFlex.R -> when (clubType) {
                ClubType.DRIVER -> 55
                ClubType.FAIRWAY_WOOD_3, ClubType.FAIRWAY_WOOD_5 -> 65
                else -> 95
            }
            ShaftFlex.S -> when (clubType) {
                ClubType.DRIVER -> 65
                ClubType.FAIRWAY_WOOD_3, ClubType.FAIRWAY_WOOD_5 -> 75
                else -> 127
            }
            ShaftFlex.X -> when (clubType) {
                ClubType.DRIVER -> 75
                ClubType.FAIRWAY_WOOD_3, ClubType.FAIRWAY_WOOD_5 -> 85
                else -> 130
            }
            ShaftFlex.XX, ShaftFlex.TX -> when (clubType) {
                ClubType.DRIVER -> 80
                ClubType.FAIRWAY_WOOD_3, ClubType.FAIRWAY_WOOD_5 -> 90
                else -> 135
            }
        }
    }
}
