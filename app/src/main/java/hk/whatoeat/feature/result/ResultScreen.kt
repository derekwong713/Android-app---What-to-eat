package hk.whatoeat.feature.result

import android.widget.TextView
import android.text.method.LinkMovementMethod
import androidx.core.text.HtmlCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import hk.whatoeat.BuildConfig
import hk.whatoeat.R
import hk.whatoeat.core.model.*
import hk.whatoeat.core.designsystem.*
import hk.whatoeat.feature.home.*
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

@Composable fun ResultDialog(s: HomeState,vm: HomeViewModel,summary: String) {
    val session=s.session ?: return
    var showDetails by remember(session.drawId) { mutableStateOf(false) }
    Dialog(onDismissRequest={if(session.status==SessionStatus.Prepared) vm.reveal() else vm.dismissResult()},
        properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Text(stringResource(if(BuildConfig.DEMO) R.string.demo_indicator else R.string.live_indicator),style=MaterialTheme.typography.labelLarge)
                if(session.status==SessionStatus.Prepared) {
                    if(session.presentationMode==Presentation.Wheel && !s.restoringSession) Wheel(session,s.wheelCandidates,s.preferences.reducedMotion,vm::reveal)
                    else { CircularProgressIndicator();Button(onClick=vm::reveal){Text(stringResource(R.string.retry))} }
                } else {
                    Text(stringResource(R.string.result_title),style=MaterialTheme.typography.headlineLarge,modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
                    s.result?.let { r ->
                        RestaurantContent(if(showDetails && s.detail?.key==r.key) s.detail!! else r,s,vm,showDetails)
                        TextButton(onClick={showDetails=!showDetails;if(showDetails) vm.openDetail(r.key)}) { Text(stringResource(R.string.details)) }
                    } ?: Text(stringResource(R.string.error_details))
                    Button(onClick={vm.dismissResult(true,summary)},enabled=!s.busy,modifier=Modifier.fillMaxWidth()) { Text(stringResource(R.string.draw_again)) }
                    OutlinedButton(onClick={vm.dismissResult()},enabled=!s.busy,modifier=Modifier.fillMaxWidth()) { Text(stringResource(R.string.close)) }
                }
            }
        }
    }
}
@Composable fun AllergyWarning() {
    Surface(color=MaterialTheme.colorScheme.errorContainer,shape=MaterialTheme.shapes.medium) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.allergy_caution),style=MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.allergy_warning))
        }
    }
}
@Composable fun RestaurantContent(r: Restaurant,s: HomeState,vm: HomeViewModel,details: Boolean) {
    val context=LocalContext.current
    var navigationError by remember { mutableStateOf(false) }
    Text(r.name ?: stringResource(R.string.saved_placeholder),style=MaterialTheme.typography.headlineMedium,modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
    if(s.restoredResult && s.session?.winnerId==r.key) Text(stringResource(R.string.distance_unavailable))
    else r.point?.let { Text(stringResource(R.string.distance,NumberFormat.getIntegerInstance().format(s.center.distanceTo(it)))) }
    Text(r.cuisines.map { stringResource(it.labelId()) }.joinToString().ifBlank { stringResource(R.string.unknown) })
    Text(stringResource(r.price.labelId()))
    if(r.rating!=null && r.ratingCount!=null) Text(stringResource(R.string.rating,NumberFormat.getNumberInstance().format(r.rating),NumberFormat.getIntegerInstance().format(r.ratingCount)))
    Text(stringResource(if(r.business==Business.PermanentlyClosed || r.business==Business.TemporarilyClosed) R.string.not_operating else when(r.opening) { Opening.Open->R.string.opening_open;Opening.Closed->R.string.opening_closed;Opening.Unknown->R.string.opening_unknown }))
    if(s.preferences.filters.avoid.isNotEmpty()) {
        AvoidanceSummary(s.preferences.filters);AllergyWarning()
        if(!BuildConfig.DEMO) Text(stringResource(R.string.live_allergen_limit))
        r.evidence.filter { it.allergen in s.preferences.filters.avoid }.forEach { e ->
            Text(stringResource(R.string.source_evidence,stringResource(e.allergen.labelId()),stringResource(e.status.labelId()),e.source,
                DateFormat.getDateInstance().format(Date(e.updatedAt)),e.scope))
        }
    }
    if(BuildConfig.DEMO) Text(stringResource(R.string.navigation_demo),style=MaterialTheme.typography.bodySmall)
    Button(onClick={navigationError=!ExternalActions.navigate(context,r)},modifier=Modifier.fillMaxWidth()) { Text(stringResource(R.string.navigate)) }
    val favorite=s.favorites.any { it.key==r.key }
    OutlinedButton(onClick={vm.toggleFavorite(r.key)},modifier=Modifier.fillMaxWidth()) { Text(stringResource(if(favorite) R.string.favorite_remove else R.string.favorite_add)) }
    if(details) {
        r.address?.let { Text(it) }
        r.openingText.forEach { Text(it) }
        r.phone?.let { phone -> TextButton(onClick={navigationError=!ExternalActions.open(context,"tel:${UriEncode(phone)}")}) {Text(stringResource(R.string.phone))} }
        r.website?.let { url -> TextButton(onClick={navigationError=!ExternalActions.open(context,url)}) {Text(stringResource(R.string.website))} }
    }
    ProviderAttribution(r)
    if(navigationError) AlertDialog(onDismissRequest={navigationError=false},text={Text(stringResource(R.string.error_navigation))},confirmButton={TextButton(onClick={navigationError=false}){Text(stringResource(R.string.close))}})
}
private fun UriEncode(value: String) = android.net.Uri.encode(value)
@Composable fun ProviderAttribution(r: Restaurant) {
    if(r.provider=="google") {
        // Compact attribution line follows official text styling for space-constrained cards.
        Text(stringResource(R.string.google_maps),style=MaterialTheme.typography.bodyMedium)
        r.attributions.forEach { html ->
            val dark=androidx.compose.foundation.isSystemInDarkTheme()
            AndroidView(factory={ c -> TextView(c).apply { movementMethod=LinkMovementMethod.getInstance();textSize=14f } },
                update={ v -> v.text=HtmlCompat.fromHtml(html,HtmlCompat.FROM_HTML_MODE_COMPACT);v.setTextColor(if(dark) android.graphics.Color.WHITE else android.graphics.Color.rgb(31,31,31)) })
        }
    }
}
