package hk.whatoeat.data.local

import androidx.room.*
import hk.whatoeat.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "favorites") data class FavoriteEntity(@PrimaryKey val key: String, val createdAt: Long)
@Entity(tableName = "history") data class HistoryEntity(@PrimaryKey val drawId: String, val key: String,
    val revealedAt: Long, val mode: String, val summary: String)
@Entity(tableName = "round_state") data class RoundEntity(@PrimaryKey val slot: Int = 1, val payload: String)
@Dao interface EatDao {
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC, `key`") fun favorites(): Flow<List<FavoriteEntity>>
    @Query("SELECT * FROM favorites WHERE `key` = :key") suspend fun favorite(key: String): FavoriteEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putFavorite(value: FavoriteEntity)
    @Query("DELETE FROM favorites WHERE `key` = :key") suspend fun deleteFavorite(key: String)
    @Query("DELETE FROM favorites") suspend fun clearFavorites()
    @Query("SELECT * FROM history ORDER BY revealedAt DESC, drawId DESC") fun history(): Flow<List<HistoryEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertHistory(value: HistoryEntity): Long
    @Query("DELETE FROM history WHERE drawId = :id") suspend fun deleteHistory(id: String)
    @Query("DELETE FROM history") suspend fun clearHistory()
    @Query("DELETE FROM history WHERE drawId NOT IN (SELECT drawId FROM history ORDER BY revealedAt DESC, drawId DESC LIMIT 200)") suspend fun trimHistory()
    @Query("SELECT * FROM round_state WHERE slot = 1") suspend fun round(): RoundEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRound(value: RoundEntity)
    @Query("DELETE FROM round_state") suspend fun clearRound()
}
@Database(entities = [FavoriteEntity::class, HistoryEntity::class, RoundEntity::class], version = 1, exportSchema = true)
abstract class EatDatabase : RoomDatabase() { abstract fun dao(): EatDao }

object StateCodec {
    private fun JSONArray.strings() = (0 until length()).map { getString(it) }
    fun encode(r: Round): String = JSONObject().apply {
        put("version",r.version); put("drawn",JSONArray(r.drawn.toList()))
        r.session?.let { s -> put("session",JSONObject().apply {
            put("id",s.drawId); put("ids",JSONArray(s.orderedCandidateIds)); put("winner",s.winnerId)
            put("index",s.winnerIndex); put("version",s.candidateVersion); put("mode",s.presentationMode.name)
            put("created",s.createdAt); put("status",s.status.name); put("summary",s.filterSummary)
        }) }
    }.toString()
    fun decode(raw: String): Round {
        val j = JSONObject(raw)
        val s = j.optJSONObject("session")?.let {
            DrawSession(it.getString("id"), it.getJSONArray("ids").strings(), it.getString("winner"),it.getInt("index"),
                it.getString("version"),Presentation.valueOf(it.getString("mode")),it.getLong("created"),
                SessionStatus.valueOf(it.getString("status")),it.getString("summary"))
        }
        return Round(j.getString("version"),j.getJSONArray("drawn").strings().toSet(),s)
    }
}
class LocalStore(private val db: EatDatabase) : FavoritesRepository, HistoryRepository, RoundStore {
    private val dao = db.dao()
    override val favorites = dao.favorites().map { list -> list.map { SavedFavorite(it.key,it.createdAt) } }
    override val history = dao.history().map { list -> list.map { DrawRecord(it.drawId,it.key,it.revealedAt,Presentation.valueOf(it.mode),it.summary) } }
    override suspend fun toggle(key: String, now: Long) = db.withTransaction {
        if (dao.favorite(key) == null) dao.putFavorite(FavoriteEntity(key,now)) else dao.deleteFavorite(key)
    }
    override suspend fun delete(drawId: String) = dao.deleteHistory(drawId)
    override suspend fun clearHistory() = dao.clearHistory()
    override suspend fun read(): Round = dao.round()?.let { StateCodec.decode(it.payload) } ?: Round()
    override suspend fun save(round: Round) = dao.putRound(RoundEntity(payload = StateCodec.encode(round)))
    override suspend fun reveal(drawId: String, now: Long): Round = db.withTransaction {
        val r = read()
        val s = r.session ?: return@withTransaction r
        if (s.drawId != drawId || s.status == SessionStatus.Revealed) return@withTransaction r
        dao.insertHistory(HistoryEntity(s.drawId,s.winnerId,now,s.presentationMode.name,s.filterSummary))
        dao.trimHistory()
        r.copy(drawn = r.drawn + s.winnerId,session = s.copy(status = SessionStatus.Revealed)).also { save(it) }
    }
    override suspend fun clearAll() = db.withTransaction { dao.clearFavorites(); dao.clearHistory(); dao.clearRound() }
}
