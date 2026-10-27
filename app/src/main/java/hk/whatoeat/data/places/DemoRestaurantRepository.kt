package hk.whatoeat.data.places

import hk.whatoeat.core.model.*
import hk.whatoeat.domain.filter.CandidateFilter
import hk.whatoeat.domain.draw.DrawEngine
import hk.whatoeat.data.backend.Codec
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** Explicitly fictional local demo; never substitutes for missing live configuration. */
class DemoRestaurantRepository(private val auth: AuthRepository,private val time: TimeSource,random: RandomSource): RestaurantRepository {
    private val chinese=Cuisine("chinese","中式");private val japanese=Cuisine("japanese","日式");private val noodles=Cuisine("noodles","粉麵")
    private val lock=Mutex();private val engine=DrawEngine(random)
    private var generation=1L
    private val rows=mutableListOf(
        Restaurant("demo-1","示範茶餐廳（虛構）","九龍示範商場 2 樓 A1 舖",Point(22.3193,114.1694),listOf(chinese,noodles),null,Business.Unknown,source="虛構示範資料",updatedAt="2026-10-27T00:00:00Z",version=1),
        Restaurant("demo-2","示範拉麵店（虛構）","港島示範商場地下 B2 舖",Point(22.2800,114.1840),listOf(japanese,noodles),"星期一至日 11:00–21:00",Business.Operating,source="虛構示範資料",updatedAt="2026-10-27T00:00:00Z",version=1),
        Restaurant("demo-3","示範小館（虛構）","新界示範中心 1 樓 C3 舖",Point(22.3814,114.1889),listOf(chinese),null,Business.Unknown,source="虛構示範資料",updatedAt="2026-10-27T00:00:00Z",version=1),
        Restaurant("demo-unclassified","未分類示範店（虛構）","示範地址",null,emptyList(),null,Business.Unknown,source="虛構示範資料",updatedAt="2026-10-27T00:00:00Z",version=1),
        Restaurant("demo-closed","已結業示範店（虛構）","示範地址",Point(22.3193,114.1694),listOf(japanese),null,Business.Closed,source="虛構示範資料",updatedAt="2026-10-27T00:00:00Z",version=1)
    )
    private data class Cached(val snapshot: CandidateSnapshot,val rows: List<Restaurant>,val generation: Long)
    private val snapshots=mutableMapOf<String,Cached>();private val draws=mutableMapOf<String,DrawSession>()
    private val stars=mutableMapOf<Pair<String,String>,Int>()
    private val reviews=mutableMapOf<String,MutableList<Pair<String,Review>>>()
    private val revisions=mutableMapOf<String,MutableList<Revision>>()
    private val beforeRows=mutableMapOf<Long,Restaurant>()
    private val events=mutableMapOf<Pair<String,String>,MutableList<Long>>()
    init {
        if(auth is hk.whatoeat.data.auth.DemoAuthRepository) auth.eraseData={id -> lock.withLock {
            stars.keys.removeAll {it.second==id};reviews.values.forEach {it.removeAll {p -> p.first==id}}
            revisions.values.forEach {list -> list.replaceAll {it.copy(nickname="已刪除帳戶")}}
        }}
    }
    private fun writer(action: String,limit: Int=20): User {
        val user=auth.user.value ?: throw AppFailure(Failure.Login)
        val list=events.getOrPut(user.id to action) {mutableListOf()};list.removeAll {time.now()-it>3600000}
        if(list.size>=limit) throw AppFailure(Failure.RateLimited);list+=time.now();return user
    }
    override suspend fun cuisines()=rows.flatMap {it.cuisines}.distinctBy {it.id}.sortedBy {it.id}
    override suspend fun snapshot(filters: Filters,location: LocationFix?)=lock.withLock {
        val result=CandidateFilter().apply(rows,filters,location,time.now())
        val s=CandidateSnapshot(UUID.randomUUID().toString(),UUID.randomUUID().toString(),time.now()+600000,CandidateFilter().cuisines(result.eligible,filters),result.eligible.size,result.unclassified)
        snapshots.entries.removeAll {it.value.snapshot.expiresAt<time.now()}
        snapshots[s.id]=Cached(s,result.eligible,generation);s
    }
    override suspend fun prepare(request: DrawRequest)=lock.withLock {
        draws[request.id]?.let {return@withLock it}
        val cached=snapshots[request.snapshot.id] ?: throw AppFailure(Failure.SnapshotExpired)
        if(cached.snapshot.token!=request.snapshot.token || cached.snapshot.expiresAt<time.now() || cached.generation!=generation) throw AppFailure(Failure.SnapshotExpired)
        val pick=engine.prepare(request,cached.rows) ?: throw AppFailure(Failure.DetailsUnavailable)
        draws[request.id]=pick;pick
    }
    private fun current(id: String)=rows.firstOrNull {it.id==id && it.enabled} ?: throw AppFailure(Failure.DetailsUnavailable)
    override suspend fun detail(id: String,before: String?,beforeId: String?): Detail=lock.withLock {
        val r=current(id);val votes=stars.filterKeys {it.first==id}.values
        val all=reviews[id].orEmpty().map {(_,v) -> v}.sortedWith(compareByDescending<Review> {it.createdAt}.thenByDescending {it.id})
        val page=all.filter {before==null || it.createdAt<before || (it.createdAt==before && it.id<beforeId.orEmpty())}.take(10)
        Detail(r.copy(averageRating=votes.takeIf {it.isNotEmpty()}?.average(),ratingCount=votes.size),page.map {it.copy(own=reviews[id].orEmpty().first {pair -> pair.second.id==it.id}.first==auth.user.value?.id)},auth.user.value?.let {stars[id to it.id]},page.lastOrNull()?.createdAt.takeIf {page.size==10},page.lastOrNull()?.id.takeIf {page.size==10})
    }
    override suspend fun edit(id: String,version: Long,patch: EditPatch)=lock.withLock {
        val user=writer("edit");if(!Validation.restaurant(patch) || patch.cuisineIds.any {it !in rows.flatMap {r -> r.cuisines}.map {c -> c.id}}) throw AppFailure(Failure.InvalidInput)
        val old=current(id);if(old.version!=version) throw AppFailure(Failure.Conflict)
        val next=old.copy(name=patch.name.trim(),address=patch.address.trim(),point=patch.point,cuisines=rows.flatMap {it.cuisines}.distinctBy {it.id}.filter {it.id in patch.cuisineIds},openingHours=patch.openingHours,status=patch.status,source="示範用家修改",version=old.version+1,updatedAt=java.time.Instant.ofEpochMilli(time.now()).toString())
        val rev=Revision((revisions.values.sumOf {it.size}+1).toLong(),next.version,user.nickname,next.updatedAt,Codec.restaurant(old).toString(2),Codec.restaurant(next).toString(2))
        beforeRows[rev.id]=old;revisions.getOrPut(id) {mutableListOf()}.add(rev);rows[rows.indexOf(old)]=next;generation++;Unit
    }
    override suspend fun rate(id: String,stars: Int)=lock.withLock {val user=writer("rating",60);current(id);if(stars !in 1..5) throw AppFailure(Failure.InvalidInput);this.stars[id to user.id]=stars;Unit}
    override suspend fun comment(id: String,text: String,requestId: String)=lock.withLock {
        val user=writer("comment",10);current(id);if(!Validation.review(text)) throw AppFailure(Failure.InvalidInput)
        val list=reviews.getOrPut(id) {mutableListOf()};if(list.none {it.second.id==requestId}) list+=user.id to Review(requestId,user.nickname,text.trim(),java.time.Instant.ofEpochMilli(time.now()).toString(),true)
    }
    override suspend fun deleteComment(id: String)=lock.withLock {val user=writer("comment");reviews.values.forEach {it.removeAll {p -> p.first==user.id && p.second.id==id}}}
    override suspend fun revisions(id: String,beforeVersion: Long?)=revisions[id].orEmpty().filter {beforeVersion==null || it.version<beforeVersion}.sortedByDescending {it.version}.take(10)
    override suspend fun restore(id: String,revisionId: Long,version: Long) {val old=beforeRows[revisionId]?.takeIf {it.id==id} ?: throw AppFailure(Failure.DetailsUnavailable);edit(id,version,EditPatch(old.name,old.address,old.point,old.cuisines.map {it.id}.toSet(),old.openingHours,old.status))}
    override suspend fun report(restaurantId: String?,commentId: String?,reason: String) {writer("report",10);if(reason.trim().length !in 1..1000) throw AppFailure(Failure.InvalidInput)}
    override suspend fun admin()=false
    override suspend fun reports(before: String?): List<Report> = throw AppFailure(Failure.Forbidden)
    override suspend fun resolve(reportId: String,action: String): Unit=throw AppFailure(Failure.Forbidden)
    override suspend fun nickname(value: String) {writer("profile");if(!Validation.nickname(value)) throw AppFailure(Failure.InvalidInput)}
    override suspend fun profile()=auth.user.value?.nickname
}
