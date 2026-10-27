package hk.whatoeat.data.local
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import hk.whatoeat.core.model.Filters
import kotlinx.coroutines.flow.first
private val Context.eatPreferences by preferencesDataStore("eat_preferences")
class UserPreferences(context: Context) {
    private val store=context.eatPreferences
    suspend fun read(): Filters {val p=store.data.first();return Filters(p[intPreferencesKey("community_radius")],p[stringSetPreferencesKey("community_cuisines")] ?: emptySet())}
    suspend fun save(f: Filters) {store.edit {p -> if(f.radius==null) p.remove(intPreferencesKey("community_radius")) else p[intPreferencesKey("community_radius")]=f.radius;p[stringSetPreferencesKey("community_cuisines")]=f.cuisines}}
    suspend fun clear() {store.edit {it.clear()}}
}
