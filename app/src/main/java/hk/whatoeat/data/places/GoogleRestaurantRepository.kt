package hk.whatoeat.data.places

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import com.google.android.libraries.places.api.net.PlacesStatusCodes
import hk.whatoeat.core.model.*
import hk.whatoeat.domain.filter.CuisineMapping
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class GoogleRestaurantRepository(context: Context, private val key: String, private val time: TimeSource) : RestaurantRepository {
    private val client by lazy {
        if (key.isBlank()) throw AppFailure(Failure.Configuration)
        if (!Places.isInitialized()) Places.initializeWithNewPlacesApiEnabled(context.applicationContext,key)
        Places.createClient(context.applicationContext)
    }
    private val fields = listOf(Place.Field.ID,Place.Field.DISPLAY_NAME,Place.Field.LOCATION,Place.Field.TYPES,
        Place.Field.PRICE_LEVEL,Place.Field.RATING,Place.Field.USER_RATING_COUNT,Place.Field.BUSINESS_STATUS,
        Place.Field.CURRENT_OPENING_HOURS,Place.Field.UTC_OFFSET,Place.Field.FORMATTED_ADDRESS)
    private val detailFields = fields + listOf(Place.Field.INTERNATIONAL_PHONE_NUMBER,Place.Field.WEBSITE_URI)
    override suspend fun nearby(center: Point, radius: Int): List<Restaurant> = request {
        val restriction = CircularBounds.newInstance(LatLng(center.latitude,center.longitude),radius.toDouble())
        val req = SearchNearbyRequest.builder(restriction,fields).setIncludedTypes(listOf("restaurant"))
            .setRankPreference(SearchNearbyRequest.RankPreference.DISTANCE).setMaxResultCount(20).build()
        client.searchNearby(req).await().places.map(::map).distinctBy { it.key }
    }
    override suspend fun details(key: String): Restaurant = request {
        if (!key.startsWith("google:")) throw AppFailure(Failure.DetailsUnavailable)
        map(client.fetchPlace(FetchPlaceRequest.newInstance(key.removePrefix("google:"),detailFields)).await().place)
    }
    private fun map(p: Place): Restaurant = Restaurant("google",requireNotNull(p.id),p.displayName,
        p.location?.let { Point(it.latitude,it.longitude) },CuisineMapping.from(p.placeTypes ?: emptyList()),
        CuisineMapping.price(p.priceLevel),p.rating,p.userRatingCount,when(p.businessStatus) {
            Place.BusinessStatus.OPERATIONAL -> Business.Operational
            Place.BusinessStatus.CLOSED_TEMPORARILY -> Business.TemporarilyClosed
            Place.BusinessStatus.CLOSED_PERMANENTLY -> Business.PermanentlyClosed
            else -> Business.Unknown
        },when(p.isOpen(time.now())) { true -> Opening.Open; false -> Opening.Closed; else -> Opening.Unknown },
        p.formattedAddress,p.internationalPhoneNumber,p.websiteUri?.toString(),time.now(),p.attributions ?: emptyList(),
        openingText=p.currentOpeningHours?.weekdayText ?: emptyList())
    private suspend fun <T> request(block: suspend () -> T): T = try { block() }
        catch(e: CancellationException) { throw e }
        catch(e: AppFailure) { throw e }
        catch(e: ApiException) { throw AppFailure(when(e.statusCode) {
            CommonStatusCodes.NETWORK_ERROR,CommonStatusCodes.TIMEOUT,CommonStatusCodes.API_NOT_CONNECTED -> Failure.Network
            PlacesStatusCodes.REQUEST_DENIED,PlacesStatusCodes.INVALID_REQUEST,CommonStatusCodes.DEVELOPER_ERROR -> Failure.Configuration
            PlacesStatusCodes.NOT_FOUND -> Failure.DetailsUnavailable
            PlacesStatusCodes.OVER_QUERY_LIMIT -> Failure.QuotaService
            else -> Failure.QuotaService
        }) }
        catch(e: Exception) { throw AppFailure(Failure.Network) }
}
class UnknownAllergens : AllergenInfoRepository { override suspend fun evidence(key: String): List<Evidence> = emptyList() }
