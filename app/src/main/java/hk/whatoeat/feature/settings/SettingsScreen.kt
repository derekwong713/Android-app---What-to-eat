package hk.whatoeat.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hk.whatoeat.BuildConfig
import hk.whatoeat.R
import hk.whatoeat.feature.filter.ToggleRow
import hk.whatoeat.feature.home.*
import hk.whatoeat.feature.result.ExternalActions

@Composable fun SettingsScreen(s: HomeState,vm: HomeViewModel,onAllergies: ()->Unit) {
    val context=LocalContext.current
    var confirm by remember { mutableStateOf(false) }
    var linkError by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.settings),style=MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.app_info,BuildConfig.VERSION_NAME,stringResource(if(BuildConfig.DEMO) R.string.demo_indicator else R.string.live_indicator)))
        OutlinedButton(onClick=onAllergies,enabled=!s.locked){Text(stringResource(R.string.avoidance))}
        ToggleRow(stringResource(R.string.reduced_motion),s.preferences.reducedMotion,vm::setReducedMotion)
        Text(stringResource(R.string.privacy),style=MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.local_privacy))
        if(BuildConfig.PRIVACY_POLICY_URL.isBlank() || BuildConfig.TERMS_URL.isBlank()) Text(stringResource(R.string.production_urls_missing))
        if(BuildConfig.PRIVACY_POLICY_URL.isNotBlank()) TextButton(onClick={linkError=!ExternalActions.open(context,BuildConfig.PRIVACY_POLICY_URL)}){Text(stringResource(R.string.privacy))}
        if(BuildConfig.TERMS_URL.isNotBlank()) TextButton(onClick={linkError=!ExternalActions.open(context,BuildConfig.TERMS_URL)}){Text(stringResource(R.string.terms))}
        Text(stringResource(R.string.attribution),style=MaterialTheme.typography.titleLarge)
        if(!BuildConfig.DEMO) {
            Text(stringResource(R.string.live_allergen_limit))
            TextButton(onClick={linkError=!ExternalActions.open(context,"https://cloud.google.com/maps-platform/terms/")}){Text(stringResource(R.string.google_terms))}
            TextButton(onClick={linkError=!ExternalActions.open(context,"https://policies.google.com/privacy")}){Text(stringResource(R.string.google_privacy))}
        }
        TextButton(onClick={confirm=true},enabled=!s.locked){Text(stringResource(R.string.clear_local_data))}
    }
    if(confirm) AlertDialog(onDismissRequest={confirm=false},text={Text(stringResource(R.string.confirm_clear_local))},
        confirmButton={TextButton(onClick={vm.clearLocalData();confirm=false}){Text(stringResource(R.string.confirm))}},dismissButton={TextButton(onClick={confirm=false}){Text(stringResource(R.string.cancel))}})
    if(linkError) AlertDialog(onDismissRequest={linkError=false},text={Text(stringResource(R.string.error_navigation))},confirmButton={TextButton(onClick={linkError=false}){Text(stringResource(R.string.close))}})
}
