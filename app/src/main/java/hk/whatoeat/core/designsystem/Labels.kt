package hk.whatoeat.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import hk.whatoeat.R
import hk.whatoeat.core.model.*
import hk.whatoeat.domain.filter.Exclusion

fun Cuisine.labelId(): Int = when(this) {
    Cuisine.Cantonese->R.string.cuisine_cantonese; Cuisine.Chinese->R.string.cuisine_chinese
    Cuisine.Japanese->R.string.cuisine_japanese; Cuisine.Korean->R.string.cuisine_korean
    Cuisine.Western->R.string.cuisine_western; Cuisine.Thai->R.string.cuisine_thai
    Cuisine.Vegetarian->R.string.cuisine_vegetarian; Cuisine.Cafe->R.string.cuisine_cafe
}
fun Price.labelId(): Int = when(this) { Price.Budget->R.string.price_budget; Price.Moderate->R.string.price_moderate; Price.Higher->R.string.price_higher; Price.Premium->R.string.price_premium; Price.Unknown->R.string.unknown }
fun Allergen.labelId(): Int = when(this) {
    Allergen.Peanuts->R.string.allergen_peanuts; Allergen.TreeNuts->R.string.allergen_tree_nuts
    Allergen.Milk->R.string.allergen_milk; Allergen.Eggs->R.string.allergen_eggs; Allergen.Fish->R.string.allergen_fish
    Allergen.Crustaceans->R.string.allergen_crustaceans; Allergen.Molluscs->R.string.allergen_molluscs
    Allergen.Soy->R.string.allergen_soy; Allergen.Wheat->R.string.allergen_wheat; Allergen.Sesame->R.string.allergen_sesame
}
fun District.labelId(): Int = when(this) { District.MongKok->R.string.district_mong_kok; District.CausewayBay->R.string.district_causeway_bay; District.Central->R.string.district_central; District.ShaTin->R.string.district_sha_tin; District.TsuenWan->R.string.district_tsuen_wan }
fun Failure.labelId(): Int = when(this) {
    Failure.Network->R.string.error_network; Failure.Permission->R.string.error_permission; Failure.PermissionPermanent->R.string.error_permission_permanent
    Failure.Configuration->R.string.error_configuration; Failure.QuotaService->R.string.error_service
    Failure.LocationDisabled->R.string.error_location_disabled; Failure.LocationUnavailable->R.string.error_location_unavailable
    Failure.LocationTimeout->R.string.error_location_timeout; Failure.StaleLocation->R.string.error_stale
    Failure.Persistence->R.string.error_persistence; Failure.DetailsUnavailable->R.string.error_details; Failure.Navigation->R.string.error_navigation
}
fun Exclusion.labelId(): Int = when(this) {
    Exclusion.Distance->R.string.exclusion_distance; Exclusion.MissingCoordinates->R.string.exclusion_coordinates
    Exclusion.Cuisine->R.string.cuisine; Exclusion.Price->R.string.price; Exclusion.Opening->R.string.open_only
    Exclusion.NotOperating->R.string.not_operating; Exclusion.AllergenConflict->R.string.exclusion_conflict; Exclusion.AllergenUnknown->R.string.exclusion_allergen_unknown
}
fun EvidenceStatus.labelId(): Int = when(this) { EvidenceStatus.KnownContains->R.string.evidence_contains; EvidenceStatus.CrossContactRisk->R.string.evidence_cross_contact; EvidenceStatus.Unknown->R.string.unknown; EvidenceStatus.SourceReportedAccommodation->R.string.evidence_accommodation }
@Composable fun filterSummary(f: Filters): String {
    val cuisine = if(f.cuisines.isEmpty()) stringResource(R.string.unrestricted) else f.cuisines.map { stringResource(it.labelId()) }.joinToString()
    val price = if(f.prices.isEmpty()) stringResource(R.string.unrestricted) else f.prices.map { stringResource(it.labelId()) }.joinToString()
    return stringResource(R.string.filter_summary,f.radius,cuisine,price,if(f.openOnly) stringResource(R.string.open_only) else stringResource(R.string.any_opening))
}
