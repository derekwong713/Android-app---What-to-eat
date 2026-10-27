package hk.whatoeat.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hk.whatoeat.core.model.*
import hk.whatoeat.data.local.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class HomeState(
    val ready: Boolean=false,val busy: Boolean=false,val filters: Filters=Filters(),val cuisines: List<Cuisine> = emptyList(),
    val fix: LocationFix?=null,val snapshot: CandidateSnapshot?=null,val session: DrawSession?=null,
    val recovery: Boolean=false,val restored: Boolean=false,val failure: Failure?=null,val notice: Boolean=false,
    val detail: Detail?=null,val detailLoading: Boolean=false,val detailId: String?=null,
    val user: User?=null,val login: Boolean=false,val authUrl: String?=null,val authPending: Boolean=false,
    val action: PendingAction?=null,val revisions: List<Revision> = emptyList(),val reports: List<Report> = emptyList(),val admin: Boolean=false,
    val favorites: List<SavedFavorite> = emptyList(),val history: List<DrawRecord> = emptyList(),val legacyCount: Int=0
) {val locked get()=!ready || busy || (session!=null && !session.revealed)}
@HiltViewModel class HomeViewModel @Inject constructor(
    private val restaurants: RestaurantRepository,private val locations: LocationRepository,private val auth: AuthRepository,
    private val store: LocalStore,private val prefs: UserPreferences,private val time: TimeSource,private val ids: IdSource,
    private val io: CoroutineDispatcher
): ViewModel() {
    private val mutable=MutableStateFlow(HomeState());val state=mutable.asStateFlow()
    private val mutex=Mutex();private var unsaved: DrawSession?=null;private var lastProfile: String?=null
    private var detailVersion=0L
    private val mutableRoute=MutableSharedFlow<String>(extraBufferCapacity=8);val routes=mutableRoute.asSharedFlow()
    init {
        viewModelScope.launch {
            try {
                val filters=withContext(io) {prefs.read()}
                val saved=withContext(io) {store.read()}
                withContext(io) {auth.restore()}
                mutable.update {it.copy(filters=filters,session=saved?.session,snapshot=saved?.request?.snapshot,recovery=saved!=null && saved.session==null,restored=saved!=null)}
                saved?.session?.let {pick ->
                    val revealed=pick.copy(revealed=true)
                    withContext(io) {store.save(SavedDraw(saved.request,revealed))}
                    mutable.update {it.copy(session=revealed)};mutableRoute.tryEmit("result");loadDetail(pick.restaurant.id)
                }
                val cuisines=withContext(io) {restaurants.cuisines()}
                val legacy=withContext(io) {store.legacyCount()}
                mutable.update {it.copy(cuisines=cuisines,ready=true,legacyCount=legacy)}
            } catch(e: Exception) {failure(e);mutable.update {it.copy(ready=true)}}
        }
        viewModelScope.launch {store.favorites.collect {rows -> mutable.update {it.copy(favorites=rows)}}}
        viewModelScope.launch {store.history.collect {rows -> mutable.update {it.copy(history=rows)}}}
        viewModelScope.launch {auth.user.collect {user ->
            mutable.update {it.copy(user=user,admin=if(user==null) false else it.admin)}
            if(user?.id!=lastProfile) {
                lastProfile=user?.id
                if(user!=null) runCatching {
                    val nickname=withContext(io) {restaurants.profile()}
                    if(nickname!=null && nickname!=user.nickname) withContext(io) {auth.cacheNickname(nickname)}
                    val admin=withContext(io) {restaurants.admin()};mutable.update {it.copy(admin=admin)}
                }
            }
        }}
    }
    private fun failure(e: Exception) {
        if(e is CancellationException) throw e
        mutable.update {it.copy(failure=(e as? AppFailure)?.kind ?: Failure.Persistence)}
    }
    fun invalidInput() {mutable.update {it.copy(failure=Failure.InvalidInput)}}
    fun navigationFailed() {mutable.update {it.copy(failure=Failure.Navigation)}}
    fun clearFailure() {mutable.update {it.copy(failure=null,notice=false)}}
    fun permissionDenied(permanent: Boolean) {mutable.update {it.copy(failure=if(permanent) Failure.PermissionPermanent else Failure.Permission)}}
    fun useLocation() {
        if(state.value.locked) return
        mutable.update {it.copy(busy=true,failure=null,fix=null,snapshot=null,recovery=false)}
        unsaved=null
        viewModelScope.launch {try {
            withContext(io) {store.clear()}
            val fix=withContext(io) {locations.locate()}
            if(time.now()-fix.timestamp !in 0..DistanceOptions.MAX_FIX_AGE) throw AppFailure(Failure.StaleLocation)
            mutable.update {it.copy(fix=fix,snapshot=null)}
            withContext(io) {store.clear()}
        } catch(e: Exception) {failure(e)} finally {mutable.update {it.copy(busy=false)}}}
    }
    fun applyFilters(filters: Filters) {
        if(state.value.locked || filters==state.value.filters) return
        mutable.update {it.copy(busy=true)}
        viewModelScope.launch {try {
            withContext(io) {prefs.save(filters);store.clear()};unsaved=null
            mutable.update {it.copy(filters=filters,snapshot=null,session=null,recovery=false,restored=false)}
            mutableRoute.tryEmit("home")
        } catch(e: Exception) {failure(e)} finally {mutable.update {it.copy(busy=false)}}}
    }
    fun noDistance()=applyFilters(state.value.filters.copy(radius=null))
    private fun validFix(s: HomeState): LocationFix? {
        if(s.filters.radius==null) return null
        return s.fix?.takeIf {it.point.usable && time.now()-it.timestamp in 0..DistanceOptions.MAX_FIX_AGE} ?: throw AppFailure(Failure.LocationUnavailable)
    }
    fun refresh() {
        if(state.value.locked) return
        mutable.update {it.copy(busy=true,failure=null)}
        viewModelScope.launch {try {
            val s=state.value;val snap=withContext(io) {restaurants.snapshot(s.filters,validFix(s))}
            withContext(io) {store.clear()};unsaved=null
            mutable.update {it.copy(snapshot=snap,recovery=false)}
        } catch(e: Exception) {failure(e)} finally {mutable.update {it.copy(busy=false)}}}
    }
    fun draw(mode: DrawMode=DrawMode.NewCuisine) {
        if(state.value.locked) return
        mutable.update {it.copy(busy=true,failure=null,notice=false)}
        viewModelScope.launch {try {mutex.withLock {
            var s=state.value
            val pending=withContext(io) {store.read()}?.takeIf {it.session==null}
            val request=if(pending!=null) pending.request else {
                val currentFix=validFix(s)
                val snap=s.snapshot ?: withContext(io) {restaurants.snapshot(s.filters,currentFix)}.also {snapshot -> mutable.update {it.copy(snapshot=snapshot)}}
                if(snap.cuisines.isEmpty()) return@withLock
                if(mode==DrawMode.SameCuisine && s.session?.cuisineId !in snap.cuisines.map {it.cuisine.id}) throw AppFailure(Failure.DetailsUnavailable)
                DrawRequest(ids.next(),snap,mode,s.session?.cuisineId,s.session?.restaurant?.id).also {withContext(io) {store.save(SavedDraw(it,null))}}
            }
            val pick=unsaved?.takeIf {it.drawId==request.id} ?: withContext(io) {restaurants.prepare(request)}
            unsaved=pick
            withContext(io) {store.save(SavedDraw(request,pick))};unsaved=null
            mutable.update {it.copy(session=pick,snapshot=request.snapshot,recovery=false,restored=false)}
            if(request.mode==DrawMode.SameCuisine || pick.snapshot.cuisines.size==1) revealInternal()
            else mutableRoute.tryEmit("wheel")
        }} catch(e: Exception) {
            failure(e)
            val pending=runCatching {withContext(io) {store.read()}}.getOrNull()
            mutable.update {it.copy(recovery=pending!=null && pending.session==null)}
        } finally {mutable.update {it.copy(busy=false)}}}
    }
    private suspend fun revealInternal() {
        val saved=withContext(io) {store.read()} ?: return
        val pick=saved.session ?: return
        val revealed=pick.copy(revealed=true)
        withContext(io) {store.save(saved.copy(session=revealed))}
        mutable.update {it.copy(session=revealed)};mutableRoute.tryEmit("result");loadDetail(pick.restaurant.id)
    }
    fun reveal() {viewModelScope.launch {try {mutex.withLock {revealInternal()}} catch(e: Exception) {failure(e)}}}
    fun discardPending() {
        if(state.value.busy) return
        mutable.update {it.copy(busy=true)}
        viewModelScope.launch {try {mutex.withLock {withContext(io) {store.clear()};unsaved=null;mutable.update {it.copy(recovery=false,session=null,snapshot=null)}}} catch(e: Exception) {failure(e)} finally {mutable.update {it.copy(busy=false)}}}
    }
    fun openDetail(key: String) {
        if(key.startsWith("google:")) {mutable.update {it.copy(failure=Failure.DetailsUnavailable)};return}
        val id=key.removePrefix("catalog:")
        mutableRoute.tryEmit("detail");viewModelScope.launch {loadDetail(id)}
    }
    private suspend fun loadDetail(id: String) {
        val version=++detailVersion
        mutable.update {it.copy(detail=null,detailId=id,detailLoading=true)}
        try {val detail=withContext(io) {restaurants.detail(id)};if(version==detailVersion) mutable.update {it.copy(detail=detail,detailLoading=false)}}
        catch(e: Exception) {if(version==detailVersion) {failure(e);mutable.update {it.copy(detailLoading=false)}}}
    }
    fun retryDetail() {state.value.detailId?.let {id -> viewModelScope.launch {loadDetail(id)}}}
    fun moreReviews() {
        val d=state.value.detail ?: return;val before=d.nextReviewTime ?: return
        operation {val page=restaurants.detail(d.restaurant.id,before,d.nextReviewId);mutable.update {it.copy(detail=page.copy(reviews=(d.reviews+page.reviews).distinctBy {r -> r.id}))}}
    }
    fun requestAction(kind: String,id: String=state.value.detailId.orEmpty()) {
        val action=PendingAction(kind,id)
        if(auth.user.value==null && kind!="history") mutable.update {it.copy(login=true,action=action)} else openAction(action)
    }
    private fun openAction(action: PendingAction) {
        mutable.update {it.copy(action=action,login=false)}
        if(action.kind=="history") operation {val rows=restaurants.revisions(action.restaurantId);mutable.update {it.copy(revisions=rows)}}
        if(action.kind=="admin") operation {val rows=restaurants.reports();mutable.update {it.copy(reports=rows)}}
    }
    fun closeAction() {mutable.update {it.copy(action=null,login=false)}}
    fun signIn(provider: Provider) {
        if(state.value.busy) return
        mutable.update {it.copy(busy=true)}
        viewModelScope.launch {try {
            val url=withContext(io) {auth.begin(provider,state.value.action)}
            if(url==null) resumePending() else mutable.update {it.copy(authUrl=url,authPending=true)}
        } catch(e: Exception) {failure(e)} finally {mutable.update {it.copy(busy=false)}}}
    }
    fun authLaunched() {mutable.update {it.copy(authUrl=null)}}
    fun authReturnedWithoutCallback() {if(state.value.authPending) mutable.update {it.copy(authPending=false,login=true,failure=Failure.Cancelled)}}
    fun authCallback(uri: String) {
        mutable.update {it.copy(busy=true,authPending=false,authUrl=null)}
        viewModelScope.launch {try {withContext(io) {auth.callback(uri)};resumePending()} catch(e: Exception) {failure(e)} finally {mutable.update {it.copy(busy=false)}}}
    }
    private suspend fun resumePending() {
        val pending=withContext(io) {auth.pending()};withContext(io) {auth.clearPending()}
        mutable.update {it.copy(login=false,user=auth.user.value,authPending=false)}
        if(pending!=null) {if(pending.restaurantId.isNotBlank()) {loadDetail(pending.restaurantId);mutableRoute.tryEmit("detail")};openAction(pending)}
    }
    fun cancelLogin() {viewModelScope.launch {withContext(io) {auth.cancel()};mutable.update {it.copy(login=false,authPending=false,action=null,authUrl=null)}}}
    private fun operation(block: suspend ()->Unit) {
        if(state.value.busy) return
        mutable.update {it.copy(busy=true,failure=null)}
        viewModelScope.launch {try {withContext(io) {block()}} catch(e: Exception) {failure(e);if((e as? AppFailure)?.kind==Failure.Login) mutable.update {it.copy(login=true)}} finally {mutable.update {it.copy(busy=false)}}}
    }
    fun edit(id: String,version: Long,patch: EditPatch) {
        if(!Validation.restaurant(patch)) {mutable.update {it.copy(failure=Failure.InvalidInput)};return}
        operation {restaurants.edit(id,version,patch);mutable.update {it.copy(action=null,snapshot=null,notice=true)};loadDetail(id)}
    }
    fun rate(id: String,stars: Int)=operation {restaurants.rate(id,stars);mutable.update {it.copy(action=null,notice=true)};loadDetail(id)}
    fun comment(id: String,body: String,key: String) {
        if(!Validation.review(body)) {mutable.update {it.copy(failure=Failure.InvalidInput)};return}
        operation {restaurants.comment(id,body,key);mutable.update {it.copy(action=null,notice=true)};loadDetail(id)}
    }
    fun deleteComment(id: String)=operation {restaurants.deleteComment(id);state.value.detailId?.let {loadDetail(it)}}
    fun report(restaurantId: String?,commentId: String?,reason: String)=operation {restaurants.report(restaurantId,commentId,reason);mutable.update {it.copy(action=null,notice=true)}}
    fun restoreRevision(id: String,revisionId: Long,version: Long)=operation {restaurants.restore(id,revisionId,version);mutable.update {it.copy(action=null,snapshot=null,notice=true)};loadDetail(id)}
    fun moreRevisions() {val id=state.value.action?.restaurantId ?: return;val cursor=state.value.revisions.lastOrNull()?.version ?: return;operation {val page=restaurants.revisions(id,cursor);mutable.update {it.copy(revisions=(it.revisions+page).distinctBy {r -> r.id})}}}
    fun resolve(id: String,action: String)=operation {restaurants.resolve(id,action);val rows=restaurants.reports();mutable.update {it.copy(reports=rows,snapshot=null,notice=true)}}
    fun nickname(value: String) {if(!Validation.nickname(value)) {mutable.update {it.copy(failure=Failure.InvalidInput)};return};operation {restaurants.nickname(value);auth.cacheNickname(value);mutable.update {it.copy(action=null,notice=true)}}}
    fun logout()=operation {auth.logout();mutable.update {it.copy(user=null,admin=false,action=null)}}
    fun deleteAccount()=operation {auth.deleteAccount();store.clearLocalData();prefs.clear();mutable.update {HomeState(ready=true,cuisines=it.cuisines,notice=true)}}
    fun toggleFavorite(key: String)=operation {store.toggle(if(key.startsWith("catalog:") || key.startsWith("google:")) key else "catalog:$key",time.now())}
    fun deleteRecord(id: String)=operation {store.delete(id)}
    fun clearHistory()=operation {store.clearHistory()}
    fun clearLocalData()=operation {store.clearLocalData();prefs.clear();auth.logout();unsaved=null;mutable.update {HomeState(ready=true,cuisines=it.cuisines,notice=true)}}
}
