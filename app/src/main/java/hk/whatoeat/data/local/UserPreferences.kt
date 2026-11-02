package hk.whatoeat.data.local
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import hk.whatoeat.core.model.*
import kotlinx.coroutines.flow.first
private val Context.eatPreferences by preferencesDataStore("eat_preferences")
class UserPreferences(context: Context) {
    private val store=context.eatPreferences
    suspend fun read(): Filters {val p=store.data.first();return Filters(p[intPreferencesKey("community_radius")],p[stringSetPreferencesKey("community_cuisines")] ?: emptySet(),p[doublePreferencesKey("catalog_min_rating")]?.takeIf {it in RatingOptions.values},RegionOptions.migrate(p[stringSetPreferencesKey("catalog_regions")] ?: emptySet())).normalized()}
    suspend fun save(value: Filters) {val f=value.normalized();store.edit {p -> if(f.radius==null) p.remove(intPreferencesKey("community_radius")) else p[intPreferencesKey("community_radius")]=f.radius;p[stringSetPreferencesKey("community_cuisines")]=f.cuisines;if(f.minRating==null) p.remove(doublePreferencesKey("catalog_min_rating")) else p[doublePreferencesKey("catalog_min_rating")]=f.minRating;p[stringSetPreferencesKey("catalog_regions")]=f.regions}}
    suspend fun clear() {store.edit {it.clear()}}
}
