package ngo.xnet.aiope.feature.remote.db

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "remote_servers")
data class RemoteServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val host: String,
    val port: Int = 22,
    val user: String = "root",
    val bootstrapPort: Int = 2222,
    val privateKey: String? = null,
    val publicKey: String? = null,
    val password: String? = null,
    val osType: String = "linux",
    val status: String = "offline", // "online", "offline", "connecting"
    val lastSeen: Long = 0,
    val osInfo: String? = null,
    val daemonVersion: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface RemoteDao {
    @Query("SELECT * FROM remote_servers ORDER BY createdAt DESC")
    fun getAllServers(): Flow<List<RemoteServerEntity>>

    @Query("SELECT * FROM remote_servers WHERE id = :id LIMIT 1")
    suspend fun getServer(id: String): RemoteServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: RemoteServerEntity)

    @Delete
    suspend fun deleteServer(server: RemoteServerEntity)
}

@Database(entities = [RemoteServerEntity::class], version = 1, exportSchema = false)
abstract class RemoteDatabase : RoomDatabase() {
    abstract fun remoteDao(): RemoteDao

    companion object {
        @Volatile
        private var INSTANCE: RemoteDatabase? = null

        fun getInstance(context: Context): RemoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RemoteDatabase::class.java,
                    "remote_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
