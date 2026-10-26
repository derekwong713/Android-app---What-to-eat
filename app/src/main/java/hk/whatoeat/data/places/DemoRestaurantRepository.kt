package hk.whatoeat.data.places

import android.content.Context
import hk.whatoeat.R
import hk.whatoeat.core.model.*

/** Entirely fictional, generated around bundled district centers, never calls Places. */
class DemoRestaurantRepository(private val context: Context, private val time: TimeSource) : RestaurantRepository, AllergenInfoRepository {
    private fun fixtures(): List<Restaurant> = District.entries.flatMapIndexed { districtIndex, district ->
        // Tsuen Wan intentionally returns zero; Sha Tin has exactly one usable candidate.
        val count = when(district) { District.TsuenWan -> 0; District.ShaTin -> 1; District.CausewayBay -> 20; else -> 12 }
        (0 until count).map { i ->
            val now = time.now()
            Restaurant("demo","${district.name}-$i", context.getString(names[i % names.size],i+1),
                if(i == 10) null else Point(district.point.latitude + (i % 5)*0.001, district.point.longitude + (i / 5)*0.001),
                if(i == 9) emptySet() else setOf(Cuisine.entries[i % Cuisine.entries.size]),
                Price.entries[i % Price.entries.size], if(i == 8) null else 3.5 + (i % 4)*0.3,
                if(i == 8) null else 20+i*13,
                if(i == 11) Business.TemporarilyClosed else Business.Operational,
                Opening.entries[i % 3],context.getString(R.string.demo_address,districtIndex+1,i+1),
                fetchedAt=now, evidence=when(i % 5) {
                    0 -> listOf(Evidence(Allergen.Peanuts,EvidenceStatus.KnownContains,context.getString(R.string.demo_source),now,context.getString(R.string.demo_scope),true))
                    1 -> listOf(Evidence(Allergen.Peanuts,EvidenceStatus.CrossContactRisk,context.getString(R.string.demo_source),now,context.getString(R.string.demo_scope),true))
                    2 -> Allergen.entries.map { Evidence(it,EvidenceStatus.SourceReportedAccommodation,context.getString(R.string.demo_source),now,context.getString(R.string.demo_scope),true) }
                    3 -> listOf(Evidence(Allergen.Milk,EvidenceStatus.SourceReportedAccommodation,context.getString(R.string.demo_source),now,context.getString(R.string.demo_dish),false))
                    else -> emptyList()
                })
        }
    }
    override suspend fun nearby(center: Point, radius: Int): List<Restaurant> = fixtures().filter { it.point?.let { p -> center.distanceTo(p) <= radius } ?: true }
        .filter { it.point != null || center.distanceTo(District.MongKok.point) < 5000 }.take(20)
    override suspend fun details(key: String): Restaurant = fixtures().firstOrNull { it.key == key } ?: throw AppFailure(Failure.DetailsUnavailable)
    override suspend fun evidence(key: String) = details(key).evidence
    private val names = listOf(R.string.demo_name_1,R.string.demo_name_2,R.string.demo_name_3,R.string.demo_name_4)
}
