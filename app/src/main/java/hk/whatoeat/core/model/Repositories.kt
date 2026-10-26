package hk.whatoeat.core.model

import kotlinx.coroutines.flow.Flow

interface RestaurantRepository {
    suspend fun nearby(center: Point, radius: Int): List<Restaurant>
    suspend fun details(key: String): Restaurant
}
interface LocationRepository { suspend fun locate(): LocationFix }
interface FavoritesRepository {
    val favorites: Flow<List<SavedFavorite>>
    suspend fun toggle(key: String, now: Long)
}
interface HistoryRepository {
    val history: Flow<List<DrawRecord>>
    suspend fun delete(drawId: String)
    suspend fun clearHistory()
}
interface PreferencesRepository {
    val preferences: Flow<AppPreferences>
    suspend fun save(value: AppPreferences)
    suspend fun clear()
}
interface AllergenInfoRepository { suspend fun evidence(key: String): List<Evidence> }
fun interface RandomSource { fun nextInt(bound: Int): Int }
fun interface TimeSource { fun now(): Long }
fun interface IdSource { fun next(): String }
interface RoundStore {
    suspend fun read(): Round
    suspend fun save(round: Round)
    /** Atomically marks revealed, inserts history once, trims history and records drawn ID. */
    suspend fun reveal(drawId: String, now: Long): Round
    suspend fun clearAll()
}
