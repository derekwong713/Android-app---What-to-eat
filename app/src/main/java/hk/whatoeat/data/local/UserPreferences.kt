package hk.whatoeat.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import hk.whatoeat.core.model.*
import kotlinx.coroutines.flow.map

private val Context.eatPreferences by preferencesDataStore("eat_preferences")
class UserPreferences(context: Context) : PreferencesRepository {
    private val store = context.eatPreferences
    private val radius = intPreferencesKey("radius")
    private val cuisines = stringSetPreferencesKey("cuisines")
    private val prices = stringSetPreferencesKey("prices")
    private val avoid = stringSetPreferencesKey("avoid")
    private val unknown = booleanPreferencesKey("unknown_price")
    private val open = booleanPreferencesKey("open")
    private val conservative = booleanPreferencesKey("conservative")
    private val reduced = booleanPreferencesKey("reduced_motion")
    private val wheel = booleanPreferencesKey("wheel")
    private val ack = stringPreferencesKey("ack")
    override val preferences = store.data.map { p -> AppPreferences(
        Filters(p[radius] ?: 1000, Cuisine.entries.filter { it.name in (p[cuisines] ?: emptySet()) }.toSet(),
            Price.entries.filter { it.name in (p[prices] ?: emptySet()) }.toSet(),p[unknown] ?: false,p[open] ?: false,
            Allergen.entries.filter { it.name in (p[avoid] ?: emptySet()) }.toSet(),
            if(p[conservative] == true) AllergyMode.Conservative else AllergyMode.Warning),
        p[reduced] ?: false, if(p[wheel] == true) Presentation.Wheel else Presentation.Direct,p[ack]) }
    override suspend fun save(value: AppPreferences) { store.edit { p ->
        val f = value.filters
        p[radius]=f.radius; p[cuisines]=f.cuisines.map { it.name }.toSet(); p[prices]=f.prices.map { it.name }.toSet()
        p[avoid]=f.avoid.map { it.name }.toSet(); p[unknown]=f.includeUnknownPrice; p[open]=f.openOnly
        p[conservative]=f.allergyMode == AllergyMode.Conservative; p[reduced]=value.reducedMotion
        p[wheel]=value.mode == Presentation.Wheel
        value.acknowledged?.let { p[ack]=it } ?: p.remove(ack)
    } }
    override suspend fun clear() { store.edit { it.clear() } }
}
