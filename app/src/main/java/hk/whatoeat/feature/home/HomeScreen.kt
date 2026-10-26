package hk.whatoeat.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hk.whatoeat.R
import hk.whatoeat.core.model.*
import hk.whatoeat.core.designsystem.*
import hk.whatoeat.feature.result.AllergyWarning
import hk.whatoeat.feature.result.ProviderAttribution
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable fun HomeScreen(s: HomeState,vm: HomeViewModel,summary: String,onFilters: ()->Unit,onLocation: ()->Unit,onDetail: (String)->Unit) {
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {
            Text(stringResource(R.string.tagline),style=MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.LocationOn,null)
                    Text(if(s.fix==null) stringResource(R.string.district_center,stringResource(s.district.labelId())) else stringResource(R.string.my_location),style=MaterialTheme.typography.titleLarge)
                    s.fix?.let { fix ->
                        if(fix.approximate) Text(stringResource(R.string.approximate_location))
                        Text(stringResource(R.string.location_metadata,DateFormat.getDateTimeInstance().format(Date(fix.timestamp)),NumberFormat.getIntegerInstance().format(fix.accuracy)))
                    }
                    Text(stringResource(R.string.choose_district),style=MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { District.entries.forEach { d ->
                        FilterChip(selected=s.fix==null && s.district==d,onClick={vm.selectDistrict(d)},enabled=!s.locked,label={Text(stringResource(d.labelId()))})
                    } }
                    OutlinedButton(onClick=onLocation,enabled=!s.locked) { Icon(Icons.Outlined.MyLocation,null);Spacer(Modifier.width(8.dp));Text(stringResource(R.string.use_location)) }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text(stringResource(R.string.filters),style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
                TextButton(onClick=onFilters,enabled=!s.locked){Text(stringResource(R.string.filters))}
            }
            Text(summary)
            AvoidanceSummary(s.preferences.filters)
            if(s.preferences.filters.avoid.isNotEmpty()) AllergyWarning()
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { Presentation.entries.forEach { mode ->
                FilterChip(selected=s.preferences.mode==mode,onClick={vm.setPresentation(mode)},enabled=!s.locked,
                    label={Text(stringResource(if(mode==Presentation.Direct) R.string.direct else R.string.wheel))})
            } }
            Button(onClick={vm.draw(summary)},enabled=s.ready && !s.locked && !s.searching && (!s.searched || s.filtered.eligible.isNotEmpty()),modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {
                Icon(Icons.Outlined.Casino,null);Spacer(Modifier.width(8.dp))
                Text(stringResource(if(s.searched) R.string.pick else R.string.find_nearby))
            }
            if(s.searching) { LinearProgressIndicator(Modifier.fillMaxWidth());Text(stringResource(R.string.searching)) }
        }
        if(s.searched) {
            item {
                Text(stringResource(R.string.found_count,s.candidates.size,s.filtered.eligible.size),style=MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.bounded_search),style=MaterialTheme.typography.bodySmall)
                TextButton(onClick=vm::search,enabled=!s.locked) { Text(stringResource(R.string.refresh)) }
                if(s.filtered.eligible.size-s.drawn.size==1) Text(stringResource(R.string.one_remaining))
                if(s.candidates.isEmpty()) Text(stringResource(R.string.no_restaurants))
                else if(s.filtered.eligible.isEmpty()) Text(stringResource(if(s.preferences.filters.avoid.isNotEmpty() && s.preferences.filters.allergyMode==AllergyMode.Conservative) R.string.no_conservative else R.string.no_matches))
                s.filtered.counts.forEach { (reason,count) -> Text(stringResource(R.string.exclusion_count,stringResource(reason.labelId()),count),style=MaterialTheme.typography.bodySmall) }
                Text(stringResource(R.string.candidates),style=MaterialTheme.typography.titleLarge)
            }
            items(s.filtered.eligible,key={it.key}) { r ->
                OutlinedCard(onClick={onDetail(r.key)},modifier=Modifier.fillMaxWidth(),enabled=!s.locked) {
                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text(r.name ?: stringResource(R.string.saved_placeholder),style=MaterialTheme.typography.titleMedium)
                        r.point?.let { Text(stringResource(R.string.distance,NumberFormat.getIntegerInstance().format(s.center.distanceTo(it)))) }
                        Text(r.cuisines.map { stringResource(it.labelId()) }.joinToString().ifBlank { stringResource(R.string.unknown) })
                        if(r.key in s.filtered.unknownAllergens) Text(stringResource(R.string.allergy_caution))
                        ProviderAttribution(r)
                    }
                }
            }
        }
    }
}
@Composable fun AvoidanceSummary(f: Filters) {
    Text(if(f.avoid.isEmpty()) stringResource(R.string.no_avoidance) else stringResource(R.string.avoidance_summary,
        f.avoid.map { stringResource(it.labelId()) }.joinToString(),stringResource(if(f.allergyMode==AllergyMode.Warning) R.string.warning_mode else R.string.conservative_mode)),style=MaterialTheme.typography.bodyMedium)
}
