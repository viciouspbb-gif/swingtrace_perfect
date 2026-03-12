package com.golftrajectory.app

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room Database for Trajectory Storage
 */
@Database(
    entities = [TrajectoryEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TrajectoryDatabase : RoomDatabase() {
    abstract fun trajectoryDao(): TrajectoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: TrajectoryDatabase? = null
        
        fun getDatabase(context: Context): TrajectoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrajectoryDatabase::class.java,
                    "trajectory_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Trajectory Entity
 */
@Entity(tableName = "trajectories")
data class TrajectoryEntity(
    @PrimaryKey val id: String,
    val recordedAt: Long,
    val pointsJson: String,        // JSON形式で保存
    val videoPath: String?,
    val duration: Long,
    val maxSpeed: Float,
    val impactSpeed: Float?,
    val swingPlaneAngle: Float?,
    val clubType: String?
)

/**
 * DAO
 */
@Dao
interface TrajectoryDao {
    
    @Query("SELECT * FROM trajectories ORDER BY recordedAt DESC")
    fun getAllTrajectories(): Flow<List<TrajectoryEntity>>
    
    @Query("SELECT * FROM trajectories WHERE id = :id")
    suspend fun getTrajectoryById(id: String): TrajectoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrajectory(trajectory: TrajectoryEntity)
    
    @Delete
    suspend fun deleteTrajectory(trajectory: TrajectoryEntity)
    
    @Query("DELETE FROM trajectories WHERE recordedAt < :timestamp")
    suspend fun deleteOldTrajectories(timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM trajectories")
    suspend fun getTrajectoryCount(): Int
}

/**
 * Type Converters
 */
class Converters {
    private val json = Json { ignoreUnknownKeys = true }
    
    @TypeConverter
    fun fromPointsList(points: List<ClubHeadPoint>): String {
        return json.encodeToString(points)
    }
    
    @TypeConverter
    fun toPointsList(pointsJson: String): List<ClubHeadPoint> {
        return json.decodeFromString(pointsJson)
    }
}

/**
 * Repository
 */
class TrajectoryRepository(private val dao: TrajectoryDao) {
    
    val allTrajectories: Flow<List<SwingTrajectory>> = 
        dao.getAllTrajectories().map { entities ->
            entities.map { it.toSwingTrajectory() }
        }
    
    suspend fun saveTrajectory(trajectory: SwingTrajectory) {
        dao.insertTrajectory(trajectory.toEntity())
    }
    
    suspend fun getTrajectory(id: String): SwingTrajectory? {
        return dao.getTrajectoryById(id)?.toSwingTrajectory()
    }
    
    suspend fun deleteTrajectory(trajectory: SwingTrajectory) {
        dao.deleteTrajectory(trajectory.toEntity())
    }
    
    suspend fun deleteOldTrajectories(daysOld: Int) {
        val timestamp = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        dao.deleteOldTrajectories(timestamp)
    }
}

/**
 * Extension functions for conversion
 */
private fun SwingTrajectory.toEntity(): TrajectoryEntity {
    val json = Json.encodeToString(points)
    return TrajectoryEntity(
        id = id,
        recordedAt = recordedAt,
        pointsJson = json,
        videoPath = videoPath,
        duration = metadata.duration,
        maxSpeed = metadata.maxSpeed,
        impactSpeed = metadata.impactSpeed,
        swingPlaneAngle = metadata.swingPlaneAngle,
        clubType = metadata.clubType
    )
}

private fun TrajectoryEntity.toSwingTrajectory(): SwingTrajectory {
    val points = Json.decodeFromString<List<ClubHeadPoint>>(pointsJson)
    return SwingTrajectory(
        id = id,
        recordedAt = recordedAt,
        points = points,
        videoPath = videoPath,
        metadata = SwingMetadata(
            duration = duration,
            maxSpeed = maxSpeed,
            impactSpeed = impactSpeed,
            swingPlaneAngle = swingPlaneAngle,
            clubType = clubType
        )
    )
}
