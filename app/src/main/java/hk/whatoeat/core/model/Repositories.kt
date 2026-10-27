package hk.whatoeat.core.model

import kotlinx.coroutines.flow.StateFlow
interface RestaurantRepository {
    suspend fun cuisines(): List<Cuisine>
    suspend fun snapshot(filters: Filters,location: LocationFix?): CandidateSnapshot
    suspend fun prepare(request: DrawRequest): DrawSession
    suspend fun detail(id: String,before: String?=null,beforeId: String?=null): Detail
    suspend fun edit(id: String,version: Long,patch: EditPatch)
    suspend fun rate(id: String,stars: Int)
    suspend fun comment(id: String,text: String,requestId: String)
    suspend fun deleteComment(id: String)
    suspend fun revisions(id: String,beforeVersion: Long?=null): List<Revision>
    suspend fun restore(id: String,revisionId: Long,version: Long)
    suspend fun report(restaurantId: String?,commentId: String?,reason: String)
    suspend fun admin(): Boolean
    suspend fun reports(before: String?=null): List<Report>
    suspend fun resolve(reportId: String,action: String)
    suspend fun nickname(value: String)
    suspend fun profile(): String?
}
interface AuthRepository {
    val user: StateFlow<User?>
    suspend fun restore()
    suspend fun begin(provider: Provider,pending: PendingAction?): String?
    suspend fun callback(uri: String): PendingAction?
    suspend fun cancel()
    suspend fun pending(): PendingAction?
    suspend fun clearPending()
    suspend fun token(): String?
    suspend fun logout()
    suspend fun deleteAccount()
    suspend fun cacheNickname(value: String)
}
interface DrawStore {
    suspend fun read(): SavedDraw?
    suspend fun save(value: SavedDraw)
    suspend fun clear()
    suspend fun clearLocalData()
    suspend fun legacyCount(): Int
}
