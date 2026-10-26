package hk.whatoeat.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hk.whatoeat.core.model.*
import hk.whatoeat.data.location.StaleFix
import hk.whatoeat.domain.draw.*
import hk.whatoeat.domain.filter.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class HomeState(
    val ready: Boolean = false, val preferences: AppPreferences = AppPreferences(),
    val district: District = District.MongKok, val fix: LocationFix? = null, val staleFix: LocationFix? = null,
    val candidates: List<Restaurant> = emptyList(), val filtered: FilterResult = FilterResult(emptyList(),emptyMap(),emptySet()),
    val searched: Boolean = false, val searching: Boolean = false, val busy: Boolean = false,
    val failure: Failure? = null, val session: DrawSession? = null, val wheelCandidates: List<Restaurant> = emptyList(),
    val restoringSession: Boolean = false,
    val restoredResult: Boolean = false,
    val result: Restaurant? = null, val detail: Restaurant? = null, val detailLoading: Boolean = false,
    val favorites: List<SavedFavorite> = emptyList(), val history: List<DrawRecord> = emptyList(),
    val needsAcknowledgment: Boolean = false, val exhausted: Boolean = false,
    val drawn: Set<String> = emptySet()
) {
    val center: Point get() = fix?.point ?: district.point
    val locked: Boolean get() = busy || session != null
}
@HiltViewModel class HomeViewModel @Inject constructor(
    private val restaurants: RestaurantRepository, private val locations: LocationRepository,
    private val favorites: FavoritesRepository, private val history: HistoryRepository,
    private val preferences: PreferencesRepository, private val allergens: AllergenInfoRepository,
    private val rounds: RoundStore, private val coordinator: DrawCoordinator,
    private val time: TimeSource, private val ids: IdSource, private val io: CoroutineDispatcher
) : ViewModel() {
    private val mutable = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = mutable.asStateFlow()
    private val filter = CandidateFilter()
    private var searchJob: Job? = null
    private var searchRequest = 0L
    private var candidateVersion = ""
    private var ackSummary = ""
    private var revealJob: Job? = null
    init {
        viewModelScope.launch {
            var recovery: DrawSession? = null
            try {
                val p = preferences.preferences.first()
                val round = withContext(io) { rounds.read() }
                recovery = round.session
                candidateVersion = round.version
                mutable.update { it.copy(ready=true,preferences=p,drawn=round.drawn,busy=round.session!=null) }
                round.session?.let { restored ->
                    // Process recreation never animates or reselects a prepared winner.
                    val revealed = withContext(io) { coordinator.reveal(restored.drawId) }
                    mutable.update { it.copy(session=revealed.session,drawn=revealed.drawn,busy=false,restoredResult=true) }
                    loadResult(restored.winnerId)
                }
            } catch(e: Exception) { fail(e,Failure.Persistence); mutable.update { it.copy(ready=true,busy=false,session=recovery,restoringSession=recovery!=null,restoredResult=recovery!=null) } }
        }
        viewModelScope.launch { favorites.favorites.catch { mutable.update { it.copy(failure=Failure.Persistence) } }.collect { list -> mutable.update { it.copy(favorites=list) } } }
        viewModelScope.launch { history.history.catch { mutable.update { it.copy(failure=Failure.Persistence) } }.collect { list -> mutable.update { it.copy(history=list) } } }
    }
    private fun refilter(s: HomeState): HomeState = s.copy(filtered=filter.apply(s.candidates,s.center,s.preferences.filters,time.now()))
    private fun fail(e: Exception, fallback: Failure = Failure.Network) {
        if(e is CancellationException) throw e
        mutable.update { it.copy(failure=(e as? AppFailure)?.kind ?: fallback) }
    }
    fun clearFailure() { mutable.update { it.copy(failure=null) } }
    fun permissionDenied(permanent: Boolean) { mutable.update { it.copy(failure=if(permanent) Failure.PermissionPermanent else Failure.Permission) } }
    fun selectDistrict(d: District) {
        if(state.value.locked) return
        mutable.update { it.copy(district=d,fix=null,staleFix=null) }
        if(state.value.searched || state.value.searching) search()
    }
    fun useLocation() {
        if(state.value.locked) return
        mutable.update { it.copy(busy=true,failure=null) }
        viewModelScope.launch {
            try {
                val fix = withContext(io) { locations.locate() }
                mutable.update { it.copy(fix=fix,staleFix=null,busy=false) }; search()
            } catch(e: StaleFix) { mutable.update { it.copy(staleFix=e.fix,busy=false,failure=Failure.StaleLocation) } }
            catch(e: Exception) { fail(e); mutable.update { it.copy(busy=false) } }
        }
    }
    fun acceptStaleLocation() {
        if(state.value.locked) return
        val fix = state.value.staleFix ?: return
        mutable.update { it.copy(fix=fix,staleFix=null,failure=null) }; search()
    }
    fun search() {
        if(state.value.locked || !state.value.ready) return
        val version = ++searchRequest
        searchJob?.cancel()
        val center = state.value.center
        val radius = state.value.preferences.filters.radius
        mutable.update { it.copy(searching=true,failure=null,candidates=emptyList(),searched=false) }
        searchJob = viewModelScope.launch {
            try {
                // No automatic retry: explicit refresh keeps costs and quota behavior visible.
                val results = withContext(io) { restaurants.nearby(center,radius).map { it.copy(evidence=allergens.evidence(it.key)) } }
                if(version != searchRequest) return@launch
                val newVersion = ids.next()
                withContext(io) { coordinator.restart(newVersion) }
                if(version != searchRequest) return@launch
                candidateVersion = newVersion
                mutable.update { refilter(it.copy(candidates=results,searching=false,searched=true,drawn=emptySet())) }
            } catch(e: Exception) {
                if(version == searchRequest) { fail(e); mutable.update { it.copy(searching=false) } }
            }
        }
    }
    fun applyFilters(f: Filters) {
        if(state.value.locked || f == state.value.preferences.filters) return
        val previous = state.value.preferences.filters
        val shouldSearch = previous.radius != f.radius && (state.value.searched || state.value.searching)
        if(shouldSearch) { ++searchRequest; searchJob?.cancel() }
        val p = state.value.preferences.copy(filters=f)
        mutable.update { it.copy(busy=true) }
        viewModelScope.launch {
            try {
                val newVersion = ids.next()
                withContext(io) { coordinator.restart(newVersion); preferences.save(p) }
                candidateVersion = newVersion
                mutable.update { refilter(it.copy(preferences=p,drawn=emptySet(),busy=false)) }
                if(shouldSearch) search()
            } catch(e: Exception) { fail(e,Failure.Persistence); mutable.update { it.copy(busy=false) } }
        }
    }
    fun setPresentation(mode: Presentation) = savePreferences(state.value.preferences.copy(mode=mode))
    fun setReducedMotion(value: Boolean) = savePreferences(state.value.preferences.copy(reducedMotion=value))
    private fun savePreferences(p: AppPreferences) {
        if(state.value.locked) return
        mutable.update { it.copy(busy=true) }
        viewModelScope.launch { try {
            withContext(io) { preferences.save(p) }; mutable.update { it.copy(preferences=p) }
        } catch(e: Exception) { fail(e,Failure.Persistence) } finally { mutable.update { it.copy(busy=false) } } }
    }
    fun draw(summary: String) {
        val s = state.value
        if(s.busy || s.session != null || s.searching) return
        if(!s.searched) { search(); return }
        if(s.filtered.unknownAllergens.isNotEmpty() && s.preferences.acknowledged != s.preferences.filters.acknowledgmentKey()) {
            ackSummary=summary; mutable.update { it.copy(needsAcknowledgment=true) }; return
        }
        mutable.update { it.copy(busy=true,failure=null) }
        viewModelScope.launch {
            try {
                val outcome = withContext(io) { coordinator.prepare(s.filtered.eligible.map { it.key },candidateVersion,s.preferences.mode,summary) }
                when(outcome) {
                    DrawOutcome.Empty -> Unit
                    DrawOutcome.Exhausted -> mutable.update { it.copy(exhausted=true) }
                    is DrawOutcome.Selected -> {
                        mutable.update { it.copy(session=outcome.session,restoredResult=false,wheelCandidates=s.filtered.eligible.filter { r -> r.key in outcome.session.orderedCandidateIds }.sortedBy { r -> r.key }) }
                        if(outcome.session.presentationMode == Presentation.Direct || s.preferences.reducedMotion || outcome.session.orderedCandidateIds.size == 1) reveal()
                    }
                }
            } catch(e: Exception) { fail(e,Failure.Persistence) }
            finally { mutable.update { it.copy(busy=false) } }
        }
    }
    fun acknowledgment(accept: Boolean) {
        mutable.update { it.copy(needsAcknowledgment=false) }
        if(!accept) return
        val p = state.value.preferences.copy(acknowledged=state.value.preferences.filters.acknowledgmentKey())
        mutable.update { it.copy(busy=true) }
        viewModelScope.launch { try {
            withContext(io) { preferences.save(p) }; mutable.update { it.copy(preferences=p,busy=false) }; draw(ackSummary)
        } catch(e: Exception) { fail(e,Failure.Persistence); mutable.update { it.copy(busy=false) } } }
    }
    fun reveal() {
        val s = state.value.session ?: return
        if(revealJob?.isActive == true || (s.status == SessionStatus.Revealed && state.value.result != null)) return
        revealJob = viewModelScope.launch {
            try {
                val r = withContext(io) { coordinator.reveal(s.drawId) }
                mutable.update { it.copy(session=r.session,drawn=r.drawn,restoringSession=false) }
                loadResult(s.winnerId)
            } catch(e: Exception) { fail(e,Failure.Persistence) }
        }
    }
    private suspend fun loadResult(key: String) {
        state.value.candidates.firstOrNull { it.key == key }?.let { r -> mutable.update { if(it.session?.winnerId==key) it.copy(result=r) else it }; return }
        try { val r=withContext(io) { restaurants.details(key).let { it.copy(evidence=allergens.evidence(key)) } }; mutable.update { if(it.session?.winnerId==key) it.copy(result=r) else it } }
        catch(e: Exception) { fail(e,Failure.DetailsUnavailable) }
    }
    fun dismissResult(thenDraw: Boolean = false, summary: String = "") {
        if(state.value.busy) return
        mutable.update { it.copy(busy=true) }
        viewModelScope.launch { try {
            state.value.session?.let { withContext(io) { coordinator.reveal(it.drawId) } }
            withContext(io) { coordinator.dismiss() }
            mutable.update { it.copy(session=null,result=null,wheelCandidates=emptyList(),busy=false) }
            if(thenDraw) draw(summary)
        } catch(e: Exception) { fail(e,Failure.Persistence); mutable.update { it.copy(busy=false) } } }
    }
    fun restartRound(confirmed: Boolean) {
        mutable.update { it.copy(exhausted=false) }
        if(!confirmed) return
        viewModelScope.launch { try { withContext(io) { coordinator.restart(candidateVersion) }; mutable.update { it.copy(drawn=emptySet()) } } catch(e: Exception) { fail(e,Failure.Persistence) } }
    }
    fun toggleFavorite(key: String) { viewModelScope.launch { try { withContext(io) { favorites.toggle(key,time.now()) } } catch(e: Exception) { fail(e,Failure.Persistence) } } }
    fun deleteRecord(id: String) { viewModelScope.launch { try { withContext(io) { history.delete(id) } } catch(e: Exception) { fail(e,Failure.Persistence) } } }
    fun clearHistory() { viewModelScope.launch { try { withContext(io) { history.clearHistory() } } catch(e: Exception) { fail(e,Failure.Persistence) } } }
    fun clearLocalData() {
        if(state.value.locked) return
        mutable.update { it.copy(busy=true) }
        viewModelScope.launch { try {
            withContext(io) { rounds.clearAll(); preferences.clear() }
            candidateVersion=""; mutable.update { HomeState(ready=true) }
        } catch(e: Exception) { fail(e,Failure.Persistence); mutable.update { it.copy(busy=false) } } }
    }
    private var detailRequest=0
    fun openDetail(key: String) {
        val request=++detailRequest
        mutable.update { it.copy(detail=null,detailLoading=true,failure=null) }
        viewModelScope.launch { try {
            val r=withContext(io) { restaurants.details(key).let { it.copy(evidence=allergens.evidence(key)) } }
            if(request==detailRequest) mutable.update { it.copy(detail=r,detailLoading=false) }
        } catch(e: Exception) { if(request==detailRequest) { fail(e,Failure.DetailsUnavailable); mutable.update { it.copy(detailLoading=false) } } } }
    }
}
