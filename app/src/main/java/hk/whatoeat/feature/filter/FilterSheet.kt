package hk.whatoeat.feature.filter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hk.whatoeat.R
import hk.whatoeat.core.designsystem.*
import hk.whatoeat.core.model.*

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun FilterSheet(filters: Filters,onDismiss: ()->Unit,onApply: (Filters)->Unit) {
    var draft by remember { mutableStateOf(filters) }
    var clearAllergies by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).navigationBarsPadding(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.filters),style=MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.radius))
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf(500,1000,2000,5000).forEach { r ->
                FilterChip(selected=draft.radius==r,onClick={draft=draft.copy(radius=r)},label={Text(stringResource(R.string.radius_m,r))})
            } }
            Text(stringResource(R.string.cuisine))
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { Cuisine.entries.forEach { c ->
                FilterChip(selected=c in draft.cuisines,onClick={draft=draft.copy(cuisines=toggle(draft.cuisines,c))},label={Text(stringResource(c.labelId()))})
            } }
            Text(stringResource(R.string.price))
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { Price.entries.filterNot { it==Price.Unknown }.forEach { p ->
                FilterChip(selected=p in draft.prices,onClick={draft=draft.copy(prices=toggle(draft.prices,p))},label={Text(stringResource(p.labelId()))})
            } }
            ToggleRow(stringResource(R.string.include_unknown_price),draft.includeUnknownPrice) { draft=draft.copy(includeUnknownPrice=it) }
            ToggleRow(stringResource(R.string.open_only),draft.openOnly) { draft=draft.copy(openOnly=it) }
            HorizontalDivider()
            Text(stringResource(R.string.avoidance),style=MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.avoidance_explanation))
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { Allergen.entries.forEach { a ->
                FilterChip(selected=a in draft.avoid,onClick={draft=draft.copy(avoid=toggle(draft.avoid,a))},label={Text(stringResource(a.labelId()))})
            } }
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { AllergyMode.entries.forEach { m ->
                FilterChip(selected=draft.allergyMode==m,onClick={draft=draft.copy(allergyMode=m)},label={Text(stringResource(if(m==AllergyMode.Warning) R.string.warning_mode else R.string.conservative_mode))})
            } }
            Text(stringResource(R.string.allergy_warning))
            TextButton(onClick={clearAllergies=true}) { Text(stringResource(R.string.clear_allergies)) }
            FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                TextButton(onClick={draft=draft.resetGeneral()}) { Text(stringResource(R.string.reset)) }
                TextButton(onClick=onDismiss) { Text(stringResource(R.string.cancel)) }
            }
            Button(onClick={onApply(draft);onDismiss()},modifier=Modifier.fillMaxWidth()) { Text(stringResource(R.string.apply)) }
            Spacer(Modifier.height(24.dp))
        }
    }
    if(clearAllergies) AlertDialog(onDismissRequest={clearAllergies=false},title={Text(stringResource(R.string.clear_allergies))},
        text={Text(stringResource(R.string.confirm_clear_allergies))},confirmButton={TextButton(onClick={draft=draft.copy(avoid=emptySet());clearAllergies=false}){Text(stringResource(R.string.confirm))}},
        dismissButton={TextButton(onClick={clearAllergies=false}){Text(stringResource(R.string.cancel))}})
}
private fun <T> toggle(set: Set<T>,value: T): Set<T> = if(value in set) set-value else set+value
@Composable fun ToggleRow(label: String,checked: Boolean,onChange: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=56.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(label,Modifier.weight(1f).padding(top=12.dp)); Switch(checked=checked,onCheckedChange=onChange)
    }
}
