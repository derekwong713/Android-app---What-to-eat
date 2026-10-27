package hk.whatoeat.domain.filter
import hk.whatoeat.core.model.*

data class FilterResult(val eligible: List<Restaurant>,val unclassified: Int)
class CandidateFilter {
    fun apply(rows: List<Restaurant>,filters: Filters,fix: LocationFix?,now: Long): FilterResult {
        if(filters.radius!=null && (fix==null || !fix.point.usable || now-fix.timestamp !in 0..DistanceOptions.MAX_FIX_AGE)) throw AppFailure(Failure.LocationUnavailable)
        val inRange=rows.distinctBy {it.id}.filter {r -> r.recommendable && (filters.radius==null || r.point?.takeIf {it.usable}?.let {fix!!.point.distanceTo(it)<=filters.radius}==true)}
        return FilterResult(inRange.filter {r -> r.cuisines.isNotEmpty() && (filters.cuisines.isEmpty() || r.cuisines.any {it.id in filters.cuisines})},inRange.count {it.cuisines.isEmpty()})
    }
    fun cuisines(rows: List<Restaurant>,filters: Filters): List<CuisineCount> = rows.flatMap {r -> r.cuisines.distinctBy {it.id}.filter {filters.cuisines.isEmpty() || it.id in filters.cuisines}.map {it to r.id}}.groupBy {it.first.id}.toSortedMap().values.map {pairs -> CuisineCount(pairs.first().first,pairs.map {it.second}.distinct().size)}
}
