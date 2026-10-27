package hk.whatoeat.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import hk.whatoeat.core.model.*
import hk.whatoeat.data.backend.Codec
import kotlinx.coroutines.flow.map

// Version-1 tables are retained; old provider IDs are never relabelled as Supabase IDs.
@Entity(tableName="favorites") data class FavoriteEntity(@PrimaryKey val key: String,val createdAt: Long)
@Entity(tableName="history") data class HistoryEntity(@PrimaryKey val drawId: String,val key: String,val revealedAt: Long,val mode: String,val summary: String)
@Entity(tableName="round_state") data class RoundEntity(@PrimaryKey val slot: Int=1,val payload: String)
@Entity(tableName="recommendation_state") data class RecommendationEntity(@PrimaryKey val slot: Int=1,val payload: String)
@Dao interface EatDao {
    @Query("SELECT * FROM recommendation_state WHERE slot=1") suspend fun pending(): RecommendationEntity?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun savePending(row: RecommendationEntity)
    @Query("DELETE FROM recommendation_state") suspend fun clearPending()
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC, `key`") fun favorites(): kotlinx.coroutines.flow.Flow<List<FavoriteEntity>>
    @Query("SELECT * FROM favorites WHERE `key`=:key") suspend fun favorite(key: String): FavoriteEntity?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun putFavorite(row: FavoriteEntity)
    @Query("DELETE FROM favorites WHERE `key`=:key") suspend fun deleteFavorite(key: String)
    @Query("SELECT * FROM history ORDER BY revealedAt DESC, drawId DESC") fun history(): kotlinx.coroutines.flow.Flow<List<HistoryEntity>>
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insertHistory(row: HistoryEntity)
    @Query("DELETE FROM history WHERE drawId=:id") suspend fun deleteHistory(id: String)
    @Query("DELETE FROM history") suspend fun clearHistory()
    @Query("DELETE FROM history WHERE drawId NOT IN (SELECT drawId FROM history ORDER BY revealedAt DESC,drawId DESC LIMIT 200)") suspend fun trimHistory()
    @Query("SELECT (SELECT COUNT(*) FROM favorites WHERE `key` LIKE 'google:%')+(SELECT COUNT(*) FROM history WHERE `key` LIKE 'google:%')") suspend fun legacyCount(): Int
    @Query("DELETE FROM favorites") suspend fun clearFavorites()
    @Query("DELETE FROM round_state") suspend fun clearOldRound()
}
@Database(entities=[FavoriteEntity::class,HistoryEntity::class,RoundEntity::class,RecommendationEntity::class],version=2,exportSchema=true)
abstract class EatDatabase: RoomDatabase() {
    abstract fun dao(): EatDao
    companion object {
        val MIGRATION_1_2=object: Migration(1,2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `recommendation_state` (`slot` INTEGER NOT NULL, `payload` TEXT NOT NULL, PRIMARY KEY(`slot`))")
            }
        }
    }
}
class LocalStore(private val db: EatDatabase): DrawStore {
    private val dao=db.dao()
    val favorites=dao.favorites().map {rows -> rows.map {SavedFavorite(it.key,it.createdAt)}}
    val history=dao.history().map {rows -> rows.map {DrawRecord(it.drawId,it.key,it.revealedAt,if(it.mode=="Direct") Presentation.Direct else Presentation.Wheel,it.summary)}}
    override suspend fun read()=dao.pending()?.let {Codec.saved(it.payload)}
    override suspend fun save(value: SavedDraw)=db.withTransaction {
        dao.savePending(RecommendationEntity(payload=Codec.saved(value)))
        value.session?.takeIf {it.revealed}?.let {s ->
            dao.insertHistory(HistoryEntity(s.drawId,"catalog:${s.restaurant.id}",System.currentTimeMillis(),if(value.request.mode==DrawMode.SameCuisine) "Direct" else "Wheel",s.snapshot.cuisines.firstOrNull {it.cuisine.id==s.cuisineId}?.cuisine?.label.orEmpty()+" · "+s.restaurant.name))
            dao.trimHistory()
        }
        Unit
    }
    override suspend fun clear()=dao.clearPending()
    override suspend fun clearLocalData()=db.withTransaction {dao.clearPending();dao.clearOldRound();dao.clearHistory();dao.clearFavorites()}
    override suspend fun legacyCount()=dao.legacyCount()
    suspend fun toggle(key: String,now: Long)=db.withTransaction {if(dao.favorite(key)==null) dao.putFavorite(FavoriteEntity(key,now)) else dao.deleteFavorite(key)}
    suspend fun delete(id: String)=dao.deleteHistory(id)
    suspend fun clearHistory()=dao.clearHistory()
}
